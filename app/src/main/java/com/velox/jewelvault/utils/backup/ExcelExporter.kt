package com.velox.jewelvault.utils.backup

import android.content.Context
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.utils.log
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles exporting RoomDB entities to Excel format
 */
class ExcelExporter(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * Export all entities to a single Excel file with multiple sheets
     */
    suspend fun exportAllEntitiesToExcel(
        database: AppDatabase, 
        outputFile: File,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<Unit> {
        return try {
            val workbook = XSSFWorkbook()
            
            // Create header style
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                val font = workbook.createFont().apply {
                    bold = true
                }
                setFont(font)
            }
            
            // Export each entity to its own sheet
            onProgress("Exporting Stores...", 5)
            exportStoreEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Users...", 10)
            exportUserEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Categories...", 15)
            exportCategoryEntity(database, workbook, headerStyle)
            
            onProgress("Exporting SubCategories...", 20)
            exportSubCategoryEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Items...", 30)
            exportItemEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Customers...", 40)
            exportCustomerEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Customer Khata Books...", 50)
            exportCustomerKhataBookEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Customer Transactions...", 60)
            exportCustomerTransactionEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Orders...", 70)
            exportOrderEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Order Items...", 75)
            exportOrderItemEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Firms...", 80)
            exportFirmEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Purchase Orders...", 85)
            exportPurchaseOrderEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Purchase Order Items...", 90)
            exportPurchaseOrderItemEntity(database, workbook, headerStyle)
            
            onProgress("Exporting Metal Exchanges...", 95)
            exportMetalExchangeEntity(database, workbook, headerStyle)
            
            // Write to file
            FileOutputStream(outputFile).use { fileOut ->
                workbook.write(fileOut)
            }
            workbook.close()
            
            log("Excel export completed successfully: ${outputFile.absolutePath}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            log("Excel export failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun exportStoreEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("StoreEntity")
        val stores = database.storeDao().getAllStores()

        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "storeId", "userId", "proprietor", "name", "email", "phone", "address",
            "registrationNo", "gstinNo", "panNo", "image", "invoiceNo", "upiId"
        )
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        stores.forEachIndexed { rowIndex, store ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(store.storeId.toDouble())
            row.createCell(1).setCellValue(store.userId.toDouble())
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
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportUserEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("UsersEntity")
        val users = database.userDao().getAllUsers()
        val headers = listOf("id", "name", "email", "mobileNo", "token", "pin")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        users.forEachIndexed { rowIndex, user ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(user.userId.toDouble())
            row.createCell(1).setCellValue(user.name)
            row.createCell(2).setCellValue(user.email ?: "")
            row.createCell(3).setCellValue(user.mobileNo)
            row.createCell(4).setCellValue(user.token ?: "")
            row.createCell(5).setCellValue(user.pin ?: "")
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportCategoryEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("CategoryEntity")
        val categories = database.categoryDao().getAllCategories()
        val headers = listOf("catId", "catName", "gsWt", "fnWt", "userId", "storeId")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        categories.forEachIndexed { rowIndex, category ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(category.catId.toDouble())
            row.createCell(1).setCellValue(category.catName)
            row.createCell(2).setCellValue(category.gsWt)
            row.createCell(3).setCellValue(category.fnWt)
            row.createCell(4).setCellValue(category.userId.toDouble())
            row.createCell(5).setCellValue(category.storeId.toDouble())
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportSubCategoryEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("SubCategoryEntity")
        val subCategories = database.subCategoryDao().getAllSubCategories()
        val headers = listOf("subCatId", "catId", "userId", "storeId", "catName", "subCatName", "quantity", "gsWt", "fnWt")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        subCategories.forEachIndexed { rowIndex, subCategory ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(subCategory.subCatId.toDouble())
            row.createCell(1).setCellValue(subCategory.catId.toDouble())
            row.createCell(2).setCellValue(subCategory.userId.toDouble())
            row.createCell(3).setCellValue(subCategory.storeId.toDouble())
            row.createCell(4).setCellValue(subCategory.catName)
            row.createCell(5).setCellValue(subCategory.subCatName)
            row.createCell(6).setCellValue(subCategory.quantity.toDouble())
            row.createCell(7).setCellValue(subCategory.gsWt)
            row.createCell(8).setCellValue(subCategory.fnWt)
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportItemEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("ItemEntity")
        val items = database.itemDao().getAllItems()
        val headers = listOf(
            "itemId", "itemAddName", "catId", "userId", "storeId", "catName", "subCatId", "subCatName", "entryType", "quantity", "gsWt", "ntWt", "fnWt", "purity", "crgType", "crg", "othCrgDes", "othCrg", "cgst", "sgst", "igst", "huid", "unit", "addDesKey", "addDesValue", "addDate", "modifiedDate", "sellerFirmId", "purchaseOrderId", "purchaseItemId"
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        items.forEachIndexed { rowIndex, item ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(item.itemId.toDouble())
            row.createCell(1).setCellValue(item.itemAddName)
            row.createCell(2).setCellValue(item.catId.toDouble())
            row.createCell(3).setCellValue(item.userId.toDouble())
            row.createCell(4).setCellValue(item.storeId.toDouble())
            row.createCell(5).setCellValue(item.catName)
            row.createCell(6).setCellValue(item.subCatId.toDouble())
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
            row.createCell(27).setCellValue(item.sellerFirmId.toDouble())
            row.createCell(28).setCellValue(item.purchaseOrderId.toDouble())
            row.createCell(29).setCellValue(item.purchaseItemId.toDouble())
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportCustomerEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("CustomerEntity")
        val customers = database.customerDao().getAllCustomersList()
        val headers = listOf("mobileNo", "name", "address", "gstin_pan", "addDate", "lastModifiedDate", "totalItemBought", "totalAmount", "notes", "isActive", "userId", "storeId")
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
            row.createCell(9).setCellValue(customer.isActive)
            row.createCell(10).setCellValue(customer.userId.toDouble())
            row.createCell(11).setCellValue(customer.storeId.toDouble())
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportCustomerKhataBookEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("CustomerKhataBookEntity")
        val khataBooks = database.customerKhataBookDao().getAllKhataBooksList()
        val headers = listOf("khataBookId", "customerMobile", "planName", "startDate", "endDate", "monthlyAmount", "totalMonths", "totalAmount", "status", "notes", "userId", "storeId")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        khataBooks.forEachIndexed { rowIndex, khataBook ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(khataBook.khataBookId.toDouble())
            row.createCell(1).setCellValue(khataBook.customerMobile)
            row.createCell(2).setCellValue(khataBook.planName)
            row.createCell(3).setCellValue(dateFormat.format(khataBook.startDate))
            row.createCell(4).setCellValue(dateFormat.format(khataBook.endDate))
            row.createCell(5).setCellValue(khataBook.monthlyAmount)
            row.createCell(6).setCellValue(khataBook.totalMonths.toDouble())
            row.createCell(7).setCellValue(khataBook.totalAmount)
            row.createCell(8).setCellValue(khataBook.status)
            row.createCell(9).setCellValue(khataBook.notes ?: "")
            row.createCell(10).setCellValue(khataBook.userId.toDouble())
            row.createCell(11).setCellValue(khataBook.storeId.toDouble())
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportCustomerTransactionEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("CustomerTransactionEntity")
        val transactions = database.customerTransactionDao().getAllTransactions()
        val headers = listOf("transactionId", "customerMobile", "transactionDate", "amount", "transactionType", "category", "description", "referenceNumber", "paymentMethod", "khataBookId", "monthNumber", "notes", "userId", "storeId")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        transactions.forEachIndexed { rowIndex, transaction ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(transaction.transactionId.toDouble())
            row.createCell(1).setCellValue(transaction.customerMobile)
            row.createCell(2).setCellValue(dateFormat.format(transaction.transactionDate))
            row.createCell(3).setCellValue(transaction.amount)
            row.createCell(4).setCellValue(transaction.transactionType)
            row.createCell(5).setCellValue(transaction.category)
            row.createCell(6).setCellValue(transaction.description ?: "")
            row.createCell(7).setCellValue(transaction.referenceNumber ?: "")
            row.createCell(8).setCellValue(transaction.paymentMethod ?: "")
            row.createCell(9).setCellValue(transaction.khataBookId?.toDouble() ?: 0.0)
            row.createCell(10).setCellValue(transaction.monthNumber?.toDouble() ?: 0.0)
            row.createCell(11).setCellValue(transaction.notes ?: "")
            row.createCell(12).setCellValue(transaction.userId.toDouble())
            row.createCell(13).setCellValue(transaction.storeId.toDouble())
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportOrderEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("OrderEntity")
        val orders = database.orderDao().getAllOrders()
        val headers = listOf("orderId", "customerMobile", "storeId", "userId", "orderDate", "totalAmount", "totalTax", "totalCharge", "note")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        orders.forEachIndexed { rowIndex, order ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(order.orderId.toDouble())
            row.createCell(1).setCellValue(order.customerMobile)
            row.createCell(2).setCellValue(order.storeId.toDouble())
            row.createCell(3).setCellValue(order.userId.toDouble())
            row.createCell(4).setCellValue(dateFormat.format(order.orderDate))
            row.createCell(5).setCellValue(order.totalAmount)
            row.createCell(6).setCellValue(order.totalTax)
            row.createCell(7).setCellValue(order.totalCharge)
            row.createCell(8).setCellValue(order.note ?: "")
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportOrderItemEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("OrderItemEntity")
        val orderItems = database.orderDao().getAllOrderItems()
        val headers = listOf(
            "orderItemId", "orderId", "orderDate", "itemId", "customerMobile", "catId", "catName", "itemAddName", "subCatId", "subCatName", "entryType", "quantity", "gsWt", "ntWt", "fnWt", "fnMetalPrice", "purity", "crgType", "crg", "othCrgDes", "othCrg", "cgst", "sgst", "igst", "huid", "addDesKey", "addDesValue", "price", "charge", "tax", "sellerFirmId", "purchaseOrderId", "purchaseItemId"
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        orderItems.forEachIndexed { rowIndex, orderItem ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(orderItem.orderItemId.toDouble())
            row.createCell(1).setCellValue(orderItem.orderId.toDouble())
            row.createCell(2).setCellValue(dateFormat.format(orderItem.orderDate))
            row.createCell(3).setCellValue(orderItem.itemId.toDouble())
            row.createCell(4).setCellValue(orderItem.customerMobile)
            row.createCell(5).setCellValue(orderItem.catId.toDouble())
            row.createCell(6).setCellValue(orderItem.catName)
            row.createCell(7).setCellValue(orderItem.itemAddName)
            row.createCell(8).setCellValue(orderItem.subCatId.toDouble())
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
            row.createCell(30).setCellValue(orderItem.sellerFirmId.toDouble())
            row.createCell(31).setCellValue(orderItem.purchaseOrderId.toDouble())
            row.createCell(32).setCellValue(orderItem.purchaseItemId.toDouble())
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportFirmEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("FirmEntity")
        val firms = database.purchaseDao().getAllFirms()
        val headers = listOf("firmId", "firmName", "firmMobileNumber", "gstNumber", "address")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        firms.forEachIndexed { rowIndex, firm ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(firm.firmId.toDouble())
            row.createCell(1).setCellValue(firm.firmName)
            row.createCell(2).setCellValue(firm.firmMobileNumber)
            row.createCell(3).setCellValue(firm.gstNumber)
            row.createCell(4).setCellValue(firm.address)
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportPurchaseOrderEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("PurchaseOrderEntity")
        val purchaseOrders = database.purchaseDao().getAllPurchaseOrders()
        val headers = listOf("purchaseOrderId", "sellerId", "billNo", "billDate", "entryDate", "extraChargeDescription", "extraCharge", "totalFinalWeight", "totalFinalAmount", "notes", "cgstPercent", "sgstPercent", "igstPercent")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        purchaseOrders.forEachIndexed { rowIndex, purchaseOrder ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(purchaseOrder.purchaseOrderId.toDouble())
            row.createCell(1).setCellValue(purchaseOrder.sellerId.toDouble())
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
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportPurchaseOrderItemEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("PurchaseOrderItemEntity")
        val purchaseOrderItems = database.purchaseDao().getAllPurchaseOrderItems()
        val headers = listOf("purchaseItemId", "purchaseOrderId", "catId", "catName", "subCatId", "subCatName", "gsWt", "purity", "ntWt", "fnWt", "fnRate", "wastagePercent")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        purchaseOrderItems.forEachIndexed { rowIndex, item ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(item.purchaseItemId.toDouble())
            row.createCell(1).setCellValue(item.purchaseOrderId.toDouble())
            row.createCell(2).setCellValue(item.catId.toDouble())
            row.createCell(3).setCellValue(item.catName)
            row.createCell(4).setCellValue(item.subCatId.toDouble())
            row.createCell(5).setCellValue(item.subCatName)
            row.createCell(6).setCellValue(item.gsWt)
            row.createCell(7).setCellValue(item.purity)
            row.createCell(8).setCellValue(item.ntWt)
            row.createCell(9).setCellValue(item.fnWt)
            row.createCell(10).setCellValue(item.fnRate)
            row.createCell(11).setCellValue(item.wastagePercent)
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
    
    private suspend fun exportMetalExchangeEntity(database: AppDatabase, workbook: Workbook, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("MetalExchangeEntity")
        val metalExchanges = database.purchaseDao().getAllMetalExchanges()
        val headers = listOf("exchangeId", "purchaseOrderId", "catId", "catName", "subCatId", "subCatName", "fnWeight")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        metalExchanges.forEachIndexed { rowIndex, metalExchange ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(metalExchange.exchangeId.toDouble())
            row.createCell(1).setCellValue(metalExchange.purchaseOrderId.toDouble())
            row.createCell(2).setCellValue(metalExchange.catId.toDouble())
            row.createCell(3).setCellValue(metalExchange.catName)
            row.createCell(4).setCellValue(metalExchange.subCatId.toDouble())
            row.createCell(5).setCellValue(metalExchange.subCatName)
            row.createCell(6).setCellValue(metalExchange.fnWeight)
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
}