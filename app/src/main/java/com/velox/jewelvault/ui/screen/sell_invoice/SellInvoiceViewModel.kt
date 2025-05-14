package com.velox.jewelvault.ui.screen.sell_invoice

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellInvoiceViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
) : ViewModel() {

    val showAddItemDialog = mutableStateOf(false)
    val showSeparateCharges = mutableStateOf(true)
    val selectedItem = mutableStateOf<ItemEntity?>(null)
    val selectedItemList = SnapshotStateList<ItemSelectedModel>()

    init {
        viewModelScope.launch {
            withIo {
                showSeparateCharges.value =  _dataStoreManager.getValue(DataStoreManager.SHOW_SEPARATE_CHARGE).first()?:false
            }
        }
    }

    fun updateChargeView(state:Boolean){
        viewModelScope.launch {
            withIo {
                _dataStoreManager.setValue(DataStoreManager.SHOW_SEPARATE_CHARGE,state)
                delay(50)
                showSeparateCharges.value =  _dataStoreManager.getValue(DataStoreManager.SHOW_SEPARATE_CHARGE).first()?:false
            }
        }
    }

    fun getItemById( itemId: Int, onSuccess: () -> Unit, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            return@launch withIo {
                val item = appDatabase.itemDao().getItemById(itemId)
                if (item != null) {
                        selectedItem.value = item
                        onSuccess()
                } else {
                    onFailure("No Item Found")
                }
            }
        }
    }


    /*fun getItemById(metalRates:SnapshotStateList<MetalRate>, itemId: Int, onSuccess: (ItemSelectedModel) -> Unit, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
           return@launch withIo {
                val item = appDatabase.itemDao().getItemById(itemId)
                if (item != null) {
                    if (item.catName.trim().lowercase() == "gold"){
                        val price24k = metalRates.firstOrNull { price -> price.metal == "Gold" && price.caratOrPurity == "24K" }?.price
                        val gold100: Double = (100 / 99.9) * (price24k?.toDoubleOrNull() ?: 0.0)

                        if (price24k == null || gold100 == 0.0){
                            onFailure("")
                            return@withIo
                        }

                        val price = gold100 * item.fnWt
                        val charge = when (item.crgType) {
                            "%" -> price * (item.crg / 100)
                            "piece" -> item.crg
                            else -> 0.0
                        }

                        val tax = (price+charge) * ((item.cgst + item.igst + item.sgst) / 100)

                        val modifiedItems = ItemSelectedModel(
                            item.itemId,
                            item.itemAddName,
                            item.catId,
                            item.userId,
                            item.storeId,
                            item.catName,
                            item.subCatId,
                            item.subCatName,
                            item.type,
                            item.quantity,
                            item.gsWt,
                            item.ntWt,
                            item.fnWt,
                            item.purity,
                            item.crgType,
                            item.crg,
                            item.othCrgDes,
                            item.othCrg,
                            item.cgst,
                            item.sgst,
                            item.igst,
                            item.huid,
                            price,
                            charge,
                            tax
                        )
                        selectedItem.value = modifiedItems
                        onSuccess(modifiedItems)
                    }else{
                        onFailure("current we are only calculating gold items only")
                    }

                } else {
                    onFailure("No Item Found")
                }
            }
        }
    }*/

}