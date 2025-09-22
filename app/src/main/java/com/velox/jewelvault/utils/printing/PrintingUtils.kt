package com.velox.jewelvault.utils.printing

import android.content.Context
import com.velox.jewelvault.data.printing.*
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for easy printing operations
 * This provides convenient methods for common printing tasks
 */
@Singleton
class PrintingUtils @Inject constructor(
    private val context: Context,
    private val printerRepository: PrinterRepository
) {
   /*
    *//**
     * Print a simple text message
     *//*
    suspend fun printText(text: String): PrintResult {
        val payload = PrintPayload.TextPayload(text)
        return printerRepository.sendToPairedPrinter(payload)
    }
    
    *//**
     * Print an HTML document
     *//*
    suspend fun printHtml(html: String, width: Int = 384, height: Int = 600): PrintResult {
        val payload = PrintPayload.HtmlPayload(html, width, height)
        return printerRepository.sendToPairedPrinter(payload)
    }
    
    *//**
     * Print an item label with QR code
     *//*
    suspend fun printItemLabel(
        item: ItemEntity,
        labelFormat: LabelFormat = LabelFormat.THERMAL_100MM,
        includeQR: Boolean = true,
        includeLogo: Boolean = true
    ): PrintResult {
        val payload = PrintPayload.ItemLabelPayload(
            item = item,
            labelFormat = labelFormat,
            includeQR = includeQR,
            includeLogo = includeLogo
        )
        return printerRepository.sendToPairedPrinter(payload)
    }
    
    *//**
     * Print a receipt/invoice
     *//*
    suspend fun printReceipt(
        items: List<ItemEntity>,
        storeName: String,
        totalAmount: Double,
        customerName: String? = null
    ): PrintResult {
        val html = generateReceiptHtml(items, storeName, totalAmount, customerName)
        return printHtml(html)
    }
    
    *//**
     * Print a barcode
     *//*
    suspend fun printBarcode(
        barcodeData: String,
        barcodeType: BarcodeType = BarcodeType.CODE128
    ): PrintResult {
        val html = generateBarcodeHtml(barcodeData, barcodeType)
        return printHtml(html)
    }
    
    *//**
     * Print with error handling and logging
     *//*
    suspend fun printWithErrorHandling(
        payload: PrintPayload,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): Boolean {
        return try {
            val result = printerRepository.sendToPairedPrinter(payload)
            
            when (result) {
                is PrintResult.Success -> {
                    log("Print successful")
                    onSuccess?.invoke()
                    true
                }
                is PrintResult.NoPairedPrinter -> {
                    val error = "No paired printer found"
                    log(error)
                    onError?.invoke(error)
                    false
                }
                is PrintResult.PrinterNotConnected -> {
                    val error = "Printer not connected"
                    log(error)
                    onError?.invoke(error)
                    false
                }
                is PrintResult.BluetoothDisabled -> {
                    val error = "Bluetooth is disabled"
                    log(error)
                    onError?.invoke(error)
                    false
                }
                is PrintResult.Error -> {
                    val error = "Print error: ${result.message}"
                    log(error)
                    onError?.invoke(error)
                    false
                }
                else -> {
                    val error = "Unknown print error: ${result::class.simpleName}"
                    log(error)
                    onError?.invoke(error)
                    false
                }
            }
        } catch (e: Exception) {
            val error = "Print exception: ${e.message}"
            log(error)
            onError?.invoke(error)
            false
        }
    }
    
    *//**
     * Print asynchronously with callback
     *//*
    fun printAsync(
        payload: PrintPayload,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            printWithErrorHandling(payload, onSuccess, onError)
        }
    }
    
    *//**
     * Check if printing is available
     *//*
    suspend fun isPrintingAvailable(): Boolean {
        val printers = printerRepository.getAvailablePrinters()
        return printers.isNotEmpty()
    }
    
    *//**
     * Get available printers
     *//*
    suspend fun getAvailablePrinters(): List<BluetoothDeviceInfo> {
        return printerRepository.getAvailablePrinters()
    }
    
    private fun generateReceiptHtml(
        items: List<ItemEntity>,
        storeName: String,
        totalAmount: Double,
        customerName: String?
    ): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Receipt</title>
            <style>
                body { font-family: monospace; margin: 0; padding: 10px; }
                .header { text-align: center; font-weight: bold; margin-bottom: 20px; }
                .item { margin-bottom: 5px; }
                .total { font-weight: bold; border-top: 1px solid #000; padding-top: 5px; margin-top: 10px; }
                .footer { text-align: center; margin-top: 20px; font-size: 12px; }
            </style>
        </head>
        <body>
            <div class="header">
                <h2>$storeName</h2>
                <p>Receipt</p>
            </div>
            
            ${if (customerName != null) "<p><strong>Customer:</strong> $customerName</p>" else ""}
            
            <div class="items">
                ${items.joinToString("") { item ->
                    """
                    <div class="item">
                        <div>${item.itemAddName} - ${item.purity}</div>
                        <div>Qty: ${item.quantity} | Weight: ${item.gsWt}gm | Price: ₹${item.crg}</div>
                    </div>
                    """.trimIndent()
                }}
            </div>
            
            <div class="total">
                <div>Total Amount: ₹$totalAmount</div>
            </div>
            
            <div class="footer">
                <p>Thank you for your business!</p>
                <p>${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}</p>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
    
    private fun generateBarcodeHtml(
        barcodeData: String,
        barcodeType: BarcodeType
    ): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Barcode</title>
            <style>
                body { font-family: monospace; margin: 0; padding: 10px; text-align: center; }
                .barcode { font-size: 24px; letter-spacing: 2px; }
                .data { margin-top: 10px; font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="barcode">$barcodeData</div>
            <div class="data">$barcodeData</div>
        </body>
        </html>
        """.trimIndent()
    }*/
}

/**
 * Barcode types for printing
 */
enum class BarcodeType {
    CODE128,
    CODE39,
    EAN13,
    EAN8,
    UPC_A,
    UPC_E
}
