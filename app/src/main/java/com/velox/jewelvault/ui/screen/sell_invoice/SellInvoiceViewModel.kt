package com.velox.jewelvault.ui.screen.sell_invoice

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.roomdb.entity.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.generateInvoicePdf
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.sql.Timestamp
import javax.inject.Inject

@HiltViewModel
class SellInvoiceViewModel @Inject constructor(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
) : ViewModel() {

    val showAddItemDialog = mutableStateOf(false)
    val showSeparateCharges = mutableStateOf(true)
    val selectedItem = mutableStateOf<ItemSelectedModel?>(null)
    val selectedItemList = SnapshotStateList<ItemSelectedModel>()

    val customerName = InputFieldState()
    val customerMobile = InputFieldState()
    val customerAddress = InputFieldState()
    val customerGstin = InputFieldState()
    private val customerExists = mutableStateOf(false)
    val ownerSign = mutableStateOf<ImageBitmap?>(null)
    val customerSign = mutableStateOf<ImageBitmap?>(null)

    var generatedPdfFile by mutableStateOf<Uri?>(null)
        private set

    init {
        viewModelScope.launch {
            withIo {
                showSeparateCharges.value =
                    _dataStoreManager.getValue(DataStoreManager.SHOW_SEPARATE_CHARGE).first()
                        ?: false
            }
        }
    }

    fun updateChargeView(state: Boolean) {
        viewModelScope.launch {
            withIo {
                _dataStoreManager.setValue(DataStoreManager.SHOW_SEPARATE_CHARGE, state)
                delay(50)
                showSeparateCharges.value =
                    _dataStoreManager.getValue(DataStoreManager.SHOW_SEPARATE_CHARGE).first()
                        ?: false
            }
        }
    }

    fun getItemById(itemId: Int, onSuccess: () -> Unit, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            return@launch withIo {
                val item = appDatabase.itemDao().getItemById(itemId)
                if (item != null) {
                    val addItem = ItemSelectedModel(
                        item.itemId,
                        item.itemAddName,
                        item.catId,
                        item.userId,
                        item.storeId,
                        item.catName,
                        item.subCatId,
                        item.subCatName,
                        item.entryType,
                        item.quantity,
                        item.gsWt,
                        item.ntWt,
                        item.fnWt,
                        0.0,
                        item.purity,
                        item.crgType,
                        item.crg,
                        item.othCrgDes,
                        item.othCrg,
                        item.cgst,
                        item.sgst,
                        item.igst,
                        item.huid,
                        addDate = item.addDate
                    )

                    selectedItem.value = addItem
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


    //region customer
    fun getCustomerByMobile(onFound: (CustomerEntity?) -> Unit) {
        viewModelScope.launch {
            val mobile = customerMobile.text.trim()
            if (mobile.isNotEmpty()) {
                val customer = appDatabase.customerDao().getCustomerByMobile(mobile)
                customer?.let {
                    customerExists.value = true
                    customerName.text = it.name
                    customerAddress.text = it.address ?: ""
                    customerGstin.text = it.gstin_pan ?: ""
                } ?: run {
                    customerExists.value = false
                }
                onFound(customer)
            } else {
                customerExists.value = false
                onFound(null)
            }
        }
    }

    fun addCustomer(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val mobile = customerMobile.text.trim()
            if (mobile.isEmpty()) {
                onError("Mobile number is required")
                return@launch
            }

            val existingCustomer = appDatabase.customerDao().getCustomerByMobile(mobile)
            if (existingCustomer != null) {
                onError("Customer already exists")
                return@launch
            }

            val newCustomer = CustomerEntity(
                mobileNo = mobile,
                name = customerName.text.trim(),
                address = customerAddress.text.trim(),
                gstin_pan = customerGstin.text.trim(),
                addDate = Timestamp(System.currentTimeMillis()),
                modifiedDate = Timestamp(System.currentTimeMillis())
            )

            val id = appDatabase.customerDao().insertCustomer(newCustomer)
            if (id > 0) {
                onSuccess()
            } else {
                onError("Failed to add customer")
            }
        }
    }
    //endregion


    fun generateInvoice() {
        viewModelScope.launch {
            withIo {
                val storeId = _dataStoreManager.storeId.first()
                val store = appDatabase.storeDao().getStoreById(storeId)

                val cus = CustomerEntity(
                    mobileNo = customerMobile.text.trim(),
                    name = customerName.text.trim(),
                    address = customerAddress.text.trim(),
                    gstin_pan = customerGstin.text.trim(),
                    addDate = Timestamp(System.currentTimeMillis()),
                    modifiedDate = Timestamp(System.currentTimeMillis())
                )

                if (store != null && customerSign.value != null && ownerSign.value !=null){
                    generateInvoicePdf(
                        context = context,
                        store = store,
                        customer = cus,
                        items = selectedItemList,
                        customerSign = customerSign.value!!,
                        ownerSign = ownerSign.value!!
                    ) { file ->
                        generatedPdfFile = file
                    }
                }else{

                }
            }
        }
    }


}