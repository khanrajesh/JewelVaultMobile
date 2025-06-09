package com.velox.jewelvault.ui.screen.purchase

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.CatSubCatDto
import com.velox.jewelvault.data.roomdb.dto.PurchaseMetalRateDto
import com.velox.jewelvault.data.roomdb.dto.PurchaseItemInputDto
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    
    val addBillNo = InputFieldState()
    val addBillDate = InputFieldState()


    val addCat = InputFieldState()
    val addSubCat = InputFieldState()
    val addGsWt = InputFieldState()
    val addPurity = InputFieldState()
    val addFnWt = InputFieldState()
    val addWastage = InputFieldState()
    val addMetalRate = InputFieldState()
    val addExtraChargeDes = InputFieldState()
    val addExtraCharge = InputFieldState()
    val addCGst = InputFieldState("1.5")
    val addIGst = InputFieldState("1.5")
    val addSGst = InputFieldState()

    val addExchangeCategory = InputFieldState()
    val addExchangeFnWeight = InputFieldState()

    val catSubCatDto: SnapshotStateList<CatSubCatDto> = SnapshotStateList()
    val purchaseItemList = mutableStateListOf<PurchaseItemInputDto>()
    val purchaseMetalRateDtos = mutableStateListOf<PurchaseMetalRateDto>()




    fun getCategoryAndSubCategoryDetails() {
        ioScope {
            try {
                val userId = _dataStoreManager.userId.first()
                val storeId = _dataStoreManager.storeId.first()
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
                        }
                    )
                }

                mainScope {
                    // update only if different
                    if (catSubCatDto != newList) {
                        catSubCatDto.clear()
                        catSubCatDto.addAll(newList)
                    }
                }
            } catch (e: Exception) {
                this@PurchaseViewModel.log("unable to fetch categorical data")
            }
        }
    }

    fun addPurchaseItem() {
        val newItem = PurchaseItemInputDto(
            billNo = addBillNo.text,
            catId = addCat.text,
            subCatId = addSubCat.text,
            name = "${addCat.text} - ${addSubCat.text}",
            gsWt = addGsWt.text,
            purity = addPurity.text,
            fnWt = addFnWt.text,
            wastage = addWastage.text,
            fnMetalRate = addMetalRate.text,
            cgst = addCGst.text,
            sgst = addSGst.text,
            igst = addIGst.text,
            extraChargeDes = addExtraChargeDes.text,
            extraCharge = addExtraCharge.text
        )

        purchaseItemList.add(newItem)

        clearPurchaseInputs()
    }

    fun clearPurchaseInputs() {
        addCat.text = ""
        addSubCat.text = ""
        addGsWt.text = ""
        addPurity.text = ""
        addFnWt.text = ""
        addWastage.text = ""
        addMetalRate.text = ""
        addExtraChargeDes.text = ""
        addExtraCharge.text = ""
        addCGst.text = "1.5"
        addSGst.text = "1.5"
        addIGst.text = ""
    }

    fun setMetalRate(categoryId: String, metalName: String, rate: String) {
        val index = purchaseMetalRateDtos.indexOfFirst { it.categoryId == categoryId && it.metalName == metalName }
        if (index >= 0) {
            purchaseMetalRateDtos[index] = PurchaseMetalRateDto(categoryId, metalName, rate)
        } else {
            purchaseMetalRateDtos.add(PurchaseMetalRateDto(categoryId, metalName, rate))
        }
    }

    fun getFirmByFirmMobile(){
        viewModelScope.launch {
            withIo {
                try {
                   val firmDetails =  appDatabase.purchaseDao().getFirmByMobile(firmMobile.text)
                    firmDetails?.let {
                        firmName.text = it.firmName
                        firmAddress.text = it.address
                        firmGstin.text = it.gstNumber
                    }
                }catch (e:Exception){
                    this@PurchaseViewModel.log("Fail to search firm by mobile")
                }
            }
        }
    }

    fun getFirmBySellerMobile() {
       viewModelScope.launch {
           withIo {
               try {
                   val sellerInfo = appDatabase.purchaseDao().getSellerByMobile(sellerMobile.text)
                   val firmDetails = appDatabase.purchaseDao().getFirmById(sellerInfo?.firmId ?: 0)
                   if (firmMobile.text.isEmpty()){
                       firmDetails?.let {
                           firmName.text = it.firmName
                           firmAddress.text = it.address
                           firmGstin.text = it.gstNumber
                           firmMobile.text = it.firmMobileNumber
                       }
                   }
               }catch (e:Exception){
                   this@PurchaseViewModel.log("Fail to search firm by seller mobile")
               }
           }
       }
    }


}