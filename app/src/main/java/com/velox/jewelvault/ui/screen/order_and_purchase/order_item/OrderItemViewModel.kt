package com.velox.jewelvault.ui.screen.order_and_purchase.order_item

import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderItemViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
) : ViewModel() {

}