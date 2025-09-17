package com.velox.jewelvault.ui.screen.inventory

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.ImportedItemRow
import com.velox.jewelvault.data.roomdb.dto.ImportRowStatus
import com.velox.jewelvault.data.roomdb.dto.ImportSummary
import com.velox.jewelvault.data.roomdb.dto.toItemEntity
import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.withIo
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.InputCorrectionUtils
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.to3FString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.CellType
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ImportItemsViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val dataStoreManager: DataStoreManager,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
    private val context: Context
) : ViewModel() {

    val snackBarState = _snackBarState
    val currentScreenHeadingState= _currentScreenHeadingState
    
    // State variables
    val isLoading = mutableStateOf(false)
    val importProgress = mutableStateOf(0f)
    val importProgressText = mutableStateOf("")
    
    val importedRows = mutableStateListOf<ImportedItemRow>()
    val importSummary = mutableStateOf(ImportSummary())
    
    val selectedRow = mutableStateOf<ImportedItemRow?>(null)
    val showMappingDialog = mutableStateOf(false)
    val showConfirmImportDialog = mutableStateOf(false)
    
    // Data from database
    val categories = mutableStateListOf<CategoryEntity>()
    val subCategories = mutableStateListOf<SubCategoryEntity>()
    
    // User mapping decisions
    val bulkCategoryMapping = mutableStateOf<String?>(null)
    val bulkSubCategoryMapping = mutableStateOf<String?>(null)
    
    init {
        loadDatabaseData()
    }
    
    fun updateItemCategory(row: ImportedItemRow, categoryId: String, subCategoryId: String) {
        val category = categories.find { it.catId == categoryId }
        val subCategory = subCategories.find { it.subCatId == subCategoryId }
        
        if (category != null && subCategory != null) {
            row.mappedCategoryId = categoryId
            row.categoryId = categoryId
            row.mappedSubCategoryId = subCategoryId
            row.subCategoryId = subCategoryId
            row.category = category.catName
            row.subCategory = subCategory.subCatName
            row.status = ImportRowStatus.VALID
            row.errorMessage = null
            updateImportSummary()
        }
    }
    
    // Remove an item from the import list
    fun removeItem(row: ImportedItemRow) {
        importedRows.remove(row)
        updateImportSummary()
        
        // Clear selection if the removed item was selected
        if (selectedRow.value == row) {
            selectedRow.value = null
        }
    }
    
    // Find similar categories and subcategories for better mapping suggestions
    fun findSimilarCategories(categoryName: String): List<CategoryEntity> {
        return categories.filter { category ->
            category.catName.contains(categoryName, ignoreCase = true) ||
            categoryName.contains(category.catName, ignoreCase = true)
        }
    }
    
    fun findSimilarSubCategories(subCategoryName: String, categoryId: String? = null): List<SubCategoryEntity> {
        return subCategories.filter { subCategory ->
            val nameMatches = subCategory.subCatName.contains(subCategoryName, ignoreCase = true) ||
                    subCategoryName.contains(subCategory.subCatName, ignoreCase = true)
            
            if (categoryId != null) {
                nameMatches && subCategory.catId == categoryId
            } else {
                nameMatches
            }
        }
    }
    
    // Suggest best mapping for a row based on similarity
    fun suggestMapping(row: ImportedItemRow): Pair<String?, String?> {
        val similarCategories = findSimilarCategories(row.category)
        val suggestedCategoryId = similarCategories.firstOrNull()?.catId
        
        val similarSubCategories = if (suggestedCategoryId != null) {
            findSimilarSubCategories(row.subCategory, suggestedCategoryId)
        } else {
            findSimilarSubCategories(row.subCategory)
        }
        
        val suggestedSubCategoryId = similarSubCategories.firstOrNull()?.subCatId
        
        return Pair(suggestedCategoryId, suggestedSubCategoryId)
    }
    
    // Auto-map rows that have exact or very close matches
    fun autoMapSimilarRows() {
        var autoMappedCount = 0
        
        importedRows.forEach { row ->
            if (row.status == ImportRowStatus.NEEDS_MAPPING) {
                val (suggestedCategoryId, suggestedSubCategoryId) = suggestMapping(row)
                
                if (suggestedCategoryId != null && suggestedSubCategoryId != null) {
                    // Check if the suggestion is a very close match
                    val category = categories.find { it.catId == suggestedCategoryId }
                    val subCategory = subCategories.find { it.subCatId == suggestedSubCategoryId }
                    
                    if (category != null && subCategory != null) {
                        val categorySimilarity = calculateSimilarity(row.category, category.catName)
                        val subCategorySimilarity = calculateSimilarity(row.subCategory, subCategory.subCatName)
                        
                        // Auto-map if both have high similarity (80% or more)
                        if (categorySimilarity >= 0.8 && subCategorySimilarity >= 0.8) {
                            updateItemCategory(row, suggestedCategoryId, suggestedSubCategoryId)
                            autoMappedCount++
                        }
                    }
                }
            }
        }
        
        if (autoMappedCount > 0) {
            _snackBarState.value = "Auto-mapped $autoMappedCount rows with similar categories"
        }
    }
    
    // Calculate similarity between two strings (simple Jaccard similarity)
    private fun calculateSimilarity(str1: String, str2: String): Double {
        val set1 = str1.lowercase().toSet()
        val set2 = str2.lowercase().toSet()
        
        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size
        
        return if (union > 0) intersection.toDouble() / union else 0.0
    }
    
    // Show mapping suggestions for a specific row
    fun showMappingSuggestions(row: ImportedItemRow): List<Pair<CategoryEntity, SubCategoryEntity>> {
        val suggestions = mutableListOf<Pair<CategoryEntity, SubCategoryEntity>>()
        
        // Find similar categories
        val similarCategories = findSimilarCategories(row.category)
        
        for (category in similarCategories) {
            // Find subcategories that belong to this category
            val categorySubCategories = subCategories.filter { it.catId == category.catId }
            
            // Find similar subcategories within this category
            val similarSubCategories = findSimilarSubCategories(row.subCategory, category.catId)
            
            for (subCategory in similarSubCategories) {
                suggestions.add(Pair(category, subCategory))
            }
            
            // If no similar subcategories found, add the first few subcategories from this category
            if (similarSubCategories.isEmpty() && categorySubCategories.isNotEmpty()) {
                suggestions.addAll(categorySubCategories.take(3).map { Pair(category, it) })
            }
        }
        
        return suggestions.distinctBy { "${it.first.catId}_${it.second.subCatId}" }
    }
    
    // Get the best mapping suggestion for a row
    fun getBestMappingSuggestion(row: ImportedItemRow): Pair<CategoryEntity, SubCategoryEntity>? {
        val suggestions = showMappingSuggestions(row)
        return suggestions.firstOrNull()
    }
    
    // Apply the best mapping suggestion for a row
    fun applyBestSuggestion(row: ImportedItemRow): Boolean {
        val suggestion = getBestMappingSuggestion(row)
        return if (suggestion != null) {
            updateItemCategory(row, suggestion.first.catId, suggestion.second.subCatId)
            true
        } else {
            false
        }
    }
    
    // Get summary of all mapping suggestions
    fun getMappingSuggestionsSummary(): Map<String, List<Pair<CategoryEntity, SubCategoryEntity>>> {
        val summary = mutableMapOf<String, List<Pair<CategoryEntity, SubCategoryEntity>>>()
        
        importedRows.filter { it.status == ImportRowStatus.NEEDS_MAPPING || it.status == ImportRowStatus.ERROR }.forEach { row ->
            val suggestions = showMappingSuggestions(row)
            if (suggestions.isNotEmpty()) {
                summary["Row ${row.rowNumber}: ${row.category} → ${row.subCategory}"] = suggestions
            }
        }
        
        return summary
    }
    
    // Apply all available suggestions at once
    fun applyAllSuggestions(): Int {
        var appliedCount = 0
        
        importedRows.filter { it.status == ImportRowStatus.NEEDS_MAPPING }.forEach { row ->
            if (applyBestSuggestion(row)) {
                appliedCount++
            }
        }
        
        return appliedCount
    }
    
    // Show detailed mapping dialog for a specific row
    fun showDetailedMappingDialog(row: ImportedItemRow): List<Pair<CategoryEntity, SubCategoryEntity>> {
        return showMappingSuggestions(row)
    }
    
    // Get count of rows that can be auto-mapped
    fun getAutoMappableRowsCount(): Int {
        return importedRows.count { row ->
            row.status == ImportRowStatus.NEEDS_MAPPING && getBestMappingSuggestion(row) != null
        }
    }
    
    // Get preview of what will be auto-mapped
    fun getAutoMappingPreview(): List<Triple<Int, String, String>> {
        return importedRows.filter { row ->
            row.status == ImportRowStatus.NEEDS_MAPPING && getBestMappingSuggestion(row) != null
        }.map { row ->
            val suggestion = getBestMappingSuggestion(row)!!
            Triple(
                row.rowNumber,
                "${row.category} → ${row.subCategory}",
                "${suggestion.first.catName} → ${suggestion.second.subCatName}"
            )
        }
    }
    
    // Get summary of what needs to be fixed before import
    fun getImportReadinessSummary(): Triple<Int, Int, Int> {
        val validRows = importedRows.count { it.status == ImportRowStatus.VALID }
        val needsMappingRows = importedRows.count { it.status == ImportRowStatus.NEEDS_MAPPING }
        val errorRows = importedRows.count { it.status == ImportRowStatus.ERROR }
        
        return Triple(validRows, needsMappingRows, errorRows)
    }
    
    // Check if import is ready (all rows are valid)
    fun isImportReady(): Boolean {
        return importedRows.isNotEmpty() && importedRows.all { it.status == ImportRowStatus.VALID }
    }
    
    // Get preview of what will be imported
    fun getImportPreview(): List<Pair<Int, String>> {
        return importedRows.filter { it.status == ImportRowStatus.VALID }.map { row ->
            Pair(row.rowNumber, "${row.category} → ${row.subCategory} → ${row.itemName}")
        }
    }
    
    // Get total count of items that will be imported
    fun getImportableItemsCount(): Int {
        return importedRows.count { it.status == ImportRowStatus.VALID }
    }
    
    // Get summary of what needs to be fixed
    fun getFixSummary(): String {
        val needsMapping = importedRows.count { it.status == ImportRowStatus.NEEDS_MAPPING }
        val errors = importedRows.count { it.status == ImportRowStatus.ERROR }
        
        return when {
            needsMapping > 0 && errors > 0 -> "$needsMapping rows need mapping, $errors rows have errors"
            needsMapping > 0 -> "$needsMapping rows need category mapping"
            errors > 0 -> "$errors rows have validation errors"
            else -> "All rows are ready for import"
        }
    }
    
    // Get summary of all improvements made
    fun getImprovementsSummary(): String {
        val totalRows = importedRows.size
        val validRows = importedRows.count { it.status == ImportRowStatus.VALID }
        val needsMappingRows = importedRows.count { it.status == ImportRowStatus.NEEDS_MAPPING }
        val errorRows = importedRows.count { it.status == ImportRowStatus.ERROR }
        
        return buildString {
            append("Import Summary: ")
            append("$validRows valid, ")
            append("$needsMappingRows need mapping, ")
            append("$errorRows have errors")
            
            if (needsMappingRows > 0) {
                val autoMappable = getAutoMappableRowsCount()
                if (autoMappable > 0) {
                    append(" ($autoMappable can be auto-mapped)")
                }
            }
        }
    }
    
    // Get detailed report of all improvements
    fun getDetailedImprovementsReport(): String {
        val report = StringBuilder()
        report.appendLine("=== IMPORT IMPROVEMENTS REPORT ===")
        report.appendLine()
        
        val totalRows = importedRows.size
        val validRows = importedRows.count { it.status == ImportRowStatus.VALID }
        val needsMappingRows = importedRows.count { it.status == ImportRowStatus.NEEDS_MAPPING }
        val errorRows = importedRows.count { it.status == ImportRowStatus.ERROR }
        
        report.appendLine("Total Rows: $totalRows")
        report.appendLine("Valid Rows: $validRows")
        report.appendLine("Need Mapping: $needsMappingRows")
        report.appendLine("Error Rows: $errorRows")
        report.appendLine()
        
        if (needsMappingRows > 0) {
            val autoMappable = getAutoMappableRowsCount()
            report.appendLine("Auto-Mappable Rows: $autoMappable")
            report.appendLine()
            
            val suggestions = getMappingSuggestionsSummary()
            if (suggestions.isNotEmpty()) {
                report.appendLine("MAPPING SUGGESTIONS:")
                suggestions.forEach { (rowInfo, suggestions) ->
                    report.appendLine("  $rowInfo")
                    suggestions.forEach { (category, subCategory) ->
                        report.appendLine("    → ${category.catName} → ${subCategory.subCatName}")
                    }
                    report.appendLine()
                }
            }
        }
        
        if (errorRows > 0) {
            report.appendLine("ERROR DETAILS:")
            importedRows.filter { it.status == ImportRowStatus.ERROR }.forEach { row ->
                report.appendLine("  Row ${row.rowNumber}: ${row.errorMessage}")
            }
        }
        
        return report.toString()
    }
    
    // Export detailed improvements report
    fun exportImprovementsReport() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                importProgressText.value = "Creating improvements report..."
                
                val report = getDetailedImprovementsReport()
                val fileName = "Import_Improvements_Report_${System.currentTimeMillis()}.txt"
                
                // Create content values for MediaStore
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(
                        android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS + "/JewelVault/ImportReports"
                    )
                }
                
                val resolver = context.contentResolver
                contentValues.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, 
                    android.os.Environment.DIRECTORY_DOWNLOADS + "/JewelVault/ImportReports")
                
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Unable to create file in MediaStore")
                
                // Write report to file
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(report.toByteArray())
                }
                
                _snackBarState.value = "Improvements report exported successfully!"
                
            } catch (e: Exception) {
                log("Error exporting improvements report: ${e.message}")
                _snackBarState.value = "Error exporting improvements report: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    // Get summary of all improvements for UI display
    fun getImprovementsSummaryForUI(): String {
        val totalRows = importedRows.size
        if (totalRows == 0) return "No items imported yet"
        
        val validRows = importedRows.count { it.status == ImportRowStatus.VALID }
        val needsMappingRows = importedRows.count { it.status == ImportRowStatus.NEEDS_MAPPING }
        val errorRows = importedRows.count { it.status == ImportRowStatus.ERROR }
        
        return buildString {
            append("📊 Import Status: ")
            append("$validRows ✅, ")
            append("$needsMappingRows ⚠️, ")
            append("$errorRows ❌")
            
            if (needsMappingRows > 0) {
                val autoMappable = getAutoMappableRowsCount()
                if (autoMappable > 0) {
                    append(" (${autoMappable} can be auto-fixed)")
                }
            }
        }
    }
    
    // Get summary of what can be auto-fixed
    fun getAutoFixSummary(): String {
        val autoMappable = getAutoMappableRowsCount()
        if (autoMappable == 0) return ""
        
        return "🔧 $autoMappable rows can be automatically fixed with category mapping suggestions"
    }
    
    private fun validateRow(row: ImportedItemRow) {
        // Enhanced validation logic with all 9 mandatory fields and input correction
        val errors = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        
        // 1. Category validation (MANDATORY) with correction
        if (row.category.isBlank()) {
            errors.add("Category is required")
        } else {
            val categoryCorrection = InputCorrectionUtils.findClosestCategory(row.category, categories)
            if (categoryCorrection != null && categoryCorrection.corrected != null) {
                if (categoryCorrection.confidence > 0.8f) {
                    // Auto-correct with high confidence
                    row.category = categoryCorrection.corrected.catName
                    row.categoryId = categoryCorrection.corrected.catId
                    row.mappedCategoryId = categoryCorrection.corrected.catId
                    suggestions.add("✅ Auto-corrected category to '${categoryCorrection.corrected.catName}'")
                } else {
                    // Suggest correction
                    suggestions.add("💡 ${categoryCorrection.suggestion}")
                    errors.add("Category '${row.category}' not found. ${categoryCorrection.suggestion}")
                }
            }
        }
        
        // 2. Sub-category validation (MANDATORY) with correction
        if (row.subCategory.isBlank()) {
            errors.add("Sub-category is required")
        } else {
            val subCategoryCorrection = InputCorrectionUtils.findClosestSubCategory(
                row.subCategory, 
                subCategories, 
                row.categoryId
            )
            if (subCategoryCorrection != null && subCategoryCorrection.corrected != null) {
                if (subCategoryCorrection.confidence > 0.8f) {
                    // Auto-correct with high confidence
                    row.subCategory = subCategoryCorrection.corrected.subCatName
                    row.subCategoryId = subCategoryCorrection.corrected.subCatId
                    row.mappedSubCategoryId = subCategoryCorrection.corrected.subCatId
                    suggestions.add("✅ Auto-corrected sub-category to '${subCategoryCorrection.corrected.subCatName}'")
                } else {
                    // Suggest correction
                    suggestions.add("💡 ${subCategoryCorrection.suggestion}")
                    errors.add("Sub-category '${row.subCategory}' not found. ${subCategoryCorrection.suggestion}")
                }
            }
        }
        
        // 3. Quantity validation (MANDATORY - not less than 1) with correction
        if (row.quantity < 1) {
            errors.add("Quantity must be at least 1")
        } else if (!InputValidator.isValidQuantity(row.quantity.toString())) {
            errors.add("Invalid quantity - must be a positive integer")
        }
        
        // 4. Gross Weight validation (MANDATORY - not less than 0)
        val gsWt = row.gsWt
        if (gsWt == null) {
            errors.add("Gross weight (Gs.Wt) is required")
        } else if (gsWt < 0) {
            errors.add("Gross weight (Gs.Wt) cannot be less than 0")
        } else if (!InputValidator.isValidWeight(gsWt.toString())) {
            errors.add("Invalid gross weight - must be a positive decimal")
        }
        
        // 5. Net Weight validation (MANDATORY - not less than 0)
        val ntWt = row.ntWt
        if (ntWt == null) {
            errors.add("Net weight (Nt.Wt) is required")
        } else if (ntWt < 0) {
            errors.add("Net weight (Nt.Wt) cannot be less than 0")
        } else if (!InputValidator.isValidWeight(ntWt.toString())) {
            errors.add("Invalid net weight - must be a positive decimal")
        }
        
        // 6. Purity validation (MANDATORY) with correction
        val purity = row.purity
        if (purity.isNullOrBlank()) {
            errors.add("Purity is required")
        } else {
            val purityCorrection = InputCorrectionUtils.correctPurity(purity)
            if (purityCorrection.corrected != null) {
                if (purityCorrection.confidence > 0.8f) {
                    // Auto-correct with high confidence
                    row.purity = purityCorrection.corrected
                    suggestions.add("✅ Auto-corrected purity to '${purityCorrection.corrected}'")
                } else {
                    // Suggest correction
                    suggestions.add("💡 ${purityCorrection.suggestion}")
                    errors.add("Invalid purity '${purity}'. ${purityCorrection.suggestion}")
                }
            } else if (Purity.fromLabel(purity) == null) {
                errors.add("Invalid purity. Must be one of: ${Purity.list().take(10).joinToString(", ")}...")
            }
        }
        
        // 7. Making Charge Type validation (MANDATORY) with correction
        if (row.mcType.isNullOrBlank()) {
            errors.add("Making charge type (Mc.Type) is required")
        } else {
            val mcTypeValue = row.mcType ?: ""
            val mcTypeCorrection = InputCorrectionUtils.correctMcType(mcTypeValue)
            if (mcTypeCorrection.corrected != null) {
                if (mcTypeCorrection.confidence > 0.8f) {
                    // Auto-correct with high confidence
                    row.mcType = mcTypeCorrection.corrected
                    suggestions.add("✅ Auto-corrected making charge type to '${mcTypeCorrection.corrected}'")
                } else {
                    // Suggest correction
                    suggestions.add("💡 ${mcTypeCorrection.suggestion}")
                    errors.add("Invalid making charge type '${mcTypeValue}'. ${mcTypeCorrection.suggestion}")
                }
            } else if (!ChargeType.list().contains(mcTypeValue)) {
                errors.add("Invalid making charge type. Must be one of: ${ChargeType.list().joinToString(", ")}")
            }
        }
        
        // 8. Making Charge validation (MANDATORY)
        val mcChr = row.mcChr
        if (mcChr == null) {
            errors.add("Making charge (M.Chr) is required")
        } else if (mcChr < 0) {
            errors.add("Making charge (M.Chr) cannot be negative")
        }
        
        // 9. Tax validation (MANDATORY - at least one GST type)
        val cgst = row.cgst
        val sgst = row.sgst
        val igst = row.igst
        val hasValidTax = (cgst != null && cgst > 0) || (sgst != null && sgst > 0) || (igst != null && igst > 0)
        
        if (!hasValidTax) {
            errors.add("At least one tax percentage (CGST, SGST, or IGST) must be specified and greater than 0")
        }
        
        // Additional validations for optional fields
        if (row.itemName.isBlank()) errors.add("Item name is required")
        if (row.entryType.isBlank()) errors.add("Entry type is required")
        if (row.unit.isBlank()) errors.add("Unit is required")
        
        // Entry type validation using utility class with correction
        if (row.entryType.isNotBlank()) {
            val entryTypeCorrection = InputCorrectionUtils.correctEntryType(row.entryType)
            if (entryTypeCorrection.corrected != null) {
                if (entryTypeCorrection.confidence > 0.8f) {
                    // Auto-correct with high confidence
                    row.entryType = entryTypeCorrection.corrected
                    suggestions.add("✅ Auto-corrected entry type to '${entryTypeCorrection.corrected}'")
                } else {
                    // Suggest correction
                    suggestions.add("💡 ${entryTypeCorrection.suggestion}")
                    errors.add("Invalid entry type '${row.entryType}'. ${entryTypeCorrection.suggestion}")
                }
            } else if (!EntryType.list().contains(row.entryType)) {
                errors.add("Invalid entry type. Must be one of: ${EntryType.list().joinToString(", ")}")
            }
        }
        
        // Fine Weight validation (not less than 0)
        val fnWt = row.fnWt
        if (fnWt != null && fnWt < 0) {
            errors.add("Fine weight (Fn.Wt) cannot be less than 0")
        } else if (fnWt != null && !InputValidator.isValidWeight(fnWt.toString())) {
            errors.add("Invalid fine weight - must be a positive decimal")
        }
        
        // Weight relationship validation
        if (gsWt != null && ntWt != null) {
            if (gsWt < ntWt) {
                errors.add("Gross weight cannot be less than net weight")
            }
        }
        
        // Auto-calculate fine weight based on gs.wt, nt.wt and purity
        if (purity?.isNotBlank() == true && ntWt != null && gsWt != null) {
            val purityObj = Purity.fromLabel(purity)
            if (purityObj != null) {
                // Calculate fine weight as: (Net Weight * Purity multiplier)
                val calculatedFnWt = ntWt * purityObj.multiplier
                if (fnWt == null) {
                    // Auto-fill fine weight
                    row.fnWt = calculatedFnWt
                } else {
                    // Validate fine weight against calculated value (allow small tolerance)
                    val tolerance = 0.01
                    if (kotlin.math.abs(fnWt - calculatedFnWt) > tolerance) {
                        errors.add("Fine weight should be ${calculatedFnWt.to3FString()}g for ${purity} purity (calculated from Nt.Wt: ${ntWt}g)")
                    }
                }
            }
        }
        
        // Other charge validation (optional)
        val chr = row.chr
        if (chr != null && chr < 0) errors.add("Other charge cannot be negative")
        
        // GST percentage range validations
        if (cgst != null && (cgst < 0 || cgst > 100)) errors.add("CGST percentage must be between 0 and 100")
        if (sgst != null && (sgst < 0 || sgst > 100)) errors.add("SGST percentage must be between 0 and 100")
        if (igst != null && (igst < 0 || igst > 100)) errors.add("IGST percentage must be between 0 and 100")

        
        // Validate and map categories and subcategories
        if (row.category.isNotBlank() && row.subCategory.isNotBlank()) {
            val category = categories.find { it.catName.equals(row.category.trim(), ignoreCase = true) }
            val subCategory = subCategories.find { it.subCatName.equals(row.subCategory.trim(), ignoreCase = true) }
            
            if (category != null && subCategory != null) {
                // Check if subcategory belongs to the found category
                if (subCategory.catId == category.catId) {
                    row.categoryId = category.catId
                    row.subCategoryId = subCategory.subCatId
                    row.mappedCategoryId = category.catId
                    row.mappedSubCategoryId = subCategory.subCatId
                } else {
                    errors.add("Sub-category '${row.subCategory}' does not belong to category '${row.category}'")
                }
            } else {
                // If either category or subcategory is not found, mark as needs mapping
                // but don't add to errors list since user can map them manually
                if (category == null) {
                    row.status = ImportRowStatus.NEEDS_MAPPING
                    row.errorMessage = "Category '${row.category}' not found - needs mapping"
                    return // Exit early, don't proceed with other validations
                } else if (subCategory == null) {
                    row.status = ImportRowStatus.NEEDS_MAPPING
                    row.errorMessage = "Sub-category '${row.subCategory}' not found - needs mapping"
                    return // Exit early, don't proceed with other validations
                }
            }
        }
        
        if (errors.isEmpty()) {
            row.status = ImportRowStatus.VALID
            row.errorMessage = if (suggestions.isNotEmpty()) {
                "✅ Valid with corrections: ${suggestions.joinToString("; ")}"
            } else {
                null
            }
        } else {
            row.status = ImportRowStatus.ERROR
            val errorMessage = errors.joinToString("; ")
            val suggestionMessage = if (suggestions.isNotEmpty()) {
                " Suggestions: ${suggestions.joinToString("; ")}"
            } else ""
            row.errorMessage = errorMessage + suggestionMessage
        }
    }
    
    private fun loadDatabaseData() {
        viewModelScope.launch {
            try {
                val categoryDao = appDatabase.categoryDao()
                val subCategoryDao = appDatabase.subCategoryDao()
                
                // Load categories and subcategories
                val allCategories = withIo { categoryDao.getAllCategories() }
                categories.clear()
                categories.addAll(allCategories)
                
                // Load subcategories
                val allSubCategories = withIo { subCategoryDao.getAllSubCategories() }
                subCategories.clear()
                subCategories.addAll(allSubCategories)
                
            } catch (e: Exception) {
                log("Error loading database data: ${e.message}")
                _snackBarState.value = "Error loading database data: ${e.message}"
            }
        }
    }
    
    // Parse Excel file and extract data
    fun parseExcelFile(uri: Uri) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                importProgress.value = 0f
                importProgressText.value = "Reading Excel file..."
                
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _snackBarState.value = "Error: Could not read file"
                    return@launch
                }
                
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                
                importProgressText.value = "Processing rows..."
                val totalRows = sheet.physicalNumberOfRows
                
                importedRows.clear()
                
                // Skip header row and process data rows
                for (rowIndex in 1 until totalRows) {
                    val row = sheet.getRow(rowIndex)
                    if (row != null) {
                        val importedRow = parseRow(row, rowIndex)
                        importedRows.add(importedRow)
                        
                        // Update progress
                        importProgress.value = rowIndex.toFloat() / totalRows
                    }
                }
                
                inputStream.close()
                workbook.close()
                
                // Validate all rows
                importedRows.forEach { validateRow(it) }
                updateImportSummary()
                
                _snackBarState.value = "Successfully imported ${importedRows.size} rows from Excel"
                
            } catch (e: Exception) {
                log("Error parsing Excel file: ${e.message}")
                _snackBarState.value = "Error parsing Excel file: ${e.message}"
            } finally {
                isLoading.value = false
                importProgress.value = 1f
                importProgressText.value = "Import completed"
            }
        }
    }
    
    private fun parseRow(row: Row, rowNumber: Int): ImportedItemRow {
        fun getCellValue(cellIndex: Int): String {
            val cell = row.getCell(cellIndex)
            return when (cell?.cellType) {
                CellType.STRING -> cell.stringCellValue ?: ""
                CellType.NUMERIC -> cell.numericCellValue.toString()
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                else -> ""
            }.trim()
        }
        
        return ImportedItemRow(
            rowNumber = rowNumber,
            category = getCellValue(0),
            subCategory = getCellValue(1),
            itemName = getCellValue(2),
            entryType = getCellValue(3),
            quantity = getCellValue(4).toIntOrNull() ?: 0,
            gsWt = getCellValue(5).toDoubleOrNull(),
            ntWt = getCellValue(6).toDoubleOrNull(),
            unit = getCellValue(7),
            purity = getCellValue(8).takeIf { it.isNotEmpty() },
            fnWt = getCellValue(9).toDoubleOrNull(),
            mcType = getCellValue(10).takeIf { it.isNotEmpty() },
            mcChr = getCellValue(11).toDoubleOrNull(),
            othChr = getCellValue(12).takeIf { it.isNotEmpty() },
            chr = getCellValue(13).toDoubleOrNull(),
            cgst = getCellValue(14).toDoubleOrNull(),
            sgst = getCellValue(15).toDoubleOrNull(),
            igst = getCellValue(16).toDoubleOrNull(),
            huid = getCellValue(17).takeIf { it.isNotEmpty() },
            addDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()), // Always use current date
            addDesKey = getCellValue(19).takeIf { it.isNotEmpty() },
            addDesValue = getCellValue(20).takeIf { it.isNotEmpty() },
            extra = getCellValue(21).takeIf { it.isNotEmpty() }
        )
    }
    
    // Apply bulk mapping for categories and subcategories
    fun applyBulkMapping() {
        val categoryId = bulkCategoryMapping.value
        val subCategoryId = bulkSubCategoryMapping.value
        
        if (categoryId != null && subCategoryId != null) {
            importedRows.forEach { row ->
                if (row.status == ImportRowStatus.NEEDS_MAPPING) {
                    updateItemCategory(row, categoryId, subCategoryId)
                }
            }
            
            // Clear bulk mapping after applying
            bulkCategoryMapping.value = null
            bulkSubCategoryMapping.value = null
            
            _snackBarState.value = "Bulk mapping applied successfully"
        } else {
            _snackBarState.value = "Please select both category and subcategory for bulk mapping"
        }
    }
    
    fun updateImportSummary() {
        val total = importedRows.size
        val valid = importedRows.count { it.status == ImportRowStatus.VALID }
        val needsMapping = importedRows.count { it.status == ImportRowStatus.NEEDS_MAPPING }
        val error = importedRows.count { it.status == ImportRowStatus.ERROR }
        
        importSummary.value = ImportSummary(
            totalRows = total,
            validRows = valid,
            needsMappingRows = needsMapping,
            errorRows = error
        )
    }
    
    fun confirmImport() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                
                val currentUser = dataStoreManager.getCurrentLoginUser()
                val (storeId, _, _) = dataStoreManager.getSelectedStoreInfo()
                val currentStoreId = storeId.first()
                
                // Ensure all rows have proper category mapping
                val rowsToImport = importedRows.filter { it.status == ImportRowStatus.VALID }
                val rowsNeedingMapping = importedRows.filter { it.status == ImportRowStatus.NEEDS_MAPPING }
                
                if (rowsNeedingMapping.isNotEmpty()) {
                    _snackBarState.value = "Cannot import: ${rowsNeedingMapping.size} rows still need category mapping"
                    return@launch
                }
                
                if (rowsToImport.isEmpty()) {
                    _snackBarState.value = "No valid rows to import"
                    return@launch
                }
                
                val itemDao = appDatabase.itemDao()
                
                // Convert to ItemEntity and insert
                val items = rowsToImport.map { row ->
                    row.toItemEntity(
                        userId = currentUser.userId,
                        storeId = currentStoreId
                    )
                }
                
                // Insert items one by one (Room handles transactions automatically for single operations)
                items.forEach { item ->
                    withIo { itemDao.insert(item) }
                }
                
                _snackBarState.value = "Successfully imported ${items.size} items!"
                showConfirmImportDialog.value = false
                
                // Clear the list and reset all values after successful import
                clearImport()
                
            } catch (e: Exception) {
                log("Error importing items: ${e.message}")
                _snackBarState.value = "Error importing items: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun exportExampleExcel() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                importProgressText.value = "Creating example Excel template..."
                
                // Create example Excel with headers matching the actual item export structure
                val headers = listOf(
                    "Category", "SubCategory", "ItemName", "EntryType", "Quantity", 
                    "GsWt", "NtWt", "Unit", "Purity", "FnWt", "McType", "McChr", 
                    "OthChr", "Chr", "CGST%", "SGST%", "IGST%", "Huid", "AddDate", "AddDesKey", "AddDesValue", "Extra"
                )
                
                // Create comprehensive sample data with various scenarios for testing
                val sampleData = listOf(
                    // VALID ITEMS (Passing scenarios)
                    listOf("Gold", "Ring", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID123", "2024-12-01", "Stone", "150.0", "Premium"),
                    listOf("Silver", "Chain", "Silver Chain 925", EntryType.Piece.type, "2", "10.0", "9.5", "gm", Purity.P999.label, "9.0", ChargeType.PerGm.type, "3.0", "Other charges", "150", "9.0", "9.0", "0.0", "HUID456", "2024-12-01", "Style", "0.0", "Elegant"),
                    listOf("Platinum", "Earring", "Platinum Earring", EntryType.Lot.type, "1", "3.2", "3.0", "gm", Purity.P1000.label, "2.8", ChargeType.Piece.type, "800", "Other charges", "400", "0.0", "0.0", "18.0", "HUID789", "2024-12-01", "Stone", "0.0", "Luxury"),
                    
                    // NEEDS MAPPING ITEMS (Warning scenarios - similar but not exact matches)
                    listOf("GOLD", "RINGS", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID124", "2024-12-01", "Stone", "150.0", "Premium"),
                    listOf("Silver", "Chains", "Silver Chain 925", EntryType.Piece.type, "2", "10.0", "9.5", "gm", Purity.P999.label, "9.0", ChargeType.PerGm.type, "3.0", "Other charges", "150", "9.0", "9.0", "0.0", "HUID457", "2024-12-01", "Style", "0.0", "Elegant"),
                    listOf("Platinum", "Earrings", "Platinum Earring", EntryType.Lot.type, "1", "3.2", "3.0", "gm", Purity.P1000.label, "2.8", ChargeType.Piece.type, "800", "Other charges", "400", "0.0", "0.0", "18.0", "HUID790", "2024-12-01", "Stone", "0.0", "Luxury"),
                    
                    // ERROR ITEMS (Various error scenarios)
                    listOf("", "Ring", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID125", "2024-12-01", "Stone", "150.0", "Premium"), // Missing category
                    listOf("Gold", "", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID126", "2024-12-01", "Stone", "150.0", "Premium"), // Missing subcategory
                    listOf("Gold", "Ring", "", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID127", "2024-12-01", "Stone", "150.0", "Premium"), // Missing item name
                    listOf("Gold", "Ring", "Gold Ring 22K", "INVALID_TYPE", "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID128", "2024-12-01", "Stone", "150.0", "Premium"), // Invalid entry type
                    listOf("Gold", "Ring", "Gold Ring 22K", EntryType.Piece.type, "0", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID129", "2024-12-01", "Stone", "150.0", "Premium"), // Zero quantity
                    listOf("Gold", "Ring", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", "INVALID_PURITY", "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID130", "2024-12-01", "Stone", "150.0", "Premium"), // Invalid purity
                    listOf("Gold", "Ring", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", "INVALID_CHARGE_TYPE", "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID131", "2024-12-01", "Stone", "150.0", "Premium"), // Invalid charge type
                    listOf("Gold", "Ring", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "INVALID_GST", "9.0", "0.0", "HUID132", "2024-12-01", "Stone", "150.0", "Premium"), // Invalid GST percentage
                    listOf("Gold", "Ring", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "INVALID_HUID", "2024-12-01", "Stone", "150.0", "Premium"), // Invalid HUID format
                    listOf("Gold", "Ring", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID133", "INVALID_DATE", "Stone", "150.0", "Premium"), // Invalid date format
                    
                    // MIXED SCENARIOS (Some valid, some with issues)
                    listOf("Diamond", "Ring", "Diamond Ring", EntryType.Piece.type, "1", "2.5", "2.0", "gm", Purity.P916.label, "1.8", ChargeType.Percentage.type, "5.0", "Other charges", "500", "9.0", "9.0", "0.0", "HUID134", "2024-12-01", "Stone", "200.0", "Premium"),
                    listOf("Gold", "Bracelet", "Gold Bracelet 18K", EntryType.Piece.type, "1", "8.0", "7.5", "gm", Purity.P750.label, "7.0", ChargeType.PerGm.type, "4.0", "Other charges", "300", "9.0", "9.0", "0.0", "HUID135", "2024-12-01", "Style", "100.0", "Elegant"),
                    listOf("Silver", "Necklace", "Silver Necklace 925", EntryType.Lot.type, "1", "15.0", "14.5", "gm", Purity.P999.label, "14.0", ChargeType.Piece.type, "1000", "Other charges", "250", "9.0", "9.0", "0.0", "HUID136", "2024-12-01", "Stone", "50.0", "Classic"),
                    listOf("Gold", "Pendant", "Gold Pendant 22K", EntryType.Piece.type, "1", "3.0", "2.8", "gm", Purity.P916.label, "2.5", ChargeType.Percentage.type, "3.0", "Other charges", "180", "9.0", "9.0", "0.0", "HUID137", "2024-12-01", "Stone", "120.0", "Traditional"),
                    listOf("Platinum", "Bangle", "Platinum Bangle", EntryType.Piece.type, "1", "12.0", "11.5", "gm", Purity.P1000.label, "11.0", ChargeType.Piece.type, "1500", "Other charges", "600", "0.0", "0.0", "18.0", "HUID138", "2024-12-01", "Style", "0.0", "Modern")
                )
                
                // Generate filename with timestamp
                val fileName = "Item_Import_Template.xlsx"
                
                // Create content values for MediaStore
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(
                        android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS + "/JewelVault/ImportTemplates"
                    )
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Unable to create file in MediaStore")
                
                // Create Excel workbook
                val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
                val sheet = workbook.createSheet("Import Template")
                
                // Create header style
                val headerStyle = workbook.createCellStyle().apply {
                    fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.index
                    fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
                    val font = workbook.createFont().apply {
                        bold = true
                    }
                    setFont(font)
                }
                
                // Add headers
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { index, header ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(header)
                    cell.cellStyle = headerStyle
                }
                
                // Add sample data
                sampleData.forEachIndexed { rowIndex, rowData ->
                    val row = sheet.createRow(rowIndex + 1)
                    rowData.forEachIndexed { colIndex, value ->
                        row.createCell(colIndex).setCellValue(value)
                    }
                }
                
                // Set fixed column widths instead of auto-sizing (Android compatibility)
                headers.indices.forEach { colIndex ->
                    sheet.setColumnWidth(colIndex, 4000) // Fixed width in 1/256th of a character width
                }
                
                // Write to file
                resolver.openOutputStream(uri)?.use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()
                
                // Open the folder containing the exported file
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(android.net.Uri.parse("content://com.android.externalstorage.documents/root/primary"), "resource/folder")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    log("Could not open folder: ${e.message}")
                }
                
                _snackBarState.value = "Example Excel template created successfully in Downloads/JewelVault/ImportTemplates!"
                
            } catch (e: Exception) {
                log("Error creating example Excel: ${e.message}")
                _snackBarState.value = "Error creating example Excel: ${e.message}"
            } finally {
                isLoading.value = false
                importProgressText.value = ""
            }
        }
    }
    
    fun exportErrors() {
        viewModelScope.launch {
            try {
                val errorRows = importedRows.filter { it.status == ImportRowStatus.ERROR }
                if (errorRows.isEmpty()) {
                    _snackBarState.value = "No errors to export"
                    return@launch
                }
                
                isLoading.value = true
                importProgressText.value = "Exporting error report..."
                
                // Generate filename with timestamp
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val fileName = "JewelVault_Error_Report_$timestamp.xlsx"
                
                // Create content values for MediaStore
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(
                        android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS + "/JewelVault/ImportTemplates"
                    )
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Unable to create file in MediaStore")
                
                // Create Excel workbook
                val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
                val sheet = workbook.createSheet("Error Report")
                
                // Create header style
                val headerStyle = workbook.createCellStyle().apply {
                    fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.RED.index
                    fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
                    val font = workbook.createFont().apply {
                        bold = true
                        color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
                    }
                    setFont(font)
                }
                
                // Headers including error information
                val headers = listOf(
                    "Row Number", "Category", "SubCategory", "ItemName", "EntryType", "Quantity", 
                    "GsWt", "NtWt", "Unit", "Purity", "FnWt", "McType", "McChr", 
                    "OthChr", "Chr", "CGST%", "SGST%", "IGST%", "Huid", "AddDate", "AddDesKey", "AddDesValue", "Extra", "Error Message"
                )
                
                // Add headers
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { index, header ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(header)
                    cell.cellStyle = headerStyle
                }
                
                // Add error rows
                errorRows.forEachIndexed { rowIndex, row ->
                    val excelRow = sheet.createRow(rowIndex + 1)
                    
                    val rowData = listOf(
                        row.rowNumber.toString(),
                        row.category,
                        row.subCategory,
                        row.itemName,
                        row.entryType,
                        row.quantity.toString(),
                        row.gsWt?.toString() ?: "",
                        row.ntWt?.toString() ?: "",
                        row.unit,
                        row.purity ?: "",
                        row.fnWt?.toString() ?: "",
                        row.mcType ?: "",
                        row.mcChr?.toString() ?: "",
                        row.othChr ?: "",
                        row.chr?.toString() ?: "",
                        row.cgst?.toString() ?: "",
                        row.sgst?.toString() ?: "",
                        row.igst?.toString() ?: "",
                        row.huid ?: "",
                        row.addDate ?: "",
                        row.addDesKey ?: "",
                        row.addDesValue ?: "",
                        row.extra ?: "",
                        row.errorMessage ?: ""
                    )
                    
                    rowData.forEachIndexed { colIndex, value ->
                        excelRow.createCell(colIndex).setCellValue(value)
                    }
                }
                
                // Set fixed column widths instead of auto-sizing (Android compatibility)
                headers.indices.forEach { colIndex ->
                    sheet.setColumnWidth(colIndex, 4000) // Fixed width in 1/256th of a character width
                }
                
                // Write to file
                resolver.openOutputStream(uri)?.use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()
                
                // Open the folder containing the exported file
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(android.net.Uri.parse("content://com.android.externalstorage.documents/root/primary"), "resource/folder")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    log("Could not open folder: ${e.message}")
                }
                
                _snackBarState.value = "Error report exported successfully in Downloads/JewelVault/ImportTemplates!"
                
            } catch (e: Exception) {
                log("Error exporting errors: ${e.message}")
                _snackBarState.value = "Error exporting errors: ${e.message}"
            } finally {
                isLoading.value = false
                importProgressText.value = ""
            }
        }
    }
    
    fun clearImport() {
        importedRows.clear()
        importSummary.value = ImportSummary()
        selectedRow.value = null
        bulkCategoryMapping.value = null
        bulkSubCategoryMapping.value = null
    }
    
    fun showMappingDialogForRow(row: ImportedItemRow) {
        selectedRow.value = row
        showMappingDialog.value = true
    }
}
