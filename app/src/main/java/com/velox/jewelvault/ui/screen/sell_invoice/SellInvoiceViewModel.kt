package com.velox.jewelvault.ui.screen.sell_invoice

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellInvoiceViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
) : ViewModel() {

    val showAddItemDialog = mutableStateOf(false)
    val selectedItem = mutableStateOf<ItemEntity?>(null)
    val selectedItemList = SnapshotStateList<ItemEntity>()

    fun getItemById(itemId: Int, onSuccess: (ItemEntity) -> Unit, onFailure: () -> Unit = {}) {
        viewModelScope.launch {
            withIo {
                val item = appDatabase.itemDao().getItemById(itemId)
                if (item != null) {
                    selectedItem.value = item
                    onSuccess(item)
                } else {
                    onFailure()
                }
            }
        }
    }

    fun addItemToList(itemEntity: ItemEntity){
        selectedItemList.add(itemEntity)
    }

}