package com.velox.jewelvault.utils.sync

import android.content.Context
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.logJvSync
import com.velox.jewelvault.utils.sync.ExcelSchema
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import android.net.Uri
import android.util.Log

/**
 * Handles exporting RoomDB entities to Excel format
 */
class ExcelExporter(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val schemaVersion = ExcelSchema.CURRENT_SCHEMA_VERSION
    private val recordedHeaders = mutableMapOf<String, List<String>>()
    
    /**
     * Export all entities to a single Excel file with multiple sheets
     */
    suspend fun exportAllEntitiesToExcel(
        database: AppDatabase,
        outputUri: Uri,
        context: Context,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<Unit>  {
        recordedHeaders.clear()
        return try {
            logJvSync("Excel export initiated for $outputUri")
            log("Starting Excel export to: $outputUri")
            val workbook = XSSFWorkbook()
            log("Created XSSFWorkbook successfully")
            
            // Create header style
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                val font = workbook.createFont().apply {
                    bold = true
                }
                setFont(font)
            }
            log("Created header style successfully")
            
            // Export each entity to its own sheet
            log("Starting entity export process...")
            
            onProgress("Exporting Stores...", 5)
            log("Step 1/17: Exporting StoreEntity...")
            exportStoreEntity(database, workbook, headerStyle)
            log("✓ StoreEntity export completed")
            
            onProgress("Exporting Users...", 10)
            log("Step 2/17: Exporting UsersEntity...")
            exportUserEntity(database, workbook, headerStyle)
            log("✓ UsersEntity export completed")
            
            onProgress("Exporting Categories...", 15)
            log("Step 3/17: Exporting CategoryEntity...")
            exportCategoryEntity(database, workbook, headerStyle)
            log("✓ CategoryEntity export completed")
            
            onProgress("Exporting SubCategories...", 20)
            log("Step 4/17: Exporting SubCategoryEntity...")
            exportSubCategoryEntity(database, workbook, headerStyle)
            log("✓ SubCategoryEntity export completed")
            
            onProgress("Exporting Items...", 30)
            log("Step 5/17: Exporting ItemEntity...")
            exportItemEntity(database, workbook, headerStyle)
            log("✓ ItemEntity export completed")
            
            onProgress("Exporting Customers...", 40)
            log("Step 6/17: Exporting CustomerEntity...")
            exportCustomerEntity(database, workbook, headerStyle)
            log("✓ CustomerEntity export completed")
            
            onProgress("Exporting Customer Khata Book Plans...", 45)
            log("Step 7/17: Exporting CustomerKhataBookPlanEntity...")
            exportCustomerKhataBookPlanEntity(database, workbook, headerStyle)
            log("CustomerKhataBookPlanEntity export completed")

            onProgress("Exporting Customer Khata Books...", 50)
            log("Step 8/17: Exporting CustomerKhataBookEntity...")
            exportCustomerKhataBookEntity(database, workbook, headerStyle)
            log("✓ CustomerKhataBookEntity export completed")
            
            onProgress("Exporting Customer Transactions...", 60)
            log("Step 9/17: Exporting CustomerTransactionEntity...")
            exportCustomerTransactionEntity(database, workbook, headerStyle)
            log("✓ CustomerTransactionEntity export completed")
            
            onProgress("Exporting Orders...", 70)
            log("Step 10/17: Exporting OrderEntity...")
            exportOrderEntity(database, workbook, headerStyle)
            log("✓ OrderEntity export completed")
            
            onProgress("Exporting Order Items...", 75)
            log("Step 11/17: Exporting OrderItemEntity...")
            exportOrderItemEntity(database, workbook, headerStyle)
            log("✓ OrderItemEntity export completed")
            
            onProgress("Exporting Exchange Items...", 78)
            log("Step 12/17: Exporting ExchangeItemEntity...")
            exportExchangeItemEntity(database, workbook, headerStyle)
            log("✓ ExchangeItemEntity export completed")
            
            onProgress("Exporting Firms...", 80)
            log("Step 13/17: Exporting FirmEntity...")
            exportFirmEntity(database, workbook, headerStyle)
            log("✓ FirmEntity export completed")
            
            onProgress("Exporting Purchase Orders...", 85)
            log("Step 14/17: Exporting PurchaseOrderEntity...")
            exportPurchaseOrderEntity(database, workbook, headerStyle)
            log("✓ PurchaseOrderEntity export completed")
            
            onProgress("Exporting Purchase Order Items...", 90)
            log("Step 15/17: Exporting PurchaseOrderItemEntity...")
            exportPurchaseOrderItemEntity(database, workbook, headerStyle)
            log("✓ PurchaseOrderItemEntity export completed")
            
            onProgress("Exporting Metal Exchanges...", 95)
            log("Step 16/17: Exporting MetalExchangeEntity...")
            exportMetalExchangeEntity(database, workbook, headerStyle)
            log("✓ MetalExchangeEntity export completed")
            
            onProgress("Exporting User Additional Info...", 98)
            log("Step 17/17: Exporting UserAdditionalInfoEntity...")
            exportUserAdditionalInfoEntity(database, workbook, headerStyle)
            log("✓ UserAdditionalInfoEntity export completed")
            
            createMetadataSheet(workbook)
            
            // Write workbook to output stream from content resolver
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                workbook.write(outputStream)
            } ?: throw Exception("Unable to open output stream for sync file")
            workbook.close()
            log("Excel export completed and file saved to: $outputUri")
            logJvSync("Excel export succeeded for $outputUri")
            Result.success(Unit)
            
        } catch (e: Exception) {
            log("Excel export failed: ${e.message}")
            logJvSync("Excel export failed for $outputUri: ${e.message}")
            Log.e("ExcelExporter", "Excel export failed for $outputUri", e)
            Result.failure(e)
        }
    }

    private fun createMetadataSheet(workbook: Workbook) {
        val sheet = workbook.createSheet("Metadata")
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("key")
        header.createCell(1).setCellValue("value")

        val versionRow = sheet.createRow(1)
        versionRow.createCell(0).setCellValue("schemaVersion")
        versionRow.createCell(1).setCellValue(schemaVersion.toDouble())

            val dateRow = sheet.createRow(2)
            dateRow.createCell(0).setCellValue("exportedAt")
            dateRow.createCell(1).setCellValue(dateFormat.format(Date()))
            
            var metadataRow = 3
            recordedHeaders.forEach { (sheetName, headers) ->
                val row = sheet.createRow(metadataRow++)
                row.createCell(0).setCellValue("headers:$sheetName")
                row.createCell(1).setCellValue(headers.joinToString("|"))
            }
        }
    
    private suspend fun exportStoreEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Creating StoreEntity sheet...")
        val sheet = workbook.createSheet("StoreEntity")
        
        log("  → Fetching stores from database...")
        val stores = database.storeDao().getAllStores()
        log("  → Found ${stores.size} stores to export")

        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "storeId", "userId", "proprietor", "name", "email", "phone", "address",
            "registrationNo", "gstinNo", "panNo", "image", "invoiceNo", "upiId", "lastUpdated"
        )
        recordSheetHeaders("StoreEntity", headers)
        log("  → Creating headers: ${headers.joinToString(", ")}")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        log("  → Processing ${stores.size} store records...")
        stores.forEachIndexed { rowIndex, store ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(store.storeId)
            row.createCell(1).setCellValue(store.userId)
            row.createCell(2).setCellValue(store.proprietor)
            row.createCell(3).setCellValue(store.name)
            row.createCell(4).setCellValue(store.email)
            row.createCell(5).setCellValue(store.phone)
            row.createCell(6).setCellValue(store.address)
            row.createCell(7).setCellValue(store.registrationNo)
            row.createCell(8).setCellValue(store.gstinNo)
            row.createCell(9).setCellValue(store.panNo)
            row.createCell(10).setCellValue(store.image)
            row.createCell(11).setCellValue(store.invoiceNo.toDouble())
            row.createCell(12).setCellValue(store.upiId)
            row.createCell(13).setCellValue(store.lastUpdated.toDouble())
        }
        log("  → StoreEntity export completed: ${stores.size} records")
    }

    private fun recordSheetHeaders(sheetName: String, headers: List<String>) {
        recordedHeaders[sheetName] = headers.toList()
    }
    
    private suspend fun exportUserEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Creating UsersEntity sheet...")
        val sheet = workbook.createSheet("UsersEntity")
        
        log("  → Fetching users from database...")
        val users = database.userDao().getAllUsers()
        log("  → Found ${users.size} users to export")
        
        val headers = listOf("id", "name", "email", "mobileNo", "token", "pin", "role", "lastUpdated")
        recordSheetHeaders("UsersEntity", headers)
        log("  → Creating headers: ${headers.joinToString(", ")}")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        log("  → Processing ${users.size} user records...")
        users.forEachIndexed { rowIndex, user ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(user.userId)
            row.createCell(1).setCellValue(user.name)
            row.createCell(2).setCellValue(user.email ?: "")
            row.createCell(3).setCellValue(user.mobileNo)
            row.createCell(4).setCellValue(user.token ?: "")
            row.createCell(5).setCellValue(user.pin ?: "")
            row.createCell(6).setCellValue(user.role)
            row.createCell(7).setCellValue(user.lastUpdated.toDouble())
        }
        log("  → UsersEntity export completed: ${users.size} records")
    }
    
    private suspend fun exportCategoryEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting CategoryEntity export...")
        val sheet = workbook.createSheet("CategoryEntity")
        val categories = database.categoryDao().getAllCategories()
        log("  → Found ${categories.size} categories to export")
        
        val headers = listOf("catId", "catName", "gsWt", "fnWt", "userId", "storeId")
        recordSheetHeaders("CategoryEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        categories.forEachIndexed { rowIndex, category ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(category.catId)
            row.createCell(1).setCellValue(category.catName)
            row.createCell(2).setCellValue(category.gsWt)
            row.createCell(3).setCellValue(category.fnWt)
            row.createCell(4).setCellValue(category.userId)
            row.createCell(5).setCellValue(category.storeId)
        }
        log("  → CategoryEntity export completed: ${categories.size} records")
    }
    
    private suspend fun exportSubCategoryEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting SubCategoryEntity export...")
        val sheet = workbook.createSheet("SubCategoryEntity")
        val subCategories = database.subCategoryDao().getAllSubCategories()
        log("  → Found ${subCategories.size} sub-categories to export")
        
        val headers = listOf("subCatId", "catId", "userId", "storeId", "catName", "subCatName", "quantity", "gsWt", "fnWt")
        recordSheetHeaders("SubCategoryEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        subCategories.forEachIndexed { rowIndex, subCategory ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(subCategory.subCatId)
            row.createCell(1).setCellValue(subCategory.catId)
            row.createCell(2).setCellValue(subCategory.userId)
            row.createCell(3).setCellValue(subCategory.storeId)
            row.createCell(4).setCellValue(subCategory.catName)
            row.createCell(5).setCellValue(subCategory.subCatName)
            row.createCell(6).setCellValue(subCategory.quantity.toDouble())
            row.createCell(7).setCellValue(subCategory.gsWt)
            row.createCell(8).setCellValue(subCategory.fnWt)
        }
        log("  → SubCategoryEntity export completed: ${subCategories.size} records")
    }
    
    private suspend fun exportItemEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting ItemEntity export...")
        val sheet = workbook.createSheet("ItemEntity")
        val items = database.itemDao().getAllItems()
        log("  → Found ${items.size} items to export")
        
        val headers = listOf(
            "itemId", "itemAddName", "catId", "userId", "storeId", "catName", "subCatId", "subCatName", "entryType", "quantity", "gsWt", "ntWt", "fnWt", "purity", "crgType", "crg", "compDes", "compCrg", "cgst", "sgst", "igst", "huid", "unit", "addDesKey", "addDesValue", "addDate", "modifiedDate", "sellerFirmId", "purchaseOrderId", "purchaseItemId"
        )
        recordSheetHeaders("ItemEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        items.forEachIndexed { rowIndex, item ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(item.itemId)
            row.createCell(1).setCellValue(item.itemAddName)
            row.createCell(2).setCellValue(item.catId)
            row.createCell(3).setCellValue(item.userId)
            row.createCell(4).setCellValue(item.storeId)
            row.createCell(5).setCellValue(item.catName)
            row.createCell(6).setCellValue(item.subCatId)
            row.createCell(7).setCellValue(item.subCatName)
            row.createCell(8).setCellValue(item.entryType)
            row.createCell(9).setCellValue(item.quantity.toDouble())
            row.createCell(10).setCellValue(item.gsWt)
            row.createCell(11).setCellValue(item.ntWt)
            row.createCell(12).setCellValue(item.fnWt)
            row.createCell(13).setCellValue(item.purity)
            row.createCell(14).setCellValue(item.crgType)
            row.createCell(15).setCellValue(item.crg)
            row.createCell(16).setCellValue(item.othCrgDes)
            row.createCell(17).setCellValue(item.othCrg)
            row.createCell(18).setCellValue(item.cgst)
            row.createCell(19).setCellValue(item.sgst)
            row.createCell(20).setCellValue(item.igst)
            row.createCell(21).setCellValue(item.huid)
            row.createCell(22).setCellValue(item.unit)
            row.createCell(23).setCellValue(item.addDesKey)
            row.createCell(24).setCellValue(item.addDesValue)
            row.createCell(25).setCellValue(dateFormat.format(item.addDate))
            row.createCell(26).setCellValue(dateFormat.format(item.modifiedDate))
            row.createCell(27).setCellValue(item.sellerFirmId)
            row.createCell(28).setCellValue(item.purchaseOrderId)
            row.createCell(29).setCellValue(item.purchaseItemId)
        }
        log("  → ItemEntity export completed: ${items.size} records")
    }
    
    private suspend fun exportCustomerEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting CustomerEntity export...")
        val sheet = workbook.createSheet("CustomerEntity")
        val customers = database.customerDao().getAllCustomersList()
        log("  → Found ${customers.size} customers to export")
        
        val headers = listOf("mobileNo", "name", "address", "gstin_pan", "addDate", "lastModifiedDate", "totalItemBought", "totalAmount", "notes", "userId", "storeId")
        recordSheetHeaders("CustomerEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        customers.forEachIndexed { rowIndex, customer ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(customer.mobileNo)
            row.createCell(1).setCellValue(customer.name)
            row.createCell(2).setCellValue(customer.address ?: "")
            row.createCell(3).setCellValue(customer.gstin_pan ?: "")
            row.createCell(4).setCellValue(dateFormat.format(customer.addDate))
            row.createCell(5).setCellValue(dateFormat.format(customer.lastModifiedDate))
            row.createCell(6).setCellValue(customer.totalItemBought.toDouble())
            row.createCell(7).setCellValue(customer.totalAmount)
            row.createCell(8).setCellValue(customer.notes ?: "")
            row.createCell(9).setCellValue(customer.userId)
            row.createCell(10).setCellValue(customer.storeId)
        }
        log("  → CustomerEntity export completed: ${customers.size} records")
    }

    private suspend fun exportCustomerKhataBookPlanEntity(
        database: AppDatabase,
        workbook: Workbook,
        headerStyle: CellStyle
    ) {
        log("  → Starting CustomerKhataBookPlanEntity export...")
        val sheet = workbook.createSheet("CustomerKhataBookPlanEntity")
        val plans = database.customerKhataBookPlanDao().getAllPlansList()
        log("  → Found ${plans.size} khata book plans to export")

        val headers = listOf(
            "planId",
            "name",
            "payMonths",
            "benefitMonths",
            "description",
            "benefitPercentage",
            "userId",
            "storeId",
            "createdAt",
            "updatedAt"
        )
        recordSheetHeaders("CustomerKhataBookPlanEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        plans.forEachIndexed { rowIndex, plan ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(plan.planId)
            row.createCell(1).setCellValue(plan.name)
            row.createCell(2).setCellValue(plan.payMonths.toDouble())
            row.createCell(3).setCellValue(plan.benefitMonths.toDouble())
            row.createCell(4).setCellValue(plan.description)
            row.createCell(5).setCellValue(plan.benefitPercentage)
            row.createCell(6).setCellValue(plan.userId)
            row.createCell(7).setCellValue(plan.storeId)
            row.createCell(8).setCellValue(dateFormat.format(Date(plan.createdAt)))
            row.createCell(9).setCellValue(dateFormat.format(Date(plan.updatedAt)))
        }
        log("  → CustomerKhataBookPlanEntity export completed: ${plans.size} records")
    }
    
    private suspend fun exportCustomerKhataBookEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting CustomerKhataBookEntity export...")
        val sheet = workbook.createSheet("CustomerKhataBookEntity")
        val khataBooks = database.customerKhataBookDao().getAllKhataBooksList()
        log("  → Found ${khataBooks.size} khata books to export")
        
        val headers = listOf("khataBookId", "customerMobile", "planName", "startDate", "endDate", "monthlyAmount", "totalMonths", "totalAmount", "status", "notes", "userId", "storeId")
        recordSheetHeaders("CustomerKhataBookEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        khataBooks.forEachIndexed { rowIndex, khataBook ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(khataBook.khataBookId)
            row.createCell(1).setCellValue(khataBook.customerMobile)
            row.createCell(2).setCellValue(khataBook.planName)
            row.createCell(3).setCellValue(dateFormat.format(khataBook.startDate))
            row.createCell(4).setCellValue(dateFormat.format(khataBook.endDate))
            row.createCell(5).setCellValue(khataBook.monthlyAmount)
            row.createCell(6).setCellValue(khataBook.totalMonths.toDouble())
            row.createCell(7).setCellValue(khataBook.totalAmount)
            row.createCell(8).setCellValue(khataBook.status)
            row.createCell(9).setCellValue(khataBook.notes ?: "")
            row.createCell(10).setCellValue(khataBook.userId)
            row.createCell(11).setCellValue(khataBook.storeId)
        }
        log("  → CustomerKhataBookEntity export completed: ${khataBooks.size} records")
    }
    
    private suspend fun exportCustomerTransactionEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting CustomerTransactionEntity export...")
        val sheet = workbook.createSheet("CustomerTransactionEntity")
        val transactions = database.customerTransactionDao().getAllTransactions()
        log("  → Found ${transactions.size} transactions to export")
        
        val headers = listOf("transactionId", "customerMobile", "transactionDate", "amount", "transactionType", "category", "description", "referenceNumber", "paymentMethod", "khataBookId", "monthNumber", "notes", "userId", "storeId")
        recordSheetHeaders("CustomerTransactionEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        transactions.forEachIndexed { rowIndex, transaction ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(transaction.transactionId)
            row.createCell(1).setCellValue(transaction.customerMobile)
            row.createCell(2).setCellValue(dateFormat.format(transaction.transactionDate))
            row.createCell(3).setCellValue(transaction.amount)
            row.createCell(4).setCellValue(transaction.transactionType)
            row.createCell(5).setCellValue(transaction.category)
            row.createCell(6).setCellValue(transaction.description ?: "")
            row.createCell(7).setCellValue(transaction.referenceNumber ?: "")
            row.createCell(8).setCellValue(transaction.paymentMethod ?: "")
            row.createCell(9).setCellValue(transaction.khataBookId ?: "")
            row.createCell(10).setCellValue(transaction.monthNumber?.toDouble() ?: 0.0)
            row.createCell(11).setCellValue(transaction.notes ?: "")
            row.createCell(12).setCellValue(transaction.userId)
            row.createCell(13).setCellValue(transaction.storeId)
        }
        log("  → CustomerTransactionEntity export completed: ${transactions.size} records")
    }
    
    private suspend fun exportOrderEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting OrderEntity export...")
        val sheet = workbook.createSheet("OrderEntity")
        val orders = database.orderDao().getAllOrders()
        log("  → Found ${orders.size} orders to export")
        
        val headers = listOf("orderId", "customerMobile", "storeId", "userId", "orderDate", "totalAmount", "totalTax", "totalCharge", "discount", "note")
        recordSheetHeaders("OrderEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        orders.forEachIndexed { rowIndex, order ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(order.orderId)
            row.createCell(1).setCellValue(order.customerMobile)
            row.createCell(2).setCellValue(order.storeId)
            row.createCell(3).setCellValue(order.userId)
            row.createCell(4).setCellValue(dateFormat.format(order.orderDate))
            row.createCell(5).setCellValue(order.totalAmount)
            row.createCell(6).setCellValue(order.totalTax)
            row.createCell(7).setCellValue(order.totalCharge)
            row.createCell(8).setCellValue(order.discount)
            row.createCell(9).setCellValue(order.note ?: "")
        }
        log("  → OrderEntity export completed: ${orders.size} records")
    }
    
    private suspend fun exportOrderItemEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting OrderItemEntity export...")
        val sheet = workbook.createSheet("OrderItemEntity")
        val orderItems = database.orderDao().getAllOrderItems()
        log("  → Found ${orderItems.size} order items to export")
        
        val headers = listOf(
            "orderItemId", "orderId", "orderDate", "itemId", "customerMobile", "catId", "catName", "itemAddName", "subCatId", "subCatName", "entryType", "quantity", "gsWt", "ntWt", "fnWt", "fnMetalPrice", "purity", "crgType", "crg", "compDes", "compCrg", "cgst", "sgst", "igst", "huid", "addDesKey", "addDesValue", "price", "charge", "tax", "sellerFirmId", "purchaseOrderId", "purchaseItemId"
        )
        recordSheetHeaders("OrderItemEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        orderItems.forEachIndexed { rowIndex, orderItem ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(orderItem.orderItemId)
            row.createCell(1).setCellValue(orderItem.orderId)
            row.createCell(2).setCellValue(dateFormat.format(orderItem.orderDate))
            row.createCell(3).setCellValue(orderItem.itemId)
            row.createCell(4).setCellValue(orderItem.customerMobile)
            row.createCell(5).setCellValue(orderItem.catId)
            row.createCell(6).setCellValue(orderItem.catName)
            row.createCell(7).setCellValue(orderItem.itemAddName)
            row.createCell(8).setCellValue(orderItem.subCatId)
            row.createCell(9).setCellValue(orderItem.subCatName)
            row.createCell(10).setCellValue(orderItem.entryType)
            row.createCell(11).setCellValue(orderItem.quantity.toDouble())
            row.createCell(12).setCellValue(orderItem.gsWt)
            row.createCell(13).setCellValue(orderItem.ntWt)
            row.createCell(14).setCellValue(orderItem.fnWt)
            row.createCell(15).setCellValue(orderItem.fnMetalPrice)
            row.createCell(16).setCellValue(orderItem.purity)
            row.createCell(17).setCellValue(orderItem.crgType)
            row.createCell(18).setCellValue(orderItem.crg)
            row.createCell(19).setCellValue(orderItem.othCrgDes)
            row.createCell(20).setCellValue(orderItem.othCrg)
            row.createCell(21).setCellValue(orderItem.cgst)
            row.createCell(22).setCellValue(orderItem.sgst)
            row.createCell(23).setCellValue(orderItem.igst)
            row.createCell(24).setCellValue(orderItem.huid)
            row.createCell(25).setCellValue(orderItem.addDesKey)
            row.createCell(26).setCellValue(orderItem.addDesValue)
            row.createCell(27).setCellValue(orderItem.price)
            row.createCell(28).setCellValue(orderItem.charge)
            row.createCell(29).setCellValue(orderItem.tax)
            row.createCell(30).setCellValue(orderItem.sellerFirmId)
            row.createCell(31).setCellValue(orderItem.purchaseOrderId)
            row.createCell(32).setCellValue(orderItem.purchaseItemId)
        }
        log("  → OrderItemEntity export completed: ${orderItems.size} records")
    }
    
    private suspend fun exportExchangeItemEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting ExchangeItemEntity export...")
        val sheet = workbook.createSheet("ExchangeItemEntity")
        
        // Get all exchange items from all orders
        val allOrders = database.orderDao().getAllOrders()
        val allExchangeItems = mutableListOf<com.velox.jewelvault.data.roomdb.entity.order.ExchangeItemEntity>()
        
        for (order in allOrders) {
            val exchangeItems = database.orderDao().getExchangeItemsByOrderId(order.orderId)
            allExchangeItems.addAll(exchangeItems)
        }
        
        log("  → Found ${allExchangeItems.size} exchange items to export")
        
        val headers = listOf(
            "exchangeItemId", "orderId", "orderDate", "customerMobile", "metalType", "purity", 
            "grossWeight", "fineWeight", "price", "isExchangedByMetal", "exchangeValue", "addDate"
        )
        recordSheetHeaders("ExchangeItemEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        allExchangeItems.forEachIndexed { rowIndex, exchangeItem ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(exchangeItem.exchangeItemId)
            row.createCell(1).setCellValue(exchangeItem.orderId)
            row.createCell(2).setCellValue(dateFormat.format(exchangeItem.orderDate))
            row.createCell(3).setCellValue(exchangeItem.customerMobile)
            row.createCell(4).setCellValue(exchangeItem.metalType)
            row.createCell(5).setCellValue(exchangeItem.purity)
            row.createCell(6).setCellValue(exchangeItem.grossWeight)
            row.createCell(7).setCellValue(exchangeItem.fineWeight)
            row.createCell(8).setCellValue(exchangeItem.price)
            row.createCell(9).setCellValue(exchangeItem.isExchangedByMetal)
            row.createCell(10).setCellValue(exchangeItem.exchangeValue)
            row.createCell(11).setCellValue(dateFormat.format(exchangeItem.addDate))
        }
        log("  → ExchangeItemEntity export completed: ${allExchangeItems.size} records")
    }
    
    private suspend fun exportFirmEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting FirmEntity export...")
        val sheet = workbook.createSheet("FirmEntity")
        val firms = database.purchaseDao().getAllFirms()
        log("  → Found ${firms.size} firms to export")
        
        val headers = listOf("firmId", "firmName", "firmMobileNumber", "gstNumber", "address")
        recordSheetHeaders("FirmEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        firms.forEachIndexed { rowIndex, firm ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(firm.firmId)
            row.createCell(1).setCellValue(firm.firmName)
            row.createCell(2).setCellValue(firm.firmMobileNumber)
            row.createCell(3).setCellValue(firm.gstNumber)
            row.createCell(4).setCellValue(firm.address)
        }
        log("  → FirmEntity export completed: ${firms.size} records")
    }
    
    private suspend fun exportPurchaseOrderEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting PurchaseOrderEntity export...")
        val sheet = workbook.createSheet("PurchaseOrderEntity")
        val purchaseOrders = database.purchaseDao().getAllPurchaseOrders()
        log("  → Found ${purchaseOrders.size} purchase orders to export")
        
        val headers = listOf("purchaseOrderId", "sellerId", "billNo", "billDate", "entryDate", "extraChargeDescription", "extraCharge", "totalFinalWeight", "totalFinalAmount", "notes", "cgstPercent", "sgstPercent", "igstPercent")
        recordSheetHeaders("PurchaseOrderEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        purchaseOrders.forEachIndexed { rowIndex, purchaseOrder ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(purchaseOrder.purchaseOrderId)
            row.createCell(1).setCellValue(purchaseOrder.sellerId)
            row.createCell(2).setCellValue(purchaseOrder.billNo)
            row.createCell(3).setCellValue(purchaseOrder.billDate)
            row.createCell(4).setCellValue(purchaseOrder.entryDate)
            row.createCell(5).setCellValue(purchaseOrder.extraChargeDescription ?: "")
            row.createCell(6).setCellValue(purchaseOrder.extraCharge ?: 0.0)
            row.createCell(7).setCellValue(purchaseOrder.totalFinalWeight ?: 0.0)
            row.createCell(8).setCellValue(purchaseOrder.totalFinalAmount ?: 0.0)
            row.createCell(9).setCellValue(purchaseOrder.notes ?: "")
            row.createCell(10).setCellValue(purchaseOrder.cgstPercent)
            row.createCell(11).setCellValue(purchaseOrder.sgstPercent)
            row.createCell(12).setCellValue(purchaseOrder.igstPercent)
        }
        log("  → PurchaseOrderEntity export completed: ${purchaseOrders.size} records")
    }
    
    private suspend fun exportPurchaseOrderItemEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting PurchaseOrderItemEntity export...")
        val sheet = workbook.createSheet("PurchaseOrderItemEntity")
        val purchaseOrderItems = database.purchaseDao().getAllPurchaseOrderItems()
        log("  → Found ${purchaseOrderItems.size} purchase order items to export")
        
        val headers = listOf("purchaseItemId", "purchaseOrderId", "catId", "catName", "subCatId", "subCatName", "gsWt", "purity", "ntWt", "fnWt", "fnRate", "wastagePercent")
        recordSheetHeaders("PurchaseOrderItemEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        purchaseOrderItems.forEachIndexed { rowIndex, item ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(item.purchaseItemId)
            row.createCell(1).setCellValue(item.purchaseOrderId)
            row.createCell(2).setCellValue(item.catId)
            row.createCell(3).setCellValue(item.catName)
            row.createCell(4).setCellValue(item.subCatId)
            row.createCell(5).setCellValue(item.subCatName)
            row.createCell(6).setCellValue(item.gsWt)
            row.createCell(7).setCellValue(item.purity)
            row.createCell(8).setCellValue(item.ntWt)
            row.createCell(9).setCellValue(item.fnWt)
            row.createCell(10).setCellValue(item.fnRate)
            row.createCell(11).setCellValue(item.wastagePercent)
        }
        log("  → PurchaseOrderItemEntity export completed: ${purchaseOrderItems.size} records")
    }
    
    private suspend fun exportMetalExchangeEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting MetalExchangeEntity export...")
        val sheet = workbook.createSheet("MetalExchangeEntity")
        val metalExchanges = database.purchaseDao().getAllMetalExchanges()
        log("  → Found ${metalExchanges.size} metal exchanges to export")
        
        val headers = listOf("exchangeId", "purchaseOrderId", "catId", "catName", "subCatId", "subCatName", "fnWeight")
        recordSheetHeaders("MetalExchangeEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        metalExchanges.forEachIndexed { rowIndex, metalExchange ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(metalExchange.exchangeId)
            row.createCell(1).setCellValue(metalExchange.purchaseOrderId)
            row.createCell(2).setCellValue(metalExchange.catId)
            row.createCell(3).setCellValue(metalExchange.catName)
            row.createCell(4).setCellValue(metalExchange.subCatId)
            row.createCell(5).setCellValue(metalExchange.subCatName)
            row.createCell(6).setCellValue(metalExchange.fnWeight)
        }
        log("  → MetalExchangeEntity export completed: ${metalExchanges.size} records")
    }
    
    private suspend fun exportUserAdditionalInfoEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        log("  → Starting UserAdditionalInfoEntity export...")
        val sheet = workbook.createSheet("UserAdditionalInfoEntity")
        val userAdditionalInfo = database.userAdditionalInfoDao().getAllActiveUserAdditionalInfo()
        log("  → Found ${userAdditionalInfo.size} user additional info records to export")
        
        val headers = listOf(
            "userId", "aadhaarNumber", "address", "emergencyContactPerson", "emergencyContactNumber", 
            "governmentIdNumber", "governmentIdType", "dateOfBirth", "bloodGroup", "isActive", 
            "createdAt", "updatedAt"
        )
        recordSheetHeaders("UserAdditionalInfoEntity", headers)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        userAdditionalInfo.forEachIndexed { rowIndex, userInfo ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(userInfo.userId)
            row.createCell(1).setCellValue(userInfo.aadhaarNumber ?: "")
            row.createCell(2).setCellValue(userInfo.address ?: "")
            row.createCell(3).setCellValue(userInfo.emergencyContactPerson ?: "")
            row.createCell(4).setCellValue(userInfo.emergencyContactNumber ?: "")
            row.createCell(5).setCellValue(userInfo.governmentIdNumber ?: "")
            row.createCell(6).setCellValue(userInfo.governmentIdType ?: "")
            row.createCell(7).setCellValue(userInfo.dateOfBirth ?: "")
            row.createCell(8).setCellValue(userInfo.bloodGroup ?: "")
            row.createCell(9).setCellValue(userInfo.isActive)
            row.createCell(10).setCellValue(dateFormat.format(Date(userInfo.createdAt)))
            row.createCell(11).setCellValue(dateFormat.format(Date(userInfo.updatedAt)))
        }
        log("  → UserAdditionalInfoEntity export completed: ${userAdditionalInfo.size} records")
    }
}
