package com.velox.jewelvault.utils.sync

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
import com.velox.jewelvault.utils.logJvSync
import com.velox.jewelvault.utils.sync.ExcelSchema
import com.velox.jewelvault.utils.logJvSync
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
    private val dataFormatter = DataFormatter(Locale.getDefault())
    
    private data class SchemaMetadata(
        val schemaVersion: Int,
        val exportedAt: String?,
        val headers: Map<String, List<String>>
    )
    
    // --- Constants for column indexes ---
    private object UserCols { const val ID = 0; const val NAME = 1; const val EMAIL = 2; const val MOBILENO = 3; const val TOKEN = 4; const val PIN = 5; const val ROLE = 6; const val LASTUPDATED = 7 }
    private object StoreCols { const val STOREID = 0; const val USERID = 1; const val PROPRIETOR = 2; const val NAME = 3; const val EMAIL = 4; const val PHONE = 5; const val ADDRESS = 6; const val REGNO = 7; const val GSTIN = 8; const val PAN = 9; const val IMAGE = 10; const val INVOICENO = 11; const val UPIID = 12; const val LASTUPDATED = 13 }
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
            logJvSync("Excel import started for ${inputFile.name}")
            onProgress("Reading Excel file...", 5)
            
            val inputStream = FileInputStream(inputFile)
            try {
                val workbook = XSSFWorkbook(inputStream)
                try {
                    val metadata = readSchemaMetadata(workbook)
                    logJvSync("Excel import started for ${inputFile.name} (schema v${metadata.schemaVersion})")
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

                    log("Excel import completed successfully: $summary")
                    logJvSync("Excel import succeeded for ${inputFile.name}")
                    Result.success(summary)
                } finally {
                    workbook.close()
                }
            } finally {
                inputStream.close()
            }
            
        } catch (e: Exception) {
            log("Excel import failed: ${e.message}")
            logJvSync("Excel import failed for ${inputFile.name}: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun importUsersEntity(workbook: Workbook, currentUserId: String, restoreMode: RestoreMode, summary: ImportSummary) {
        val sheet = requireSheet(workbook, "UsersEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingUsers = database.userDao().getAllUsers().associateBy { it.mobileNo }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val mobileNo = getCellValueAsString(cell(row, headerMap, "mobileNo", UserCols.MOBILENO))
                val existingUser = existingUsers[mobileNo]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingUser == null) {
                            // Add new user
                            val user = UsersEntity(
                                userId = getCellValueAsString(cell(row, headerMap, "id", UserCols.ID)),
                                name = getCellValueAsString(cell(row, headerMap, "name", UserCols.NAME)),
                                email = getCellValueAsString(cell(row, headerMap, "email", UserCols.EMAIL)),
                                mobileNo = mobileNo,
                                token = getCellValueAsString(cell(row, headerMap, "token", UserCols.TOKEN)),
                                pin = getCellValueAsString(cell(row, headerMap, "pin", UserCols.PIN)),
                                role = getCellValueAsString(cell(row, headerMap, "role", UserCols.ROLE)),
                                lastUpdated = getCellValueAsLong(cell(row, headerMap, "lastUpdated", UserCols.LASTUPDATED))
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
                                userId = getCellValueAsString(cell(row, headerMap, "id", UserCols.ID)),
                                name = getCellValueAsString(cell(row, headerMap, "name", UserCols.NAME)),
                                email = getCellValueAsString(cell(row, headerMap, "email", UserCols.EMAIL)),
                                mobileNo = mobileNo,
                                token = getCellValueAsString(cell(row, headerMap, "token", UserCols.TOKEN)),
                                pin = getCellValueAsString(cell(row, headerMap, "pin", UserCols.PIN)),
                                role = getCellValueAsString(cell(row, headerMap, "role", UserCols.ROLE)),
                                lastUpdated = getCellValueAsLong(cell(row, headerMap, "lastUpdated", UserCols.LASTUPDATED))
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
        val sheet = requireSheet(workbook, "UserAdditionalInfoEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingInfos = database.userAdditionalInfoDao()
            .getAllUserAdditionalInfos()
            .associateBy { it.userId }

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val userId = getCellValueAsString(cell(row, headerMap, "userId", UserAdditionalInfoCols.USERID))
                if (userId.isBlank()) {
                    summary.userAdditionalInfoFailed++
                    continue
                }
                val existingInfo = existingInfos[userId]

                val createdAtValue =
                    parseDateString(getCellValueAsString(cell(row, headerMap, "createdAt", UserAdditionalInfoCols.CREATEDAT))).time
                val updatedAtValue =
                    parseDateString(getCellValueAsString(cell(row, headerMap, "updatedAt", UserAdditionalInfoCols.UPDATEDAT))).time

                val userInfo = UserAdditionalInfoEntity(
                    userId = userId,
                    aadhaarNumber = getCellValueAsString(cell(row, headerMap, "aadhaarNumber", UserAdditionalInfoCols.AADHAAR))
                        .takeIf { it.isNotBlank() },
                    address = getCellValueAsString(cell(row, headerMap, "address", UserAdditionalInfoCols.ADDRESS))
                        .takeIf { it.isNotBlank() },
                    emergencyContactPerson = getCellValueAsString(cell(row, headerMap, "emergencyContactPerson", UserAdditionalInfoCols.EMERGENCYCONTACTPERSON))
                        .takeIf { it.isNotBlank() },
                    emergencyContactNumber = getCellValueAsString(cell(row, headerMap, "emergencyContactNumber", UserAdditionalInfoCols.EMERGENCYCONTACTNUMBER))
                        .takeIf { it.isNotBlank() },
                    governmentIdNumber = getCellValueAsString(cell(row, headerMap, "governmentIdNumber", UserAdditionalInfoCols.GOVERNMENTIDNUMBER))
                        .takeIf { it.isNotBlank() },
                    governmentIdType = getCellValueAsString(cell(row, headerMap, "governmentIdType", UserAdditionalInfoCols.GOVERNMENTIDTYPE))
                        .takeIf { it.isNotBlank() },
                    dateOfBirth = getCellValueAsString(cell(row, headerMap, "dateOfBirth", UserAdditionalInfoCols.DATEOFBIRTH))
                        .takeIf { it.isNotBlank() },
                    bloodGroup = getCellValueAsString(cell(row, headerMap, "bloodGroup", UserAdditionalInfoCols.BLOODGROUP))
                        .takeIf { it.isNotBlank() },
                    isActive = getCellValueAsBoolean(cell(row, headerMap, "isActive", UserAdditionalInfoCols.ISACTIVE)),
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
        val sheet = requireSheet(workbook, "StoreEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingStores = database.storeDao().getAllStores().associateBy { it.storeId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val originalStoreId = getCellValueAsString(cell(row, headerMap, "storeId", StoreCols.STOREID))
                val originalUserId = getCellValueAsString(cell(row, headerMap, "userId", StoreCols.USERID))
                
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
                                proprietor = getCellValueAsString(cell(row, headerMap, "proprietor", StoreCols.PROPRIETOR)),
                                name = getCellValueAsString(cell(row, headerMap, "name", StoreCols.NAME)),
                                email = getCellValueAsString(cell(row, headerMap, "email", StoreCols.EMAIL)),
                                phone = getCellValueAsString(cell(row, headerMap, "phone", StoreCols.PHONE)),
                                address = getCellValueAsString(cell(row, headerMap, "address", StoreCols.ADDRESS)),
                                registrationNo = getCellValueAsString(cell(row, headerMap, "registrationNo", StoreCols.REGNO)),
                                gstinNo = getCellValueAsString(cell(row, headerMap, "gstinNo", StoreCols.GSTIN)),
                                panNo = getCellValueAsString(cell(row, headerMap, "panNo", StoreCols.PAN)),
                                image = getCellValueAsString(cell(row, headerMap, "image", StoreCols.IMAGE)),
                                invoiceNo = getCellValueAsInt(cell(row, headerMap, "invoiceNo", StoreCols.INVOICENO)),
                                upiId = getCellValueAsString(cell(row, headerMap, "upiId", StoreCols.UPIID)),
                                lastUpdated = getCellValueAsLong(cell(row, headerMap, "lastUpdated", StoreCols.LASTUPDATED))
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
                                proprietor = getCellValueAsString(cell(row, headerMap, "proprietor", StoreCols.PROPRIETOR)),
                                name = getCellValueAsString(cell(row, headerMap, "name", StoreCols.NAME)),
                                email = getCellValueAsString(cell(row, headerMap, "email", StoreCols.EMAIL)),
                                phone = getCellValueAsString(cell(row, headerMap, "phone", StoreCols.PHONE)),
                                address = getCellValueAsString(cell(row, headerMap, "address", StoreCols.ADDRESS)),
                                registrationNo = getCellValueAsString(cell(row, headerMap, "registrationNo", StoreCols.REGNO)),
                                gstinNo = getCellValueAsString(cell(row, headerMap, "gstinNo", StoreCols.GSTIN)),
                                panNo = getCellValueAsString(cell(row, headerMap, "panNo", StoreCols.PAN)),
                                image = getCellValueAsString(cell(row, headerMap, "image", StoreCols.IMAGE)),
                                invoiceNo = getCellValueAsInt(cell(row, headerMap, "invoiceNo", StoreCols.INVOICENO)),
                                upiId = getCellValueAsString(cell(row, headerMap, "upiId", StoreCols.UPIID)),
                                lastUpdated = getCellValueAsLong(cell(row, headerMap, "lastUpdated", StoreCols.LASTUPDATED))
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
        val sheet = requireSheet(workbook, "CategoryEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingCategories = database.categoryDao().getAllCategories().associateBy { "${it.userId}_${it.storeId}_${it.catName}" }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val catName = getCellValueAsString(cell(row, headerMap, "catName", CategoryCols.CATNAME))
                val originalUserId = getCellValueAsString(cell(row, headerMap, "userId", CategoryCols.USERID))
                val originalStoreId = getCellValueAsString(cell(row, headerMap, "storeId", CategoryCols.STOREID))
                
                // For MERGE mode, check if category exists with current user/store IDs
                // For REPLACE mode, always import
                val key = "${currentUserId}_${currentStoreId}_$catName"
                val existingCategory = existingCategories[key]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingCategory == null) {
                            val category = CategoryEntity(
                                catId = getCellValueAsString(cell(row, headerMap, "catId", CategoryCols.CATID)),
                                catName = catName,
                                gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", CategoryCols.GSWt)),
                                fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", CategoryCols.FNWt)),
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
                            catId = getCellValueAsString(cell(row, headerMap, "catId", CategoryCols.CATID)),
                            catName = catName,
                            gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", CategoryCols.GSWt)),
                            fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", CategoryCols.FNWt)),
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
        val sheet = requireSheet(workbook, "SubCategoryEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingSubCategories = database.subCategoryDao().getAllSubCategories().associateBy { "${it.catId}_${it.userId}_${it.storeId}_${it.subCatName}" }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val originalCatId = getCellValueAsString(cell(row, headerMap, "catId", SubCategoryCols.CATID))
                val originalUserId = getCellValueAsString(cell(row, headerMap, "userId", SubCategoryCols.USERID))
                val originalStoreId = getCellValueAsString(cell(row, headerMap, "storeId", SubCategoryCols.STOREID))
                val subCatName = getCellValueAsString(cell(row, headerMap, "subCatName", SubCategoryCols.SUBCATNAME))
                
                // For MERGE mode, check if subcategory exists with current user/store IDs
                // For REPLACE mode, always import
                val key = "${originalCatId}_${currentUserId}_${currentStoreId}_${subCatName}"
                val existingSubCategory = existingSubCategories[key]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingSubCategory == null) {
                            val subCategory = SubCategoryEntity(
                                subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", SubCategoryCols.SUBCATID)),
                                catId = originalCatId,
                                userId = currentUserId,
                                storeId = currentStoreId,
                                catName = getCellValueAsString(cell(row, headerMap, "catName", SubCategoryCols.CATNAME)),
                                subCatName = subCatName,
                                quantity = getCellValueAsInt(cell(row, headerMap, "quantity", SubCategoryCols.QUANTITY)),
                                gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", SubCategoryCols.GSWt)),
                                fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", SubCategoryCols.FNWt))
                            )
                            database.subCategoryDao().insertSubCategory(subCategory)
                            summary.subCategoriesAdded++
                        } else {
                            summary.subCategoriesSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val subCategory = SubCategoryEntity(
                            subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", SubCategoryCols.SUBCATID)),
                            catId = originalCatId,
                            userId = currentUserId,
                            storeId = currentStoreId,
                            catName = getCellValueAsString(cell(row, headerMap, "catName", SubCategoryCols.CATNAME)),
                            subCatName = subCatName,
                            quantity = getCellValueAsInt(cell(row, headerMap, "quantity", SubCategoryCols.QUANTITY)),
                            gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", SubCategoryCols.GSWt)),
                            fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", SubCategoryCols.FNWt))
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
        val sheet = requireSheet(workbook, "ItemEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingItems = database.itemDao().getAllItems().associateBy { "${it.userId}_${it.storeId}_${it.itemAddName}" }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val originalUserId = getCellValueAsString(cell(row, headerMap, "userId", ItemCols.USERID))
                val originalStoreId = getCellValueAsString(cell(row, headerMap, "storeId", ItemCols.STOREID))
                val itemAddName = getCellValueAsString(cell(row, headerMap, "itemAddName", ItemCols.ITEMADDNAME))
                val key = "${currentUserId}_${currentStoreId}_${itemAddName}"
                val existingItem = existingItems[key]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingItem == null) {
                            val item = ItemEntity(
                                itemId = getCellValueAsString(cell(row, headerMap, "itemId", ItemCols.ITEMID)),
                                itemAddName = itemAddName,
                                catId = getCellValueAsString(cell(row, headerMap, "catId", ItemCols.CATID)),
                                userId = currentUserId,
                                storeId = currentStoreId,
                                catName = getCellValueAsString(cell(row, headerMap, "catName", ItemCols.CATNAME)),
                                subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", ItemCols.SUBCATID)),
                                subCatName = getCellValueAsString(cell(row, headerMap, "subCatName", ItemCols.SUBCATNAME)),
                                entryType = getCellValueAsString(cell(row, headerMap, "entryType", ItemCols.ENTRYTYPE)),
                                quantity = getCellValueAsInt(cell(row, headerMap, "quantity", ItemCols.QUANTITY)),
                                gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", ItemCols.GSWt)),
                                ntWt = getCellValueAsDouble(cell(row, headerMap, "ntWt", ItemCols.NTWt)),
                                fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", ItemCols.FNWt)),
                                purity = getCellValueAsString(cell(row, headerMap, "purity", ItemCols.PURITY)),
                                crgType = getCellValueAsString(cell(row, headerMap, "crgType", ItemCols.CRGTYPE)),
                                crg = getCellValueAsDouble(cell(row, headerMap, "crg", ItemCols.CRG)),
                                othCrgDes = getCellValueAsString(cell(row, headerMap, "compDes", ItemCols.OTHCRGDES)),
                                othCrg = getCellValueAsDouble(cell(row, headerMap, "compCrg", ItemCols.OTHCRG)),
                                cgst = getCellValueAsDouble(cell(row, headerMap, "cgst", ItemCols.CGST)),
                                sgst = getCellValueAsDouble(cell(row, headerMap, "sgst", ItemCols.SGST)),
                                igst = getCellValueAsDouble(cell(row, headerMap, "igst", ItemCols.IGST)),
                                huid = getCellValueAsString(cell(row, headerMap, "huid", ItemCols.HUID)),
                                unit = getCellValueAsString(cell(row, headerMap, "unit", ItemCols.UNIT)),
                                addDesKey = getCellValueAsString(cell(row, headerMap, "addDesKey", ItemCols.ADDDESKEY)),
                                addDesValue = getCellValueAsString(cell(row, headerMap, "addDesValue", ItemCols.ADDDESVALUE)),
                                addDate = parseDateString(getCellValueAsString(cell(row, headerMap, "addDate", ItemCols.ADDDATE))),
                                modifiedDate = parseDateString(getCellValueAsString(cell(row, headerMap, "modifiedDate", ItemCols.MODIFIEDDATE))),
                                sellerFirmId = getCellValueAsString(cell(row, headerMap, "sellerFirmId", ItemCols.SELLERFIRMID)),
                                purchaseOrderId = getCellValueAsString(cell(row, headerMap, "purchaseOrderId", ItemCols.PURCHASEORDERID)),
                                purchaseItemId = getCellValueAsString(cell(row, headerMap, "purchaseItemId", ItemCols.PURCHASEITEMID))
                            )
                            database.itemDao().insertItem(item)
                            summary.itemsAdded++
                        } else {
                            summary.itemsSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val item = ItemEntity(
                            itemId = getCellValueAsString(cell(row, headerMap, "itemId", ItemCols.ITEMID)),
                            itemAddName = itemAddName,
                            catId = getCellValueAsString(cell(row, headerMap, "catId", ItemCols.CATID)),
                            userId = currentUserId,
                            storeId = currentStoreId,
                            catName = getCellValueAsString(cell(row, headerMap, "catName", ItemCols.CATNAME)),
                            subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", ItemCols.SUBCATID)),
                            subCatName = getCellValueAsString(cell(row, headerMap, "subCatName", ItemCols.SUBCATNAME)),
                            entryType = getCellValueAsString(cell(row, headerMap, "entryType", ItemCols.ENTRYTYPE)),
                            quantity = getCellValueAsInt(cell(row, headerMap, "quantity", ItemCols.QUANTITY)),
                            gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", ItemCols.GSWt)),
                            ntWt = getCellValueAsDouble(cell(row, headerMap, "ntWt", ItemCols.NTWt)),
                            fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", ItemCols.FNWt)),
                            purity = getCellValueAsString(cell(row, headerMap, "purity", ItemCols.PURITY)),
                            crgType = getCellValueAsString(cell(row, headerMap, "crgType", ItemCols.CRGTYPE)),
                            crg = getCellValueAsDouble(cell(row, headerMap, "crg", ItemCols.CRG)),
                            othCrgDes = getCellValueAsString(cell(row, headerMap, "compDes", ItemCols.OTHCRGDES)),
                            othCrg = getCellValueAsDouble(cell(row, headerMap, "compCrg", ItemCols.OTHCRG)),
                            cgst = getCellValueAsDouble(cell(row, headerMap, "cgst", ItemCols.CGST)),
                            sgst = getCellValueAsDouble(cell(row, headerMap, "sgst", ItemCols.SGST)),
                            igst = getCellValueAsDouble(cell(row, headerMap, "igst", ItemCols.IGST)),
                            huid = getCellValueAsString(cell(row, headerMap, "huid", ItemCols.HUID)),
                            unit = getCellValueAsString(cell(row, headerMap, "unit", ItemCols.UNIT)),
                            addDesKey = getCellValueAsString(cell(row, headerMap, "addDesKey", ItemCols.ADDDESKEY)),
                            addDesValue = getCellValueAsString(cell(row, headerMap, "addDesValue", ItemCols.ADDDESVALUE)),
                            addDate = parseDateString(getCellValueAsString(cell(row, headerMap, "addDate", ItemCols.ADDDATE))),
                            modifiedDate = parseDateString(getCellValueAsString(cell(row, headerMap, "modifiedDate", ItemCols.MODIFIEDDATE))),
                            sellerFirmId = getCellValueAsString(cell(row, headerMap, "sellerFirmId", ItemCols.SELLERFIRMID)),
                            purchaseOrderId = getCellValueAsString(cell(row, headerMap, "purchaseOrderId", ItemCols.PURCHASEORDERID)),
                            purchaseItemId = getCellValueAsString(cell(row, headerMap, "purchaseItemId", ItemCols.PURCHASEITEMID))
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
        val sheet = requireSheet(workbook, "CustomerEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingCustomers = database.customerDao().getAllCustomersList()
            .filter { it.userId == currentUserId && it.storeId == currentStoreId }
            .associateBy { it.mobileNo }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val mobileNo = getCellValueAsString(cell(row, headerMap, "mobileNo", CustomerCols.MOBILENO))
                val existingCustomer = existingCustomers[mobileNo]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingCustomer == null) {
                            val customer = CustomerEntity(
                                mobileNo = mobileNo,
                                name = getCellValueAsString(cell(row, headerMap, "name", CustomerCols.NAME)),
                                address = getCellValueAsString(cell(row, headerMap, "address", CustomerCols.ADDRESS)),
                                gstin_pan = getCellValueAsString(cell(row, headerMap, "gstin_pan", CustomerCols.GSTINPAN)),
                                addDate = parseDateString(getCellValueAsString(cell(row, headerMap, "addDate", CustomerCols.ADDDATE))),
                                lastModifiedDate = parseDateString(getCellValueAsString(cell(row, headerMap, "lastModifiedDate", CustomerCols.LASTMODIFIED))),
                                totalItemBought = getCellValueAsInt(cell(row, headerMap, "totalItemBought", CustomerCols.TOTALITEMBOUGHT)),
                                totalAmount = getCellValueAsDouble(cell(row, headerMap, "totalAmount", CustomerCols.TOTALAMOUNT)),
                                notes = getCellValueAsString(cell(row, headerMap, "notes", CustomerCols.NOTES)),
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
                            name = getCellValueAsString(cell(row, headerMap, "name", CustomerCols.NAME)),
                            address = getCellValueAsString(cell(row, headerMap, "address", CustomerCols.ADDRESS)),
                            gstin_pan = getCellValueAsString(cell(row, headerMap, "gstin_pan", CustomerCols.GSTINPAN)),
                            addDate = parseDateString(getCellValueAsString(cell(row, headerMap, "addDate", CustomerCols.ADDDATE))),
                            lastModifiedDate = parseDateString(getCellValueAsString(cell(row, headerMap, "lastModifiedDate", CustomerCols.LASTMODIFIED))),
                            totalItemBought = getCellValueAsInt(cell(row, headerMap, "totalItemBought", CustomerCols.TOTALITEMBOUGHT)),
                            totalAmount = getCellValueAsDouble(cell(row, headerMap, "totalAmount", CustomerCols.TOTALAMOUNT)),
                            notes = getCellValueAsString(cell(row, headerMap, "notes", CustomerCols.NOTES)),
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
        val sheet = requireSheet(workbook, "CustomerKhataBookPlanEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingPlans = database.customerKhataBookPlanDao()
            .getPlansByUserAndStore(currentUserId, currentStoreId)
            .associateBy { it.planId }

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val planId = getCellValueAsString(cell(row, headerMap, "planId", KhataBookPlanCols.PLANID))
                if (planId.isBlank()) {
                    summary.khataBookPlansFailed++
                    continue
                }
                val existingPlan = existingPlans[planId]

                val createdAtValue =
                    parseDateString(getCellValueAsString(cell(row, headerMap, "createdAt", KhataBookPlanCols.CREATEDAT))).time
                val updatedAtValue =
                    parseDateString(getCellValueAsString(cell(row, headerMap, "updatedAt", KhataBookPlanCols.UPDATEDAT))).time

                val plan = CustomerKhataBookPlanEntity(
                    planId = planId,
                    name = getCellValueAsString(cell(row, headerMap, "name", KhataBookPlanCols.NAME)),
                    payMonths = getCellValueAsInt(cell(row, headerMap, "payMonths", KhataBookPlanCols.PAYMONTHS)),
                    benefitMonths = getCellValueAsInt(cell(row, headerMap, "benefitMonths", KhataBookPlanCols.BENEFITMONTHS)),
                    description = getCellValueAsString(cell(row, headerMap, "description", KhataBookPlanCols.DESCRIPTION)),
                    benefitPercentage = getCellValueAsDouble(cell(row, headerMap, "benefitPercentage", KhataBookPlanCols.BENEFITPERCENTAGE)),
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
        val sheet = requireSheet(workbook, "CustomerKhataBookEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingKhataBooks = database.customerKhataBookDao().getAllKhataBooksList()
            .filter { it.userId == currentUserId && it.storeId == currentStoreId }
            .associateBy { it.khataBookId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val khataBookId = getCellValueAsString(cell(row, headerMap, "khataBookId", KhataBookCols.KHATABOOKID))
                val existingKhataBook = existingKhataBooks[khataBookId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingKhataBook == null) {
                            val khataBook = CustomerKhataBookEntity(
                                khataBookId = khataBookId,
                                customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", KhataBookCols.CUSTOMERMOBILE)),
                                planName = getCellValueAsString(cell(row, headerMap, "planName", KhataBookCols.PLANNAME)),
                                startDate = parseDateString(getCellValueAsString(cell(row, headerMap, "startDate", KhataBookCols.STARTDATE))),
                                endDate = parseDateString(getCellValueAsString(cell(row, headerMap, "endDate", KhataBookCols.ENDDATE))),
                                monthlyAmount = getCellValueAsDouble(cell(row, headerMap, "monthlyAmount", KhataBookCols.MONTHLYAMOUNT)),
                                totalMonths = getCellValueAsInt(cell(row, headerMap, "totalMonths", KhataBookCols.TOTALMONTHS)),
                                totalAmount = getCellValueAsDouble(cell(row, headerMap, "totalAmount", KhataBookCols.TOTALAMOUNT)),
                                status = getCellValueAsString(cell(row, headerMap, "status", KhataBookCols.STATUS)),
                                notes = getCellValueAsString(cell(row, headerMap, "notes", KhataBookCols.NOTES)),
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
                            customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", KhataBookCols.CUSTOMERMOBILE)),
                            planName = getCellValueAsString(cell(row, headerMap, "planName", KhataBookCols.PLANNAME)),
                            startDate = parseDateString(getCellValueAsString(cell(row, headerMap, "startDate", KhataBookCols.STARTDATE))),
                            endDate = parseDateString(getCellValueAsString(cell(row, headerMap, "endDate", KhataBookCols.ENDDATE))),
                            monthlyAmount = getCellValueAsDouble(cell(row, headerMap, "monthlyAmount", KhataBookCols.MONTHLYAMOUNT)),
                            totalMonths = getCellValueAsInt(cell(row, headerMap, "totalMonths", KhataBookCols.TOTALMONTHS)),
                            totalAmount = getCellValueAsDouble(cell(row, headerMap, "totalAmount", KhataBookCols.TOTALAMOUNT)),
                            status = getCellValueAsString(cell(row, headerMap, "status", KhataBookCols.STATUS)),
                            notes = getCellValueAsString(cell(row, headerMap, "notes", KhataBookCols.NOTES)),
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
        val sheet = requireSheet(workbook, "CustomerTransactionEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingTransactions = database.customerTransactionDao().getAllTransactions()
            .filter { it.userId == currentUserId && it.storeId == currentStoreId }
            .associateBy { it.transactionId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val transactionId = getCellValueAsString(cell(row, headerMap, "transactionId", TransactionCols.TRANSACTIONID))
                val existingTransaction = existingTransactions[transactionId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingTransaction == null) {
                            val transaction = CustomerTransactionEntity(
                                transactionId = transactionId,
                                customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", TransactionCols.CUSTOMERMOBILE)),
                                transactionDate = parseDateString(getCellValueAsString(cell(row, headerMap, "transactionDate", TransactionCols.TRANSACTIONDATE))),
                                amount = getCellValueAsDouble(cell(row, headerMap, "amount", TransactionCols.AMOUNT)),
                                transactionType = getCellValueAsString(cell(row, headerMap, "transactionType", TransactionCols.TRANSACTIONTYPE)),
                                category = getCellValueAsString(cell(row, headerMap, "category", TransactionCols.CATEGORY)),
                                description = getCellValueAsString(cell(row, headerMap, "description", TransactionCols.DESCRIPTION)),
                                referenceNumber = getCellValueAsString(cell(row, headerMap, "referenceNumber", TransactionCols.REFERENCENUMBER)),
                                paymentMethod = getCellValueAsString(cell(row, headerMap, "paymentMethod", TransactionCols.PAYMENTMETHOD)),
                                khataBookId = getCellValueAsString(cell(row, headerMap, "khataBookId", TransactionCols.KHATABOOKID)).takeIf { it.isNotEmpty() },
                                monthNumber = getCellValueAsInt(cell(row, headerMap, "monthNumber", TransactionCols.MONTHNUMBER)).takeIf { it > 0 },
                                notes = getCellValueAsString(cell(row, headerMap, "notes", TransactionCols.NOTES)),
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
                            customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", TransactionCols.CUSTOMERMOBILE)),
                            transactionDate = parseDateString(getCellValueAsString(cell(row, headerMap, "transactionDate", TransactionCols.TRANSACTIONDATE))),
                            amount = getCellValueAsDouble(cell(row, headerMap, "amount", TransactionCols.AMOUNT)),
                            transactionType = getCellValueAsString(cell(row, headerMap, "transactionType", TransactionCols.TRANSACTIONTYPE)),
                            category = getCellValueAsString(cell(row, headerMap, "category", TransactionCols.CATEGORY)),
                            description = getCellValueAsString(cell(row, headerMap, "description", TransactionCols.DESCRIPTION)),
                            referenceNumber = getCellValueAsString(cell(row, headerMap, "referenceNumber", TransactionCols.REFERENCENUMBER)),
                            paymentMethod = getCellValueAsString(cell(row, headerMap, "paymentMethod", TransactionCols.PAYMENTMETHOD)),
                            khataBookId = getCellValueAsString(cell(row, headerMap, "khataBookId", TransactionCols.KHATABOOKID)).takeIf { it.isNotEmpty() },
                            monthNumber = getCellValueAsInt(cell(row, headerMap, "monthNumber", TransactionCols.MONTHNUMBER)).takeIf { it > 0 },
                            notes = getCellValueAsString(cell(row, headerMap, "notes", TransactionCols.NOTES)),
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
        val sheet = requireSheet(workbook, "OrderEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingOrders = database.orderDao().getAllOrders()
            .filter { it.userId == currentUserId && it.storeId == currentStoreId }
            .associateBy { it.orderId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val orderId = getCellValueAsString(cell(row, headerMap, "orderId", OrderCols.ORDERID))
                val existingOrder = existingOrders[orderId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingOrder == null) {
                            val order = OrderEntity(
                                orderId = orderId,
                                customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", OrderCols.CUSTOMERMOBILE)),
                                storeId = currentStoreId,
                                userId = currentUserId,
                                orderDate = parseDateString(getCellValueAsString(cell(row, headerMap, "orderDate", OrderCols.ORDERDATE))),
                                totalAmount = getCellValueAsDouble(cell(row, headerMap, "totalAmount", OrderCols.TOTALAMOUNT)),
                                totalTax = getCellValueAsDouble(cell(row, headerMap, "totalTax", OrderCols.TOTALTAX)),
                                totalCharge = getCellValueAsDouble(cell(row, headerMap, "totalCharge", OrderCols.TOTALCHARGE)),
                                discount = getCellValueAsDouble(cell(row, headerMap, "discount", OrderCols.DISCOUNT)),
                                note = getCellValueAsString(cell(row, headerMap, "note", OrderCols.NOTE))
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
                            customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", OrderCols.CUSTOMERMOBILE)),
                            storeId = currentStoreId,
                            userId = currentUserId,
                            orderDate = parseDateString(getCellValueAsString(cell(row, headerMap, "orderDate", OrderCols.ORDERDATE))),
                            totalAmount = getCellValueAsDouble(cell(row, headerMap, "totalAmount", OrderCols.TOTALAMOUNT)),
                            totalTax = getCellValueAsDouble(cell(row, headerMap, "totalTax", OrderCols.TOTALTAX)),
                            totalCharge = getCellValueAsDouble(cell(row, headerMap, "totalCharge", OrderCols.TOTALCHARGE)),
                            discount = getCellValueAsDouble(cell(row, headerMap, "discount", OrderCols.DISCOUNT)),
                            note = getCellValueAsString(cell(row, headerMap, "note", OrderCols.NOTE))
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
        val sheet = requireSheet(workbook, "OrderItemEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingOrderItems = database.orderDao().getAllOrderItems().associateBy { it.orderItemId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val orderItemId = getCellValueAsString(cell(row, headerMap, "orderItemId", OrderItemCols.ORDERITEMID))
                val existingOrderItem = existingOrderItems[orderItemId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingOrderItem == null) {
                            val orderItem = OrderItemEntity(
                                orderItemId = orderItemId,
                                orderId = getCellValueAsString(cell(row, headerMap, "orderId", OrderItemCols.ORDERID)),
                                orderDate = parseDateString(getCellValueAsString(cell(row, headerMap, "orderDate", OrderItemCols.ORDERDATE))),
                                itemId = getCellValueAsString(cell(row, headerMap, "itemId", OrderItemCols.ITEMID)),
                                customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", OrderItemCols.CUSTOMERMOBILE)),
                                catId = getCellValueAsString(cell(row, headerMap, "catId", OrderItemCols.CATID)),
                                catName = getCellValueAsString(cell(row, headerMap, "catName", OrderItemCols.CATNAME)),
                                itemAddName = getCellValueAsString(cell(row, headerMap, "itemAddName", OrderItemCols.ITEMADDNAME)),
                                subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", OrderItemCols.SUBCATID)),
                                subCatName = getCellValueAsString(cell(row, headerMap, "subCatName", OrderItemCols.SUBCATNAME)),
                                entryType = getCellValueAsString(cell(row, headerMap, "entryType", OrderItemCols.ENTRYTYPE)),
                                quantity = getCellValueAsInt(cell(row, headerMap, "quantity", OrderItemCols.QUANTITY)),
                                gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", OrderItemCols.GSWt)),
                                ntWt = getCellValueAsDouble(cell(row, headerMap, "ntWt", OrderItemCols.NTWt)),
                                fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", OrderItemCols.FNWt)),
                                fnMetalPrice = getCellValueAsDouble(cell(row, headerMap, "fnMetalPrice", OrderItemCols.FNMETALPRICE)),
                                purity = getCellValueAsString(cell(row, headerMap, "purity", OrderItemCols.PURITY)),
                                crgType = getCellValueAsString(cell(row, headerMap, "crgType", OrderItemCols.CRGTYPE)),
                                crg = getCellValueAsDouble(cell(row, headerMap, "crg", OrderItemCols.CRG)),
                                othCrgDes = getCellValueAsString(cell(row, headerMap, "compDes", OrderItemCols.OTHCRGDES)),
                                othCrg = getCellValueAsDouble(cell(row, headerMap, "compCrg", OrderItemCols.OTHCRG)),
                                cgst = getCellValueAsDouble(cell(row, headerMap, "cgst", OrderItemCols.CGST)),
                                sgst = getCellValueAsDouble(cell(row, headerMap, "sgst", OrderItemCols.SGST)),
                                igst = getCellValueAsDouble(cell(row, headerMap, "igst", OrderItemCols.IGST)),
                                huid = getCellValueAsString(cell(row, headerMap, "huid", OrderItemCols.HUID)),
                                addDesKey = getCellValueAsString(cell(row, headerMap, "addDesKey", OrderItemCols.ADDDESKEY)),
                                addDesValue = getCellValueAsString(cell(row, headerMap, "addDesValue", OrderItemCols.ADDDESVALUE)),
                                price = getCellValueAsDouble(cell(row, headerMap, "price", OrderItemCols.PRICE)),
                                charge = getCellValueAsDouble(cell(row, headerMap, "charge", OrderItemCols.CHARGE)),
                                tax = getCellValueAsDouble(cell(row, headerMap, "tax", OrderItemCols.TAX)),
                                sellerFirmId = getCellValueAsString(cell(row, headerMap, "sellerFirmId", OrderItemCols.SELLERFIRMID)),
                                purchaseOrderId = getCellValueAsString(cell(row, headerMap, "purchaseOrderId", OrderItemCols.PURCHASEORDERID)),
                                purchaseItemId = getCellValueAsString(cell(row, headerMap, "purchaseItemId", OrderItemCols.PURCHASEITEMID))
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
                            orderId = getCellValueAsString(cell(row, headerMap, "orderId", OrderItemCols.ORDERID)),
                            orderDate = parseDateString(getCellValueAsString(cell(row, headerMap, "orderDate", OrderItemCols.ORDERDATE))),
                            itemId = getCellValueAsString(cell(row, headerMap, "itemId", OrderItemCols.ITEMID)),
                            customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", OrderItemCols.CUSTOMERMOBILE)),
                            catId = getCellValueAsString(cell(row, headerMap, "catId", OrderItemCols.CATID)),
                            catName = getCellValueAsString(cell(row, headerMap, "catName", OrderItemCols.CATNAME)),
                            itemAddName = getCellValueAsString(cell(row, headerMap, "itemAddName", OrderItemCols.ITEMADDNAME)),
                            subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", OrderItemCols.SUBCATID)),
                            subCatName = getCellValueAsString(cell(row, headerMap, "subCatName", OrderItemCols.SUBCATNAME)),
                            entryType = getCellValueAsString(cell(row, headerMap, "entryType", OrderItemCols.ENTRYTYPE)),
                            quantity = getCellValueAsInt(cell(row, headerMap, "quantity", OrderItemCols.QUANTITY)),
                            gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", OrderItemCols.GSWt)),
                            ntWt = getCellValueAsDouble(cell(row, headerMap, "ntWt", OrderItemCols.NTWt)),
                            fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", OrderItemCols.FNWt)),
                            fnMetalPrice = getCellValueAsDouble(cell(row, headerMap, "fnMetalPrice", OrderItemCols.FNMETALPRICE)),
                            purity = getCellValueAsString(cell(row, headerMap, "purity", OrderItemCols.PURITY)),
                            crgType = getCellValueAsString(cell(row, headerMap, "crgType", OrderItemCols.CRGTYPE)),
                            crg = getCellValueAsDouble(cell(row, headerMap, "crg", OrderItemCols.CRG)),
                            othCrgDes = getCellValueAsString(cell(row, headerMap, "compDes", OrderItemCols.OTHCRGDES)),
                            othCrg = getCellValueAsDouble(cell(row, headerMap, "compCrg", OrderItemCols.OTHCRG)),
                            cgst = getCellValueAsDouble(cell(row, headerMap, "cgst", OrderItemCols.CGST)),
                            sgst = getCellValueAsDouble(cell(row, headerMap, "sgst", OrderItemCols.SGST)),
                            igst = getCellValueAsDouble(cell(row, headerMap, "igst", OrderItemCols.IGST)),
                            huid = getCellValueAsString(cell(row, headerMap, "huid", OrderItemCols.HUID)),
                            addDesKey = getCellValueAsString(cell(row, headerMap, "addDesKey", OrderItemCols.ADDDESKEY)),
                            addDesValue = getCellValueAsString(cell(row, headerMap, "addDesValue", OrderItemCols.ADDDESVALUE)),
                            price = getCellValueAsDouble(cell(row, headerMap, "price", OrderItemCols.PRICE)),
                            charge = getCellValueAsDouble(cell(row, headerMap, "charge", OrderItemCols.CHARGE)),
                            tax = getCellValueAsDouble(cell(row, headerMap, "tax", OrderItemCols.TAX)),
                            sellerFirmId = getCellValueAsString(cell(row, headerMap, "sellerFirmId", OrderItemCols.SELLERFIRMID)),
                            purchaseOrderId = getCellValueAsString(cell(row, headerMap, "purchaseOrderId", OrderItemCols.PURCHASEORDERID)),
                            purchaseItemId = getCellValueAsString(cell(row, headerMap, "purchaseItemId", OrderItemCols.PURCHASEITEMID))
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
        val sheet = requireSheet(workbook, "ExchangeItemEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingExchangeItems = database.orderDao().getAllExchangeItems().associateBy { it.exchangeItemId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val exchangeItemId = getCellValueAsString(cell(row, headerMap, "exchangeItemId", ExchangeItemCols.EXCHANGEITEMID))
                val existingExchangeItem = existingExchangeItems[exchangeItemId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingExchangeItem == null) {
                            val exchangeItem = ExchangeItemEntity(
                                exchangeItemId = exchangeItemId,
                                orderId = getCellValueAsString(cell(row, headerMap, "orderId", ExchangeItemCols.ORDERID)),
                                orderDate = parseDateString(getCellValueAsString(cell(row, headerMap, "orderDate", ExchangeItemCols.ORDERDATE))),
                                customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", ExchangeItemCols.CUSTOMERMOBILE)),
                                metalType = getCellValueAsString(cell(row, headerMap, "metalType", ExchangeItemCols.METALTYPE)),
                                purity = getCellValueAsString(cell(row, headerMap, "purity", ExchangeItemCols.PURITY)),
                                grossWeight = getCellValueAsDouble(cell(row, headerMap, "grossWeight", ExchangeItemCols.GROSSWEIGHT)),
                                fineWeight = getCellValueAsDouble(cell(row, headerMap, "fineWeight", ExchangeItemCols.FINEWEIGHT)),
                                price = getCellValueAsDouble(cell(row, headerMap, "price", ExchangeItemCols.PRICE)),
                                isExchangedByMetal = getCellValueAsBoolean(cell(row, headerMap, "isExchangedByMetal", ExchangeItemCols.ISEXCHANGEDBYMETAL)),
                                exchangeValue = getCellValueAsDouble(cell(row, headerMap, "exchangeValue", ExchangeItemCols.EXCHANGEVALUE)),
                                addDate = parseDateString(getCellValueAsString(cell(row, headerMap, "addDate", ExchangeItemCols.ADDDATE)))
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
                            orderId = getCellValueAsString(cell(row, headerMap, "orderId", ExchangeItemCols.ORDERID)),
                            orderDate = parseDateString(getCellValueAsString(cell(row, headerMap, "orderDate", ExchangeItemCols.ORDERDATE))),
                            customerMobile = getCellValueAsString(cell(row, headerMap, "customerMobile", ExchangeItemCols.CUSTOMERMOBILE)),
                            metalType = getCellValueAsString(cell(row, headerMap, "metalType", ExchangeItemCols.METALTYPE)),
                            purity = getCellValueAsString(cell(row, headerMap, "purity", ExchangeItemCols.PURITY)),
                            grossWeight = getCellValueAsDouble(cell(row, headerMap, "grossWeight", ExchangeItemCols.GROSSWEIGHT)),
                            fineWeight = getCellValueAsDouble(cell(row, headerMap, "fineWeight", ExchangeItemCols.FINEWEIGHT)),
                            price = getCellValueAsDouble(cell(row, headerMap, "price", ExchangeItemCols.PRICE)),
                            isExchangedByMetal = getCellValueAsBoolean(cell(row, headerMap, "isExchangedByMetal", ExchangeItemCols.ISEXCHANGEDBYMETAL)),
                            exchangeValue = getCellValueAsDouble(cell(row, headerMap, "exchangeValue", ExchangeItemCols.EXCHANGEVALUE)),
                            addDate = parseDateString(getCellValueAsString(cell(row, headerMap, "addDate", ExchangeItemCols.ADDDATE)))
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
        val sheet = requireSheet(workbook, "FirmEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingFirms = database.purchaseDao().getAllFirms().associateBy { it.firmName }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val firmName = getCellValueAsString(cell(row, headerMap, "firmName", FirmCols.FIRMNAME))
                val existingFirm = existingFirms[firmName]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingFirm == null) {
                            val firm = FirmEntity(
                                firmId = getCellValueAsString(cell(row, headerMap, "firmId", FirmCols.FIRMID)),
                                firmName = firmName,
                                firmMobileNumber = getCellValueAsString(cell(row, headerMap, "firmMobileNumber", FirmCols.FIRMMOBILENUMBER)),
                                gstNumber = getCellValueAsString(cell(row, headerMap, "gstNumber", FirmCols.GSTNUMBER)),
                                address = getCellValueAsString(cell(row, headerMap, "address", FirmCols.ADDRESS))
                            )
                            database.purchaseDao().insertFirm(firm)
                            summary.firmsAdded++
                        } else {
                            summary.firmsSkipped++
                        }
                    }
                    RestoreMode.REPLACE -> {
                        val firm = FirmEntity(
                            firmId = getCellValueAsString(cell(row, headerMap, "firmId", FirmCols.FIRMID)),
                            firmName = firmName,
                            firmMobileNumber = getCellValueAsString(cell(row, headerMap, "firmMobileNumber", FirmCols.FIRMMOBILENUMBER)),
                            gstNumber = getCellValueAsString(cell(row, headerMap, "gstNumber", FirmCols.GSTNUMBER)),
                            address = getCellValueAsString(cell(row, headerMap, "address", FirmCols.ADDRESS))
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
        val sheet = requireSheet(workbook, "PurchaseOrderEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingPurchaseOrders = database.purchaseDao().getAllPurchaseOrders().associateBy { it.purchaseOrderId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val purchaseOrderId = getCellValueAsString(cell(row, headerMap, "purchaseOrderId", PurchaseOrderCols.PURCHASEORDERID))
                val existingPurchaseOrder = existingPurchaseOrders[purchaseOrderId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingPurchaseOrder == null) {
                            val purchaseOrder = PurchaseOrderEntity(
                                purchaseOrderId = purchaseOrderId,
                                sellerId = getCellValueAsString(cell(row, headerMap, "sellerId", PurchaseOrderCols.SELLERID)),
                                billNo = getCellValueAsString(cell(row, headerMap, "billNo", PurchaseOrderCols.BILLNO)),
                                billDate = getCellValueAsString(cell(row, headerMap, "billDate", PurchaseOrderCols.BILLDATE)),
                                entryDate = getCellValueAsString(cell(row, headerMap, "entryDate", PurchaseOrderCols.ENTRYDATE)),
                                extraChargeDescription = getCellValueAsString(cell(row, headerMap, "extraChargeDescription", PurchaseOrderCols.EXTRACHARGEDESCRIPTION)),
                                extraCharge = getCellValueAsDouble(cell(row, headerMap, "extraCharge", PurchaseOrderCols.EXTRACHARGE)),
                                totalFinalWeight = getCellValueAsDouble(cell(row, headerMap, "totalFinalWeight", PurchaseOrderCols.TOTALFINALWEIGHT)),
                                totalFinalAmount = getCellValueAsDouble(cell(row, headerMap, "totalFinalAmount", PurchaseOrderCols.TOTALFINALAMOUNT)),
                                notes = getCellValueAsString(cell(row, headerMap, "notes", PurchaseOrderCols.NOTES)),
                                cgstPercent = getCellValueAsDouble(cell(row, headerMap, "cgstPercent", PurchaseOrderCols.CGSTPERCENT)),
                                sgstPercent = getCellValueAsDouble(cell(row, headerMap, "sgstPercent", PurchaseOrderCols.SGSTPERCENT)),
                                igstPercent = getCellValueAsDouble(cell(row, headerMap, "igstPercent", PurchaseOrderCols.IGSTPERCENT))
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
                            sellerId = getCellValueAsString(cell(row, headerMap, "sellerId", PurchaseOrderCols.SELLERID)),
                            billNo = getCellValueAsString(cell(row, headerMap, "billNo", PurchaseOrderCols.BILLNO)),
                            billDate = getCellValueAsString(cell(row, headerMap, "billDate", PurchaseOrderCols.BILLDATE)),
                            entryDate = getCellValueAsString(cell(row, headerMap, "entryDate", PurchaseOrderCols.ENTRYDATE)),
                            extraChargeDescription = getCellValueAsString(cell(row, headerMap, "extraChargeDescription", PurchaseOrderCols.EXTRACHARGEDESCRIPTION)),
                            extraCharge = getCellValueAsDouble(cell(row, headerMap, "extraCharge", PurchaseOrderCols.EXTRACHARGE)),
                            totalFinalWeight = getCellValueAsDouble(cell(row, headerMap, "totalFinalWeight", PurchaseOrderCols.TOTALFINALWEIGHT)),
                            totalFinalAmount = getCellValueAsDouble(cell(row, headerMap, "totalFinalAmount", PurchaseOrderCols.TOTALFINALAMOUNT)),
                            notes = getCellValueAsString(cell(row, headerMap, "notes", PurchaseOrderCols.NOTES)),
                            cgstPercent = getCellValueAsDouble(cell(row, headerMap, "cgstPercent", PurchaseOrderCols.CGSTPERCENT)),
                            sgstPercent = getCellValueAsDouble(cell(row, headerMap, "sgstPercent", PurchaseOrderCols.SGSTPERCENT)),
                            igstPercent = getCellValueAsDouble(cell(row, headerMap, "igstPercent", PurchaseOrderCols.IGSTPERCENT))
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
        val sheet = requireSheet(workbook, "PurchaseOrderItemEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingPurchaseOrderItems = database.purchaseDao().getAllPurchaseOrderItems().associateBy { it.purchaseItemId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val purchaseItemId = getCellValueAsString(cell(row, headerMap, "purchaseItemId", PurchaseOrderItemCols.PURCHASEITEMID))
                val existingPurchaseOrderItem = existingPurchaseOrderItems[purchaseItemId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingPurchaseOrderItem == null) {
                            val purchaseOrderItem = PurchaseOrderItemEntity(
                                purchaseItemId = purchaseItemId,
                                purchaseOrderId = getCellValueAsString(cell(row, headerMap, "purchaseOrderId", PurchaseOrderItemCols.PURCHASEORDERID)),
                                catId = getCellValueAsString(cell(row, headerMap, "catId", PurchaseOrderItemCols.CATID)),
                                catName = getCellValueAsString(cell(row, headerMap, "catName", PurchaseOrderItemCols.CATNAME)),
                                subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", PurchaseOrderItemCols.SUBCATID)),
                                subCatName = getCellValueAsString(cell(row, headerMap, "subCatName", PurchaseOrderItemCols.SUBCATNAME)),
                                gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", PurchaseOrderItemCols.GSWt)),
                                purity = getCellValueAsString(cell(row, headerMap, "purity", PurchaseOrderItemCols.PURITY)),
                                ntWt = getCellValueAsDouble(cell(row, headerMap, "ntWt", PurchaseOrderItemCols.NTWt)),
                                fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", PurchaseOrderItemCols.FNWt)),
                                fnRate = getCellValueAsDouble(cell(row, headerMap, "fnRate", PurchaseOrderItemCols.FNRATE)),
                                wastagePercent = getCellValueAsDouble(cell(row, headerMap, "wastagePercent", PurchaseOrderItemCols.WASTAGEPERCENT))
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
                            purchaseOrderId = getCellValueAsString(cell(row, headerMap, "purchaseOrderId", PurchaseOrderItemCols.PURCHASEORDERID)),
                            catId = getCellValueAsString(cell(row, headerMap, "catId", PurchaseOrderItemCols.CATID)),
                            catName = getCellValueAsString(cell(row, headerMap, "catName", PurchaseOrderItemCols.CATNAME)),
                            subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", PurchaseOrderItemCols.SUBCATID)),
                            subCatName = getCellValueAsString(cell(row, headerMap, "subCatName", PurchaseOrderItemCols.SUBCATNAME)),
                            gsWt = getCellValueAsDouble(cell(row, headerMap, "gsWt", PurchaseOrderItemCols.GSWt)),
                            purity = getCellValueAsString(cell(row, headerMap, "purity", PurchaseOrderItemCols.PURITY)),
                            ntWt = getCellValueAsDouble(cell(row, headerMap, "ntWt", PurchaseOrderItemCols.NTWt)),
                            fnWt = getCellValueAsDouble(cell(row, headerMap, "fnWt", PurchaseOrderItemCols.FNWt)),
                            fnRate = getCellValueAsDouble(cell(row, headerMap, "fnRate", PurchaseOrderItemCols.FNRATE)),
                            wastagePercent = getCellValueAsDouble(cell(row, headerMap, "wastagePercent", PurchaseOrderItemCols.WASTAGEPERCENT))
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
        val sheet = requireSheet(workbook, "MetalExchangeEntity", summary) ?: return
        val headerMap = buildHeaderMap(sheet)
        val existingMetalExchanges = database.purchaseDao().getAllMetalExchanges().associateBy { it.exchangeId }
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            try {
                val exchangeId = getCellValueAsString(cell(row, headerMap, "exchangeId", MetalExchangeCols.EXCHANGEID))
                val existingMetalExchange = existingMetalExchanges[exchangeId]
                
                when (restoreMode) {
                    RestoreMode.MERGE -> {
                        if (existingMetalExchange == null) {
                            val metalExchange = MetalExchangeEntity(
                                exchangeId = exchangeId,
                                purchaseOrderId = getCellValueAsString(cell(row, headerMap, "purchaseOrderId", MetalExchangeCols.PURCHASEORDERID)),
                                catId = getCellValueAsString(cell(row, headerMap, "catId", MetalExchangeCols.CATID)),
                                catName = getCellValueAsString(cell(row, headerMap, "catName", MetalExchangeCols.CATNAME)),
                                subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", MetalExchangeCols.SUBCATID)),
                                subCatName = getCellValueAsString(cell(row, headerMap, "subCatName", MetalExchangeCols.SUBCATNAME)),
                                fnWeight = getCellValueAsDouble(cell(row, headerMap, "fnWeight", MetalExchangeCols.FNWEIGHT))
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
                            purchaseOrderId = getCellValueAsString(cell(row, headerMap, "purchaseOrderId", MetalExchangeCols.PURCHASEORDERID)),
                            catId = getCellValueAsString(cell(row, headerMap, "catId", MetalExchangeCols.CATID)),
                            catName = getCellValueAsString(cell(row, headerMap, "catName", MetalExchangeCols.CATNAME)),
                            subCatId = getCellValueAsString(cell(row, headerMap, "subCatId", MetalExchangeCols.SUBCATID)),
                            subCatName = getCellValueAsString(cell(row, headerMap, "subCatName", MetalExchangeCols.SUBCATNAME)),
                            fnWeight = getCellValueAsDouble(cell(row, headerMap, "fnWeight", MetalExchangeCols.FNWEIGHT))
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
    private fun buildHeaderMap(sheet: Sheet): Map<String, Int> {
        val headerRow = sheet.getRow(0) ?: return emptyMap()
        val headerMap = mutableMapOf<String, Int>()
        for (index in 0 until headerRow.lastCellNum) {
            val name = getCellValueAsString(headerRow.getCell(index)).trim().lowercase(Locale.US)
            if (name.isNotEmpty()) {
                headerMap[name] = index
            }
        }
        return headerMap
    }

    private fun readSchemaMetadata(workbook: Workbook): SchemaMetadata {
        val sheet = workbook.getSheet("Metadata") ?: run {
            logJvSync("ExcelImporter: Metadata sheet missing - assuming schema v${ExcelSchema.earliestKnownVersion}")
            return SchemaMetadata(
                schemaVersion = ExcelSchema.earliestKnownVersion,
                exportedAt = null,
                headers = emptyMap()
            )
        }

        var schemaVersion = ExcelSchema.earliestKnownVersion
        var exportedAt: String? = null
        val headers = mutableMapOf<String, List<String>>()

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            val key = getCellValueAsString(row.getCell(0)).trim()
            val value = getCellValueAsString(row.getCell(1))

            when {
                key.equals("schemaVersion", true) -> schemaVersion = value.toIntOrNull() ?: schemaVersion
                key.equals("exportedAt", true) -> exportedAt = value
                key.startsWith("headers:", true) -> {
                    val sheetName = key.substringAfter("headers:").trim()
                    if (sheetName.isNotEmpty()) {
                        headers[sheetName] = if (value.isBlank()) emptyList()
                        else value.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                }
            }
        }

        logJvSync("ExcelImporter: Metadata loaded (schema v$schemaVersion, exportedAt=${exportedAt ?: "unknown"})")
        return SchemaMetadata(schemaVersion, exportedAt, headers)
    }

    private fun determineRequiredHeaders(metadata: SchemaMetadata): Map<String, List<String>> {
        val targetVersion = ExcelSchema.requiredHeadersByVersion.keys
            .filter { it <= metadata.schemaVersion }
            .maxOrNull()
            ?: ExcelSchema.CURRENT_SCHEMA_VERSION

        val baseHeaders = ExcelSchema.requiredHeadersByVersion[targetVersion]
            ?: ExcelSchema.requiredHeadersByVersion[ExcelSchema.CURRENT_SCHEMA_VERSION]
            ?: emptyMap()

        val normalizedRecorded = metadata.headers.mapValues { (_, values) ->
            values.map { it.trim().lowercase(Locale.US) }
        }

        val combined = baseHeaders.toMutableMap()
        normalizedRecorded.keys.forEach { sheetName ->
            if (!combined.containsKey(sheetName)) {
                combined[sheetName] = metadata.headers[sheetName]?.map { it.trim() } ?: emptyList()
            }
        }

        return combined.mapValues { (sheetName, headers) ->
            val recorded = normalizedRecorded[sheetName]?.toSet() ?: emptySet()
            if (recorded.isEmpty()) {
                headers
            } else {
                headers.filter { header ->
                    recorded.contains(header.trim().lowercase(Locale.US))
                }
            }
        }
    }

    private fun requireSheet(workbook: Workbook, sheetName: String, summary: ImportSummary): Sheet? {
        val sheet = workbook.getSheet(sheetName)
        if (sheet == null) {
            if (!summary.missingSheets.contains(sheetName)) {
                summary.missingSheets.add(sheetName)
            }
            logJvSync("ExcelImporter: Sheet '$sheetName' missing in legacy import; skipping (schema support preserved).")
        }
        return sheet
    }

    private fun cell(row: Row, headerMap: Map<String, Int>, headerName: String, fallbackIndex: Int): Cell? {
        val idx = headerMap[headerName.lowercase(Locale.US)]
        if (idx != null) {
            return row.getCell(idx)
        }
        if (headerMap.isEmpty()) {
            return row.getCell(fallbackIndex)
        }
        return null
    }

    private fun getCellValueAsString(cell: Cell?, defaultValue: String = ""): String {
        if (cell == null) return defaultValue
        val value = dataFormatter.formatCellValue(cell).trim()
        return if (value.isBlank()) defaultValue else value
    }
    
    private fun getCellValueAsInt(cell: Cell?, defaultValue: Int = 0): Int {
        return when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toInt()
            CellType.STRING -> normalizeNumberString(cell.stringCellValue).toIntOrNull() ?: defaultValue
            else -> normalizeNumberString(getCellValueAsString(cell)).toIntOrNull() ?: defaultValue
        }
    }
    
    private fun getCellValueAsDouble(cell: Cell?, defaultValue: Double = 0.0): Double {
        return when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> normalizeNumberString(cell.stringCellValue).toDoubleOrNull() ?: defaultValue
            else -> normalizeNumberString(getCellValueAsString(cell)).toDoubleOrNull() ?: defaultValue
        }
    }
    
    private fun getCellValueAsLong(cell: Cell?, defaultValue: Long = 0L): Long {
        return when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toLong()
            CellType.STRING -> normalizeNumberString(cell.stringCellValue).toLongOrNull() ?: defaultValue
            CellType.BOOLEAN -> if (cell.booleanCellValue) 1L else 0L
            else -> normalizeNumberString(getCellValueAsString(cell)).toLongOrNull() ?: defaultValue
        }
    }
    
    private fun getCellValueAsBoolean(cell: Cell?, defaultValue: Boolean = false): Boolean {
        return when (cell?.cellType) {
            CellType.BOOLEAN -> cell.booleanCellValue
            CellType.STRING -> cell.stringCellValue.toBooleanStrictOrNull() ?: defaultValue
            CellType.NUMERIC -> cell.numericCellValue != 0.0
            else -> defaultValue
        }
    }

    private fun normalizeNumberString(value: String): String {
        return value.replace(",", "").trim()
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
            logJvSync("Excel validation started for ${inputFile.name}")
            val errors = mutableListOf<String>()
            FileInputStream(inputFile).use { inputStream ->
                XSSFWorkbook(inputStream).use { workbook ->
                    val metadata = readSchemaMetadata(workbook)
                    val requiredHeaders = determineRequiredHeaders(metadata)
                    val latestHeaders = ExcelSchema.requiredHeadersByVersion[ExcelSchema.CURRENT_SCHEMA_VERSION] ?: emptyMap()
                    val optionalHeaders = mapOf(
                        "UsersEntity" to listOf("lastUpdated"),
                        "StoreEntity" to listOf("lastUpdated")
                    )

                    val existingSheets = mutableListOf<String>()
                    for (i in 0 until workbook.numberOfSheets) {
                        existingSheets.add(workbook.getSheetName(i))
                    }

                    val missingSheets = requiredHeaders.keys.filter { !existingSheets.contains(it) }
                    if (missingSheets.isNotEmpty()) {
                        logJvSync("Missing sheets (allowed for backward compatibility): ${missingSheets.joinToString(", ")}")
                    }

                    for ((sheetName, headers) in requiredHeaders) {
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

                        val headerMap = buildHeaderMap(sheet)
                        val missingRequired = headers.filter { header ->
                            !headerMap.containsKey(header.lowercase(Locale.US))
                        }
                        if (missingRequired.isNotEmpty()) {
                            val message = "Sheet '$sheetName' missing required columns: ${missingRequired.joinToString(", ")}"
                            if (metadata.schemaVersion < ExcelSchema.CURRENT_SCHEMA_VERSION) {
                                logJvSync("$message (schema v${metadata.schemaVersion})")
                            } else {
                                errors.add(message)
                            }
                        }

                        val optional = optionalHeaders[sheetName] ?: emptyList()
                        val missingOptional = optional.filter { header ->
                            !headerMap.containsKey(header.lowercase(Locale.US))
                        }
                        if (missingOptional.isNotEmpty()) {
                            logJvSync("Sheet '$sheetName' missing optional columns: ${missingOptional.joinToString(", ")}")
                        }

                        val latest = latestHeaders[sheetName] ?: emptyList()
                        if (metadata.schemaVersion < ExcelSchema.CURRENT_SCHEMA_VERSION) {
                            val recorded = metadata.headers[sheetName]?.map { it.trim().lowercase(Locale.US) } ?: emptyList()
                            val missingNewColumns = latest.filter { header ->
                                header.lowercase(Locale.US) !in recorded
                            }
                            if (missingNewColumns.isNotEmpty()) {
                                logJvSync("Sheet '$sheetName' missing new schema columns (${missingNewColumns.joinToString(", ")}) for schema v${metadata.schemaVersion}")
                            }
                        }
                    }
                }
            }

            if (errors.isNotEmpty()) {
                return Result.failure(Exception(errors.joinToString("\n")))
            }

            logJvSync("Excel validation succeeded for ${inputFile.name}")
            Result.success(Unit)

        } catch (e: Exception) {
            log("Excel structure validation failed: ${e.message}")
            logJvSync("Excel validation failed for ${inputFile.name}: ${e.message}")
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
    var userAdditionalInfoFailed: Int = 0,
    var missingSheets: MutableList<String> = mutableListOf()
) {
    override fun toString(): String {
        return ("Import Summary: " +
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
                "UserAdditionalInfo($userAdditionalInfoAdded added, $userAdditionalInfoSkipped skipped, $userAdditionalInfoFailed failed)") +
                if (missingSheets.isNotEmpty()) " MissingSheets(${missingSheets.joinToString(", ")})" else ""
    }
}
