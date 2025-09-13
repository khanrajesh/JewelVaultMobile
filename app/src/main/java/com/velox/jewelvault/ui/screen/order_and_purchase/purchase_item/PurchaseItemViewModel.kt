package com.velox.jewelvault.ui.screen.order_and_purchase.purchase_item

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.PurchaseOrderWithDetails
import com.velox.jewelvault.data.roomdb.entity.purchase.FirmEntity
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.to3FString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PurchaseItemViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    @Named("snackMessage") private val _snackBar: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState

    val purchaseOrderWithDetails = mutableStateOf<PurchaseOrderWithDetails?>(null)
    val firmEntity = mutableStateOf<FirmEntity?>(null)
    val snackBar = _snackBar
    


    val orderHeaderList = listOf(
        "S.No",
        "Category",
        "Sub Category",
        "Item Id",
        "To Name",
        "Type",
        "Qty",
        "Gr.Wt",
        "Nt.Wt",
        "Purity",
        "Fn.Wt",
        "MC.Type",
        "M.Chr",
        "Oth Chr",
        "Chr",
        "Tax",
        "H-UID",
        "Des",
        "Value",
        "Extra"
    )

    val orderList: SnapshotStateList<List<String>> = SnapshotStateList()

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
    val itemList: SnapshotStateList<List<String>> = SnapshotStateList()


    fun getPurchaseOrderById(purchaseOrderId: String) {
        ioLaunch {
            _loadingState.value = true
            if (purchaseOrderId != null) {
                val order = appDatabase.purchaseDao().getOrderWithDetails(purchaseOrderId)
                if (order != null) {
                    val firmId = order.seller?.firmId
                    firmId?.let {
                        val firmEntity = appDatabase.purchaseDao().getFirmById(firmId)
                        if (firmEntity != null) {
                            purchaseOrderWithDetails.value = order
                            this@PurchaseItemViewModel.firmEntity.value = firmEntity
                        }
                    }
                }
            }
            _loadingState.value = false
        }
    }

    fun getInventoryItemByPurchaseOrderID(purchaseOrderId: String) {
        ioLaunch {
            _loadingState.value = true
            val orderId = purchaseOrderId
            val data = appDatabase.itemDao().getItemByPurchaseOrderId(orderId)
            data.collectLatest { items->
                val contentList = items.mapIndexed { index, item ->
                    listOf(
                        "${index + 1}",
                        "${item.catName} (${item.catId})",
                        "${item.subCatName} (${item.subCatId})",
                        "${item.itemId}",
                        item.itemAddName,
                        item.entryType,
                        item.quantity.toString(),
                        item.gsWt.to3FString(),
                        item.ntWt.to3FString(),
                        item.unit,
                        item.purity,
                        item.fnWt.to3FString(),
                        item.crgType,
                        item.crg.to3FString(),
                        item.othCrgDes,
                        item.othCrg.to3FString(),
                        (item.cgst + item.sgst + item.igst).to3FString(),
                        item.huid,
                        item.addDate.toString(),
                        item.addDesKey,
                        item.addDesValue,
                        "Extra value"
                    )
                }

                itemList.clear()
                itemList.addAll(contentList)
            }
            _loadingState.value = false
        }
    }

    fun getOrderItemsByPurchaseOrderId(purchaseOrderId: String) {
        ioLaunch {
            _loadingState.value = true
            val orderId = purchaseOrderId.toIntOrNull()
            if (orderId != null){
                val data = appDatabase.orderDao().getAllOrdersByPurchaseOrderIdInDesc(orderId)
                data.collectLatest { items->
                    val contentList = items.mapIndexed { index, item ->
                        listOf(
                            "${index + 1}",
                            "${item.catName} (${item.catId})",
                            "${item.subCatName} (${item.subCatId})",
                            "${item.itemId}",
                            item.itemAddName,
                            item.entryType,
                            item.quantity.toString(),
                            item.gsWt.to3FString(),
                            item.ntWt.to3FString(),
                            item.purity,
                            item.fnWt.to3FString(),
                            item.crgType,
                            item.crg.to3FString(),
                            item.othCrgDes,
                            item.othCrg.to3FString(),
                            (item.cgst + item.sgst + item.igst).to3FString(),
                            item.huid,
                            item.addDesKey,
                            item.addDesValue,
                            "Extra value"
                        )
                    }

                    orderList.clear()
                    orderList.addAll(contentList)
                }
            }
            _loadingState.value = false
        }
    }

    fun deletePurchaseWithItems(
        purchaseOrderId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _loadingState.value = true
                
                // Delete purchase order items
                val purchaseItems = appDatabase.purchaseDao().getItemsByOrderId(purchaseOrderId)
                purchaseItems.forEach { item ->
                    appDatabase.purchaseDao().deleteItem(item)
                }
                
                // Delete metal exchange items for this purchase order
                val metalExchanges = appDatabase.purchaseDao().getExchangeByOrderId(purchaseOrderId.toLong())
                metalExchanges.forEach { exchange ->
                    appDatabase.purchaseDao().deleteExchange(exchange)
                }
                
                // Delete the purchase order itself
                val purchaseOrder = purchaseOrderWithDetails.value?.order
                purchaseOrder?.let {
                    appDatabase.purchaseDao().deleteOrder(it)
                }
                
                onSuccess()
            } catch (e: Exception) {
                onFailure("Failed to delete purchase: ${e.message}")
            } finally {
                _loadingState.value = false
            }
        }
    }
    

}