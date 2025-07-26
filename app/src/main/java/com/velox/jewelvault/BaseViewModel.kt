package com.velox.jewelvault

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.fetchAllMetalRates
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class BaseViewModel @Inject constructor(
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState : MutableState<Boolean>,
    private val _snackBarState: MutableState<String>,
    private val _metalRates: SnapshotStateList<MetalRate>,
    private val appDatabase: AppDatabase
) : ViewModel() {

    var loading by _loadingState
    var snackMessage by _snackBarState
    val dataStoreManager = _dataStoreManager
    val metalRates = _metalRates
    val metalRatesLoading = mutableStateOf(false)
    val isConnectedState = mutableStateOf(false)
    val storeImage = mutableStateOf<String?>(null)
    val storeName = mutableStateOf<String?>(null)

    suspend fun refreshMetalRates(state: String = "visakhapatnam", context: Context) {
        metalRates.clear()
        metalRates.addAll(fetchAllMetalRates(state, context,metalRatesLoading))
    }

    fun loadStoreImage() {
        ioLaunch {
            try {
                val userId = _dataStoreManager.userId.first()
                val store = appDatabase.storeDao().getStoreById(userId)
                log("Loading store image: ${store?.image}")
                storeImage.value = store?.image
            } catch (e: Exception) {
                log("Error loading store image: ${e.message}")
            }
        }
    }

    fun loadStoreName() {
        ioLaunch {
            try {
                val userId = _dataStoreManager.userId.first()
                val store = appDatabase.storeDao().getStoreById(userId)
                log("Loading store image: ${store?.name}")
                storeName.value = store?.name
            } catch (e: Exception) {
                log("Error loading store image: ${e.message}")
            }
        }
    }

}