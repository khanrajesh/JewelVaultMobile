package com.velox.jewelvault.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.graphics.Color
import com.velox.jewelvault.utils.to2FString

// Function to generate UPI QR code
fun generateUpiQrCode(upiId: String, amount: Double, name: String = "Merchant"): ImageBitmap? {
    return try {
        val upiUrl = "upi://pay?pa=$upiId&pn=$name&am=$amount&cu=INR"
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(upiUrl, BarcodeFormat.QR_CODE, 300, 300)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

data class PaymentInfo(
    val paymentMethod: String = "Cash",
    val totalAmount: Double?=null,
    val paidAmount: Double?=null,
    val outstandingAmount: Double?=null,
    val isPaidInFull: Boolean = true,
    val notes: String = ""
) {
    fun toNoteString(): String {
        return "Payment: $paymentMethod | Total: ₹$totalAmount | Paid: ₹$paidAmount | Outstanding: ₹$outstandingAmount | Full Payment: $isPaidInFull${if (notes.isNotEmpty()) " | Notes: $notes" else ""}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    totalAmount: Double,
    upiId: String = "", // UPI ID for QR generation
    merchantName: String = "Merchant",
    onPaymentConfirmed: (PaymentInfo, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var selectedPaymentType by remember { mutableStateOf("Paid in Full") }
    var paidAmountText by remember { mutableStateOf(totalAmount.to2FString()) }
    var discount by remember { mutableStateOf("0.0") }
    var notes by remember { mutableStateOf("") }
    var paymentMethodExpanded by remember { mutableStateOf(false) }
    var paymentTypeExpanded by remember { mutableStateOf(false) }
    
    val paymentMethods = listOf("Cash", "Check", "Card", "UPI/Digital")
    val paymentTypes = listOf("Paid in Full", "Partial Payment")
    
    val isPaidInFull = selectedPaymentType == "Paid in Full"
    val paidAmount = paidAmountText.toDoubleOrNull() ?: 0.0
    val outstandingAmount = (totalAmount - paidAmount).coerceAtLeast(0.0)
    
    // Generate QR code for UPI payment - regenerate when payment method, amount, or payment type changes
    val qrCodeBitmap = remember(selectedPaymentMethod, paidAmount, selectedPaymentType) {
        if (selectedPaymentMethod == "UPI/Digital" && upiId.isNotEmpty() && paidAmount > 0) {
            val amountForQr = if (isPaidInFull) totalAmount else paidAmount
            generateUpiQrCode(upiId, amountForQr, merchantName)
        } else null
    }
    
    // Update paid amount when payment type changes
    LaunchedEffect(selectedPaymentType) {
        if (selectedPaymentType == "Paid in Full") {
            paidAmountText = totalAmount.to2FString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {

        }
    }
}