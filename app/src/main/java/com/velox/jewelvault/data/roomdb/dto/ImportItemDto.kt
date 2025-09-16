package com.velox.jewelvault.data.roomdb.dto

import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import java.sql.Timestamp

enum class ImportRowStatus {
    VALID,
    NEEDS_MAPPING,
    ERROR
}

data class ImportedItemRow(
    val rowNumber: Int,
    var category: String,
    var subCategory: String,
    var itemName: String,
    var entryType: String,
    var quantity: Int,
    var gsWt: Double?, // Required field - will be validated as non-null
    var ntWt: Double?, // Required field - will be validated as non-null
    var unit: String,
    var purity: String?, // Required field - will be validated as non-null
    var fnWt: Double?, // Auto-calculated from ntWt and purity
    var mcType: String?, // Required field - will be validated as non-null
    var mcChr: Double?, // Required field - will be validated as non-null
    var othChr: String?,
    var chr: Double?,
    var cgst: Double?, // At least one tax field required
    var sgst: Double?, // At least one tax field required
    var igst: Double?, // At least one tax field required
    var huid: String?,
    var addDate: String?,
    var addDesKey: String?,
    var addDesValue: String?,
    var extra: String?,
    
    // Validation and mapping fields
    var status: ImportRowStatus = ImportRowStatus.ERROR,
    var errorMessage: String? = null,
    var categoryId: String? = null,
    var subCategoryId: String? = null,
    
    // User mapping decisions
    var mappedCategoryId: String? = null,
    var mappedSubCategoryId: String? = null
)

data class ImportSummary(
    val totalRows: Int = 0,
    val validRows: Int = 0,
    val needsMappingRows: Int = 0,
    val errorRows: Int = 0
)

// Extension function to convert ImportedItemRow to ItemEntity
fun ImportedItemRow.toItemEntity(
    userId: String,
    storeId: String
): ItemEntity {
    return ItemEntity(
        itemId = com.velox.jewelvault.utils.generateId(),
        itemAddName = itemName.trim(),
        catId = mappedCategoryId ?: categoryId ?: "",
        userId = userId,
        storeId = storeId,
        catName = category.trim(),
        subCatId = mappedSubCategoryId ?: subCategoryId ?: "",
        subCatName = subCategory.trim(),
        entryType = entryType.trim(),
        quantity = quantity,
        // Required fields - these should not be null after validation
        gsWt = gsWt ?: throw IllegalStateException("Gross weight is required but was null"),
        ntWt = ntWt ?: throw IllegalStateException("Net weight is required but was null"),
        fnWt = fnWt ?: 0.0, // Auto-calculated, can be 0.0 if not calculated
        purity = purity?.trim() ?: throw IllegalStateException("Purity is required but was null"),
        crgType = mcType?.trim() ?: throw IllegalStateException("Making charge type is required but was null"),
        crg = mcChr ?: throw IllegalStateException("Making charge is required but was null"),
        othCrgDes = othChr?.trim() ?: "",
        othCrg = chr ?: 0.0,
        cgst = cgst ?: 0.0, // GST percentages from import
        sgst = sgst ?: 0.0,
        igst = igst ?: 0.0,
        huid = huid?.trim() ?: "",
        unit = unit.trim(),
        addDesKey = addDesKey?.trim() ?: "imported",
        addDesValue = addDesValue?.trim() ?: "excel_import",
        addDate = Timestamp(System.currentTimeMillis()), // Always use current date for imports
        modifiedDate = Timestamp(System.currentTimeMillis()),
        sellerFirmId = "", // Empty for imported items
        purchaseOrderId = "", // Empty for imported items
        purchaseItemId = "" // Empty for imported items
    )
}
