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
    var gsWt: Double?,
    var ntWt: Double?,
    var unit: String,
    var purity: String?,
    var fnWt: Double?,
    var mcType: String?,
    var mcChr: Double?,
    var othChr: String?,
    var chr: Double?,
    var cgst: Double?,
    var sgst: Double?,
    var igst: Double?,
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
        gsWt = gsWt ?: 0.0,
        ntWt = ntWt ?: 0.0,
        fnWt = fnWt ?: 0.0,
        purity = purity?.trim() ?: "",
        crgType = mcType?.trim() ?: "none",
        crg = mcChr ?: 0.0,
        othCrgDes = othChr?.trim() ?: "",
        othCrg = chr ?: 0.0,
        cgst = cgst ?: 0.0, // GST percentages from import
        sgst = sgst ?: 0.0,
        igst = igst ?: 0.0,
        huid = huid?.trim() ?: "",
        unit = unit.trim(),
        addDesKey = addDesKey?.trim() ?: "imported",
        addDesValue = addDesValue?.trim() ?: "excel_import",
        addDate = if (addDate != null) {
            try {
                Timestamp(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(addDate)?.time ?: System.currentTimeMillis())
            } catch (e: Exception) {
                Timestamp(System.currentTimeMillis())
            }
        } else {
            Timestamp(System.currentTimeMillis())
        },
        modifiedDate = Timestamp(System.currentTimeMillis()),
        sellerFirmId = "", // Empty for imported items
        purchaseOrderId = "", // Empty for imported items
        purchaseItemId = "" // Empty for imported items
    )
}
