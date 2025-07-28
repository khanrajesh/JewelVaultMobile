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
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val outstandingAmount: Double = 0.0,
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
    onPaymentConfirmed: (PaymentInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var selectedPaymentType by remember { mutableStateOf("Paid in Full") }
    var paidAmountText by remember { mutableStateOf(totalAmount.to2FString()) }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Payment Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Total Amount Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Total Amount: ₹${String.format("%.2f", totalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedPaymentType == "Partial Payment") {
                            Text(
                                text = "Outstanding: ₹${String.format("%.2f", outstandingAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                // Payment Method Selection
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                ExposedDropdownMenuBox(
                    expanded = paymentMethodExpanded,
                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPaymentMethod,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Select Payment Method") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = paymentMethodExpanded,
                        onDismissRequest = { paymentMethodExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    selectedPaymentMethod = method
                                    paymentMethodExpanded = false
                                }
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Payment Type Options
                Text(
                    text = "Payment Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                ExposedDropdownMenuBox(
                    expanded = paymentTypeExpanded,
                    onExpandedChange = { paymentTypeExpanded = !paymentTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPaymentType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Select Payment Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentTypeExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = paymentTypeExpanded,
                        onDismissRequest = { paymentTypeExpanded = false }
                    ) {
                        paymentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedPaymentType = type
                                    paymentTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Paid Amount Input (only show if partial payment)
                if (selectedPaymentType == "Partial Payment") {
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it },
                        label = { Text("Amount Paid") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("₹") },
                        isError = paidAmount > totalAmount
                    )
                    
                    if (paidAmount > totalAmount) {
                        Text(
                            text = "Paid amount cannot exceed total amount",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // UPI QR Code Display
                if (selectedPaymentMethod == "UPI/Digital" && qrCodeBitmap != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Scan QR Code to Pay",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = qrCodeBitmap,
                                contentDescription = "UPI QR Code",
                                modifier = Modifier.size(200.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Amount: ₹${String.format("%.2f", if (selectedPaymentType == "Paid in Full") totalAmount else paidAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (selectedPaymentMethod == "UPI/Digital" && upiId.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "UPI ID not configured. Please contact administrator.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                HorizontalDivider()
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val finalPaidAmount = if (selectedPaymentType == "Paid in Full") totalAmount else paidAmount
                            val finalOutstandingAmount = totalAmount - finalPaidAmount
                            val isPaidInFullValue = selectedPaymentType == "Paid in Full"
                            
                            val paymentInfo = PaymentInfo(
                                paymentMethod = selectedPaymentMethod,
                                totalAmount = totalAmount,
                                paidAmount = finalPaidAmount,
                                outstandingAmount = finalOutstandingAmount,
                                isPaidInFull = isPaidInFullValue,
                                notes = notes.trim()
                            )
                            onPaymentConfirmed(paymentInfo)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = if (selectedPaymentType == "Paid in Full") true else (paidAmount > 0 && paidAmount <= totalAmount)
                    ) {
                        Text("Confirm Payment")
                    }
                }
            }
        }
    }
}