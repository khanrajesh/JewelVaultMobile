package com.velox.jewelvault.ui.screen.inventory

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.CatSubCatDto
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderItemEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.generateId
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.roundTo3Decimal
import com.velox.jewelvault.utils.to3FString
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject
import javax.inject.Named

// Data class for inventory summary statistics
data class InventorySummary(
    val totalItems: Int = 0,
    val totalGrossWeight: Double = 0.0,
    val totalNetWeight: Double = 0.0,
    val totalFineWeight: Double = 0.0,
    val totalCategories: Int = 0,
    val totalSubCategories: Int = 0,
    val recentItemsAdded: Int = 0
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val appDatabase: AppDatabase, private val _dataStoreManager: DataStoreManager,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
    private val _loadingState: MutableState<Boolean>,

    ) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState
    val dataStoreManager = _dataStoreManager
    val loadingState = _loadingState

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = _dataStoreManager.getAdminInfo()

    /**
     * return Triple of Flow<String> for storeId, upiId, storeName
     * */
    val store: Triple<Flow<String>, Flow<String>, Flow<String>> =
        _dataStoreManager.getSelectedStoreInfo()

    val snackBarState = _snackBarState
    val itemList: SnapshotStateList<ItemEntity> = SnapshotStateList()
    val catSubCatDto: SnapshotStateList<CatSubCatDto> = SnapshotStateList()
    val purchaseOrdersByDate: SnapshotStateList<PurchaseOrderEntity> = SnapshotStateList()

    val purchaseItems: SnapshotStateList<PurchaseOrderItemEntity> = SnapshotStateList()

    // Inventory summary statistics
    val inventorySummary = mutableStateOf(InventorySummary())

    // Filter states
    val addToName = InputFieldState()
    val entryType = InputFieldState()
    val qty = InputFieldState()
    val grWt = InputFieldState()
    val ntWt = InputFieldState()
    val purity = InputFieldState()
    val fnWt = InputFieldState()
    val chargeType = InputFieldState()
    val charge = InputFieldState()
    val otherChargeDes = InputFieldState()
    val othCharge = InputFieldState()
    val cgst = InputFieldState("1.5")
    val sgst = InputFieldState("1.5")
    val igst = InputFieldState()
    val desKey = InputFieldState()
    val desValue = InputFieldState()
    val huid = InputFieldState()
    val billDate = InputFieldState()
    val billNo = InputFieldState()
    val billItemDetails = mutableStateOf("")

    // Enhanced filter states
    val categoryFilter = InputFieldState()
    val subCategoryFilter = InputFieldState()
    val entryTypeFilter = InputFieldState()
    val purityFilter = InputFieldState()
    val chargeTypeFilter = InputFieldState()
    val startDateFilter = InputFieldState()
    val endDateFilter = InputFieldState()
    val minGsWtFilter = InputFieldState()
    val maxGsWtFilter = InputFieldState()
    val minNtWtFilter = InputFieldState()
    val maxNtWtFilter = InputFieldState()
    val minFnWtFilter = InputFieldState()
    val maxFnWtFilter = InputFieldState()
    val minQuantityFilter = InputFieldState()
    val maxQuantityFilter = InputFieldState()
    val huidSearchFilter = InputFieldState()
    val itemNameSearchFilter = InputFieldState()
    val addDesKeySearchFilter = InputFieldState()
    val addDesValueSearchFilter = InputFieldState()
    val firmIdFilter = InputFieldState()
    val purchaseOrderIdFilter = InputFieldState()

    // Firm and seller lists for dropdowns
    val firmList = mutableStateListOf<Pair<String, String>>() // Pair<firmId, firmName>
    val purchaseOrderList = mutableStateListOf<Pair<String, String>>() // Pair<orderId, billNo>

    // Sorting states
    val sortBy = mutableStateOf("addDate")
    val sortOrder = mutableStateOf("DESC")

    val isSelf = mutableStateOf(true)

    val itemHeaderList = listOf(
        "S.No",
        "Category",
        "Sub Category",
        "Item Id",
        "To Name",
        "Type",
        "Qty",
        "Gr.Wt",
        "Nt.Wt",
        "Unit",
        "Purity",
        "Fn.Wt",
        "MC.Type",
        "M.Chr",
        "Oth Chr",
        "Chr",
        "Tax",
        "H-UID",
        "DOA",
        "Des",
        "Value",
        "Purchase OrderId",
    )

    // Sorting options
    val sortOptions = listOf(
        "addDate" to "Date Added",
        "itemId" to "Item ID",
        "gsWt" to "Gross Weight",
        "ntWt" to "Net Weight",
        "fnWt" to "Fine Weight",
        "quantity" to "Quantity",
        "catName" to "Category",
        "subCatName" to "Sub Category",
        "purity" to "Purity",
        "entryType" to "Entry Type"
    )

    init {
        loadInitialData()
        ioLaunch {
            try {
                val defCgst = _dataStoreManager.getValue(DataStoreManager.DEFAULT_CGST, "1.5").first() ?: "1.5"
                val defSgst = _dataStoreManager.getValue(DataStoreManager.DEFAULT_SGST, "1.5").first() ?: "1.5"
                val defIgst = _dataStoreManager.getValue(DataStoreManager.DEFAULT_IGST, "0.0").first() ?: "0.0"
                mainScope {
                    if (cgst.text.isBlank()) cgst.text = defCgst
                    if (sgst.text.isBlank()) sgst.text = defSgst
                    if (igst.text.isBlank()) igst.text = defIgst
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadInitialData() {
        getCategoryAndSubCategoryDetails()
        loadRecentItems()
        loadFirmAndOrderLists()
        loadInventorySummary()
    }

    fun loadFirmAndOrderLists() {
        ioLaunch {
            try {
                val firms = appDatabase.purchaseDao().getAllFirmsWithSellers()
                mainScope {
                    firmList.clear()
                    firmList.addAll(firms.map { it.firm.firmId to it.firm.firmName })
                }
                val orders = appDatabase.purchaseDao().getAllPurchaseOrders()
                mainScope {
                    purchaseOrderList.clear()
                    purchaseOrderList.addAll(orders.map {
                        it.purchaseOrderId to it.billNo
                    })
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun getCategoryAndSubCategoryDetails() {
        viewModelScope.launch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                val result =
                    appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId)

                val newList = result.map { cat ->
                    val subList = appDatabase.subCategoryDao().getSubCategoriesByCatId(cat.catId)
                    CatSubCatDto(
                        catId = cat.catId,
                        catName = cat.catName,
                        gsWt = cat.gsWt,
                        fnWt = cat.fnWt,
                        userId = userId,
                        storeId = storeId,
                        subCategoryList = mutableStateListOf<SubCategoryEntity>().apply {
                            addAll(subList)
                        })
                }

                mainScope {
                    // update only if different
                    if (catSubCatDto != newList) {
                        catSubCatDto.clear()
                        catSubCatDto.addAll(newList)
                    }
                }
            } catch (e: Exception) {
                this@InventoryViewModel.log("unable to fetch categorical data")
            }
        }
    }

    fun loadRecentItems() {
        ioLaunch {
            try {
                appDatabase.itemDao().getRecentItems().collectLatest { items ->
                    itemList.clear()
                    itemList.addAll(items)
                }
            } catch (e: Exception) {
                this@InventoryViewModel.log("failed to load recent items: ${e.message}")
            }
        }
    }

    fun loadInventorySummary() {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()

                // Get all items for the user and store
                val allItems = appDatabase.itemDao().getAllItemsByUserIdAndStoreId(userId, storeId)

                // Calculate summary statistics
                val totalItems = allItems.sumOf { it.quantity }
                val totalGrossWeight = allItems.sumOf { it.gsWt }
                val totalNetWeight = allItems.sumOf { it.ntWt }
                val totalFineWeight = allItems.sumOf { it.fnWt }

                // Get category and subcategory counts
                val categories =
                    appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId)
                val totalCategories = categories.size
                val totalSubCategories = categories.sumOf { cat ->
                    appDatabase.subCategoryDao().getSubCategoriesByCatId(cat.catId).size
                }

                // Get recent items (last 7 days)
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.DAY_OF_MONTH, -7)
                val weekAgo = Timestamp(calendar.timeInMillis)
                val recentItems = allItems.count { it.addDate.after(weekAgo) }

                val summary = InventorySummary(
                    totalItems = totalItems,
                    totalGrossWeight = totalGrossWeight,
                    totalNetWeight = totalNetWeight,
                    totalFineWeight = totalFineWeight,
                    totalCategories = totalCategories,
                    totalSubCategories = totalSubCategories,
                    recentItemsAdded = recentItems
                )

                mainScope {
                    inventorySummary.value = summary
                }
            } catch (e: Exception) {
                this@InventoryViewModel.log("failed to load inventory summary: ${e.message}")
            }
        }
    }

    fun addCategory(catName: String) {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                val s = appDatabase.categoryDao().insertCategory(
                    CategoryEntity(
                        catId = generateId(), catName = catName, userId = userId, storeId = storeId
                    )
                )
                if (s != -1L) {
                    _snackBarState.value = "Added new category id: $s"
                    this@InventoryViewModel.log("Added new category id: $s")
                    getCategoryAndSubCategoryDetails()
                    loadInventorySummary()
                } else {
                    _snackBarState.value = "failed to add category"
                    this@InventoryViewModel.log("failed to add category")
                }
            } catch (e: Exception) {
                _snackBarState.value = "unable to add category error: ${e.message}"
                this@InventoryViewModel.log("unable to add category error: ${e.message}")
            }
        }
    }

    fun addSubCategory(subCatName: String, catName: String, catId: String) {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                val s = appDatabase.subCategoryDao().insertSubCategory(
                    SubCategoryEntity(
                        subCatId = generateId(),
                        subCatName = subCatName,
                        catId = catId,
                        catName = catName,
                        userId = userId,
                        storeId = storeId
                    )
                )
                if (s != -1L) {
                    _snackBarState.value = "Added new sub category id: $s"
                    this@InventoryViewModel.log("Added new sub category id: $s")
                    getCategoryAndSubCategoryDetails()
                    loadInventorySummary()
                } else {
                    _snackBarState.value = "failed to add sub category"
                    this@InventoryViewModel.log("failed to add sub category")
                }
            } catch (e: Exception) {
                _snackBarState.value = "unable to add sub category"
                this@InventoryViewModel.log("unable to add sub category")
            }
        }
    }

    fun deleteCategoryWithPin(
        category: CatSubCatDto, adminPin: String, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        ioLaunch {
            try {
                // Verify admin pin first
                withIo {
                    if (!verifyAdminPin(adminPin)) {
                        _snackBarState.value = "Invalid Admin PIN"
                        onFailure("Invalid Admin PIN")
                        return@withIo
                    }
                }

                // Delete category and all related data
                deleteCategoryAndRelatedData(category.catId)

                _snackBarState.value =
                    "Category '${category.catName}' and all related data deleted successfully"
                this@InventoryViewModel.log("Category '${category.catName}' deleted successfully")

                // Refresh data
                getCategoryAndSubCategoryDetails()
                loadInventorySummary()
                filterItems()

                onSuccess()
            } catch (e: Exception) {
                _snackBarState.value = "Failed to delete category: ${e.message}"
                this@InventoryViewModel.log("Failed to delete category: ${e.message}")
                onFailure(e.message ?: "Unknown error")
            } finally {
            }
        }
    }

    fun deleteSubCategoryWithPin(
        subCategory: SubCategoryEntity,
        adminPin: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        ioLaunch {
            try {
                // Verify admin pin first
                withIo {
                    if (!verifyAdminPin(adminPin)) {
                        _snackBarState.value = "Invalid Admin PIN"
                        onFailure("Invalid Admin PIN")
                        return@withIo
                    }
                }

                // Delete subcategory and all related data
                deleteSubCategoryAndRelatedData(subCategory.subCatId)

                _snackBarState.value =
                    "Subcategory '${subCategory.subCatName}' and all related data deleted successfully"
                this@InventoryViewModel.log("Subcategory '${subCategory.subCatName}' deleted successfully")

                // Refresh data
                getCategoryAndSubCategoryDetails()
                loadInventorySummary()
                filterItems()

                onSuccess()
            } catch (e: Exception) {
                _snackBarState.value = "Failed to delete subcategory: ${e.message}"
                this@InventoryViewModel.log("Failed to delete subcategory: ${e.message}")
                onFailure(e.message ?: "Unknown error")
            } finally {
            }
        }
    }

    fun updateSubCategoryName(
        subCategory: SubCategoryEntity,
        newName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        ioLaunch {
            try {
                val updatedSubCategory = subCategory.copy(subCatName = newName)
                val rowsUpdated = appDatabase.subCategoryDao().updateSubCategory(updatedSubCategory)

                if (rowsUpdated > 0) {
                    _snackBarState.value = "Subcategory name updated successfully"
                    this@InventoryViewModel.log("Subcategory name updated successfully")

                    // Refresh data
                    getCategoryAndSubCategoryDetails()
                    loadInventorySummary()
                    filterItems()

                    onSuccess()
                } else {
                    _snackBarState.value = "Failed to update subcategory name"
                    onFailure("Failed to update subcategory name")
                }
            } catch (e: Exception) {
                _snackBarState.value = "Failed to update subcategory: ${e.message}"
                this@InventoryViewModel.log("Failed to update subcategory: ${e.message}")
                onFailure(e.message ?: "Unknown error")
            } finally {
            }
        }
    }

    private suspend fun verifyAdminPin(adminPin: String): Boolean {
        return try {
            val userId = admin.first.first()
            val currentUser = appDatabase.userDao().getUserById(userId)

            if (currentUser != null && currentUser.pin != null) {
                com.velox.jewelvault.utils.SecurityUtils.verifyPin(adminPin, currentUser.pin)
            } else {
                false
            }
        } catch (e: Exception) {
            this@InventoryViewModel.log("Error verifying admin pin: ${e.message}")
            false
        }
    }

    private suspend fun deleteCategoryAndRelatedData(catId: String) {
        withIo {
            try {
                // Delete all items in this category
                appDatabase.masterDao().deleteItemsByCategory(catId)

                // Delete all subcategories in this category
                appDatabase.masterDao().deleteSubCategoriesByCategory(catId)

                // Finally delete the category itself
                val category = appDatabase.categoryDao().getCategoryById(catId)
                if (category != null) {
                    appDatabase.categoryDao().deleteCategory(category)
                }

                this@InventoryViewModel.log("Successfully deleted category $catId and all related data")
            } catch (e: Exception) {
                this@InventoryViewModel.log("Error deleting category $catId: ${e.message}")
                throw e
            }
        }
    }

    private suspend fun deleteSubCategoryAndRelatedData(subCatId: String) {
        withIo {
            try {
                // Delete all items in this subcategory
                appDatabase.masterDao().deleteItemsBySubCategory(subCatId)

                // Delete the subcategory itself
                val subCategory = appDatabase.subCategoryDao().getSubCategoryById(subCatId)
                if (subCategory != null) {
                    appDatabase.subCategoryDao().deleteSubCategory(subCategory)
                }

                this@InventoryViewModel.log("Successfully deleted subcategory $subCatId and all related data")
            } catch (e: Exception) {
                this@InventoryViewModel.log("Error deleting subcategory $subCatId: ${e.message}")
                throw e
            }
        }
    }

    // Enhanced filter function with all parameters
    private var filterJob: Job? = null
    fun filterItems() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if coroutine is still active before proceeding
                if (!isActive) {
                    this@InventoryViewModel.log("filterItems coroutine was cancelled before starting")
                    return@launch
                }

                // Immediately clear current list so UI shows "no items" while loading/filtering.
                // This ensures we always return a value to UI even if something fails later.
                itemList.clear()

                // Parse inputs safely outside the flow collection to avoid throwing inside the Flow
                val catId = runCatching {
                    catSubCatDto.asSequence().find { it.catName == categoryFilter.text }?.catId
                }.getOrNull()

                val subCatId = runCatching {
                    catSubCatDto.asSequence().flatMap { it.subCategoryList.asSequence() }
                        .find { it.subCatName == subCategoryFilter.text }?.subCatId
                }.getOrNull()

                val startDate = runCatching {
                    if (startDateFilter.text.isNotBlank()) {
                        val dateFormat =
                            java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                        dateFormat.parse(startDateFilter.text)?.let { Timestamp(it.time) }
                    } else null
                }.getOrNull()

                val endDate = runCatching {
                    if (endDateFilter.text.isNotBlank()) {
                        val dateFormat =
                            java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                        dateFormat.parse(endDateFilter.text)?.let {
                            val calendar = java.util.Calendar.getInstance()
                            calendar.time = it
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                            calendar.set(java.util.Calendar.MINUTE, 59)
                            calendar.set(java.util.Calendar.SECOND, 59)
                            Timestamp(calendar.timeInMillis)
                        }
                    } else null
                }.getOrNull()

                // Numeric parsing (null on invalid)
                fun String.toDoubleOrNullSafe() = this.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                fun String.toIntOrNullSafe() = this.takeIf { it.isNotBlank() }?.toIntOrNull()

                val minGsWt = minGsWtFilter.text.toDoubleOrNullSafe()
                val maxGsWt = maxGsWtFilter.text.toDoubleOrNullSafe()
                val minNtWt = minNtWtFilter.text.toDoubleOrNullSafe()
                val maxNtWt = maxNtWtFilter.text.toDoubleOrNullSafe()
                val minFnWt = minFnWtFilter.text.toDoubleOrNullSafe()
                val maxFnWt = maxFnWtFilter.text.toDoubleOrNullSafe()
                val minQuantity = minQuantityFilter.text.toIntOrNullSafe()
                val maxQuantity = maxQuantityFilter.text.toIntOrNullSafe()

                val firmId = firmIdFilter.text.ifBlank { null }
                val purchaseOrderId = purchaseOrderIdFilter.text.ifBlank { null }
                val type = entryTypeFilter.text.ifEmpty { null }
                val purity = purityFilter.text.ifEmpty { null }
                val crgType = chargeTypeFilter.text.ifEmpty { null }

                // Check if coroutine is still active before database call
                if (!isActive) {
                    this@InventoryViewModel.log("filterItems coroutine was cancelled before database call")
                    return@launch
                }

                // Now subscribe to the DAO flow with defensive flow operators.
                appDatabase.itemDao().filterItems(
                    catId = catId,
                    subCatId = subCatId,
                    type = type,
                    purity = purity,
                    crgType = crgType,
                    startDate = startDate,
                    endDate = endDate,
                    minGsWt = minGsWt,
                    maxGsWt = maxGsWt,
                    minNtWt = minNtWt,
                    maxNtWt = maxNtWt,
                    minFnWt = minFnWt,
                    maxFnWt = maxFnWt,
                    minQuantity = minQuantity,
                    maxQuantity = maxQuantity,
                    firmId = firmId,
                    purchaseOrderId = purchaseOrderId
                ).onStart {
                    // Set loading state to true when filtering starts
                    _loadingState.value = true
                }.catch { e ->
                    // If cancelled, don't restart automatically to avoid endless loops
                    if (e is kotlinx.coroutines.CancellationException) {
                        this@InventoryViewModel.log("filterItems flow was cancelled")
                        return@catch
                    } else {
                        this@InventoryViewModel.log("filterItems flow error: ${e.message}")
                        _snackBarState.value =
                            "Failed to filter items: ${e.message ?: "Unknown error"}"
                    }
                    // Set loading state to false on error
                    _loadingState.value = false
                    // Ensure itemList stays empty or previous value (we already cleared above)
                }.collectLatest { items ->
                    // Check if coroutine is still active before processing results
                    if (!isActive) {
                        this@InventoryViewModel.log("filterItems coroutine was cancelled during processing")
                        return@collectLatest
                    }

                    // Sort on the collected list, then update UI list once.
                    val sortedItems = when (sortBy.value) {
                        "itemId" -> if (sortOrder.value == "ASC") items.sortedBy { it.itemId } else items.sortedByDescending { it.itemId }
                        "gsWt" -> if (sortOrder.value == "ASC") items.sortedBy { it.gsWt } else items.sortedByDescending { it.gsWt }
                        "ntWt" -> if (sortOrder.value == "ASC") items.sortedBy { it.ntWt } else items.sortedByDescending { it.ntWt }
                        "fnWt" -> if (sortOrder.value == "ASC") items.sortedBy { it.fnWt } else items.sortedByDescending { it.fnWt }
                        "quantity" -> if (sortOrder.value == "ASC") items.sortedBy { it.quantity } else items.sortedByDescending { it.quantity }
                        "catName" -> if (sortOrder.value == "ASC") items.sortedBy { it.catName } else items.sortedByDescending { it.catName }
                        "subCatName" -> if (sortOrder.value == "ASC") items.sortedBy { it.subCatName } else items.sortedByDescending { it.subCatName }
                        "purity" -> if (sortOrder.value == "ASC") items.sortedBy { it.purity } else items.sortedByDescending { it.purity }
                        "entryType" -> if (sortOrder.value == "ASC") items.sortedBy { it.entryType } else items.sortedByDescending { it.entryType }
                        else -> if (sortOrder.value == "ASC") items.sortedBy { it.addDate } else items.sortedByDescending { it.addDate }
                    }

                    // Update UI state on the main thread (we're already in coroutine context started by ioLaunch).
                    itemList.clear()
                    itemList.addAll(sortedItems)
                    
                    // Set loading state to false when data is loaded
                    _loadingState.value = false
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Handle cancellation gracefully without restarting
                this@InventoryViewModel.log("filterItems coroutine was cancelled")
            } catch (e: Exception) {
                // This try/catch defends against unexpected errors constructing the Flow or subscribing,
                // but most errors should be handled in .catch above.
                this@InventoryViewModel.log("failed to filter item list (outer): ${e.message}")
                _snackBarState.value = "Failed to filter items: ${e.message}"
            } finally {
                // Ensure loading state is set to false in all cases
                _loadingState.value = false
            }
        }
    }


    fun clearAllFilters() {
        categoryFilter.text = ""
        subCategoryFilter.text = ""
        entryTypeFilter.text = ""
        purityFilter.text = ""
        chargeTypeFilter.text = ""
        startDateFilter.text = ""
        endDateFilter.text = ""
        minGsWtFilter.text = ""
        maxGsWtFilter.text = ""
        minNtWtFilter.text = ""
        maxNtWtFilter.text = ""
        minFnWtFilter.text = ""
        maxFnWtFilter.text = ""
        minQuantityFilter.text = ""
        maxQuantityFilter.text = ""
        huidSearchFilter.text = ""
        itemNameSearchFilter.text = ""
        addDesKeySearchFilter.text = ""
        addDesValueSearchFilter.text = ""
        firmIdFilter.text = ""
        purchaseOrderIdFilter.text = ""
        sortBy.value = "addDate"
        sortOrder.value = "DESC"

        loadRecentItems()
    }

    fun clearAddItemFields() {
        addToName.clear()
        entryType.clear()
        qty.clear()
        grWt.clear()
        ntWt.clear()
        purity.clear()
        fnWt.clear()
        chargeType.clear()
        charge.clear()
        otherChargeDes.clear()
        othCharge.clear()
        ioLaunch {
            try {
                val defCgst = _dataStoreManager.getValue(DataStoreManager.DEFAULT_CGST, "1.5").first() ?: "1.5"
                val defSgst = _dataStoreManager.getValue(DataStoreManager.DEFAULT_SGST, "1.5").first() ?: "1.5"
                val defIgst = _dataStoreManager.getValue(DataStoreManager.DEFAULT_IGST, "0.0").first() ?: "0.0"
                mainScope {
                    cgst.text = defCgst
                    sgst.text = defSgst
                    igst.text = defIgst
                }
            } catch (_: Exception) { }
        }
        desKey.clear()
        desValue.clear()
        huid.clear()
        billDate.clear()
        billNo.clear()
        billItemDetails.value = ""
        isSelf.value = true
        // Clear errors from InputFieldState
        purity.error = ""
        fnWt.error = ""
    }

    fun refreshAndFilterItems() {
        ioLaunch {
            try {
                // First refresh category data
                val userId = admin.first.first()
                val storeId = store.first.first()
                val result =
                    appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId)

                val newList = result.map { cat ->
                    val subList = appDatabase.subCategoryDao().getSubCategoriesByCatId(cat.catId)
                    CatSubCatDto(
                        catId = cat.catId,
                        catName = cat.catName,
                        gsWt = cat.gsWt,
                        fnWt = cat.fnWt,
                        userId = userId,
                        storeId = storeId,
                        subCategoryList = mutableStateListOf<SubCategoryEntity>().apply {
                            addAll(subList)
                        })
                }

                mainScope {
                    catSubCatDto.clear()
                    catSubCatDto.addAll(newList)
                    // Now filter items with updated category data
                    filterItems()
                }
            } catch (e: Exception) {
                this@InventoryViewModel.log("failed to refresh and filter items: ${e.message}")
                // Fallback to just filtering
                filterItems()
            } finally {
            }
        }
    }

    suspend fun getRemainingFineWeightForPurity(purity: String, subCatName: String): Double {
        if (isSelf.value || purchaseItems.isEmpty()) {
            return Double.MAX_VALUE // No limit for self items or when no purchase order selected
        }

        // Get purchase items for this purity and subcategory
        val purchaseItemsForPurity = purchaseItems.filter {
            it.subCatName.lowercase() == subCatName.lowercase() && it.purity == purity
        }

        if (purchaseItemsForPurity.isEmpty()) {
            return 0.0 // No items of this purity in purchase order
        }

        val totalPurchaseFnWt = purchaseItemsForPurity.sumOf { it.fnWt }

        // Get already added items for this purity and subcategory from current purchase order
        val userId = admin.first.first()
        val storeId = store.first.first()
        val inventoryItems = withIo {
            appDatabase.itemDao().getAllItemsByUserIdAndStoreId(userId, storeId)
        }.filter {
            it.subCatName.lowercase() == subCatName.lowercase() && it.purity == purity && it.purchaseOrderId == purchaseItemsForPurity.first().purchaseOrderId
        }

        val totalAddedFnWt = inventoryItems.sumOf { it.fnWt }

        return (totalPurchaseFnWt - totalAddedFnWt).coerceAtLeast(0.0)
    }

    suspend fun validateFineWeightInput(
        fnWtText: String, purity: String, subCatName: String
    ): String? {
        if (isSelf.value || purchaseItems.isEmpty()) {
            return null // No validation needed for self items
        }

        val inputFnWt = fnWtText.toDoubleOrNull() ?: 0.0
        val remainingFnWt = getRemainingFineWeightForPurity(purity, subCatName)

        // Use tolerance of 0.01g (10 milligrams) for gold precision
        val tolerance = 0.01

        // Check if input is significantly above remaining weight (beyond tolerance)
        return if (inputFnWt > (remainingFnWt + tolerance)) {
            "Cannot add ${inputFnWt.to3FString()}g. Remaining: ${remainingFnWt.to3FString()}g for purity $purity"
        } else null
    }

    fun searchItemsByHUID(huid: String) {
        if (huid.isNotEmpty()) {
            ioLaunch {
                try {
                    appDatabase.itemDao().searchItemsByHUID(huid).collectLatest { items ->
                        itemList.clear()
                        itemList.addAll(items)
                    }
                } catch (e: Exception) {
                    this@InventoryViewModel.log("failed to search by HUID: ${e.message}")
                } finally {
                }
            }
        } else {
            loadRecentItems()
        }
    }

    fun searchItemsByName(name: String) {
        if (name.isNotEmpty()) {
            ioLaunch {
                try {
                    appDatabase.itemDao().searchItemsByName(name).collectLatest { items ->
                        itemList.clear()
                        itemList.addAll(items)
                    }
                } catch (e: Exception) {
                    this@InventoryViewModel.log("failed to search by name: ${e.message}")
                } finally {
                }
            }
        } else {
            loadRecentItems()
        }
    }

    fun safeInsertItem(
        item: ItemEntity, onSuccess: (ItemEntity, Long) -> Unit, onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withIo {
                    val userId = admin.first.first()
                    val storeId = store.first.first()
                    val it = item.copy(
                        userId = userId, storeId = storeId
                    )

                    val newItemId = appDatabase.itemDao().insert(it)

                    if (newItemId != -1L) {
                        val insertedItem = it

                        try {
                            val subCategory = appDatabase.subCategoryDao()
                                .getSubCategoryById(subCatId = it.subCatId)
                            subCategory?.let { subCat ->
                                val subCatGsWt = (subCat.gsWt + insertedItem.gsWt).roundTo3Decimal()
                                val subCatFnWt = (subCat.fnWt + insertedItem.fnWt).roundTo3Decimal()
                                val subCatQty = subCat.quantity + insertedItem.quantity

                                val upSub = appDatabase.subCategoryDao().updateWeightsAndQuantity(
                                    subCatId = insertedItem.subCatId,
                                    gsWt = subCatGsWt,
                                    fnWt = subCatFnWt,
                                    quantity = subCatQty
                                )

                                if (upSub != -1) {
                                    this@InventoryViewModel.log("Sub Cat id: ${insertedItem.subCatId} update with weight")
                                    val cat = appDatabase.categoryDao()
                                        .getCategoryById(catId = insertedItem.catId)
                                    cat?.let { catEntity ->
                                        val catGsWt =
                                            (catEntity.gsWt + insertedItem.gsWt).roundTo3Decimal()
                                        val catFnWt =
                                            (catEntity.fnWt + insertedItem.fnWt).roundTo3Decimal()
                                        val upCat = appDatabase.categoryDao().updateWeights(
                                            catId = insertedItem.catId,
                                            gsWt = catGsWt,
                                            fnWt = catFnWt
                                        )
                                        if (upCat != -1) {
                                            this@InventoryViewModel.log("Cat id: ${insertedItem.catId} update with weight")
                                            _snackBarState.value =
                                                "Item Added and categories updated."
                                            onSuccess(insertedItem, newItemId)
                                            // Item list will be refreshed by the calling UI
                                        }
                                    }
                                } else {
                                    _snackBarState.value = "Failed to update Sub Category Weight"
                                    onFailure(Exception("Failed to update Sub Category Weight"))
                                }
                            } ?: run {
                                _snackBarState.value = "Sub Category not found"
                                onFailure(Exception("Sub Category not found"))
                            }
                        } catch (e: Exception) {
                            _snackBarState.value =
                                ("Error updating cat and sub cat weight: ${e.message}")
                            this@InventoryViewModel.log("Error updating cat and sub cat weight: ${e.message}")
                            onFailure(e)
                        }
                    } else {
                        _snackBarState.value = ("Failed to insert item")
                        this@InventoryViewModel.log("Failed to insert item")
                        onFailure(Exception("Failed to insert item"))
                    }
                }
            } catch (e: Exception) {
                _snackBarState.value = "Error inserting item error: ${e.message}"
                onFailure(e)
            } finally {
            }
        }
    }

    fun safeDeleteItem(
        itemId: String,
        catId: String,
        subCatId: String,
        onSuccess: (Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withIo {
                    val item = appDatabase.itemDao().getItemById(itemId)
                    if (item != null) {
                        val rowsDeleted = appDatabase.itemDao().deleteById(itemId, catId, subCatId)
                        if (rowsDeleted > 0) {
                            try {
                                val subCategory =
                                    appDatabase.subCategoryDao().getSubCategoryById(subCatId)
                                subCategory?.let {
                                    val updatedSubCatGsWt =
                                        (it.gsWt - item.gsWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedSubCatFnWt =
                                        (it.fnWt - item.fnWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedSubCatQty =
                                        (it.quantity - item.quantity).coerceAtLeast(0)

                                    appDatabase.subCategoryDao().updateWeightsAndQuantity(
                                        subCatId = subCatId,
                                        gsWt = updatedSubCatGsWt,
                                        fnWt = updatedSubCatFnWt,
                                        quantity = updatedSubCatQty
                                    )
                                } ?: run {
                                    this@InventoryViewModel.log("Sub Category not found for deletion")
                                    onFailure(Exception("Sub Category not found"))
                                    return@withIo
                                }

                                val category = appDatabase.categoryDao().getCategoryById(catId)
                                category?.let {
                                    val updatedCatGsWt =
                                        (it.gsWt - item.gsWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedCatFnWt =
                                        (it.fnWt - item.fnWt).coerceAtLeast(0.0).roundTo3Decimal()

                                    appDatabase.categoryDao().updateWeights(
                                        catId = catId, gsWt = updatedCatGsWt, fnWt = updatedCatFnWt
                                    )
                                } ?: run {
                                    this@InventoryViewModel.log("Category not found for deletion")
                                    onFailure(Exception("Category not found"))
                                    return@withIo
                                }

                                onSuccess(rowsDeleted)
                                // Refresh the item list after deleting
                                filterItems()
                            } catch (e: Exception) {
                                onFailure(e)
                            }
                        } else {
                            this@InventoryViewModel.log("No rows deleted. Check itemId, catId, and subCatId.")
                            onFailure(Exception("No rows deleted. Check itemId, catId, and subCatId."))
                        }
                    } else {
                        this@InventoryViewModel.log("No Item found with ID $itemId")
                        onFailure(Exception("No Item found with ID $itemId"))
                    }
                }
            } catch (e: Exception) {
                onFailure(e)
            } finally {
            }
        }
    }

    fun getBillsFromDate() {
        ioLaunch {
            try {
                billItemDetails.value = ""
                purchaseOrdersByDate.clear()
                billNo.clear()
                val data = appDatabase.purchaseDao().getOrdersByBillDate(billDate.text)
                purchaseOrdersByDate.addAll(data)

                if (purchaseOrdersByDate.isEmpty()) {
                    _snackBarState.value = "No purchase bill found"
                }
            } catch (e: Exception) {
                this@InventoryViewModel.log("failed to get bills from date: ${e.message}")
            } finally {
            }
        }
    }

    fun getPurchaseOrderItemDetails(purchase: PurchaseOrderEntity, subCatId: String) {
        ioLaunch {
            try {
                val purchaseItemList = appDatabase.purchaseDao()
                    .getItemsByOrderIdAndSubCatId(purchase.purchaseOrderId, subCatId)
                purchaseItems.clear()
                billItemDetails.value = ""
                if (purchaseItemList.isNotEmpty()) {
                    val sellerInfo = appDatabase.purchaseDao().getSellerById(purchase.sellerId)

                    if (sellerInfo != null) {
                        val firInfo = appDatabase.purchaseDao().getFirmById(sellerInfo.firmId)

                        // Get items from inventory for this subcategory and purchase order to calculate remaining
                        val userId = admin.first.first()
                        val storeId = store.first.first()
                        val inventoryItems =
                            appDatabase.itemDao().getAllItemsByUserIdAndStoreId(userId, storeId)
                                .filter { it.subCatName == subCatId && it.purchaseOrderId == purchase.purchaseOrderId }

                        // Group purchase items by purity
                        val purchaseItemsByPurity = purchaseItemList.groupBy { it.purity }
                        val inventoryItemsByPurity = inventoryItems.groupBy { it.purity }

                        // Build concise 4-line summary
                        val summary = StringBuilder()
                        summary.appendLine("${firInfo?.firmName ?: "Unknown"} - ${sellerInfo.name}, Bill: ${purchase.billNo} | ${purchase.billDate}")

                        // Calculate total fine weight
                        val totalPurchaseFnWt = purchaseItemList.sumOf { it.fnWt }
                        val totalInventoryFnWt = inventoryItems.sumOf { it.fnWt }
                        val totalRemainingFnWt =
                            (totalPurchaseFnWt - totalInventoryFnWt).coerceAtLeast(0.0)

                        summary.appendLine("Total FnWt: ${totalPurchaseFnWt.to3FString()}g | Added: ${totalInventoryFnWt.to3FString()}g | Remaining: ${totalRemainingFnWt.to3FString()}g")

                        // Show remaining items by purity (max 1 line)
                        val remainingByPurity =
                            purchaseItemsByPurity.mapNotNull { (purity, items) ->
                                val totalFnWt = items.sumOf { it.fnWt }
                                val inventoryItemsForPurity =
                                    inventoryItemsByPurity[purity] ?: emptyList()
                                val inventoryFnWt = inventoryItemsForPurity.sumOf { it.fnWt }
                                val remainingFnWt = (totalFnWt - inventoryFnWt).coerceAtLeast(0.0)

                                if (remainingFnWt > 0) {
                                    "${purity}: ${remainingFnWt.to3FString()}g"
                                } else null
                            }

                        if (remainingByPurity.isNotEmpty()) {
                            summary.appendLine(
                                "Remaining by purity: ${
                                    remainingByPurity.joinToString(
                                        ", "
                                    )
                                } ⚠️"
                            )
                        } else {
                            summary.appendLine("✅ All items added to inventory")
                        }

                        billItemDetails.value = summary.toString()
                        purchaseItems.addAll(purchaseItemList)
                    }
                } else {
                    billItemDetails.value =
                        "No items found for this purchase order and sub-category."
                }
            } catch (e: Exception) {
                this@InventoryViewModel.log("failed to get purchase order item details: ${e.message}")
            } finally {
            }
        }
    }

    // Detailed report for dialog
    suspend fun getDetailedPurchaseOrderReport(
        purchase: PurchaseOrderEntity, subCatName: String
    ): String {
        val purchaseItemList = withIo {
            appDatabase.purchaseDao()
                .getItemsByOrderIdAndSubCatId(purchase.purchaseOrderId, subCatName)
        }

        if (purchaseItemList.isEmpty()) {
            return "No items found for this purchase order and sub-category."
        }

        val sellerInfo = withIo { appDatabase.purchaseDao().getSellerById(purchase.sellerId) }
        if (sellerInfo == null) {
            return "Seller information not found."
        }

        val firInfo = withIo { appDatabase.purchaseDao().getFirmById(sellerInfo.firmId) }

        // Get items from inventory for this subcategory and purchase order to calculate remaining
        val userId = admin.first.first()
        val storeId = store.first.first()
        val inventoryItems = withIo {
            appDatabase.itemDao().getAllItemsByUserIdAndStoreId(userId, storeId)
        }.filter { it.subCatName == subCatName && it.purchaseOrderId == purchase.purchaseOrderId }

        // Build detailed report
        val report = StringBuilder()

        // Header information
        report.appendLine("📋 PURCHASE ORDER DETAILS")
        report.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        report.appendLine("🏢 Firm: ${firInfo?.firmName ?: "Unknown"}")
        report.appendLine("👤 Seller: ${sellerInfo.name}")
        report.appendLine("📱 Contact: ${sellerInfo.mobileNumber}")
        report.appendLine("📅 Bill Date: ${purchase.billDate}")
        report.appendLine("🧾 Bill No: ${purchase.billNo}")
        report.appendLine("📦 Sub-Category: $subCatName")
        report.appendLine()

        // Group purchase items by purity
        val purchaseItemsByPurity = purchaseItemList.groupBy { it.purity }
        val inventoryItemsByPurity = inventoryItems.groupBy { it.purity }

        // Calculate totals and remaining for each purity
        purchaseItemsByPurity.forEach { (purity, items) ->
            val totalGsWt = items.sumOf { it.gsWt }
            val totalFnWt = items.sumOf { it.fnWt }
            val itemCount = items.size

            // Find corresponding inventory items for this purity
            val inventoryItemsForPurity = inventoryItemsByPurity[purity] ?: emptyList()
            val inventoryGsWt = inventoryItemsForPurity.sumOf { it.gsWt }
            val inventoryFnWt = inventoryItemsForPurity.sumOf { it.fnWt }

            // Calculate remaining (purchase - inventory)
            val remainingGsWt = (totalGsWt - inventoryGsWt).coerceAtLeast(0.0)
            val remainingFnWt = (totalFnWt - inventoryFnWt).coerceAtLeast(0.0)

            report.appendLine("🔸 PURITY: $purity")
            report.appendLine("   ┌─ PURCHASE ORDER:")
            report.appendLine("   │  • Items: $itemCount pieces")
            report.appendLine("   │  • Gross Weight: ${totalGsWt.to3FString()} gm")
            report.appendLine("   │  • Fine Weight: ${totalFnWt.to3FString()} gm")
            report.appendLine("   ├─ ADDED TO INVENTORY:")
            report.appendLine("   │  • Gross Weight: ${inventoryGsWt.to3FString()} gm")
            report.appendLine("   │  • Fine Weight: ${inventoryFnWt.to3FString()} gm")
            report.appendLine("   └─ REMAINING TO ADD:")
            report.appendLine("      • Gross Weight: ${remainingGsWt.to3FString()} gm")
            report.appendLine("      • Fine Weight: ${remainingFnWt.to3FString()} gm")

            if (remainingGsWt > 0 || remainingFnWt > 0) {
                report.appendLine("      ⚠️  PENDING ITEMS TO ADD")
            } else {
                report.appendLine("      ✅ ALL ITEMS ADDED")
            }
            report.appendLine()
        }

        // Overall summary
        val totalPurchaseGsWt = purchaseItemList.sumOf { it.gsWt }
        val totalPurchaseFnWt = purchaseItemList.sumOf { it.fnWt }
        val totalInventoryGsWt = inventoryItems.sumOf { it.gsWt }
        val totalInventoryFnWt = inventoryItems.sumOf { it.fnWt }
        val totalRemainingGsWt = (totalPurchaseGsWt - totalInventoryGsWt).coerceAtLeast(0.0)
        val totalRemainingFnWt = (totalPurchaseFnWt - totalInventoryFnWt).coerceAtLeast(0.0)

        report.appendLine("📊 OVERALL SUMMARY")
        report.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        report.appendLine("🛒 Total Purchased:")
        report.appendLine("   • Gross Weight: ${totalPurchaseGsWt.to3FString()} gm")
        report.appendLine("   • Fine Weight: ${totalPurchaseFnWt.to3FString()} gm")
        report.appendLine("📦 Total Added to Inventory:")
        report.appendLine("   • Gross Weight: ${totalInventoryGsWt.to3FString()} gm")
        report.appendLine("   • Fine Weight: ${totalInventoryFnWt.to3FString()} gm")
        report.appendLine("⏳ Total Remaining:")
        report.appendLine("   • Gross Weight: ${totalRemainingGsWt.to3FString()} gm")
        report.appendLine("   • Fine Weight: ${totalRemainingFnWt.to3FString()} gm")

        if (totalRemainingGsWt > 0 || totalRemainingFnWt > 0) {
            report.appendLine()
            report.appendLine("⚠️  WARNING: Some items from this purchase order are not yet added to inventory!")
        } else {
            report.appendLine()
            report.appendLine("✅ All items from this purchase order have been added to inventory.")
        }

        return report.toString()
    }

    fun updateCatAndSubQtyAndWt() {
        ioLaunch {
            try {
                appDatabase.masterDao().recalcAll()
                getCategoryAndSubCategoryDetails()
                loadInventorySummary()
            } catch (e: Exception) {
                this@InventoryViewModel.log("failed to update category and subcategory quantities and weights: ${e.message}")
            } finally {
            }
        }
    }

    /**
     * Get purchase order details by ID
     */
    suspend fun getPurchaseOrderById(purchaseOrderId: String): PurchaseOrderEntity? {
        return withIo {
            appDatabase.purchaseDao().getOrderWithDetails(purchaseOrderId)?.order
        }
    }

    /**
     * Populate form fields with item data for update
     */
    fun populateUpdateFields(item: ItemEntity, subCatId: String, onComplete: () -> Unit) {
        ioScope {
            try {
                if (item.purchaseOrderId.isNotBlank()) {
                    val purchaseOrder = getPurchaseOrderById(item.purchaseOrderId)
                    if (purchaseOrder == null) {
                        isSelf.value = true
                        billDate.clear()
                        billNo.clear()
                        billItemDetails.value = ""
                    } else {
                        mainScope {
                            isSelf.value = false
                            billDate.text = purchaseOrder.billDate
                            billNo.text = purchaseOrder.billNo
                            getPurchaseOrderItemDetails(purchaseOrder, subCatId)
                        }
                    }
                } else {
                    // Reset to self mode if no purchase order
                    isSelf.value = true
                    billDate.clear()
                    billNo.clear()
                    billItemDetails.value = ""
                }


                mainScope {
                    // First populate all the basic item fields
                    addToName.text = item.itemAddName
                    entryType.text = item.entryType
                    qty.text = item.quantity.toString()
                    grWt.text = item.gsWt.to3FString()
                    ntWt.text = item.ntWt.to3FString()
                    fnWt.text = item.fnWt.to3FString()
                    purity.text = item.purity
                    chargeType.text = item.crgType
                    charge.text = item.crg.to3FString()
                    otherChargeDes.text = item.othCrgDes
                    othCharge.text = item.othCrg.to3FString()
                    cgst.text = item.cgst.to3FString()
                    sgst.text = item.sgst.to3FString()
                    igst.text = item.igst.to3FString()
                    desKey.text = item.addDesKey
                    desValue.text = item.addDesValue
                    huid.text = item.huid
                }

                onComplete()
            } catch (e: Exception) {

                this@InventoryViewModel.log("Failed to fetch purchase order details: ${e.message}")
                // Fallback to self mode if purchase order not found
                mainScope {
                    isSelf.value = true
                    billDate.text = ""
                    billNo.text = ""
                    billItemDetails.value = ""
                }
            }
        }
    }

    /**
     * Safely update an item with error handling
     */
    fun safeUpdateItem(
        item: ItemEntity, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withIo {
                    val result = appDatabase.itemDao().updateItem(item)
                    if (result > 0) {
                        mainScope {
                            onSuccess()
                        }
                    } else {
                        mainScope {
                            onFailure(Exception("Update failed - no rows affected"))
                        }
                    }
                }
            } catch (e: Exception) {
                mainScope {
                    onFailure(e)
                }
            }
        }
    }
}