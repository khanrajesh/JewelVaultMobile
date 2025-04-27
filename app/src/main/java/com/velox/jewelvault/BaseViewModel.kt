package com.velox.jewelvault

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.fetchAllMetalRates
import com.velox.jewelvault.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BaseViewModel @Inject constructor(
    private val _dataStoreManager: DataStoreManager
) : ViewModel() {

    var loading by mutableStateOf(false)
    var snackMessage by mutableStateOf("")
    val dataStoreManager = _dataStoreManager


    val metalRates = mutableStateListOf<MetalRate>()
    val metalRatesLoading = mutableStateOf(false)


    suspend fun refreshMetalRates(state: String = "visakhapatnam", context: Context) {
        metalRates.clear()
        metalRates.addAll(fetchAllMetalRates(state, context,metalRatesLoading))
    }

}