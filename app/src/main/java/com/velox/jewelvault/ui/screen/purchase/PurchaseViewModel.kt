package com.velox.jewelvault.ui.screen.purchase

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    private val _snackBarState: MutableState<String>
):ViewModel() {

    val firmName = InputFieldState()
    val firmMobile = InputFieldState()
    val firmAddress = InputFieldState()
    val firmGstin = InputFieldState()
    val sellerName = InputFieldState()
    val sellerMobile = InputFieldState()

    
    val addCat = InputFieldState()
    val addSubCat = InputFieldState()
    val addGsWt = InputFieldState()
    val addPurity = InputFieldState()
    val addFnWt = InputFieldState()
    val addWastage = InputFieldState()
    val addMetalRate = InputFieldState()
    val addExtraChargeDes = InputFieldState()
    val addExtraCharge = InputFieldState()



    fun getFirmByFirmMobile(){

    }

    fun getFirmBySellerMobile() {
        TODO("Not yet implemented")
    }


}