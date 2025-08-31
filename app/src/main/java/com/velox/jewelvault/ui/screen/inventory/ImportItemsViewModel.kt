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
import com.velox.jewelvault.utils.Purity
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
        
        importedRows.filter { it.status == ImportRowStatus.NEEDS_MAPPING }.forEach { row ->
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
        // Basic validation logic
        val errors = mutableListOf<String>()
        
        if (row.itemName.isBlank()) errors.add("Item name is required")
        if (row.entryType.isBlank()) errors.add("Entry type is required")
        if (row.quantity <= 0) errors.add("Quantity must be greater than 0")
        if (row.unit.isBlank()) errors.add("Unit is required")
        if (row.category.isBlank()) errors.add("Category is required")
        if (row.subCategory.isBlank()) errors.add("Sub-category is required")
        
        // Validate entry type using utility class
        if (row.entryType.isNotBlank() && !EntryType.list().contains(row.entryType)) {
            errors.add("Invalid entry type. Must be one of: ${EntryType.list().joinToString(", ")}")
        }
        
        // Validate making charge type using utility class
        if (row.mcType?.isNotBlank() == true && !ChargeType.list().contains(row.mcType)) {
            errors.add("Invalid making charge type. Must be one of: ${ChargeType.list().joinToString(", ")}")
        }
        
        // Validate purity using utility class
        val purity = row.purity
        if (purity?.isNotBlank() == true && Purity.fromLabel(purity) == null) {
            errors.add("Invalid purity. Must be one of: ${Purity.list().joinToString(", ")}")
        }
        
        // Validate weights
        val gsWt = row.gsWt
        val ntWt = row.ntWt
        val fnWt = row.fnWt
        if (gsWt != null && gsWt < 0) errors.add("Gross weight cannot be negative")
        if (ntWt != null && ntWt < 0) errors.add("Net weight cannot be negative")
        if (fnWt != null && fnWt < 0) errors.add("Fine weight cannot be negative")
        
        // Validate charges
        val mcChr = row.mcChr
        val chr = row.chr
        if (mcChr != null && mcChr < 0) errors.add("Making charge cannot be negative")
        if (chr != null && chr < 0) errors.add("Other charge cannot be negative")
        
        // Validate GST percentages
        val cgst = row.cgst
        val sgst = row.sgst
        val igst = row.igst
        if (cgst != null && (cgst < 0 || cgst > 100)) errors.add("CGST percentage must be between 0 and 100")
        if (sgst != null && (sgst < 0 || sgst > 100)) errors.add("SGST percentage must be between 0 and 100")
        if (igst != null && (igst < 0 || igst > 100)) errors.add("IGST percentage must be between 0 and 100")
        
        // Validate that at least one GST type is specified
        if ((cgst == null || cgst == 0.0) && (sgst == null || sgst == 0.0) && (igst == null || igst == 0.0)) {
            errors.add("At least one GST percentage (CGST, SGST, or IGST) must be specified")
        }
        
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
            row.errorMessage = null
        } else {
            row.status = ImportRowStatus.ERROR
            row.errorMessage = errors.joinToString(", ")
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
            addDate = getCellValue(18).takeIf { it.isNotEmpty() },
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
                
                // Create sample data rows matching the actual item structure with valid utility values
                val sampleData = listOf(
                    listOf("Gold", "Ring", "Gold Ring 22K", EntryType.Piece.type, "1", "5.5", "5.0", "gm", Purity.P916.label, "4.8", ChargeType.Percentage.type, "2.5", "Other charges", "200", "9.0", "9.0", "0.0", "HUID123", "2024-12-01", "Stone", "150.0", "Premium"),
                    listOf("Silver", "Chain", "Silver Chain 925", EntryType.Piece.type, "2", "10.0", "9.5", "gm", Purity.P916.label, "9.0", ChargeType.PerGm.type, "3.0", "Other charges", "150", "9.0", "9.0", "0.0", "HUID456", "2024-12-01", "Style", "0.0", "Elegant"),
                    listOf("Platinum", "Earring", "Platinum Earring", EntryType.Lot.type, "1", "3.2", "3.0", "gm", Purity.P1000.label, "2.8", ChargeType.Piece.type, "800", "Other charges", "400", "0.0", "0.0", "18.0", "HUID789", "2024-12-01", "Stone", "0.0", "Luxury")
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
