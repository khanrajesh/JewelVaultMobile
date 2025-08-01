/*
package com.velox.jewelvault.utils.backup

import android.content.Context
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.*
import com.velox.jewelvault.data.roomdb.entity.customer.*
import com.velox.jewelvault.data.roomdb.entity.order.*
import com.velox.jewelvault.data.roomdb.entity.purchase.*
import com.velox.jewelvault.utils.log
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

*/
/**
 * Handles importing data from Excel format to RoomDB entities
 *//*

class ExcelImporter(
    private val context: Context,
    private val database: AppDatabase
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    // --- Constants for column indexes ---
    private object UserCols { const val ID = 0; const val NAME = 1; const val EMAIL = 2; const val MOBILENO = 3; const val TOKEN = 4; const val PIN = 5 }
    private object StoreCols { const val STOREID = 0; const val USERID = 1; const val PROPRIETOR = 2; const val NAME = 3; const val EMAIL = 4; const val PHONE = 5; const val ADDRESS = 6; const val REGNO = 7; const val GSTIN = 8; const val PAN = 9; const val IMAGE = 10; const val INVOICENO = 11; const val UPIID = 12 }
    private object CategoryCols { const val CATID = 0; const val CATNAME = 1; const val GSWt = 2; const val FNWt = 3; const val USERID = 4; const val STOREID = 5 }
    private object SubCategoryCols { const val SUBCATID = 0; const val CATID = 1; const val USERID = 2; const val STOREID = 3; const val CATNAME = 4; const val SUBCATNAME = 5; const val QUANTITY = 6; const val GSWt = 7; const val FNWt = 8 }
    private object ItemCols { const val ITEMID = 0; const val ITEMADDNAME = 1; const val CATID = 2; const val USERID = 3; const val STOREID = 4; const val CATNAME = 5; const val SUBCATID = 6; const val SUBCATNAME = 7; const val ENTRYTYPE = 8; const val QUANTITY = 9; const val GSWt = 10; const val NTWt = 11; const val FNWt = 12; const val PURITY = 13; const val CRGTYPE = 14; const val CRG = 15; const val OTHCRGDES = 16; const val OTHCRG = 17; const val CGST = 18; const val SGST = 19; const val IGST = 20; const val HUID = 21; const val UNIT = 22; const val ADDDESKEY = 23; const val ADDDESVAL = 24; const val ADDDATE = 25; const val MODIFIEDDATE = 26; const val SELLERFIRMID = 27; const val PURCHASEORDERID = 28; const val PURCHASEITEMID = 29 }
    private object CustomerCols { const val MOBILENO = 0; const val NAME = 1; const val ADDRESS = 2; const val GSTINPAN = 3; const val ADDDATE = 4; const val LASTMODIFIED = 5; const val TOTALITEMBOUGHT = 6; const val TOTALAMOUNT = 7; const val NOTES = 8; const val ISACTIVE = 9; const val USERID = 10; const val STOREID = 11 }
    private object KhataBookCols { const val KHATABOOKID = 0; const val CUSTOMERMOBILE = 1; const val PLANNAME = 2; const val STARTDATE = 3; const val ENDDATE = 4; const val MONTHLYAMOUNT = 5; const val TOTALMONTHS = 6; const val TOTALAMOUNT = 7; const val STATUS = 8; const val NOTES = 9; const val USERID = 10; const val STOREID = 11 }
    private object TransactionCols { const val TRANSACTIONID = 0; const val CUSTOMERMOBILE = 1; const val TRANSACTIONDATE = 2; const val AMOUNT = 3; const val TRANSACTIONTYPE = 4; const val CATEGORY = 5; const val DESCRIPTION = 6; const val REFERENCENUMBER = 7; const val PAYMENTMETHOD = 8; const val KHATABOOKID = 9; const val MONTHNUMBER = 10; const val NOTES = 11; const val USERID = 12; const val STOREID = 13 }
    private object OrderCols { const val ORDERID = 0; const val CUSTOMERMOBILE = 1; const val STOREID = 2; const val USERID = 3; const val ORDERDATE = 4; const val TOTALAMOUNT = 5; const val TOTALTAX = 6; const val TOTALCHARGE = 7; const val NOTE = 8 }
    private object OrderItemCols { const val ORDERITEMID = 0; const val ORDERID = 1; const val ORDERDATE = 2; const val ITEMID = 3; const val CUSTOMERMOBILE = 4; const val CATID = 5; const val CATNAME = 6; const val ITEMADDNAME = 7; const val SUBCATID = 8; const val SUBCATNAME = 9; const val ENTRYTYPE = 10; const val QUANTITY = 11; const val GSWt = 12; const val NTWt = 13; const val FNWt = 14; const val FNMETALPRICE = 15; const val PURITY = 16; const val CRGTYPE = 17; const val CRG = 18; const val OTHCRGDES = 19; const val OTHCRG = 20; const val CGST = 21; const val SGST = 22; const val IGST = 23; const val HUID = 24; const val ADDDESKEY = 25; const val ADDDESVAL = 26; const val PRICE = 27; const val CHARGE = 28; const val TAX = 29; const val SELLERFIRMID = 30; const val PURCHASEORDERID = 31; const val PURCHASEITEMID = 32 }
    private object FirmCols { const val FIRMID = 0; const val FIRMNAME = 1; const val FIRMMOBILENUMBER = 2; const val GSTNUMBER = 3; const val ADDRESS = 4 }
    private object PurchaseOrderCols { const val PURCHASEORDERID = 0; const val SELLERID = 1; const val BILLNO = 2; const val BILLDATE = 3; const val ENTRYDATE = 4; const val EXTRACHARGEDESC = 5; const val EXTRACHARGE = 6; const val TOTALFINALWT = 7; const val TOTALFINALAMT = 8; const val NOTES = 9; const val CGSTPERCENT = 10; const val SGSTPERCENT = 11; const val IGSTPERCENT = 12 }
    private object PurchaseOrderItemCols { const val PURCHASEITEMID = 0; const val PURCHASEORDERID = 1; const val CATID = 2; const val CATNAME = 3; const val SUBCATID = 4; const val SUBCATNAME = 5; const val GSWt = 6; const val PURITY = 7; const val NTWt = 8; const val FNWt = 9; const val FNRATE = 10; const val WASTAGEPERCENT = 11 }
    private object MetalExchangeCols { const val EXCHANGEID = 0; const val PURCHASEORDERID = 1; const val CATID = 2; const val CATNAME = 3; const val SUBCATID = 4; const val SUBCATNAME = 5; const val FNWEIGHT = 6 }
    
    */
/**
     * Import all entities from Excel file with multiple sheets
     *//*

    suspend fun importAllEntitiesFromExcel(
        inputFile: File,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<Unit> {
        return try {
            onProgress("Reading Excel file...", 65)
            
            val workbook = XSSFWorkbook(FileInputStream(inputFile))
            
            // Import each entity from its sheet (order matters due to foreign key constraints)
            onProgress("Importing users...", 70)
            importUsersEntity(workbook)
            
            onProgress("Importing stores...", 72)
            importStoreEntity(workbook)
            
            onProgress("Importing categories...", 74)
            importCategoryEntity(workbook)
            
            onProgress("Importing subcategories...", 76)
            importSubCategoryEntity(workbook)
            
            onProgress("Importing items...", 78)
            importItemEntity(workbook)
            
            onProgress("Importing customers...", 80)
            importCustomerEntity(workbook)
            
            onProgress("Importing khata books...", 82)
            importCustomerKhataBookEntity(workbook)
            
            onProgress("Importing transactions...", 84)
            importCustomerTransactionEntity(workbook)
            
            onProgress("Importing orders...", 86)
            importOrderEntity(workbook)
            
            onProgress("Importing order items...", 88)
            importOrderItemEntity(workbook)
            
            onProgress("Importing firms...", 90)
            importFirmEntity(workbook)
            
            onProgress("Importing purchase orders...", 92)
            importPurchaseOrderEntity(workbook)
            
            onProgress("Importing purchase order items...", 94)
            importPurchaseOrderItemEntity(workbook)
            
            onProgress("Importing metal exchanges...", 96)
            importMetalExchangeEntity(workbook)
            
            workbook.close()
            
            log("Excel import completed successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            log("Excel import failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun importUsersEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("UsersEntity") ?: return
        val existingUsers = database.userDao().getAllUsers().associateBy { getCellValueAsString(it.mobileNo) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val mobileNo = getCellValueAsString(row.getCell(UserCols.MOBILENO))
                if (existingUsers.containsKey(mobileNo)) continue
                val user = UsersEntity(
                    id = getCellValueAsInt(row.getCell(UserCols.ID)),
                    name = getCellValueAsString(row.getCell(UserCols.NAME)),
                    email = getCellValueAsString(row.getCell(UserCols.EMAIL)),
                    mobileNo = mobileNo,
                    token = getCellValueAsString(row.getCell(UserCols.TOKEN)),
                    pin = getCellValueAsString(row.getCell(UserCols.PIN))
                )
                database.userDao().insertUser(user)
            } catch (e: Exception) {
                log("Error importing user at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importStoreEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("StoreEntity") ?: return
        val existingStores = database.storeDao().getAllStores().associateBy { "${getCellValueAsString(it.userId)}_${getCellValueAsString(it.name)}" }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val userId = getCellValueAsInt(row.getCell(StoreCols.USERID))
                val name = getCellValueAsString(row.getCell(StoreCols.NAME))
                val key = "${userId}_${name}"
                if (existingStores.containsKey(key)) continue
                val store = StoreEntity(
                    storeId = getCellValueAsInt(row.getCell(StoreCols.STOREID)),
                    userId = userId,
                    proprietor = getCellValueAsString(row.getCell(StoreCols.PROPRIETOR)),
                    name = name,
                    email = getCellValueAsString(row.getCell(StoreCols.EMAIL)),
                    phone = getCellValueAsString(row.getCell(StoreCols.PHONE)),
                    address = getCellValueAsString(row.getCell(StoreCols.ADDRESS)),
                    registrationNo = getCellValueAsString(row.getCell(StoreCols.REGNO)),
                    gstinNo = getCellValueAsString(row.getCell(StoreCols.GSTIN)),
                    panNo = getCellValueAsString(row.getCell(StoreCols.PAN)),
                    image = getCellValueAsString(row.getCell(StoreCols.IMAGE)),
                    invoiceNo = getCellValueAsInt(row.getCell(StoreCols.INVOICENO)),
                    upiId = getCellValueAsString(row.getCell(StoreCols.UPIID))
                )
                database.storeDao().insertStore(store)
            } catch (e: Exception) {
                log("Error importing store at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importCategoryEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("CategoryEntity") ?: return
        val existingCategories = database.categoryDao().getAllCategories().associateBy { "${getCellValueAsString(it.userId)}_${getCellValueAsString(it.storeId)}_${getCellValueAsString(it.catName)}" }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val userId = getCellValueAsInt(row.getCell(CategoryCols.USERID))
                val storeId = getCellValueAsInt(row.getCell(CategoryCols.STOREID))
                val catName = getCellValueAsString(row.getCell(CategoryCols.CATNAME))
                val key = "${userId}_${storeId}_${catName}"
                if (existingCategories.containsKey(key)) continue
                val category = CategoryEntity(
                    catId = getCellValueAsInt(row.getCell(CategoryCols.CATID)),
                    catName = catName,
                    gsWt = getCellValueAsDouble(row.getCell(CategoryCols.GSWt)),
                    fnWt = getCellValueAsDouble(row.getCell(CategoryCols.FNWt)),
                    userId = userId,
                    storeId = storeId
                )
                database.categoryDao().insertCategory(category)
            } catch (e: Exception) {
                log("Error importing category at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importSubCategoryEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("SubCategoryEntity") ?: return
        val existingSubCategories = database.subCategoryDao().getAllSubCategories().associateBy { "${getCellValueAsString(it.catId)}_${getCellValueAsString(it.userId)}_${getCellValueAsString(it.storeId)}_${getCellValueAsString(it.subCatName)}" }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val catId = getCellValueAsInt(row.getCell(SubCategoryCols.CATID))
                val userId = getCellValueAsInt(row.getCell(SubCategoryCols.USERID))
                val storeId = getCellValueAsInt(row.getCell(SubCategoryCols.STOREID))
                val subCatName = getCellValueAsString(row.getCell(SubCategoryCols.SUBCATNAME))
                val key = "${catId}_${userId}_${storeId}_${subCatName}"
                if (existingSubCategories.containsKey(key)) continue
                val subCategory = SubCategoryEntity(
                    subCatId = getCellValueAsInt(row.getCell(SubCategoryCols.SUBCATID)),
                    catId = catId,
                    userId = userId,
                    storeId = storeId,
                    catName = getCellValueAsString(row.getCell(SubCategoryCols.CATNAME)),
                    subCatName = subCatName,
                    quantity = getCellValueAsInt(row.getCell(SubCategoryCols.QUANTITY)),
                    gsWt = getCellValueAsDouble(row.getCell(SubCategoryCols.GSWt)),
                    fnWt = getCellValueAsDouble(row.getCell(SubCategoryCols.FNWt))
                )
                database.subCategoryDao().insertSubCategory(subCategory)
            } catch (e: Exception) {
                log("Error importing subcategory at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importItemEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("ItemEntity") ?: return
        val existingItems = database.itemDao().getAllItems().associateBy { "${getCellValueAsString(it.userId)}_${getCellValueAsString(it.storeId)}_${getCellValueAsString(it.itemAddName)}" }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val userId = getCellValueAsInt(row.getCell(ItemCols.USERID))
                val storeId = getCellValueAsInt(row.getCell(ItemCols.STOREID))
                val itemAddName = getCellValueAsString(row.getCell(ItemCols.ITEMADDNAME))
                val key = "${userId}_${storeId}_${itemAddName}"
                if (existingItems.containsKey(key)) continue
                val item = ItemEntity(
                    itemId = getCellValueAsInt(row.getCell(ItemCols.ITEMID)),
                    itemAddName = itemAddName,
                    catId = getCellValueAsInt(row.getCell(ItemCols.CATID)),
                    userId = userId,
                    storeId = storeId,
                    catName = getCellValueAsString(row.getCell(ItemCols.CATNAME)),
                    subCatId = getCellValueAsInt(row.getCell(ItemCols.SUBCATID)),
                    subCatName = getCellValueAsString(row.getCell(ItemCols.SUBCATNAME)),
                    entryType = getCellValueAsString(row.getCell(ItemCols.ENTRYTYPE)),
                    quantity = getCellValueAsInt(row.getCell(ItemCols.QUANTITY)),
                    gsWt = getCellValueAsDouble(row.getCell(ItemCols.GSWt)),
                    ntWt = getCellValueAsDouble(row.getCell(ItemCols.NTWt)),
                    fnWt = getCellValueAsDouble(row.getCell(ItemCols.FNWt)),
                    purity = getCellValueAsString(row.getCell(ItemCols.PURITY)),
                    crgType = getCellValueAsString(row.getCell(ItemCols.CRGTYPE)),
                    crg = getCellValueAsDouble(row.getCell(ItemCols.CRG)),
                    othCrgDes = getCellValueAsString(row.getCell(ItemCols.OTHCRGDES)),
                    othCrg = getCellValueAsDouble(row.getCell(ItemCols.OTHCRG)),
                    cgst = getCellValueAsDouble(row.getCell(ItemCols.CGST)),
                    sgst = getCellValueAsDouble(row.getCell(ItemCols.SGST)),
                    igst = getCellValueAsDouble(row.getCell(ItemCols.IGST)),
                    huid = getCellValueAsString(row.getCell(ItemCols.HUID)),
                    unit = getCellValueAsString(row.getCell(ItemCols.UNIT)),
                    addDesKey = getCellValueAsString(row.getCell(ItemCols.ADDDESKEY)),
                    addDesValue = getCellValueAsString(row.getCell(ItemCols.ADDDESVAL)),
                    addDate = parseDateString(getCellValueAsString(row.getCell(ItemCols.ADDDATE))),
                    modifiedDate = parseDateString(getCellValueAsString(row.getCell(ItemCols.MODIFIEDDATE))),
                    sellerFirmId = getCellValueAsInt(row.getCell(ItemCols.SELLERFIRMID)),
                    purchaseOrderId = getCellValueAsInt(row.getCell(ItemCols.PURCHASEORDERID)),
                    purchaseItemId = getCellValueAsInt(row.getCell(ItemCols.PURCHASEITEMID))
                )
                database.itemDao().insertItem(item)
            } catch (e: Exception) {
                log("Error importing item at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importCustomerEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("CustomerEntity") ?: return
        val existingCustomers = database.customerDao().getAllCustomers().associateBy { getCellValueAsString(it.mobileNo) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val mobileNo = getCellValueAsString(row.getCell(CustomerCols.MOBILENO))
                if (existingCustomers.containsKey(mobileNo)) continue
                val customer = CustomerEntity(
                    mobileNo = mobileNo,
                    name = getCellValueAsString(row.getCell(CustomerCols.NAME)),
                    address = getCellValueAsString(row.getCell(CustomerCols.ADDRESS)),
                    gstin_pan = getCellValueAsString(row.getCell(CustomerCols.GSTINPAN)),
                    addDate = parseDateString(getCellValueAsString(row.getCell(CustomerCols.ADDDATE))),
                    lastModifiedDate = parseDateString(getCellValueAsString(row.getCell(CustomerCols.LASTMODIFIED))),
                    totalItemBought = getCellValueAsInt(row.getCell(CustomerCols.TOTALITEMBOUGHT)),
                    totalAmount = getCellValueAsDouble(row.getCell(CustomerCols.TOTALAMOUNT)),
                    notes = getCellValueAsString(row.getCell(CustomerCols.NOTES)),
                    isActive = getCellValueAsBoolean(row.getCell(CustomerCols.ISACTIVE)),
                    userId = getCellValueAsInt(row.getCell(CustomerCols.USERID)),
                    storeId = getCellValueAsInt(row.getCell(CustomerCols.STOREID))
                )
                database.customerDao().insertCustomer(customer)
            } catch (e: Exception) {
                log("Error importing customer at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importCustomerKhataBookEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("CustomerKhataBookEntity") ?: return
        val existingKhataBooks = database.customerKhataBookDao().getAllKhataBooks().associateBy { getCellValueAsString(it.khataBookId) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val khataBookId = getCellValueAsInt(row.getCell(KhataBookCols.KHATABOOKID))
                if (existingKhataBooks.containsKey(khataBookId)) continue
                val khataBook = CustomerKhataBookEntity(
                    khataBookId = khataBookId,
                    customerMobile = getCellValueAsString(row.getCell(KhataBookCols.CUSTOMERMOBILE)),
                    planName = getCellValueAsString(row.getCell(KhataBookCols.PLANNAME)),
                    startDate = parseDateString(getCellValueAsString(row.getCell(KhataBookCols.STARTDATE))),
                    endDate = parseDateString(getCellValueAsString(row.getCell(KhataBookCols.ENDDATE))),
                    monthlyAmount = getCellValueAsDouble(row.getCell(KhataBookCols.MONTHLYAMOUNT)),
                    totalMonths = getCellValueAsInt(row.getCell(KhataBookCols.TOTALMONTHS)),
                    totalAmount = getCellValueAsDouble(row.getCell(KhataBookCols.TOTALAMOUNT)),
                    status = getCellValueAsString(row.getCell(KhataBookCols.STATUS)),
                    notes = getCellValueAsString(row.getCell(KhataBookCols.NOTES)),
                    userId = getCellValueAsInt(row.getCell(KhataBookCols.USERID)),
                    storeId = getCellValueAsInt(row.getCell(KhataBookCols.STOREID))
                )
                database.customerKhataBookDao().insertKhataBook(khataBook)
            } catch (e: Exception) {
                log("Error importing khata book at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importCustomerTransactionEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("CustomerTransactionEntity") ?: return
        val existingTransactions = database.customerTransactionDao().getAllTransactions().associateBy { getCellValueAsString(it.transactionId) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val transactionId = getCellValueAsInt(row.getCell(TransactionCols.TRANSACTIONID))
                if (existingTransactions.containsKey(transactionId)) continue
                val transaction = CustomerTransactionEntity(
                    transactionId = transactionId,
                    customerMobile = getCellValueAsString(row.getCell(TransactionCols.CUSTOMERMOBILE)),
                    transactionDate = parseDateString(getCellValueAsString(row.getCell(TransactionCols.TRANSACTIONDATE))),
                    amount = getCellValueAsDouble(row.getCell(TransactionCols.AMOUNT)),
                    transactionType = getCellValueAsString(row.getCell(TransactionCols.TRANSACTIONTYPE)),
                    category = getCellValueAsString(row.getCell(TransactionCols.CATEGORY)),
                    description = getCellValueAsString(row.getCell(TransactionCols.DESCRIPTION)),
                    referenceNumber = getCellValueAsString(row.getCell(TransactionCols.REFERENCENUMBER)),
                    paymentMethod = getCellValueAsString(row.getCell(TransactionCols.PAYMENTMETHOD)),
                    khataBookId = getCellValueAsInt(row.getCell(TransactionCols.KHATABOOKID)).takeIf { it > 0 },
                    monthNumber = getCellValueAsInt(row.getCell(TransactionCols.MONTHNUMBER)).takeIf { it > 0 },
                    notes = getCellValueAsString(row.getCell(TransactionCols.NOTES)),
                    userId = getCellValueAsInt(row.getCell(TransactionCols.USERID)),
                    storeId = getCellValueAsInt(row.getCell(TransactionCols.STOREID))
                )
                database.customerTransactionDao().insertTransaction(transaction)
            } catch (e: Exception) {
                log("Error importing transaction at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importOrderEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("OrderEntity") ?: return
        val existingOrders = database.orderDao().getAllOrders().associateBy { getCellValueAsString(it.orderId) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val orderId = getCellValueAsInt(row.getCell(OrderCols.ORDERID))
                if (existingOrders.containsKey(orderId)) continue
                val order = OrderEntity(
                    orderId = orderId,
                    customerMobile = getCellValueAsString(row.getCell(OrderCols.CUSTOMERMOBILE)),
                    storeId = getCellValueAsInt(row.getCell(OrderCols.STOREID)),
                    userId = getCellValueAsInt(row.getCell(OrderCols.USERID)),
                    orderDate = parseDateString(getCellValueAsString(row.getCell(OrderCols.ORDERDATE))),
                    totalAmount = getCellValueAsDouble(row.getCell(OrderCols.TOTALAMOUNT)),
                    totalTax = getCellValueAsDouble(row.getCell(OrderCols.TOTALTAX)),
                    totalCharge = getCellValueAsDouble(row.getCell(OrderCols.TOTALCHARGE)),
                    note = getCellValueAsString(row.getCell(OrderCols.NOTE))
                )
                database.orderDao().insertOrder(order)
            } catch (e: Exception) {
                log("Error importing order at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importOrderItemEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("OrderItemEntity") ?: return
        val existingOrderItems = database.orderDao().getAllOrderItems().associateBy { getCellValueAsString(it.orderItemId) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val orderItemId = getCellValueAsInt(row.getCell(OrderItemCols.ORDERITEMID))
                if (existingOrderItems.containsKey(orderItemId)) continue
                val orderItem = OrderItemEntity(
                    orderItemId = orderItemId,
                    orderId = getCellValueAsInt(row.getCell(OrderItemCols.ORDERID)),
                    orderDate = parseDateString(getCellValueAsString(row.getCell(OrderItemCols.ORDERDATE))),
                    itemId = getCellValueAsInt(row.getCell(OrderItemCols.ITEMID)),
                    customerMobile = getCellValueAsString(row.getCell(OrderItemCols.CUSTOMERMOBILE)),
                    catId = getCellValueAsInt(row.getCell(OrderItemCols.CATID)),
                    catName = getCellValueAsString(row.getCell(OrderItemCols.CATNAME)),
                    itemAddName = getCellValueAsString(row.getCell(OrderItemCols.ITEMADDNAME)),
                    subCatId = getCellValueAsInt(row.getCell(OrderItemCols.SUBCATID)),
                    subCatName = getCellValueAsString(row.getCell(OrderItemCols.SUBCATNAME)),
                    entryType = getCellValueAsString(row.getCell(OrderItemCols.ENTRYTYPE)),
                    quantity = getCellValueAsInt(row.getCell(OrderItemCols.QUANTITY)),
                    gsWt = getCellValueAsDouble(row.getCell(OrderItemCols.GSWt)),
                    ntWt = getCellValueAsDouble(row.getCell(OrderItemCols.NTWt)),
                    fnWt = getCellValueAsDouble(row.getCell(OrderItemCols.FNWt)),
                    fnMetalPrice = getCellValueAsDouble(row.getCell(OrderItemCols.FNMETALPRICE)),
                    purity = getCellValueAsString(row.getCell(OrderItemCols.PURITY)),
                    crgType = getCellValueAsString(row.getCell(OrderItemCols.CRGTYPE)),
                    crg = getCellValueAsDouble(row.getCell(OrderItemCols.CRG)),
                    othCrgDes = getCellValueAsString(row.getCell(OrderItemCols.OTHCRGDES)),
                    othCrg = getCellValueAsDouble(row.getCell(OrderItemCols.OTHCRG)),
                    cgst = getCellValueAsDouble(row.getCell(OrderItemCols.CGST)),
                    sgst = getCellValueAsDouble(row.getCell(OrderItemCols.SGST)),
                    igst = getCellValueAsDouble(row.getCell(OrderItemCols.IGST)),
                    huid = getCellValueAsString(row.getCell(OrderItemCols.HUID)),
                    addDesKey = getCellValueAsString(row.getCell(OrderItemCols.ADDDESKEY)),
                    addDesValue = getCellValueAsString(row.getCell(OrderItemCols.ADDDESVAL)),
                    price = getCellValueAsDouble(row.getCell(OrderItemCols.PRICE)),
                    charge = getCellValueAsDouble(row.getCell(OrderItemCols.CHARGE)),
                    tax = getCellValueAsDouble(row.getCell(OrderItemCols.TAX)),
                    sellerFirmId = getCellValueAsInt(row.getCell(OrderItemCols.SELLERFIRMID)),
                    purchaseOrderId = getCellValueAsInt(row.getCell(OrderItemCols.PURCHASEORDERID)),
                    purchaseItemId = getCellValueAsInt(row.getCell(OrderItemCols.PURCHASEITEMID))
                )
                database.orderDao().insertOrderItem(orderItem)
            } catch (e: Exception) {
                log("Error importing order item at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importFirmEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("FirmEntity") ?: return
        val existingFirms = database.purchaseDao().getAllFirms().associateBy { getCellValueAsString(it.firmName) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val firmName = getCellValueAsString(row.getCell(FirmCols.FIRMNAME))
                if (existingFirms.containsKey(firmName)) continue
                val firm = FirmEntity(
                    firmId = getCellValueAsInt(row.getCell(FirmCols.FIRMID)),
                    firmName = firmName,
                    firmMobileNumber = getCellValueAsString(row.getCell(FirmCols.FIRMMOBILENUMBER)),
                    gstNumber = getCellValueAsString(row.getCell(FirmCols.GSTNUMBER)),
                    address = getCellValueAsString(row.getCell(FirmCols.ADDRESS))
                )
                database.purchaseDao().insertFirm(firm)
            } catch (e: Exception) {
                log("Error importing firm at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importPurchaseOrderEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("PurchaseOrderEntity") ?: return
        val existingPurchaseOrders = database.purchaseDao().getAllPurchaseOrders().associateBy { getCellValueAsString(it.purchaseOrderId) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val purchaseOrderId = getCellValueAsInt(row.getCell(PurchaseOrderCols.PURCHASEORDERID))
                if (existingPurchaseOrders.containsKey(purchaseOrderId)) continue
                val purchaseOrder = PurchaseOrderEntity(
                    purchaseOrderId = purchaseOrderId,
                    sellerId = getCellValueAsInt(row.getCell(PurchaseOrderCols.SELLERID)),
                    billNo = getCellValueAsString(row.getCell(PurchaseOrderCols.BILLNO)),
                    billDate = getCellValueAsString(row.getCell(PurchaseOrderCols.BILLDATE)),
                    entryDate = getCellValueAsString(row.getCell(PurchaseOrderCols.ENTRYDATE)),
                    extraChargeDescription = getCellValueAsString(row.getCell(PurchaseOrderCols.EXTRACHARGEDESC)),
                    extraCharge = getCellValueAsDouble(row.getCell(PurchaseOrderCols.EXTRACHARGE)),
                    totalFinalWeight = getCellValueAsDouble(row.getCell(PurchaseOrderCols.TOTALFINALWT)),
                    totalFinalAmount = getCellValueAsDouble(row.getCell(PurchaseOrderCols.TOTALFINALAMT)),
                    notes = getCellValueAsString(row.getCell(PurchaseOrderCols.NOTES)),
                    cgstPercent = getCellValueAsDouble(row.getCell(PurchaseOrderCols.CGSTPERCENT)),
                    sgstPercent = getCellValueAsDouble(row.getCell(PurchaseOrderCols.SGSTPERCENT)),
                    igstPercent = getCellValueAsDouble(row.getCell(PurchaseOrderCols.IGSTPERCENT))
                )
                database.purchaseDao().insertPurchaseOrder(purchaseOrder)
            } catch (e: Exception) {
                log("Error importing purchase order at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importPurchaseOrderItemEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("PurchaseOrderItemEntity") ?: return
        val existingPurchaseOrderItems = database.purchaseDao().getAllPurchaseOrderItems().associateBy { getCellValueAsString(it.purchaseItemId) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val purchaseItemId = getCellValueAsInt(row.getCell(PurchaseOrderItemCols.PURCHASEITEMID))
                if (existingPurchaseOrderItems.containsKey(purchaseItemId)) continue
                val purchaseOrderItem = PurchaseOrderItemEntity(
                    purchaseItemId = purchaseItemId,
                    purchaseOrderId = getCellValueAsInt(row.getCell(PurchaseOrderItemCols.PURCHASEORDERID)),
                    catId = getCellValueAsInt(row.getCell(PurchaseOrderItemCols.CATID)),
                    catName = getCellValueAsString(row.getCell(PurchaseOrderItemCols.CATNAME)),
                    subCatId = getCellValueAsInt(row.getCell(PurchaseOrderItemCols.SUBCATID)),
                    subCatName = getCellValueAsString(row.getCell(PurchaseOrderItemCols.SUBCATNAME)),
                    gsWt = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.GSWt)),
                    purity = getCellValueAsString(row.getCell(PurchaseOrderItemCols.PURITY)),
                    ntWt = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.NTWt)),
                    fnWt = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.FNWt)),
                    fnRate = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.FNRATE)),
                    wastagePercent = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.WASTAGEPERCENT))
                )
                database.purchaseDao().insertPurchaseOrderItem(purchaseOrderItem)
            } catch (e: Exception) {
                log("Error importing purchase order item at row $rowIndex: ${e.message}")
            }
        }
    }
    
    private suspend fun importMetalExchangeEntity(workbook: Workbook) {
        val sheet = workbook.getSheet("MetalExchangeEntity") ?: return
        val existingMetalExchanges = database.purchaseDao().getAllMetalExchanges().associateBy { getCellValueAsString(it.exchangeId) }
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val exchangeId = getCellValueAsInt(row.getCell(MetalExchangeCols.EXCHANGEID))
                if (existingMetalExchanges.containsKey(exchangeId)) continue
                val metalExchange = MetalExchangeEntity(
                    exchangeId = exchangeId,
                    purchaseOrderId = getCellValueAsInt(row.getCell(MetalExchangeCols.PURCHASEORDERID)),
                    catId = getCellValueAsInt(row.getCell(MetalExchangeCols.CATID)),
                    catName = getCellValueAsString(row.getCell(MetalExchangeCols.CATNAME)),
                    subCatId = getCellValueAsInt(row.getCell(MetalExchangeCols.SUBCATID)),
                    subCatName = getCellValueAsString(row.getCell(MetalExchangeCols.SUBCATNAME)),
                    fnWeight = getCellValueAsDouble(row.getCell(MetalExchangeCols.FNWEIGHT))
                )
                database.purchaseDao().insertMetalExchange(metalExchange)
            } catch (e: Exception) {
                log("Error importing metal exchange at row $rowIndex: ${e.message}")
            }
        }
    }
    
    // Helper functions for cell value extraction
    private fun getCellValueAsString(cell: Cell?): String {
        return when (cell?.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }
    
    private fun getCellValueAsInt(cell: Cell?): Int {
        return when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toInt()
            CellType.STRING -> cell.stringCellValue.toIntOrNull() ?: 0
            else -> 0
        }
    }
    
    private fun getCellValueAsDouble(cell: Cell?): Double {
        return when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }
    
    private fun getCellValueAsBoolean(cell: Cell?): Boolean {
        return when (cell?.cellType) {
            CellType.BOOLEAN -> cell.booleanCellValue
            CellType.STRING -> cell.stringCellValue.toBoolean()
            else -> false
        }
    }
    
    private fun parseDateString(dateString: String): Long {
        return try {
            dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}*/
