package com.velox.jewelvault.utils.backup

import android.content.Context
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.*
import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.entity.customer.*
import com.velox.jewelvault.data.roomdb.entity.order.*
import com.velox.jewelvault.data.roomdb.entity.purchase.*
import com.velox.jewelvault.data.roomdb.entity.users.UserAdditionalInfoEntity
import com.velox.jewelvault.data.roomdb.entity.users.UsersEntity
import com.velox.jewelvault.utils.log
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles importing data from Excel format to RoomDB entities with smart conflict resolution
 */
class ExcelImporter(
    private val context: Context,
    private val database: AppDatabase
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    // --- Constants for column indexes ---
    private object UserCols { const val ID = 0; const val NAME = 1; const val EMAIL = 2; const val MOBILENO = 3; const val TOKEN = 4; const val PIN = 5; const val ROLE = 6 }
    private object StoreCols { const val STOREID = 0; const val USERID = 1; const val PROPRIETOR = 2; const val NAME = 3; const val EMAIL = 4; const val PHONE = 5; const val ADDRESS = 6; const val REGNO = 7; const val GSTIN = 8; const val PAN = 9; const val IMAGE = 10; const val INVOICENO = 11; const val UPIID = 12 }
    private object CategoryCols { const val CATID = 0; const val CATNAME = 1; const val GSWt = 2; const val FNWt = 3; const val USERID = 4; const val STOREID = 5 }
    private object SubCategoryCols { const val SUBCATID = 0; const val CATID = 1; const val USERID = 2; const val STOREID = 3; const val CATNAME = 4; const val SUBCATNAME = 5; const val QUANTITY = 6; const val GSWt = 7; const val FNWt = 8 }
    private object ItemCols { const val ITEMID = 0; const val ITEMADDNAME = 1; const val CATID = 2; const val USERID = 3; const val STOREID = 4; const val CATNAME = 5; const val SUBCATID = 6; const val SUBCATNAME = 7; const val ENTRYTYPE = 8; const val QUANTITY = 9; const val GSWt = 10; const val NTWt = 11; const val FNWt = 12; const val PURITY = 13; const val CRGTYPE = 14; const val CRG = 15; const val OTHCRGDES = 16; const val OTHCRG = 17; const val CGST = 18; const val SGST = 19; const val IGST = 20; const val HUID = 21; const val UNIT = 22; const val ADDDESKEY = 23; const val ADDDESVALUE = 24; const val ADDDATE = 25; const val MODIFIEDDATE = 26; const val SELLERFIRMID = 27; const val PURCHASEORDERID = 28; const val PURCHASEITEMID = 29 }
    private object CustomerCols { const val MOBILENO = 0; const val NAME = 1; const val ADDRESS = 2; const val GSTINPAN = 3; const val ADDDATE = 4; const val LASTMODIFIED = 5; const val TOTALITEMBOUGHT = 6; const val TOTALAMOUNT = 7; const val NOTES = 8;  const val USERID = 9; const val STOREID = 10 }
    private object KhataBookPlanCols { const val PLANID = 0; const val NAME = 1; const val PAYMONTHS = 2; const val BENEFITMONTHS = 3; const val DESCRIPTION = 4; const val BENEFITPERCENTAGE = 5; const val USERID = 6; const val STOREID = 7; const val CREATEDAT = 8; const val UPDATEDAT = 9 }
    private object KhataBookCols { const val KHATABOOKID = 0; const val CUSTOMERMOBILE = 1; const val PLANNAME = 2; const val STARTDATE = 3; const val ENDDATE = 4; const val MONTHLYAMOUNT = 5; const val TOTALMONTHS = 6; const val TOTALAMOUNT = 7; const val STATUS = 8; const val NOTES = 9; const val USERID = 10; const val STOREID = 11 }
    private object TransactionCols { const val TRANSACTIONID = 0; const val CUSTOMERMOBILE = 1; const val TRANSACTIONDATE = 2; const val AMOUNT = 3; const val TRANSACTIONTYPE = 4; const val CATEGORY = 5; const val DESCRIPTION = 6; const val REFERENCENUMBER = 7; const val PAYMENTMETHOD = 8; const val KHATABOOKID = 9; const val MONTHNUMBER = 10; const val NOTES = 11; const val USERID = 12; const val STOREID = 13 }
    private object OrderCols { const val ORDERID = 0; const val CUSTOMERMOBILE = 1; const val STOREID = 2; const val USERID = 3; const val ORDERDATE = 4; const val TOTALAMOUNT = 5; const val TOTALTAX = 6; const val TOTALCHARGE = 7; const val DISCOUNT = 8 ; const val NOTE = 9 }
    private object OrderItemCols { const val ORDERITEMID = 0; const val ORDERID = 1; const val ORDERDATE = 2; const val ITEMID = 3; const val CUSTOMERMOBILE = 4; const val CATID = 5; const val CATNAME = 6; const val ITEMADDNAME = 7; const val SUBCATID = 8; const val SUBCATNAME = 9; const val ENTRYTYPE = 10; const val QUANTITY = 11; const val GSWt = 12; const val NTWt = 13; const val FNWt = 14; const val FNMETALPRICE = 15; const val PURITY = 16; const val CRGTYPE = 17; const val CRG = 18; const val OTHCRGDES = 19; const val OTHCRG = 20; const val CGST = 21; const val SGST = 22; const val IGST = 23; const val HUID = 24; const val ADDDESKEY = 25; const val ADDDESVALUE = 26; const val PRICE = 27; const val CHARGE = 28; const val TAX = 29; const val SELLERFIRMID = 30; const val PURCHASEORDERID = 31; const val PURCHASEITEMID = 32 }
    private object ExchangeItemCols { const val EXCHANGEITEMID = 0; const val ORDERID = 1; const val ORDERDATE = 2; const val CUSTOMERMOBILE = 3; const val METALTYPE = 4; const val PURITY = 5; const val GROSSWEIGHT = 6; const val FINEWEIGHT = 7; const val PRICE = 8; const val ISEXCHANGEDBYMETAL = 9; const val EXCHANGEVALUE = 10; const val ADDDATE = 11 }
    private object FirmCols { const val FIRMID = 0; const val FIRMNAME = 1; const val FIRMMOBILENUMBER = 2; const val GSTNUMBER = 3; const val ADDRESS = 4 }
    private object PurchaseOrderCols { const val PURCHASEORDERID = 0; const val SELLERID = 1; const val BILLNO = 2; const val BILLDATE = 3; const val ENTRYDATE = 4; const val EXTRACHARGEDESCRIPTION = 5; const val EXTRACHARGE = 6; const val TOTALFINALWEIGHT = 7; const val TOTALFINALAMOUNT = 8; const val NOTES = 9; const val CGSTPERCENT = 10; const val SGSTPERCENT = 11; const val IGSTPERCENT = 12 }
    private object PurchaseOrderItemCols { const val PURCHASEITEMID = 0; const val PURCHASEORDERID = 1; const val CATID = 2; const val CATNAME = 3; const val SUBCATID = 4; const val SUBCATNAME = 5; const val GSWt = 6; const val PURITY = 7; const val NTWt = 8; const val FNWt = 9; const val FNRATE = 10; const val WASTAGEPERCENT = 11 }
    private object MetalExchangeCols { const val EXCHANGEID = 0; const val PURCHASEORDERID = 1; const val CATID = 2; const val CATNAME = 3; const val SUBCATID = 4; const val SUBCATNAME = 5; const val FNWEIGHT = 6 }
    private object UserAdditionalInfoCols { const val USERID = 0; const val AADHAAR = 1; const val ADDRESS = 2; const val EMERGENCYCONTACTPERSON = 3; const val EMERGENCYCONTACTNUMBER = 4; const val GOVERNMENTIDNUMBER = 5; const val GOVERNMENTIDTYPE = 6; const val DATEOFBIRTH = 7; const val BLOODGROUP = 8; const val ISACTIVE = 9; const val CREATEDAT = 10; const val UPDATEDAT = 11 }
    
    /**
     * Import all entities from Excel file with smart conflict resolution
     */
    suspend fun importAllEntitiesFromExcel(
        inputFile: File,
        currentUserId: String, // Current logged-in user ID
        currentStoreId: String, // Current selected store ID
        restoreMode: RestoreMode = RestoreMode.MERGE, // How to handle conflicts
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<ImportSummary> {
        return try {
            onProgress("Reading Excel file...", 5)
            
            val workbook = XSSFWorkbook(FileInputStream(inputFile))
            val summary = ImportSummary()
            
            // Import each entity from its sheet (order matters due to foreign key constraints)
            onProgress("Importing users...", 10)
            importUsersEntity(workbook, currentUserId, restoreMode, summary)

            onProgress("Importing user additional info...", 15)
            importUserAdditionalInfoEntity(workbook, currentUserId, restoreMode, summary)
            
            onProgress("Importing stores...", 20)
            importStoreEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing categories...", 25)
            importCategoryEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing subcategories...", 30)
            importSubCategoryEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing items...", 35)
            importItemEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing customers...", 40)
            importCustomerEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)

            onProgress("Importing khata book plans...", 45)
            importCustomerKhataBookPlanEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing khata books...", 50)
            importCustomerKhataBookEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing transactions...", 55)
            importCustomerTransactionEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing orders...", 60)
            importOrderEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing order items...", 65)
            importOrderItemEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing exchange items...", 70)
            importExchangeItemEntity(workbook, currentUserId, currentStoreId, restoreMode, summary)
            
            onProgress("Importing firms...", 75)
            importFirmEntity(workbook, restoreMode, summary)
            
            onProgress("Importing purchase orders...", 80)
            importPurchaseOrderEntity(workbook, restoreMode, summary)
            
            onProgress("Importing purchase order items...", 85)
            importPurchaseOrderItemEntity(workbook, restoreMode, summary)
            
            onProgress("Importing metal exchanges...", 90)
            importMetalExchangeEntity(workbook, restoreMode, summary)
            
            workbook.close()
            
            log("Excel import completed successfully: $summary")
            Result.success(summary)
            
        } catch (e: Exception) {
            log("Excel import failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun importUsersEntity(workbook: Workbook, currentUserId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("UsersEntity") ?: return
        val existingUsers = database.userDao().getAllUsers().associateBy { it.mobileNo }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val mobileNo = getCellValueAsString(row.getCell(UserCols.MOBILENO))
                val existingUser = existingUsers[mobileNo]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingUser == null) {
                            // Add new user
                            val user = UsersEntity(
                                userId = getCellValueAsString(row.getCell(UserCols.ID)),
                                name = getCellValueAsString(row.getCell(UserCols.NAME)),
                                email = getCellValueAsString(row.getCell(UserCols.EMAIL)),
                                mobileNo = mobileNo,
                                token = getCellValueAsString(row.getCell(UserCols.TOKEN)),
                                pin = getCellValueAsString(row.getCell(UserCols.PIN)),
                                role = getCellValueAsString(row.getCell(UserCols.ROLE))
                            )
                            database.userDao().insertUser(user)
                            summary.usersAdded++
                        } else {
                            // Skip existing user (preserve current data)
                            summary.usersSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        // Replace all users except current admin
                        if (mobileNo != currentUserId) {
                            val user = UsersEntity(
                                userId = getCellValueAsString(row.getCell(UserCols.ID)),
                                name = getCellValueAsString(row.getCell(UserCols.NAME)),
                                email = getCellValueAsString(row.getCell(UserCols.EMAIL)),
                                mobileNo = mobileNo,
                                token = getCellValueAsString(row.getCell(UserCols.TOKEN)),
                                pin = getCellValueAsString(row.getCell(UserCols.PIN)),
                                role = getCellValueAsString(row.getCell(UserCols.ROLE))
                            )
                            database.userDao().insertUser(user)
                            summary.usersAdded++
                        } else {
                            summary.usersSkipped++
                        }
                    }
                }
            } catch (e: Exception) {
                log("Error importing user at row $rowIndex: ${e.message}")
                summary.usersFailed++
            }
        }
    }

    private suspend fun importUserAdditionalInfoEntity(
        workbook: Workbook,
        currentUserId: String,
        restoreMode: RestoreMode,
        summary: ImportSummary
    ) {
        val sheet = workbook.getSheet("UserAdditionalInfoEntity") ?: return
        val existingInfos = database.userAdditionalInfoDao()
            .getAllUserAdditionalInfos()
            .associateBy { it.userId }

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val userId = getCellValueAsString(row.getCell(UserAdditionalInfoCols.USERID))
                if (userId.isBlank()) {
                    summary.userAdditionalInfoFailed++
                    continue
                }
                val existingInfo = existingInfos[userId]

                val createdAtValue =
                    parseDateString(getCellValueAsString(row.getCell(UserAdditionalInfoCols.CREATEDAT))).time
                val updatedAtValue =
                    parseDateString(getCellValueAsString(row.getCell(UserAdditionalInfoCols.UPDATEDAT))).time

                val userInfo = UserAdditionalInfoEntity(
                    userId = userId,
                    aadhaarNumber = getCellValueAsString(row.getCell(UserAdditionalInfoCols.AADHAAR))
                        .takeIf { it.isNotBlank() },
                    address = getCellValueAsString(row.getCell(UserAdditionalInfoCols.ADDRESS))
                        .takeIf { it.isNotBlank() },
                    emergencyContactPerson = getCellValueAsString(row.getCell(UserAdditionalInfoCols.EMERGENCYCONTACTPERSON))
                        .takeIf { it.isNotBlank() },
                    emergencyContactNumber = getCellValueAsString(row.getCell(UserAdditionalInfoCols.EMERGENCYCONTACTNUMBER))
                        .takeIf { it.isNotBlank() },
                    governmentIdNumber = getCellValueAsString(row.getCell(UserAdditionalInfoCols.GOVERNMENTIDNUMBER))
                        .takeIf { it.isNotBlank() },
                    governmentIdType = getCellValueAsString(row.getCell(UserAdditionalInfoCols.GOVERNMENTIDTYPE))
                        .takeIf { it.isNotBlank() },
                    dateOfBirth = getCellValueAsString(row.getCell(UserAdditionalInfoCols.DATEOFBIRTH))
                        .takeIf { it.isNotBlank() },
                    bloodGroup = getCellValueAsString(row.getCell(UserAdditionalInfoCols.BLOODGROUP))
                        .takeIf { it.isNotBlank() },
                    isActive = getCellValueAsBoolean(row.getCell(UserAdditionalInfoCols.ISACTIVE)),
                    createdAt = createdAtValue,
                    updatedAt = updatedAtValue
                )

                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingInfo == null) {
                            database.userAdditionalInfoDao().insertUserAdditionalInfo(userInfo)
                            summary.userAdditionalInfoAdded++
                        } else {
                            summary.userAdditionalInfoSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        if (userId != currentUserId) {
                            database.userAdditionalInfoDao().insertUserAdditionalInfo(userInfo)
                            summary.userAdditionalInfoAdded++
                        } else {
                            summary.userAdditionalInfoSkipped++
                        }
                    }
                }
            } catch (e: Exception) {
                log("Error importing user additional info at row $rowIndex: ${e.message}")
                summary.userAdditionalInfoFailed++
            }
        }
    }
    
    private suspend fun importStoreEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("StoreEntity") ?: return
        val existingStores = database.storeDao().getAllStores().associateBy { it.storeId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val originalStoreId = getCellValueAsString(row.getCell(StoreCols.STOREID))
                val originalUserId = getCellValueAsString(row.getCell(StoreCols.USERID))
                
                // For MERGE mode, we'll use the original store ID but map it to current user
                // For REPLACE mode, we'll replace all stores except current
                val storeId = if (restoreMode == RestoreMode.MERGE) originalStoreId else originalStoreId
                val existingStore = existingStores[storeId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingStore == null) {
                            // Add new store with current user ID
                            val store = StoreEntity(
                                storeId = storeId,
                                userId = currentUserId, // Use current user ID
                                proprietor = getCellValueAsString(row.getCell(StoreCols.PROPRIETOR)),
                                name = getCellValueAsString(row.getCell(StoreCols.NAME)),
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
                            summary.storesAdded++
                        } else {
                            // Skip existing store
                            summary.storesSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        // Replace all stores except current store
                        if (storeId != currentStoreId) {
                            val store = StoreEntity(
                                storeId = storeId,
                                userId = currentUserId,
                                proprietor = getCellValueAsString(row.getCell(StoreCols.PROPRIETOR)),
                                name = getCellValueAsString(row.getCell(StoreCols.NAME)),
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
                            summary.storesAdded++
                        } else {
                            summary.storesSkipped++
                        }
                    }
                }
            } catch (e: Exception) {
                log("Error importing store at row $rowIndex: ${e.message}")
                summary.storesFailed++
            }
        }
    }
    
    private suspend fun importCategoryEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("CategoryEntity") ?: return
        val existingCategories = database.categoryDao().getAllCategories().associateBy { "${it.userId}_${it.storeId}_${it.catName}" }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val catName = getCellValueAsString(row.getCell(CategoryCols.CATNAME))
                val originalUserId = getCellValueAsString(row.getCell(CategoryCols.USERID))
                val originalStoreId = getCellValueAsString(row.getCell(CategoryCols.STOREID))
                
                // For MERGE mode, check if category exists with current user/store IDs
                // For REPLACE mode, always import
                val key = "${currentUserId}_${currentStoreId}_$catName"
                val existingCategory = existingCategories[key]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingCategory == null) {
                            val category = CategoryEntity(
                                catId = getCellValueAsString(row.getCell(CategoryCols.CATID)),
                                catName = catName,
                                gsWt = getCellValueAsDouble(row.getCell(CategoryCols.GSWt)),
                                fnWt = getCellValueAsDouble(row.getCell(CategoryCols.FNWt)),
                                userId = currentUserId,
                                storeId = currentStoreId
                            )
                            database.categoryDao().insertCategory(category)
                            summary.categoriesAdded++
                        } else {
                            summary.categoriesSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val category = CategoryEntity(
                            catId = getCellValueAsString(row.getCell(CategoryCols.CATID)),
                            catName = catName,
                            gsWt = getCellValueAsDouble(row.getCell(CategoryCols.GSWt)),
                            fnWt = getCellValueAsDouble(row.getCell(CategoryCols.FNWt)),
                            userId = currentUserId,
                            storeId = currentStoreId
                        )
                        database.categoryDao().insertCategory(category)
                        summary.categoriesAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing category at row $rowIndex: ${e.message}")
                summary.categoriesFailed++
            }
        }
    }
    
    private suspend fun importSubCategoryEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("SubCategoryEntity") ?: return
        val existingSubCategories = database.subCategoryDao().getAllSubCategories().associateBy { "${it.catId}_${it.userId}_${it.storeId}_${it.subCatName}" }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val originalCatId = getCellValueAsString(row.getCell(SubCategoryCols.CATID))
                val originalUserId = getCellValueAsString(row.getCell(SubCategoryCols.USERID))
                val originalStoreId = getCellValueAsString(row.getCell(SubCategoryCols.STOREID))
                val subCatName = getCellValueAsString(row.getCell(SubCategoryCols.SUBCATNAME))
                
                // For MERGE mode, check if subcategory exists with current user/store IDs
                // For REPLACE mode, always import
                val key = "${originalCatId}_${currentUserId}_${currentStoreId}_${subCatName}"
                val existingSubCategory = existingSubCategories[key]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingSubCategory == null) {
                            val subCategory = SubCategoryEntity(
                                subCatId = getCellValueAsString(row.getCell(SubCategoryCols.SUBCATID)),
                                catId = originalCatId,
                                userId = currentUserId,
                                storeId = currentStoreId,
                                catName = getCellValueAsString(row.getCell(SubCategoryCols.CATNAME)),
                                subCatName = subCatName,
                                quantity = getCellValueAsInt(row.getCell(SubCategoryCols.QUANTITY)),
                                gsWt = getCellValueAsDouble(row.getCell(SubCategoryCols.GSWt)),
                                fnWt = getCellValueAsDouble(row.getCell(SubCategoryCols.FNWt))
                            )
                            database.subCategoryDao().insertSubCategory(subCategory)
                            summary.subCategoriesAdded++
                        } else {
                            summary.subCategoriesSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val subCategory = SubCategoryEntity(
                            subCatId = getCellValueAsString(row.getCell(SubCategoryCols.SUBCATID)),
                            catId = originalCatId,
                            userId = currentUserId,
                            storeId = currentStoreId,
                            catName = getCellValueAsString(row.getCell(SubCategoryCols.CATNAME)),
                            subCatName = subCatName,
                            quantity = getCellValueAsInt(row.getCell(SubCategoryCols.QUANTITY)),
                            gsWt = getCellValueAsDouble(row.getCell(SubCategoryCols.GSWt)),
                            fnWt = getCellValueAsDouble(row.getCell(SubCategoryCols.FNWt))
                        )
                        database.subCategoryDao().insertSubCategory(subCategory)
                        summary.subCategoriesAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing subcategory at row $rowIndex: ${e.message}")
                summary.subCategoriesFailed++
            }
        }
    }
    
    private suspend fun importItemEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("ItemEntity") ?: return
        val existingItems = database.itemDao().getAllItems().associateBy { "${it.userId}_${it.storeId}_${it.itemAddName}" }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val originalUserId = getCellValueAsString(row.getCell(ItemCols.USERID))
                val originalStoreId = getCellValueAsString(row.getCell(ItemCols.STOREID))
                val itemAddName = getCellValueAsString(row.getCell(ItemCols.ITEMADDNAME))
                val key = "${currentUserId}_${currentStoreId}_${itemAddName}"
                val existingItem = existingItems[key]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingItem == null) {
                            val item = ItemEntity(
                                itemId = getCellValueAsString(row.getCell(ItemCols.ITEMID)),
                                itemAddName = itemAddName,
                                catId = getCellValueAsString(row.getCell(ItemCols.CATID)),
                                userId = currentUserId,
                                storeId = currentStoreId,
                                catName = getCellValueAsString(row.getCell(ItemCols.CATNAME)),
                                subCatId = getCellValueAsString(row.getCell(ItemCols.SUBCATID)),
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
                                addDesValue = getCellValueAsString(row.getCell(ItemCols.ADDDESVALUE)),
                                addDate = parseDateString(getCellValueAsString(row.getCell(ItemCols.ADDDATE))),
                                modifiedDate = parseDateString(getCellValueAsString(row.getCell(ItemCols.MODIFIEDDATE))),
                                sellerFirmId = getCellValueAsString(row.getCell(ItemCols.SELLERFIRMID)),
                                purchaseOrderId = getCellValueAsString(row.getCell(ItemCols.PURCHASEORDERID)),
                                purchaseItemId = getCellValueAsString(row.getCell(ItemCols.PURCHASEITEMID))
                            )
                            database.itemDao().insertItem(item)
                            summary.itemsAdded++
                        } else {
                            summary.itemsSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val item = ItemEntity(
                            itemId = getCellValueAsString(row.getCell(ItemCols.ITEMID)),
                            itemAddName = itemAddName,
                            catId = getCellValueAsString(row.getCell(ItemCols.CATID)),
                            userId = currentUserId,
                            storeId = currentStoreId,
                            catName = getCellValueAsString(row.getCell(ItemCols.CATNAME)),
                            subCatId = getCellValueAsString(row.getCell(ItemCols.SUBCATID)),
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
                            addDesValue = getCellValueAsString(row.getCell(ItemCols.ADDDESVALUE)),
                            addDate = parseDateString(getCellValueAsString(row.getCell(ItemCols.ADDDATE))),
                            modifiedDate = parseDateString(getCellValueAsString(row.getCell(ItemCols.MODIFIEDDATE))),
                            sellerFirmId = getCellValueAsString(row.getCell(ItemCols.SELLERFIRMID)),
                            purchaseOrderId = getCellValueAsString(row.getCell(ItemCols.PURCHASEORDERID)),
                            purchaseItemId = getCellValueAsString(row.getCell(ItemCols.PURCHASEITEMID))
                        )
                        database.itemDao().insertItem(item)
                        summary.itemsAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing item at row $rowIndex: ${e.message}")
                summary.itemsFailed++
            }
        }
    }
    
    private suspend fun importCustomerEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("CustomerEntity") ?: return
        val existingCustomers = database.customerDao().getAllCustomersList()
            .filter { it.userId == currentUserId && it.storeId == currentStoreId }
            .associateBy { it.mobileNo }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val mobileNo = getCellValueAsString(row.getCell(CustomerCols.MOBILENO))
                val existingCustomer = existingCustomers[mobileNo]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingCustomer == null) {
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
                                userId = currentUserId,
                                storeId = currentStoreId
                            )
                            database.customerDao().insertCustomer(customer)
                            summary.customersAdded++
                        } else {
                            summary.customersSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
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
                            userId = currentUserId,
                            storeId = currentStoreId
                        )
                        database.customerDao().insertCustomer(customer)
                        summary.customersAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing customer at row $rowIndex: ${e.message}")
                summary.customersFailed++
            }
        }
    }

    private suspend fun importCustomerKhataBookPlanEntity(
        workbook: Workbook,
        currentUserId: String,
        currentStoreId: String,
        restoreMode: RestoreMode,
        summary: ImportSummary
    ) {
        val sheet = workbook.getSheet("CustomerKhataBookPlanEntity") ?: return
        val existingPlans = database.customerKhataBookPlanDao()
            .getPlansByUserAndStore(currentUserId, currentStoreId)
            .associateBy { it.planId }

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val planId = getCellValueAsString(row.getCell(KhataBookPlanCols.PLANID))
                if (planId.isBlank()) {
                    summary.khataBookPlansFailed++
                    continue
                }
                val existingPlan = existingPlans[planId]

                val createdAtValue =
                    parseDateString(getCellValueAsString(row.getCell(KhataBookPlanCols.CREATEDAT))).time
                val updatedAtValue =
                    parseDateString(getCellValueAsString(row.getCell(KhataBookPlanCols.UPDATEDAT))).time

                val plan = CustomerKhataBookPlanEntity(
                    planId = planId,
                    name = getCellValueAsString(row.getCell(KhataBookPlanCols.NAME)),
                    payMonths = getCellValueAsInt(row.getCell(KhataBookPlanCols.PAYMONTHS)),
                    benefitMonths = getCellValueAsInt(row.getCell(KhataBookPlanCols.BENEFITMONTHS)),
                    description = getCellValueAsString(row.getCell(KhataBookPlanCols.DESCRIPTION)),
                    benefitPercentage = getCellValueAsDouble(row.getCell(KhataBookPlanCols.BENEFITPERCENTAGE)),
                    userId = currentUserId,
                    storeId = currentStoreId,
                    createdAt = createdAtValue,
                    updatedAt = updatedAtValue
                )

                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingPlan == null) {
                            database.customerKhataBookPlanDao().insertPlan(plan)
                            summary.khataBookPlansAdded++
                        } else {
                            summary.khataBookPlansSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        database.customerKhataBookPlanDao().insertPlan(plan)
                        summary.khataBookPlansAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing khata book plan at row $rowIndex: ${e.message}")
                summary.khataBookPlansFailed++
            }
        }
    }
    
    private suspend fun importCustomerKhataBookEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("CustomerKhataBookEntity") ?: return
        val existingKhataBooks = database.customerKhataBookDao().getAllKhataBooksList()
            .filter { it.userId == currentUserId && it.storeId == currentStoreId }
            .associateBy { it.khataBookId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val khataBookId = getCellValueAsString(row.getCell(KhataBookCols.KHATABOOKID))
                val existingKhataBook = existingKhataBooks[khataBookId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingKhataBook == null) {
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
                                userId = currentUserId,
                                storeId = currentStoreId
                            )
                            database.customerKhataBookDao().insertKhataBook(khataBook)
                            summary.khataBooksAdded++
                        } else {
                            summary.khataBooksSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
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
                            userId = currentUserId,
                            storeId = currentStoreId
                        )
                        database.customerKhataBookDao().insertKhataBook(khataBook)
                        summary.khataBooksAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing khata book at row $rowIndex: ${e.message}")
                summary.khataBooksFailed++
            }
        }
    }
    
    private suspend fun importCustomerTransactionEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("CustomerTransactionEntity") ?: return
        val existingTransactions = database.customerTransactionDao().getAllTransactions()
            .filter { it.userId == currentUserId && it.storeId == currentStoreId }
            .associateBy { it.transactionId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val transactionId = getCellValueAsString(row.getCell(TransactionCols.TRANSACTIONID))
                val existingTransaction = existingTransactions[transactionId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingTransaction == null) {
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
                                khataBookId = getCellValueAsString(row.getCell(TransactionCols.KHATABOOKID)).takeIf { it.isNotEmpty() },
                                monthNumber = getCellValueAsInt(row.getCell(TransactionCols.MONTHNUMBER)).takeIf { it > 0 },
                                notes = getCellValueAsString(row.getCell(TransactionCols.NOTES)),
                                userId = currentUserId,
                                storeId = currentStoreId
                            )
                            database.customerTransactionDao().insertTransaction(transaction)
                            summary.transactionsAdded++
                        } else {
                            summary.transactionsSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
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
                                                            khataBookId = getCellValueAsString(row.getCell(TransactionCols.KHATABOOKID)).takeIf { it.isNotEmpty() },
                            monthNumber = getCellValueAsInt(row.getCell(TransactionCols.MONTHNUMBER)).takeIf { it > 0 },
                            notes = getCellValueAsString(row.getCell(TransactionCols.NOTES)),
                            userId = currentUserId,
                            storeId = currentStoreId
                        )
                        database.customerTransactionDao().insertTransaction(transaction)
                        summary.transactionsAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing transaction at row $rowIndex: ${e.message}")
                summary.transactionsFailed++
            }
        }
    }
    
    private suspend fun importOrderEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("OrderEntity") ?: return
        val existingOrders = database.orderDao().getAllOrders()
            .filter { it.userId == currentUserId && it.storeId == currentStoreId }
            .associateBy { it.orderId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val orderId = getCellValueAsString(row.getCell(OrderCols.ORDERID))
                val existingOrder = existingOrders[orderId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingOrder == null) {
                            val order = OrderEntity(
                                orderId = orderId,
                                customerMobile = getCellValueAsString(row.getCell(OrderCols.CUSTOMERMOBILE)),
                                storeId = currentStoreId,
                                userId = currentUserId,
                                orderDate = parseDateString(getCellValueAsString(row.getCell(OrderCols.ORDERDATE))),
                                totalAmount = getCellValueAsDouble(row.getCell(OrderCols.TOTALAMOUNT)),
                                totalTax = getCellValueAsDouble(row.getCell(OrderCols.TOTALTAX)),
                                totalCharge = getCellValueAsDouble(row.getCell(OrderCols.TOTALCHARGE)),
                                discount = getCellValueAsDouble(row.getCell(OrderCols.DISCOUNT)),
                                note = getCellValueAsString(row.getCell(OrderCols.NOTE))
                            )
                            database.orderDao().insertOrder(order)
                            summary.ordersAdded++
                        } else {
                            summary.ordersSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val order = OrderEntity(
                            orderId = orderId,
                            customerMobile = getCellValueAsString(row.getCell(OrderCols.CUSTOMERMOBILE)),
                            storeId = currentStoreId,
                            userId = currentUserId,
                            orderDate = parseDateString(getCellValueAsString(row.getCell(OrderCols.ORDERDATE))),
                            totalAmount = getCellValueAsDouble(row.getCell(OrderCols.TOTALAMOUNT)),
                            totalTax = getCellValueAsDouble(row.getCell(OrderCols.TOTALTAX)),
                            totalCharge = getCellValueAsDouble(row.getCell(OrderCols.TOTALCHARGE)),
                            discount = getCellValueAsDouble(row.getCell(OrderCols.DISCOUNT)),
                            note = getCellValueAsString(row.getCell(OrderCols.NOTE))
                        )
                        database.orderDao().insertOrder(order)
                        summary.ordersAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing order at row $rowIndex: ${e.message}")
                summary.ordersFailed++
            }
        }
    }
    
    private suspend fun importOrderItemEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("OrderItemEntity") ?: return
        val existingOrderItems = database.orderDao().getAllOrderItems().associateBy { it.orderItemId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val orderItemId = getCellValueAsString(row.getCell(OrderItemCols.ORDERITEMID))
                val existingOrderItem = existingOrderItems[orderItemId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingOrderItem == null) {
                            val orderItem = OrderItemEntity(
                                orderItemId = orderItemId,
                                orderId = getCellValueAsString(row.getCell(OrderItemCols.ORDERID)),
                                orderDate = parseDateString(getCellValueAsString(row.getCell(OrderItemCols.ORDERDATE))),
                                itemId = getCellValueAsString(row.getCell(OrderItemCols.ITEMID)),
                                customerMobile = getCellValueAsString(row.getCell(OrderItemCols.CUSTOMERMOBILE)),
                                catId = getCellValueAsString(row.getCell(OrderItemCols.CATID)),
                                catName = getCellValueAsString(row.getCell(OrderItemCols.CATNAME)),
                                itemAddName = getCellValueAsString(row.getCell(OrderItemCols.ITEMADDNAME)),
                                subCatId = getCellValueAsString(row.getCell(OrderItemCols.SUBCATID)),
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
                                addDesValue = getCellValueAsString(row.getCell(OrderItemCols.ADDDESVALUE)),
                                price = getCellValueAsDouble(row.getCell(OrderItemCols.PRICE)),
                                charge = getCellValueAsDouble(row.getCell(OrderItemCols.CHARGE)),
                                tax = getCellValueAsDouble(row.getCell(OrderItemCols.TAX)),
                                sellerFirmId = getCellValueAsString(row.getCell(OrderItemCols.SELLERFIRMID)),
                                purchaseOrderId = getCellValueAsString(row.getCell(OrderItemCols.PURCHASEORDERID)),
                                purchaseItemId = getCellValueAsString(row.getCell(OrderItemCols.PURCHASEITEMID))
                            )
                            database.orderDao().insertOrderItem(orderItem)
                            summary.orderItemsAdded++
                        } else {
                            summary.orderItemsSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val orderItem = OrderItemEntity(
                            orderItemId = orderItemId,
                            orderId = getCellValueAsString(row.getCell(OrderItemCols.ORDERID)),
                            orderDate = parseDateString(getCellValueAsString(row.getCell(OrderItemCols.ORDERDATE))),
                            itemId = getCellValueAsString(row.getCell(OrderItemCols.ITEMID)),
                            customerMobile = getCellValueAsString(row.getCell(OrderItemCols.CUSTOMERMOBILE)),
                            catId = getCellValueAsString(row.getCell(OrderItemCols.CATID)),
                            catName = getCellValueAsString(row.getCell(OrderItemCols.CATNAME)),
                            itemAddName = getCellValueAsString(row.getCell(OrderItemCols.ITEMADDNAME)),
                            subCatId = getCellValueAsString(row.getCell(OrderItemCols.SUBCATID)),
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
                            addDesValue = getCellValueAsString(row.getCell(OrderItemCols.ADDDESVALUE)),
                            price = getCellValueAsDouble(row.getCell(OrderItemCols.PRICE)),
                            charge = getCellValueAsDouble(row.getCell(OrderItemCols.CHARGE)),
                            tax = getCellValueAsDouble(row.getCell(OrderItemCols.TAX)),
                            sellerFirmId = getCellValueAsString(row.getCell(OrderItemCols.SELLERFIRMID)),
                            purchaseOrderId = getCellValueAsString(row.getCell(OrderItemCols.PURCHASEORDERID)),
                            purchaseItemId = getCellValueAsString(row.getCell(OrderItemCols.PURCHASEITEMID))
                        )
                        database.orderDao().insertOrderItem(orderItem)
                        summary.orderItemsAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing order item at row $rowIndex: ${e.message}")
                summary.orderItemsFailed++
            }
        }
    }
    
    private suspend fun importExchangeItemEntity(workbook: Workbook, currentUserId: String, currentStoreId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("ExchangeItemEntity") ?: return
        val existingExchangeItems = database.orderDao().getAllExchangeItems().associateBy { it.exchangeItemId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val exchangeItemId = getCellValueAsString(row.getCell(ExchangeItemCols.EXCHANGEITEMID))
                val existingExchangeItem = existingExchangeItems[exchangeItemId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingExchangeItem == null) {
                            val exchangeItem = ExchangeItemEntity(
                                exchangeItemId = exchangeItemId,
                                orderId = getCellValueAsString(row.getCell(ExchangeItemCols.ORDERID)),
                                orderDate = parseDateString(getCellValueAsString(row.getCell(ExchangeItemCols.ORDERDATE))),
                                customerMobile = getCellValueAsString(row.getCell(ExchangeItemCols.CUSTOMERMOBILE)),
                                metalType = getCellValueAsString(row.getCell(ExchangeItemCols.METALTYPE)),
                                purity = getCellValueAsString(row.getCell(ExchangeItemCols.PURITY)),
                                grossWeight = getCellValueAsDouble(row.getCell(ExchangeItemCols.GROSSWEIGHT)),
                                fineWeight = getCellValueAsDouble(row.getCell(ExchangeItemCols.FINEWEIGHT)),
                                price = getCellValueAsDouble(row.getCell(ExchangeItemCols.PRICE)),
                                isExchangedByMetal = getCellValueAsBoolean(row.getCell(ExchangeItemCols.ISEXCHANGEDBYMETAL)),
                                exchangeValue = getCellValueAsDouble(row.getCell(ExchangeItemCols.EXCHANGEVALUE)),
                                addDate = parseDateString(getCellValueAsString(row.getCell(ExchangeItemCols.ADDDATE)))
                            )
                            database.orderDao().insertExchangeItem(exchangeItem)
                            summary.exchangeItemsAdded++
                        } else {
                            summary.exchangeItemsSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val exchangeItem = ExchangeItemEntity(
                            exchangeItemId = exchangeItemId,
                            orderId = getCellValueAsString(row.getCell(ExchangeItemCols.ORDERID)),
                            orderDate = parseDateString(getCellValueAsString(row.getCell(ExchangeItemCols.ORDERDATE))),
                            customerMobile = getCellValueAsString(row.getCell(ExchangeItemCols.CUSTOMERMOBILE)),
                            metalType = getCellValueAsString(row.getCell(ExchangeItemCols.METALTYPE)),
                            purity = getCellValueAsString(row.getCell(ExchangeItemCols.PURITY)),
                            grossWeight = getCellValueAsDouble(row.getCell(ExchangeItemCols.GROSSWEIGHT)),
                            fineWeight = getCellValueAsDouble(row.getCell(ExchangeItemCols.FINEWEIGHT)),
                            price = getCellValueAsDouble(row.getCell(ExchangeItemCols.PRICE)),
                            isExchangedByMetal = getCellValueAsBoolean(row.getCell(ExchangeItemCols.ISEXCHANGEDBYMETAL)),
                            exchangeValue = getCellValueAsDouble(row.getCell(ExchangeItemCols.EXCHANGEVALUE)),
                            addDate = parseDateString(getCellValueAsString(row.getCell(ExchangeItemCols.ADDDATE)))
                        )
                        database.orderDao().insertExchangeItem(exchangeItem)
                        summary.exchangeItemsAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing exchange item at row $rowIndex: ${e.message}")
                summary.exchangeItemsFailed++
            }
        }
    }
    
    private suspend fun importFirmEntity(workbook: Workbook, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("FirmEntity") ?: return
        val existingFirms = database.purchaseDao().getAllFirms().associateBy { it.firmName }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val firmName = getCellValueAsString(row.getCell(FirmCols.FIRMNAME))
                val existingFirm = existingFirms[firmName]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingFirm == null) {
                            val firm = FirmEntity(
                                firmId = getCellValueAsString(row.getCell(FirmCols.FIRMID)),
                                firmName = firmName,
                                firmMobileNumber = getCellValueAsString(row.getCell(FirmCols.FIRMMOBILENUMBER)),
                                gstNumber = getCellValueAsString(row.getCell(FirmCols.GSTNUMBER)),
                                address = getCellValueAsString(row.getCell(FirmCols.ADDRESS))
                            )
                            database.purchaseDao().insertFirm(firm)
                            summary.firmsAdded++
                        } else {
                            summary.firmsSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val firm = FirmEntity(
                            firmId = getCellValueAsString(row.getCell(FirmCols.FIRMID)),
                            firmName = firmName,
                            firmMobileNumber = getCellValueAsString(row.getCell(FirmCols.FIRMMOBILENUMBER)),
                            gstNumber = getCellValueAsString(row.getCell(FirmCols.GSTNUMBER)),
                            address = getCellValueAsString(row.getCell(FirmCols.ADDRESS))
                        )
                        database.purchaseDao().insertFirm(firm)
                        summary.firmsAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing firm at row $rowIndex: ${e.message}")
                summary.firmsFailed++
            }
        }
    }
    
    private suspend fun importPurchaseOrderEntity(workbook: Workbook, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("PurchaseOrderEntity") ?: return
        val existingPurchaseOrders = database.purchaseDao().getAllPurchaseOrders().associateBy { it.purchaseOrderId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val purchaseOrderId = getCellValueAsString(row.getCell(PurchaseOrderCols.PURCHASEORDERID))
                val existingPurchaseOrder = existingPurchaseOrders[purchaseOrderId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingPurchaseOrder == null) {
                            val purchaseOrder = PurchaseOrderEntity(
                                purchaseOrderId = purchaseOrderId,
                                sellerId = getCellValueAsString(row.getCell(PurchaseOrderCols.SELLERID)),
                                billNo = getCellValueAsString(row.getCell(PurchaseOrderCols.BILLNO)),
                                billDate = getCellValueAsString(row.getCell(PurchaseOrderCols.BILLDATE)),
                                entryDate = getCellValueAsString(row.getCell(PurchaseOrderCols.ENTRYDATE)),
                                extraChargeDescription = getCellValueAsString(row.getCell(PurchaseOrderCols.EXTRACHARGEDESCRIPTION)),
                                extraCharge = getCellValueAsDouble(row.getCell(PurchaseOrderCols.EXTRACHARGE)),
                                totalFinalWeight = getCellValueAsDouble(row.getCell(PurchaseOrderCols.TOTALFINALWEIGHT)),
                                totalFinalAmount = getCellValueAsDouble(row.getCell(PurchaseOrderCols.TOTALFINALAMOUNT)),
                                notes = getCellValueAsString(row.getCell(PurchaseOrderCols.NOTES)),
                                cgstPercent = getCellValueAsDouble(row.getCell(PurchaseOrderCols.CGSTPERCENT)),
                                sgstPercent = getCellValueAsDouble(row.getCell(PurchaseOrderCols.SGSTPERCENT)),
                                igstPercent = getCellValueAsDouble(row.getCell(PurchaseOrderCols.IGSTPERCENT))
                            )
                            database.purchaseDao().insertPurchaseOrder(purchaseOrder)
                            summary.purchaseOrdersAdded++
                        } else {
                            summary.purchaseOrdersSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val purchaseOrder = PurchaseOrderEntity(
                            purchaseOrderId = purchaseOrderId,
                            sellerId = getCellValueAsString(row.getCell(PurchaseOrderCols.SELLERID)),
                            billNo = getCellValueAsString(row.getCell(PurchaseOrderCols.BILLNO)),
                            billDate = getCellValueAsString(row.getCell(PurchaseOrderCols.BILLDATE)),
                            entryDate = getCellValueAsString(row.getCell(PurchaseOrderCols.ENTRYDATE)),
                            extraChargeDescription = getCellValueAsString(row.getCell(PurchaseOrderCols.EXTRACHARGEDESCRIPTION)),
                            extraCharge = getCellValueAsDouble(row.getCell(PurchaseOrderCols.EXTRACHARGE)),
                            totalFinalWeight = getCellValueAsDouble(row.getCell(PurchaseOrderCols.TOTALFINALWEIGHT)),
                            totalFinalAmount = getCellValueAsDouble(row.getCell(PurchaseOrderCols.TOTALFINALAMOUNT)),
                            notes = getCellValueAsString(row.getCell(PurchaseOrderCols.NOTES)),
                            cgstPercent = getCellValueAsDouble(row.getCell(PurchaseOrderCols.CGSTPERCENT)),
                            sgstPercent = getCellValueAsDouble(row.getCell(PurchaseOrderCols.SGSTPERCENT)),
                            igstPercent = getCellValueAsDouble(row.getCell(PurchaseOrderCols.IGSTPERCENT))
                        )
                        database.purchaseDao().insertPurchaseOrder(purchaseOrder)
                        summary.purchaseOrdersAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing purchase order at row $rowIndex: ${e.message}")
                summary.purchaseOrdersFailed++
            }
        }
    }
    
    private suspend fun importPurchaseOrderItemEntity(workbook: Workbook, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("PurchaseOrderItemEntity") ?: return
        val existingPurchaseOrderItems = database.purchaseDao().getAllPurchaseOrderItems().associateBy { it.purchaseItemId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val purchaseItemId = getCellValueAsString(row.getCell(PurchaseOrderItemCols.PURCHASEITEMID))
                val existingPurchaseOrderItem = existingPurchaseOrderItems[purchaseItemId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingPurchaseOrderItem == null) {
                            val purchaseOrderItem = PurchaseOrderItemEntity(
                                purchaseItemId = purchaseItemId,
                                purchaseOrderId = getCellValueAsString(row.getCell(PurchaseOrderItemCols.PURCHASEORDERID)),
                                catId = getCellValueAsString(row.getCell(PurchaseOrderItemCols.CATID)),
                                catName = getCellValueAsString(row.getCell(PurchaseOrderItemCols.CATNAME)),
                                subCatId = getCellValueAsString(row.getCell(PurchaseOrderItemCols.SUBCATID)),
                                subCatName = getCellValueAsString(row.getCell(PurchaseOrderItemCols.SUBCATNAME)),
                                gsWt = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.GSWt)),
                                purity = getCellValueAsString(row.getCell(PurchaseOrderItemCols.PURITY)),
                                ntWt = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.NTWt)),
                                fnWt = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.FNWt)),
                                fnRate = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.FNRATE)),
                                wastagePercent = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.WASTAGEPERCENT))
                            )
                            database.purchaseDao().insertPurchaseOrderItem(purchaseOrderItem)
                            summary.purchaseOrderItemsAdded++
                        } else {
                            summary.purchaseOrderItemsSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val purchaseOrderItem = PurchaseOrderItemEntity(
                            purchaseItemId = purchaseItemId,
                            purchaseOrderId = getCellValueAsString(row.getCell(PurchaseOrderItemCols.PURCHASEORDERID)),
                            catId = getCellValueAsString(row.getCell(PurchaseOrderItemCols.CATID)),
                            catName = getCellValueAsString(row.getCell(PurchaseOrderItemCols.CATNAME)),
                            subCatId = getCellValueAsString(row.getCell(PurchaseOrderItemCols.SUBCATID)),
                            subCatName = getCellValueAsString(row.getCell(PurchaseOrderItemCols.SUBCATNAME)),
                            gsWt = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.GSWt)),
                            purity = getCellValueAsString(row.getCell(PurchaseOrderItemCols.PURITY)),
                            ntWt = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.NTWt)),
                            fnWt = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.FNWt)),
                            fnRate = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.FNRATE)),
                            wastagePercent = getCellValueAsDouble(row.getCell(PurchaseOrderItemCols.WASTAGEPERCENT))
                        )
                        database.purchaseDao().insertPurchaseOrderItem(purchaseOrderItem)
                        summary.purchaseOrderItemsAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing purchase order item at row $rowIndex: ${e.message}")
                summary.purchaseOrderItemsFailed++
            }
        }
    }
    
    private suspend fun importMetalExchangeEntity(workbook: Workbook, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = workbook.getSheet("MetalExchangeEntity") ?: return
        val existingMetalExchanges = database.purchaseDao().getAllMetalExchanges().associateBy { it.exchangeId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val exchangeId = getCellValueAsString(row.getCell(MetalExchangeCols.EXCHANGEID))
                val existingMetalExchange = existingMetalExchanges[exchangeId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingMetalExchange == null) {
                            val metalExchange = MetalExchangeEntity(
                                exchangeId = exchangeId,
                                purchaseOrderId = getCellValueAsString(row.getCell(MetalExchangeCols.PURCHASEORDERID)),
                                catId = getCellValueAsString(row.getCell(MetalExchangeCols.CATID)),
                                catName = getCellValueAsString(row.getCell(MetalExchangeCols.CATNAME)),
                                subCatId = getCellValueAsString(row.getCell(MetalExchangeCols.SUBCATID)),
                                subCatName = getCellValueAsString(row.getCell(MetalExchangeCols.SUBCATNAME)),
                                fnWeight = getCellValueAsDouble(row.getCell(MetalExchangeCols.FNWEIGHT))
                            )
                            database.purchaseDao().insertMetalExchange(metalExchange)
                            summary.metalExchangesAdded++
                        } else {
                            summary.metalExchangesSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val metalExchange = MetalExchangeEntity(
                            exchangeId = exchangeId,
                            purchaseOrderId = getCellValueAsString(row.getCell(MetalExchangeCols.PURCHASEORDERID)),
                            catId = getCellValueAsString(row.getCell(MetalExchangeCols.CATID)),
                            catName = getCellValueAsString(row.getCell(MetalExchangeCols.CATNAME)),
                            subCatId = getCellValueAsString(row.getCell(MetalExchangeCols.SUBCATID)),
                            subCatName = getCellValueAsString(row.getCell(MetalExchangeCols.SUBCATNAME)),
                            fnWeight = getCellValueAsDouble(row.getCell(MetalExchangeCols.FNWEIGHT))
                        )
                        database.purchaseDao().insertMetalExchange(metalExchange)
                        summary.metalExchangesAdded++
                    }
                }
            } catch (e: Exception) {
                log("Error importing metal exchange at row $rowIndex: ${e.message}")
                summary.metalExchangesFailed++
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
            CellType.NUMERIC -> cell.numericCellValue != 0.0
            else -> false
        }
    }
    
    private fun parseDateString(dateString: String): java.sql.Timestamp {
        return try {
            val date = dateFormat.parse(dateString)
            java.sql.Timestamp(date?.time ?: System.currentTimeMillis())
        } catch (e: Exception) {
            java.sql.Timestamp(System.currentTimeMillis())
        }
    }

    /**
     * Validate Excel file structure for import
     */
    suspend fun validateExcelStructure(inputFile: File): Result<Unit> {
        return try {
            val workbook = XSSFWorkbook(FileInputStream(inputFile))

            val expectedHeaders = mapOf(
                "UsersEntity" to listOf(
                    "id",
                    "name",
                    "email",
                    "mobileNo",
                    "token",
                    "pin",
                    "role"
                ),
                "UserAdditionalInfoEntity" to listOf(
                    "userId",
                    "aadhaarNumber",
                    "address",
                    "emergencyContactPerson",
                    "emergencyContactNumber",
                    "governmentIdNumber",
                    "governmentIdType",
                    "dateOfBirth",
                    "bloodGroup",
                    "isActive",
                    "createdAt",
                    "updatedAt"
                ),
                "StoreEntity" to listOf(
                    "storeId",
                    "userId",
                    "proprietor",
                    "name",
                    "email",
                    "phone",
                    "address",
                    "registrationNo",
                    "gstinNo",
                    "panNo",
                    "image",
                    "invoiceNo",
                    "upiId"
                ),
                "CategoryEntity" to listOf(
                    "catId",
                    "catName",
                    "gsWt",
                    "fnWt",
                    "userId",
                    "storeId"
                ),
                "SubCategoryEntity" to listOf(
                    "subCatId",
                    "catId",
                    "userId",
                    "storeId",
                    "catName",
                    "subCatName",
                    "quantity",
                    "gsWt",
                    "fnWt"
                ),
                "ItemEntity" to listOf(
                    "itemId",
                    "itemAddName",
                    "catId",
                    "userId",
                    "storeId",
                    "catName",
                    "subCatId",
                    "subCatName",
                    "entryType",
                    "quantity",
                    "gsWt",
                    "ntWt",
                    "fnWt",
                    "purity",
                    "crgType",
                    "crg",
                    "othCrgDes",
                    "othCrg",
                    "cgst",
                    "sgst",
                    "igst",
                    "huid",
                    "unit",
                    "addDesKey",
                    "addDesValue",
                    "addDate",
                    "modifiedDate",
                    "sellerFirmId",
                    "purchaseOrderId",
                    "purchaseItemId"
                ),
                "CustomerEntity" to listOf(
                    "mobileNo",
                    "name",
                    "address",
                    "gstin_pan",
                    "addDate",
                    "lastModifiedDate",
                    "totalItemBought",
                    "totalAmount",
                    "notes",
                    "userId",
                    "storeId"
                ),
                "CustomerKhataBookPlanEntity" to listOf(
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
                ),
                "CustomerKhataBookEntity" to listOf(
                    "khataBookId",
                    "customerMobile",
                    "planName",
                    "startDate",
                    "endDate",
                    "monthlyAmount",
                    "totalMonths",
                    "totalAmount",
                    "status",
                    "notes",
                    "userId",
                    "storeId"
                ),
                "CustomerTransactionEntity" to listOf(
                    "transactionId",
                    "customerMobile",
                    "transactionDate",
                    "amount",
                    "transactionType",
                    "category",
                    "description",
                    "referenceNumber",
                    "paymentMethod",
                    "khataBookId",
                    "monthNumber",
                    "notes",
                    "userId",
                    "storeId"
                ),
                "OrderEntity" to listOf(
                    "orderId",
                    "customerMobile",
                    "storeId",
                    "userId",
                    "orderDate",
                    "totalAmount",
                    "totalTax",
                    "totalCharge",
                    "discount",
                    "note"
                ),
                "OrderItemEntity" to listOf(
                    "orderItemId",
                    "orderId",
                    "orderDate",
                    "itemId",
                    "customerMobile",
                    "catId",
                    "catName",
                    "itemAddName",
                    "subCatId",
                    "subCatName",
                    "entryType",
                    "quantity",
                    "gsWt",
                    "ntWt",
                    "fnWt",
                    "fnMetalPrice",
                    "purity",
                    "crgType",
                    "crg",
                    "othCrgDes",
                    "othCrg",
                    "cgst",
                    "sgst",
                    "igst",
                    "huid",
                    "addDesKey",
                    "addDesValue",
                    "price",
                    "charge",
                    "tax",
                    "sellerFirmId",
                    "purchaseOrderId",
                    "purchaseItemId"
                ),
                "ExchangeItemEntity" to listOf(
                    "exchangeItemId",
                    "orderId",
                    "orderDate",
                    "customerMobile",
                    "metalType",
                    "purity",
                    "grossWeight",
                    "fineWeight",
                    "price",
                    "isExchangedByMetal",
                    "exchangeValue",
                    "addDate"
                ),
                "FirmEntity" to listOf(
                    "firmId",
                    "firmName",
                    "firmMobileNumber",
                    "gstNumber",
                    "address"
                ),
                "PurchaseOrderEntity" to listOf(
                    "purchaseOrderId",
                    "sellerId",
                    "billNo",
                    "billDate",
                    "entryDate",
                    "extraChargeDescription",
                    "extraCharge",
                    "totalFinalWeight",
                    "totalFinalAmount",
                    "notes",
                    "cgstPercent",
                    "sgstPercent",
                    "igstPercent"
                ),
                "PurchaseOrderItemEntity" to listOf(
                    "purchaseItemId",
                    "purchaseOrderId",
                    "catId",
                    "catName",
                    "subCatId",
                    "subCatName",
                    "gsWt",
                    "purity",
                    "ntWt",
                    "fnWt",
                    "fnRate",
                    "wastagePercent"
                ),
                "MetalExchangeEntity" to listOf(
                    "exchangeId",
                    "purchaseOrderId",
                    "catId",
                    "catName",
                    "subCatId",
                    "subCatName",
                    "fnWeight"
                )
            )

            val errors = mutableListOf<String>()
            val requiredSheets = expectedHeaders.keys

            val existingSheets = mutableListOf<String>()
            for (i in 0 until workbook.numberOfSheets) {
                existingSheets.add(workbook.getSheetName(i))
            }

            val missingSheets = requiredSheets.filter { !existingSheets.contains(it) }
            if (missingSheets.isNotEmpty()) {
                errors.add("Missing sheets: ${missingSheets.joinToString(", ")}")
            }

            for ((sheetName, headers) in expectedHeaders) {
                val sheet = workbook.getSheet(sheetName) ?: continue
                if (sheet.lastRowNum < 0) {
                    errors.add("Sheet '$sheetName' is empty or corrupted")
                    continue
                }

                val headerRow = sheet.getRow(0)
                if (headerRow == null) {
                    errors.add("Sheet '$sheetName' is missing the header row")
                    continue
                }

                val actualHeaders = headers.indices.map { index ->
                    getCellValueAsString(headerRow.getCell(index)).trim()
                }
                val mismatchedHeaders = headers.filterIndexed { index, expected ->
                    actualHeaders.getOrNull(index)?.equals(expected, ignoreCase = true) != true
                }
                if (mismatchedHeaders.isNotEmpty()) {
                    errors.add(
                        "Sheet '$sheetName' missing or mismatched columns: ${mismatchedHeaders.joinToString(", ")}"
                    )
                }
            }

            workbook.close()

            if (errors.isNotEmpty()) {
                return Result.failure(Exception(errors.joinToString("\n")))
            }

            Result.success(Unit)

        } catch (e: Exception) {
            log("Excel structure validation failed: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Enum for restore modes
 */
enum class RestoreMode {
    MERGE,    // Add new data, skip existing
    REPLACE   // Replace all data except current user/store
}

/**
 * Data class to track import summary
 */
data class ImportSummary(
    var usersAdded: Int = 0,
    var usersSkipped: Int = 0,
    var usersFailed: Int = 0,
    var storesAdded: Int = 0,
    var storesSkipped: Int = 0,
    var storesFailed: Int = 0,
    var categoriesAdded: Int = 0,
    var categoriesSkipped: Int = 0,
    var categoriesFailed: Int = 0,
    var subCategoriesAdded: Int = 0,
    var subCategoriesSkipped: Int = 0,
    var subCategoriesFailed: Int = 0,
    var itemsAdded: Int = 0,
    var itemsSkipped: Int = 0,
    var itemsFailed: Int = 0,
    var customersAdded: Int = 0,
    var customersSkipped: Int = 0,
    var customersFailed: Int = 0,
    var khataBookPlansAdded: Int = 0,
    var khataBookPlansSkipped: Int = 0,
    var khataBookPlansFailed: Int = 0,
    var ordersAdded: Int = 0,
    var ordersSkipped: Int = 0,
    var ordersFailed: Int = 0,
    var orderItemsAdded: Int = 0,
    var orderItemsSkipped: Int = 0,
    var orderItemsFailed: Int = 0,
    var firmsAdded: Int = 0,
    var firmsSkipped: Int = 0,
    var firmsFailed: Int = 0,
    var purchaseOrdersAdded: Int = 0,
    var purchaseOrdersSkipped: Int = 0,
    var purchaseOrdersFailed: Int = 0,
    var purchaseOrderItemsAdded: Int = 0,
    var purchaseOrderItemsSkipped: Int = 0,
    var purchaseOrderItemsFailed: Int = 0,
    var metalExchangesAdded: Int = 0,
    var metalExchangesSkipped: Int = 0,
    var metalExchangesFailed: Int = 0,
    var khataBooksAdded: Int = 0,
    var khataBooksSkipped: Int = 0,
    var khataBooksFailed: Int = 0,
    var transactionsAdded: Int = 0,
    var transactionsSkipped: Int = 0,
    var transactionsFailed: Int = 0,
    var exchangeItemsAdded: Int = 0,
    var exchangeItemsSkipped: Int = 0,
    var exchangeItemsFailed: Int = 0,
    var userAdditionalInfoAdded: Int = 0,
    var userAdditionalInfoSkipped: Int = 0,
    var userAdditionalInfoFailed: Int = 0
) {
    override fun toString(): String {
        return "Import Summary: " +
                "Users($usersAdded added, $usersSkipped skipped, $usersFailed failed), " +
                "Stores($storesAdded added, $storesSkipped skipped, $storesFailed failed), " +
                "Categories($categoriesAdded added, $categoriesSkipped skipped, $categoriesFailed failed), " +
                "SubCategories($subCategoriesAdded added, $subCategoriesSkipped skipped, $subCategoriesFailed failed), " +
                "Items($itemsAdded added, $itemsSkipped skipped, $itemsFailed failed), " +
                "Customers($customersAdded added, $customersSkipped skipped, $customersFailed failed), " +
                "KhataBookPlans($khataBookPlansAdded added, $khataBookPlansSkipped skipped, $khataBookPlansFailed failed), " +
                "Orders($ordersAdded added, $ordersSkipped skipped, $ordersFailed failed), " +
                "OrderItems($orderItemsAdded added, $orderItemsSkipped skipped, $orderItemsFailed failed), " +
                "Firms($firmsAdded added, $firmsSkipped skipped, $firmsFailed failed), " +
                "PurchaseOrders($purchaseOrdersAdded added, $purchaseOrdersSkipped skipped, $purchaseOrdersFailed failed), " +
                "PurchaseOrderItems($purchaseOrderItemsAdded added, $purchaseOrderItemsSkipped skipped, $purchaseOrderItemsFailed failed), " +
                "MetalExchanges($metalExchangesAdded added, $metalExchangesSkipped skipped, $metalExchangesFailed failed), " +
                "KhataBooks($khataBooksAdded added, $khataBooksSkipped skipped, $khataBooksFailed failed), " +
                "Transactions($transactionsAdded added, $transactionsSkipped skipped, $transactionsFailed failed), " +
                "ExchangeItems($exchangeItemsAdded added, $exchangeItemsSkipped skipped, $exchangeItemsFailed failed), " +
                "UserAdditionalInfo($userAdditionalInfoAdded added, $userAdditionalInfoSkipped skipped, $userAdditionalInfoFailed failed)"
    }
}
