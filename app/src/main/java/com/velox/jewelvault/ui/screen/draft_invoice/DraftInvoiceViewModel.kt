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
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.*
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Extension function for formatting double to 2 decimal places
fun Double.to2FString(): String = String.format("%.2f", this)

@HiltViewModel
class DraftInvoiceViewModel @Inject constructor(
    private val _snackBarState: MutableState<String>
) : ViewModel() {
    
    // Customer details
    val customerName = InputFieldState("")
    val customerMobile = InputFieldState("")
    val customerAddress = InputFieldState("")
    val customerGstin = InputFieldState("")
    
    // Item form fields
    val itemName = InputFieldState("")
    val categoryName = InputFieldState("")
    val subCategoryName = InputFieldState("")
    val entryType = InputFieldState("")
    val quantity = InputFieldState("")
    val grossWeight = InputFieldState("")
    val netWeight = InputFieldState("")
    val fineWeight = InputFieldState("")
    val purity = InputFieldState("")
    val chargeType = InputFieldState("")
    val charge = InputFieldState("")
    val otherChargeDescription = InputFieldState("")
    val otherCharge = InputFieldState("")
    val cgst = InputFieldState("")
    val sgst = InputFieldState("")
    val igst = InputFieldState("")
    val huid = InputFieldState("")
    val description = InputFieldState("")
    val descriptionValue = InputFieldState("")
    
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
    
    // Totals
    val subTotal = mutableStateOf(0.0)
    val totalTax = mutableStateOf(0.0)
    val grandTotal = mutableStateOf(0.0)
    
    // Functions
    fun getItemById(itemId: Int, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            try {
                // TODO: Implement actual database query when repository is available
                // For now, create a mock item for testing
                val mockItem = ItemSelectedModel(
                    itemId = itemId,
                    itemAddName = "Mock Item $itemId",
                    catId = 1,
                    userId = 1,
                    storeId = 1,
                    catName = "Ring",
                    subCatId = 1,
                    subCatName = "Gold Ring",
                    entryType = "Piece",
                    quantity = 1,
                    gsWt = 5.50,
                    ntWt = 5.00,
                    fnWt = 4.58,
                    fnMetalPrice = 6000.0,
                    purity = "916",
                    crgType = "Percentage",
                    crg = 500.0,
                    othCrgDes = "Stone",
                    othCrg = 100.0,
                    cgst = 150.0,
                    sgst = 150.0,
                    igst = 0.0,
                    addDesKey = "Design",
                    addDesValue = "Floral Pattern",
                    huid = "H123456789",
                    price = 30000.0,
                    chargeAmount = 600.0,
                    tax = 300.0,
                    addDate = java.sql.Timestamp(System.currentTimeMillis()),
                    sellerFirmId = 0,
                    purchaseOrderId = 0,
                    purchaseItemId = 0
                )
                
                selectedItem.value = mockItem
                
                // Populate form fields with item data
                itemName.text = mockItem.itemAddName
                categoryName.text = mockItem.catName
                subCategoryName.text = mockItem.subCatName
                entryType.text = mockItem.entryType
                quantity.text = mockItem.quantity.toString()
                grossWeight.text = mockItem.gsWt.toString()
                netWeight.text = mockItem.ntWt.toString()
                fineWeight.text = mockItem.fnWt.toString()
                purity.text = mockItem.purity
                chargeType.text = mockItem.crgType
                charge.text = mockItem.crg.toString()
                otherChargeDescription.text = mockItem.othCrgDes
                otherCharge.text = mockItem.othCrg.toString()
                cgst.text = mockItem.cgst.toString()
                sgst.text = mockItem.sgst.toString()
                igst.text = mockItem.igst.toString()
                huid.text = mockItem.huid
                description.text = mockItem.addDesKey
                descriptionValue.text = mockItem.addDesValue
                
                onSuccess()
            } catch (e: Exception) {
                snackBarState.value = "Error loading item: ${e.message}"
                onFailure()
            }
        }
    }
    
    fun addSampleItem() {
        // Add a sample item for testing
        val sampleItem = ItemSelectedModel(
            itemId = 1,
            itemAddName = "Sample Gold Ring",
            catId = 1,
            userId = 1,
            storeId = 1,
            catName = "Ring",
            subCatId = 1,
            subCatName = "Gold Ring",
            entryType = "Piece",
            quantity = 1,
            gsWt = 5.50,
            ntWt = 5.00,
            fnWt = 4.58,
            fnMetalPrice = 6000.0,
            purity = "916",
            crgType = "Percentage",
            crg = 500.0,
            othCrgDes = "Stone",
            othCrg = 100.0,
            cgst = 150.0,
            sgst = 150.0,
            igst = 0.0,
            addDesKey = "Design",
            addDesValue = "Floral Pattern",
            huid = "H123456789",
            price = 30000.0,
            chargeAmount = 600.0,
            tax = 300.0,
            addDate = java.sql.Timestamp(System.currentTimeMillis()),
            sellerFirmId = 0,
            purchaseOrderId = 0,
            purchaseItemId = 0
        )
        
        selectedItemList.add(sampleItem)
        calculateTotals()
        snackBarState.value = "Sample item added for testing"
    }
    
    fun getCustomerByMobile() {
        if (customerMobile.text.isBlank()) {
            snackBarState.value = "Please enter mobile number"
            return
        }
        
        if (!InputValidator.isValidPhoneNumber(customerMobile.text)) {
            snackBarState.value = "Please enter valid mobile number"
            return
        }
        
        viewModelScope.launch {
            try {
                // TODO: Implement actual database query when repository is available
                // For now, create mock customer data for testing
                val mockCustomer = when (customerMobile.text) {
                    "9876543210" -> {
                        customerName.text = "John Doe"
                        customerAddress.text = "123 Main Street, City"
                        customerGstin.text = "22AAAAA0000A1Z5"
                        snackBarState.value = "Customer found"
                    }
                    "9123456789" -> {
                        customerName.text = "Jane Smith"
                        customerAddress.text = "456 Oak Avenue, Town"
                        customerGstin.text = "27BBBBB1111B2Y6"
                        snackBarState.value = "Customer found"
                    }
                    else -> {
                        // New customer - clear fields for manual entry
                        customerName.text = ""
                        customerAddress.text = ""
                        customerGstin.text = ""
                        snackBarState.value = "New customer - please fill details"
                    }
                }
            } catch (e: Exception) {
                snackBarState.value = "Error searching customer: ${e.message}"
            }
        }
    }
    
    fun updateChargeView(show: Boolean) {
        showSeparateCharges.value = show
    }
    
    fun addItem() {
        // Validate inputs
        if (itemName.text.isBlank()) {
            snackBarState.value = "Item name is required"
            return
        }
        
        if (categoryName.text.isBlank()) {
            snackBarState.value = "Category name is required"
            return
        }
        
        if (subCategoryName.text.isBlank()) {
            snackBarState.value = "Sub-category name is required"
            return
        }
        
        if (!InputValidator.isValidQuantity(quantity.text)) {
            snackBarState.value = "Invalid quantity"
            return
        }
        
        if (!InputValidator.isValidWeight(grossWeight.text)) {
            snackBarState.value = "Invalid gross weight"
            return
        }
        
        if (!InputValidator.isValidWeight(netWeight.text)) {
            snackBarState.value = "Invalid net weight"
            return
        }
        
        if (!InputValidator.isValidWeight(fineWeight.text)) {
            snackBarState.value = "Invalid fine weight"
            return
        }
        
        if (purity.text.isBlank()) {
            snackBarState.value = "Purity is required"
            return
        }
        
        // Calculate price (basic calculation - can be enhanced)
        val netWt = netWeight.text.toDoubleOrNull() ?: 0.0
        val chargeAmt = charge.text.toDoubleOrNull() ?: 0.0
        val otherChargeAmt = otherCharge.text.toDoubleOrNull() ?: 0.0
        val cgstAmt = cgst.text.toDoubleOrNull() ?: 0.0
        val sgstAmt = sgst.text.toDoubleOrNull() ?: 0.0
        val igstAmt = igst.text.toDoubleOrNull() ?: 0.0
        
        // Basic price calculation (you can enhance this based on your business logic)
        val basePrice = netWt * 6000 // Assuming 6000 per gram as base rate
        val totalCharges = chargeAmt + otherChargeAmt
        val totalTaxes = cgstAmt + sgstAmt + igstAmt
        val finalPrice = basePrice + totalCharges + totalTaxes
        
        val newItem = ItemSelectedModel(
            itemId = selectedItemList.size + 1, // Temporary ID for draft
            itemAddName = InputValidator.sanitizeText(itemName.text),
            catId = 1, // Default category ID for draft
            userId = 1,
            storeId = 1,
            catName = InputValidator.sanitizeText(categoryName.text),
            subCatId = 1, // Default sub-category ID for draft
            subCatName = InputValidator.sanitizeText(subCategoryName.text),
            entryType = InputValidator.sanitizeText(entryType.text),
            quantity = quantity.text.toIntOrNull() ?: 1,
            gsWt = grossWeight.text.toDoubleOrNull() ?: 0.0,
            ntWt = netWeight.text.toDoubleOrNull() ?: 0.0,
            fnWt = fineWeight.text.toDoubleOrNull() ?: 0.0,
            fnMetalPrice = 6000.0, // Default metal price
            purity = InputValidator.sanitizeText(purity.text),
            crgType = InputValidator.sanitizeText(chargeType.text),
            crg = charge.text.toDoubleOrNull() ?: 0.0,
            othCrgDes = InputValidator.sanitizeText(otherChargeDescription.text),
            othCrg = otherCharge.text.toDoubleOrNull() ?: 0.0,
            cgst = cgst.text.toDoubleOrNull() ?: 0.0,
            sgst = sgst.text.toDoubleOrNull() ?: 0.0,
            igst = igst.text.toDoubleOrNull() ?: 0.0,
            addDesKey = InputValidator.sanitizeText(description.text),
            addDesValue = InputValidator.sanitizeText(descriptionValue.text),
            huid = huid.text.trim().uppercase(),
            price = finalPrice,
            chargeAmount = totalCharges,
            tax = totalTaxes,
            addDate = java.sql.Timestamp(System.currentTimeMillis()),
            sellerFirmId = 0,
            purchaseOrderId = 0,
            purchaseItemId = 0
        )
        
        selectedItemList.add(newItem)
        calculateTotals()
        clearItemForm()
        showAddItemDialog.value = false
        snackBarState.value = "Item added successfully"
    }
    
    fun removeItem(item: ItemSelectedModel) {
        selectedItemList.remove(item)
        calculateTotals()
        snackBarState.value = "Item removed"
    }
    
    private fun calculateTotals() {
        subTotal.value = selectedItemList.sumOf { it.price - it.tax }
        totalTax.value = selectedItemList.sumOf { it.tax }
        grandTotal.value = selectedItemList.sumOf { it.price }
    }
    
    private fun clearItemForm() {
        itemName.clear()
        categoryName.clear()
        subCategoryName.clear()
        entryType.clear()
        quantity.clear()
        grossWeight.clear()
        netWeight.clear()
        fineWeight.clear()
        purity.clear()
        chargeType.clear()
        charge.clear()
        otherChargeDescription.clear()
        otherCharge.clear()
        cgst.clear()
        sgst.clear()
        igst.clear()
        huid.clear()
        description.clear()
        descriptionValue.clear()
    }
    
    private fun convertNumberToWords(number: Int): String {
        if (number == 0) return "Zero"
        
        val ones = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", 
                          "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", 
                          "Seventeen", "Eighteen", "Nineteen")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
        
        fun convertHundreds(n: Int): String {
            var result = ""
            if (n >= 100) {
                result += ones[n / 100] + " Hundred "
            }
            val remainder = n % 100
            if (remainder >= 20) {
                result += tens[remainder / 10] + " "
                if (remainder % 10 != 0) {
                    result += ones[remainder % 10] + " "
                }
            } else if (remainder > 0) {
                result += ones[remainder] + " "
            }
            return result.trim()
        }
        
        var result = ""
        var num = number
        
        if (num >= 10000000) { // Crores
            result += convertHundreds(num / 10000000) + " Crore "
            num %= 10000000
        }
        if (num >= 100000) { // Lakhs
            result += convertHundreds(num / 100000) + " Lakh "
            num %= 100000
        }
        if (num >= 1000) { // Thousands
            result += convertHundreds(num / 1000) + " Thousand "
            num %= 1000
        }
        if (num > 0) {
            result += convertHundreds(num)
        }
        
        return result.trim()
    }
    
    private fun createDummyBitmap(): ImageBitmap {
        val bitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        canvas.drawText("Signature", 10f, 30f, paint)
        return bitmap.asImageBitmap()
    }
    
    fun clearData() {
        customerGstin.clear()
        customerAddress.clear()
        customerName.clear()
        customerMobile.clear()
        clearItemForm()
        customerSign.value = null
        ownerSign.value = null
        selectedItemList.clear()
        selectedItem.value = null
        showAddItemDialog.value = false
        generatedPdfFile.value = null
        subTotal.value = 0.0
        totalTax.value = 0.0
        grandTotal.value = 0.0
    }
    
    private fun createDraftInvoiceData(
        store: StoreEntity,
        customer: CustomerEntity,
        items: List<ItemSelectedModel>
    ): DraftInvoiceData {
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        // Convert ItemSelectedModel to DraftItemModel
        val draftItems = items.mapIndexed { index, item ->
            DraftInvoiceData.DraftItemModel(
                serialNumber = "${index + 1}",
                referenceNumber = item.purchaseItemId.toString(),
                productDescription = "${item.catName} ${item.subCatName} ${item.itemAddName}",
                quantitySet = "${item.quantity}",
                grossWeightGms = "${item.gsWt.to2FString()}",
                netWeightGms = "${item.ntWt.to2FString()}",
                ratePerGm = "₹${item.fnMetalPrice.to2FString()}",
                makingAmount = "₹${item.chargeAmount.to2FString()}",
                purityPercent = item.purity,
                egColumnValue = "", // Empty for now
                totalAmount = "₹${item.price.to2FString()}"
            )
        }
        
        // Calculate totals
        val subTotalAmount = selectedItemList.sumOf { it.price - it.tax }
        val totalTaxAmount = selectedItemList.sumOf { it.tax }
        val grandTotalAmount = selectedItemList.sumOf { it.price }
        
        return DraftInvoiceData(
            storeInfo = store,
            customerInfo = customer,
            invoiceMeta = DraftInvoiceData.InvoiceMetadata(
                invoiceNumber = "DRAFT-${System.currentTimeMillis() % 10000}",
                date = currentDate,
                time = currentTime,
                salesMan = "Counter 1",
                documentType = "DRAFT INVOICE"
            ),
            items = draftItems,
            goldRate = "₹6000/gm", // Default rate
            silverRate = "₹80/gm", // Default rate
            Jurisdiction = "Malkangiri",
            paymentSummary = DraftInvoiceData.PaymentSummary(
                subTotal = "₹${subTotalAmount.to2FString()}",
                gstAmount = "₹${totalTaxAmount.to2FString()}",
                gstLabel = "GST @3%",
                discount = "₹0.00",
                cardCharges = "₹0.00",
                totalAmountBeforeOldExchange = "₹${grandTotalAmount.to2FString()}",
                oldExchange = "₹0.00",
                roundOff = "₹0.00",
                netAmountPayable = "₹${grandTotalAmount.to2FString()}",
                amountInWords = "Indian Rupee ${convertNumberToWords(grandTotalAmount.toInt())} Only"
            ),
            paymentReceived = DraftInvoiceData.PaymentReceivedDetails(
                cashLabel1 = "CASH",
                cashAmount1 = "₹${grandTotalAmount.to2FString()}"
            ),
            declarationPoints = listOf(
                "We declare that this invoice shows the actual price of the goods described.",
                "All particulars are true and correct.",
                "This is a draft invoice for estimation purposes."
            ),
            termsAndConditions = "Terms and conditions apply as per company policy.",
            customerSignature = customerSign.value,
            ownerSignature = ownerSign.value,
            thankYouMessage = "Thank You! Please Visit Again"
        )
    }
    
    fun completeOrder(context: Context,onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        ioLaunch {
            try {
                if (selectedItemList.isNotEmpty()) {
                    // Create mock customer and store for draft invoice
                    val customer = CustomerEntity(
                        mobileNo = customerMobile.text,
                        name = customerName.text,
                        address = customerAddress.text,
                        gstin_pan = customerGstin.text,
                        addDate = Timestamp(System.currentTimeMillis()),
                        lastModifiedDate = Timestamp(System.currentTimeMillis())
                    )
                    
                    val store = StoreEntity(
                        userId = 1,
                        proprietor = "Raj Kumar",
                        name = "RAJ JEWELLERS",
                        eamil = "rajjewellers@gmail.com",
                        phone = "9437206994",
                        address = "Old Medical Road, Malkangiri, Odisha - 764048",
                        registrationNo = "RAJ-REG-001",
                        gstinNo = "21APEPK7976C1ZZ",
                        panNo = "APEPK7976C",
                        image = "",
                        invoiceNo = 0
                    )

                    // Create DraftInvoiceData object
                    val invoiceData = createDraftInvoiceData(store, customer, selectedItemList)
                    
                    generateDraftInvoicePdf(
                        context = context,
                        data = invoiceData,
                        scale = 2f
                    ) { file ->
                        generatedPdfFile.value = file
                        onSuccess()
                    }
                } else {
                    onFailure("Please add at least one item to generate the invoice")
                }
            } catch (e: Exception) {
                onFailure("Unable to Generate Draft Invoice PDF: ${e.message}")
            }
        }
    }
}