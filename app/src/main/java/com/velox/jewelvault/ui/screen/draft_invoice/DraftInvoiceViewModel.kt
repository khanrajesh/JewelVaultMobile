package com.velox.jewelvault.ui.screen.draft_invoice

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.roomdb.entity.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.generateDraftInvoicePdf
import com.velox.jewelvault.utils.ioLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DraftInvoiceViewModel @Inject constructor(
    private val _snackBarState: MutableState<String>
) : ViewModel() {
    
    // Customer details
    val customerName = InputFieldState("")
    val customerMobile = InputFieldState("")
    val customerAddress = InputFieldState("")
    val customerGstin = InputFieldState("")
    
    // Signatures
    val customerSign = mutableStateOf<ImageBitmap?>(null)
    val ownerSign = mutableStateOf<ImageBitmap?>(null)
    
    // UI states
    val showSeparateCharges = mutableStateOf(false)
    val showAddItemDialog = mutableStateOf(false)
    val snackBarState =_snackBarState
    
    // Selected item
    val selectedItem = mutableStateOf<ItemSelectedModel?>(null)
    val selectedItemList = mutableListOf<ItemSelectedModel>()
    
    // Generated PDF
    val generatedPdfFile = mutableStateOf<Uri?>(null)
    
    // Functions
    fun getItemById(itemId: Int, onSuccess: () -> Unit, onFailure: () -> Unit) {
        // Implementation will be added later
        onFailure()
    }
    
    fun getCustomerByMobile() {
        // Implementation will be added later
    }
    
    fun updateChargeView(show: Boolean) {
        showSeparateCharges.value = show
    }
    
    fun clearData() {
        customerGstin.text = ""
        customerAddress.text = ""
        customerName.text = ""
        customerMobile.text = ""
        customerSign.value = null
        ownerSign.value = null
        selectedItemList.clear()
        selectedItem.value = null
        showAddItemDialog.value = false
        generatedPdfFile.value = null
    }
    
    fun completeOrder(context: Context,onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        ioLaunch {
            try {
                if (customerSign.value != null && ownerSign.value != null /*&& selectedItemList.isNotEmpty()*/) {
                    // Create mock customer and store for draft invoice
                    val customer = CustomerEntity(
                        mobileNo = customerMobile.text,
                        name = customerName.text,
                        address = customerAddress.text,
                        gstin_pan = customerGstin.text,
                        addDate = java.sql.Timestamp(System.currentTimeMillis()),
                        lastModifiedDate = java.sql.Timestamp(System.currentTimeMillis())
                    )
                    
                    val store = StoreEntity(
                        userId = 1,
                        proprietor = "Draft Proprietor",
                        name = "Draft Store",
                        eamil = "draft@store.com",
                        phone = "1234567890",
                        address = "Draft Address",
                        registrationNo = "DRAFT-REG-001",
                        gstinNo = "DRAFT-GSTIN",
                        panNo = "DRAFTPAN123",
                        image = "",
                        invoiceNo = 0
                    )
                    
                    generateDraftInvoicePdf(
                        context = context,
                        store = store,
                        customer = customer,
                        items = selectedItemList,
                        customerSign = customerSign.value!!,
                        ownerSign = ownerSign.value!!
                    ) { file ->
                        generatedPdfFile.value = file
                        onSuccess()
                    }
                } else {
                    onFailure("Please ensure all required fields are filled and signatures are captured")
                }
            } catch (e: Exception) {
                onFailure("Unable to Generate Draft Invoice PDF: ${e.message}")
            }
        }
    }
}