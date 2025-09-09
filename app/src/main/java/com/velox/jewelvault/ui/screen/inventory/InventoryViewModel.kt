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
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.roundTo3Decimal
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
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
//    private val _loadingState: MutableState<Boolean>,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,

    ) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState
    val dataStoreManager = _dataStoreManager

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = _dataStoreManager.getAdminInfo()

    /**
     * return Triple of Flow<String> for storeId, upiId, storeName
     * */
    val store: Triple<Flow<String>, Flow<String>, Flow<String>> =
        _dataStoreManager.getSelectedStoreInfo()

    //    val loadingState = _loadingState
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

    // Filter visibility state
    val isFilterExpanded = mutableStateOf(false)

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
        "Extra"
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

    // Enhanced filter function with all parameters
    private var filterJob: Job? = null
    fun filterItems() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.IO) {
            // Immediately clear current list so UI shows "no items" while loading/filtering.
            // This ensures we always return a value to UI even if something fails later.
            itemList.clear()

            // Parse inputs safely outside the flow collection to avoid throwing inside the Flow
            val catId = runCatching {
                catSubCatDto.asSequence()
                    .find { it.catName == categoryFilter.text }
                    ?.catId
            }.getOrNull()

            val subCatId = runCatching {
                catSubCatDto.asSequence()
                    .flatMap { it.subCategoryList.asSequence() }
                    .find { it.subCatName == subCategoryFilter.text }
                    ?.subCatId
            }.getOrNull()

            val startDate = runCatching {
                if (startDateFilter.text.isNotBlank()) {
                    val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                    dateFormat.parse(startDateFilter.text)?.let { java.sql.Timestamp(it.time) }
                } else null
            }.getOrNull()

            val endDate = runCatching {
                if (endDateFilter.text.isNotBlank()) {
                    val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                    dateFormat.parse(endDateFilter.text)?.let {
                        val calendar = java.util.Calendar.getInstance()
                        calendar.time = it
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                        calendar.set(java.util.Calendar.MINUTE, 59)
                        calendar.set(java.util.Calendar.SECOND, 59)
                        java.sql.Timestamp(calendar.timeInMillis)
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

            // Now subscribe to the DAO flow with defensive flow operators.
            try {
                appDatabase.itemDao()
                    .filterItems(
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
                    )
                    .onStart {
                        // optional: show loading state (if you have one)
                    }
                    .catch { e ->
                        // Flow-level error handling (e.g., DB errors)
                        this@InventoryViewModel.log("filterItems flow error: ${e.message}")
                        _snackBarState.value = "Failed to filter items: ${e.message ?: "Unknown error"}"
                        // Ensure itemList stays empty or previous value (we already cleared above)
                    }
                    .collectLatest { items ->
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
                    }
            } catch (e: Exception) {
                // This try/catch defends against unexpected errors constructing the Flow or subscribing,
                // but most errors should be handled in .catch above.
                this@InventoryViewModel.log("failed to filter item list (outer): ${e.message}")
                _snackBarState.value = "Failed to filter items: ${e.message}"
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
//            _loadingState.value = true
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
//                                            _loadingState.value = false
                                            onSuccess(insertedItem, newItemId)
                                            // Refresh the item list after adding
                                            loadRecentItems()
                                        }
                                    }
                                } else {
                                    _snackBarState.value = "Failed to update Sub Category Weight"
//                                    _loadingState.value = false
                                }
                            }
                        } catch (e: Exception) {
//                            _loadingState.value = false
                            _snackBarState.value =
                                ("Error updating cat and sub cat weight: ${e.message}")
                            this@InventoryViewModel.log("Error updating cat and sub cat weight: ${e.message}")
                        }
//                        _loadingState.value = true
                    } else {
//                        _loadingState.value = false
                        _snackBarState.value = ("Failed to insert item")
                        this@InventoryViewModel.log("Failed to insert item")
                    }
                }
            } catch (e: Exception) {
//                _loadingState.value = false
                _snackBarState.value = "Error inserting item error: ${e.message}"
                onFailure(e)
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
                                }

                                onSuccess(rowsDeleted)
                                // Refresh the item list after deleting
                                filterItems()
                            } catch (e: Exception) {
                                onFailure(e)
                            }
                        } else {
                            this@InventoryViewModel.log("No rows deleted. Check itemId, catId, and subCatId.")
                        }
                    } else {
                        this@InventoryViewModel.log("No Item found with ID $itemId")
                    }
                }
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    fun getBillsFromDate() {
        ioLaunch {
            billItemDetails.value = ""
            purchaseOrdersByDate.clear()
            billNo.clear()
            val data = appDatabase.purchaseDao().getOrdersByBillDate(billDate.text)
            purchaseOrdersByDate.addAll(data)

            if (purchaseOrdersByDate.isEmpty()) {
                _snackBarState.value = "No purchase bill found"
            }
        }
    }

    fun getPurchaseOrderItemDetails(item: PurchaseOrderEntity, subCatName: String) {
        ioLaunch {
            val purchaseItemList = appDatabase.purchaseDao()
                .getItemsByOrderIdAndSubCatName(item.purchaseOrderId, subCatName)
            purchaseItems.clear()
            billItemDetails.value = ""
            if (purchaseItemList.isNotEmpty()) {
                val sellerInfo = appDatabase.purchaseDao().getSellerById(item.sellerId)

                if (sellerInfo != null) {
                    //todo
//                    sellerFirmId = 0,
//                    purchaseOrderId = 0,
//                    purchaseItemId = 0,
                    val firInfo = appDatabase.purchaseDao().getFirmById(sellerInfo.firmId)
                    val t =
                        "${firInfo?.firmName} by ${sellerInfo.name} (${sellerInfo.mobileNumber})"
                    val u = purchaseItemList.groupBy { it.purity }.map { (purity, items) ->
                        val total = items.sumOf { it.gsWt }
                        "Purity: $purity: Total Gs Wt: $total gm "
                    }.joinToString(", ")
                    billItemDetails.value = "$t \n$u"

                    purchaseItems.addAll(purchaseItemList)
                }
            }
        }
    }

    fun updateCatAndSubQtyAndWt() {
        ioLaunch {
            appDatabase.masterDao().recalcAll()
            getCategoryAndSubCategoryDetails()
            loadInventorySummary()
        }
    }
}