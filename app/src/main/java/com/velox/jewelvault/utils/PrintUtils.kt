package com.velox.jewelvault.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.poi.hssf.usermodel.HeaderFooter.fontSize
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for printing item details
 */
object PrintUtils {

    /**
     * Function to generate Excel for individual item and print it
     */
    fun generateItemExcelAndPrint(
        context: Context, item: ItemEntity, onComplete: () -> Unit
    ) {
        ioScope {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Item Details")

                // Create header style
                val headerStyle = workbook.createCellStyle().apply {
                    val font = workbook.createFont().apply {
                        bold = true
                        fontSize(12)
                    }
                    setFont(font)
                }

                // Item Information as key-value pairs
                val itemData = listOf(
                    "Item ID" to item.itemId,
                    "Name" to item.itemAddName,
                    "Category" to "${item.catName} (${item.catId})",
                    "Sub Category" to "${item.subCatName} (${item.subCatId})",
                    "Entry Type" to item.entryType,
                    "Quantity" to item.quantity.toString(),
                    "Gross Weight" to "${item.gsWt.to3FString()} gm",
                    "Net Weight" to "${item.ntWt.to3FString()} gm",
                    "Fine Weight" to "${item.fnWt.to3FString()} gm",
                    "Purity" to item.purity,
                    "Charge Type" to item.crgType,
                    "Charge" to "₹${item.crg.to3FString()}",
                    "Other Charge Description" to item.othCrgDes,
                    "Other Charge" to "₹${item.othCrg.to3FString()}",
                    "CGST" to "₹${item.cgst.to3FString()}",
                    "SGST" to "₹${item.sgst.to3FString()}",
                    "IGST" to "₹${item.igst.to3FString()}",
                    "Total Tax" to "₹${(item.cgst + item.sgst + item.igst).to3FString()}",
                    "HUID" to item.huid,
                    "Description Key" to item.addDesKey,
                    "Description Value" to item.addDesValue,
                    "Purchase Order ID" to item.purchaseOrderId,
                    "Added Date" to SimpleDateFormat(
                        "dd-MMM-yyyy HH:mm", Locale.getDefault()
                    ).format(Date(item.addDate.time))
                )

                // Create header row with all property names
                val headerRow = sheet.createRow(0)
                itemData.forEachIndexed { index, (key, _) ->
                    val headerCell = headerRow.createCell(index)
                    headerCell.setCellValue(key)
                    headerCell.cellStyle = headerStyle
                }

                // Create data row with all values
                val dataRow = sheet.createRow(1)
                itemData.forEachIndexed { index, (_, value) ->
                    val dataCell = dataRow.createCell(index)
                    dataCell.setCellValue(value)
                }

                // Set column widths manually (avoid autoSizeColumn which uses AWT classes not available on Android)
                itemData.forEachIndexed { index, _ ->
                    sheet.setColumnWidth(index, 20 * 256) // 20 characters wide for each column
                }

                // Create temporary file
                val fileName = "Item_${item.itemId}_${System.currentTimeMillis()}.xlsx"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/JewelVault/TempPrint"
                    )
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    var success = false
                    try {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            workbook.write(outputStream)
                            outputStream.flush()
                            success = true
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        workbook.close()
                    }

                    if (success) {
                        // Launch print intent
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                uri,
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Print Item Details"))

                        // Schedule file deletion after a delay (5 seconds)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                            .launch {
                                delay(5000) // Wait 5 seconds
                                try {
                                    resolver.delete(uri, null, null)
                                } catch (e: Exception) {
                                    // File might already be deleted or not accessible
                                }
                            }

                        onComplete()
                    } else {
                        resolver.delete(uri, null, null)
                    }
                } else {
                    workbook.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Function to print item details directly using Android's native printing
     */
    fun printItemDirectly(
        context: Context, item: ItemEntity, onComplete: () -> Unit
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        // Item Information as key-value pairs (same as Excel format)
        val itemData = listOf(
            "Item ID" to item.itemId,
            "Name" to item.itemAddName,
            "Category" to "${item.catName} (${item.catId})",
            "Sub Category" to "${item.subCatName} (${item.subCatId})",
            "Entry Type" to item.entryType,
            "Quantity" to item.quantity.toString(),
            "Gross Weight" to "${item.gsWt.to3FString()} gm",
            "Net Weight" to "${item.ntWt.to3FString()} gm",
            "Fine Weight" to "${item.fnWt.to3FString()} gm",
            "Purity" to item.purity,
            "Charge Type" to item.crgType,
            "Charge" to "₹${item.crg.to3FString()}",
            "Other Charge Description" to item.othCrgDes,
            "Other Charge" to "₹${item.othCrg.to3FString()}",
            "CGST" to "₹${item.cgst.to3FString()}",
            "SGST" to "₹${item.sgst.to3FString()}",
            "IGST" to "₹${item.igst.to3FString()}",
            "Total Tax" to "₹${(item.cgst + item.sgst + item.igst).to3FString()}",
            "HUID" to item.huid,
            "Description Key" to item.addDesKey,
            "Description Value" to item.addDesValue,
            "Purchase Order ID" to item.purchaseOrderId,
            "Added Date" to SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(
                Date(item.addDate.time)
            )
        )

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Item Details - ${item.itemId}</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    .header { text-align: center; font-size: 24px; font-weight: bold; margin-bottom: 30px; border-bottom: 2px solid #333; padding-bottom: 10px; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background-color: #f2f2f2; font-weight: bold; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #888; }
                    @media print {
                        body { margin: 0; }
                        table { page-break-inside: avoid; }
                    }
                </style>
            </head>
            <body>
                <div class="header">Item Details Report</div>
                
                <table>
                    <tr>
                        ${itemData.joinToString("") { (key, _) -> "<th>$key</th>" }}
                    </tr>
                    <tr>
                        ${itemData.joinToString("") { (_, value) -> "<td>$value</td>" }}
                    </tr>
                </table>
                
                <div class="footer">
                    Generated by JewelVault Mobile App
                </div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val printAdapter =
                    webView.createPrintDocumentAdapter("Item_${item.itemId}_${System.currentTimeMillis()}")
                val printAttributes =
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("print", "print", 600, 600))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()

                printManager.print("Item Details Print Job", printAdapter, printAttributes)
                onComplete()
            }
        }

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    /**
     * Generate QR code bitmap from itemId
     */
    private fun generateQRCode(itemId: String, size: Int = 100): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(itemId, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convert bitmap to base64 string for HTML embedding
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Print thermal label for jewelry item
     * Label: 100mm x 13mm (total), printable area: 65mm (centered), 35mm tail (non-printable)
     */
    fun printThermalLabel(
        context: Context, item: ItemEntity, storeLogoBase64: String? = null, onComplete: () -> Unit
    ) {
        // Load hallmark from drawable
        val hallmarkBase64 = try {
            val hallmarkDrawable =
                context.resources.getIdentifier("hallmark", "drawable", context.packageName)
            if (hallmarkDrawable != 0) {
                val bitmap = android.graphics.BitmapFactory.decodeResource(
                    context.resources,
                    hallmarkDrawable
                )
                bitmap?.let { bitmapToBase64(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            log("PrintUtils: Error loading hallmark drawable: ${e.message}")
            null
        }

        // Load store logo from local file or use provided store logo
        val storeLogoBase64 = try {
            val localLogoUri = FileManager.getLogoFileUri(context)
            if (localLogoUri != null) {
                val inputStream = context.contentResolver.openInputStream(localLogoUri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let { bitmapToBase64(it) }
            } else {
                storeLogoBase64
            }
        } catch (e: Exception) {
            log("PrintUtils: Error loading local logo: ${e.message}")
            storeLogoBase64
        }
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        // Generate QR code for itemId (80px)
        val qrCodeBitmap = generateQRCode(item.itemId, 80)
        val qrCodeBase64 = qrCodeBitmap?.let { bitmapToBase64(it) } ?: ""

        // Format weights and purity
        val grossWeight = "${item.gsWt.to3FString()} gm"
        val fineWeight = "${item.fnWt.to3FString()} gm"
        val purity = item.purity ?: ""

        // HTML: page 100mm x 13mm, centered printable area 65mm
        val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Thermal Label - ${item.itemId}</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                @page {
                    size: 100mm 13mm;
                    margin: 0;
                }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100mm;
                    height: 13mm;
                    background: white;
                    font-family: Arial, sans-serif;
                    -webkit-text-size-adjust: none;
                }
                /* center the printable area inside the full label width */
                .container {
                    width: 100mm;
                    height: 13mm;
                    display: flex;
                    justify-content: center; /* centers 65mm area horizontally */
                    align-items: center;
                }
                /* Printable area (actual content) */
                .printable-area {
                    width: 65mm;
                    height: 13mm;
                    display: flex;
                    box-sizing: border-box;
                    /* no border for actual print, but visible during preview */
                }
                /* left and right split (each 32.5mm) */
                .left-section, .right-section {
                    height: 13mm;
                    box-sizing: border-box;
                }
                .left-section {
                    width: 32.5mm;
                    height: 13mm; /* Keep full height */
                    display: flex;
                    flex-direction: row;
                    align-items: center;
                    justify-content: space-between;
                    padding: 0.5mm; /* 0.5mm all around */
                    flex-wrap: nowrap; /* Prevent wrapping to avoid page breaks */
                    box-sizing: border-box;
                }
                .right-section {
                    width: 32.5mm;
                    height: 13mm; /* Keep full height */
                    display: flex;
                    flex-direction: row;
                    align-items: center;
                    justify-content: space-between;
                    padding: 0.5mm; /* 0.5mm all around */
                    box-sizing: border-box;
                }
                .store-logo {
                    max-width: 13mm;
                    max-height: 8mm;
                    object-fit: contain;
                    display: block;
                }
                   .hallmark-logo {
                    max-width: 10mm;
                    max-height: 6mm;
                    object-fit: contain;
                    display: block;
                }
                
                .qr-code {
                    width: 12mm;
                    height: 12mm;
                    object-fit: contain;
                    display: block;
                }
                .item-id {
                    font-size: 1.8mm; 
                    margin: 0 0.5mm;
                    text-align: center;
                    line-height: 1;
                    white-space: normal;
                    word-wrap: break-word;
                    max-width: 10mm;
                }
                .weight-info {
                    font-size: 2.5mm;
                    font-weight: bold;
                    margin: 0 0.5mm;
                    line-height: 1;
                    text-align: center;
                }
                .purity-info {
                    font-size: 3.0mm;
                    font-weight: bold;
                    margin: 0 0.5mm;
                    margin-top: 0.4mm;
                    text-align: center;
                }
                
                .store-name {
                    font-size: 2.2mm;
                    margin: 0;
                    text-align: left;
                    font-weight: 600;
                }
               
                @media print {
                    body { margin: 0; }
                    .printable-area {
                        page-break-inside: avoid;
                        break-inside: avoid;
                    }
                    .left-section, .right-section {
                        page-break-inside: avoid;
                        break-inside: avoid;
                    }
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="printable-area" aria-label="Printable area 65mm">
                     <div class="left-section">
                              ${
            if (!storeLogoBase64.isNullOrEmpty()) {
                """<img src="data:image/png;base64,$storeLogoBase64" class="store-logo" alt="Store Logo">"""
            } else {
                """<div style="width:13mm;height:8mm;background:#f0f0f0;display:flex;align-items:center;justify-content:center;font-size:2.2mm;color:#666;">STORE</div>"""
            }
        }
                    
                         ${
            if (qrCodeBase64.isNotEmpty()) {
                """<img src="data:image/png;base64,$qrCodeBase64" class="qr-code" alt="QR Code">"""
            } else {
                """<div style="width:12mm;height:12mm;background:#f0f0f0;display:flex;align-items:center;justify-content:center;font-size:2.2mm;color:#666;">QR</div>"""
            }
        }
         
              <p class="item-id">${item.itemId}</p>
                     </div>

                    <div class="right-section">
                        <div style="display:flex;flex-direction:column;justify-content:center;align-items:center;">
                            ${
            if (!hallmarkBase64.isNullOrEmpty()) {
                """<img src="data:image/png;base64,$hallmarkBase64" class="hallmark-logo" alt="Hallmark Logo">"""
            } else {
                """<div style="width:10mm;height:6mm;background:#f0f0f0;display:flex;align-items:center;justify-content:center;font-size:2.2mm;color:#666;">HALLMARK</div>"""
            }
        }
                            <p class="purity-info">$purity</p>
                        </div>
                        <div style="display:flex;flex-direction:column;justify-content:center;align-items:center;">
                            <p class="weight-info">GS: $grossWeight</p>
                            <p class="weight-info">FN: $fineWeight</p>
                        </div>
                    </div>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()

        // Prepare WebView and printing
        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = false
            builtInZoomControls = false
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Android PrintAttributes expects width/height in mils (1/1000 inch).
                // Convert mm -> mils: mils = mm / 25.4 * 1000
                // 100 mm  -> 100 / 25.4 * 1000 ≈ 3937 mils
                // 13 mm   -> 13 / 25.4 * 1000 ≈ 512 mils
                val mediaSize =
                    PrintAttributes.MediaSize("THERMAL_LABEL", "Thermal Label", 3937, 512)

                val printAdapter =
                    webView.createPrintDocumentAdapter("Thermal_Label_${item.itemId}_${System.currentTimeMillis()}")
                val printAttributes = PrintAttributes.Builder().setMediaSize(mediaSize)
                    .setResolution(PrintAttributes.Resolution("thermal", "thermal", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()

                try {
                    printManager.print("Thermal Label Print Job", printAdapter, printAttributes)
                } catch (e: Exception) {
                    // handle printer errors if needed (log or show toast)
                    e.printStackTrace()
                } finally {
                    onComplete()
                }
            }
        }

        // Load HTML. Using loadDataWithBaseURL so data: URIs (base64) are allowed.
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    /**
     * Print HTML content using Bluetooth printer
     * This function checks if printer is connected and prints the HTML content
     * @param context Application context
     * @param htmlContent HTML content to print
     * @param onComplete Callback when print operation completes
     * @param onError Callback when print operation fails
     */
    fun printHtmlToBluetoothPrinter(
        context: Context,
        htmlContent: String,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if printer is connected using PrinterUtils
                if (!PrinterUtils.isConnected()) {
                    onError("Printer is not connected. Please connect to a Bluetooth printer first.")
                    return@launch
                }
                
                // Print HTML content
                PrinterUtils.printHtmlAsync(
                    htmlContent = htmlContent,
                    onSuccess = {
                        onComplete()
                    },
                    onError = { error ->
                        onError(error)
                    }
                )
            } catch (e: Exception) {
                onError("Error printing to Bluetooth printer: ${e.message}")
            }
        }
    }

    /**
     * Print item details using Bluetooth printer
     * This function replaces the direct print functionality
     * @param context Application context
     * @param item Item entity to print
     * @param onComplete Callback when print operation completes
     * @param onError Callback when print operation fails
     */
    fun printItemToBluetoothPrinter(
        context: Context,
        item: ItemEntity,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if printer is connected
                if (!PrinterUtils.isConnected()) {
                    onError("Printer is not connected. Please connect to a Bluetooth printer first.")
                    return@launch
                }
                
                // Generate HTML content for the item
                val htmlContent = generateItemHtml(item)
                
                // Print HTML content
                PrinterUtils.printHtmlAsync(
                    htmlContent = htmlContent,
                    onSuccess = {
                        onComplete()
                    },
                    onError = { error ->
                        onError(error)
                    }
                )
            } catch (e: Exception) {
                onError("Error printing item to Bluetooth printer: ${e.message}")
            }
        }
    }

    /**
     * Generate HTML content for item details
     * @param item Item entity
     * @return HTML content string
     */
    private fun generateItemHtml(item: ItemEntity): String {
        val itemData = listOf(
            "Item ID" to item.itemId,
            "Name" to item.itemAddName,
            "Category" to "${item.catName} (${item.catId})",
            "Sub Category" to "${item.subCatName} (${item.subCatId})",
            "Entry Type" to item.entryType,
            "Quantity" to item.quantity.toString(),
            "Gross Weight" to "${item.gsWt.to3FString()} gm",
            "Net Weight" to "${item.ntWt.to3FString()} gm",
            "Fine Weight" to "${item.fnWt.to3FString()} gm",
            "Purity" to item.purity,
            "Charge Type" to item.crgType,
            "Charge" to "₹${item.crg.to3FString()}",
            "Other Charge Description" to item.othCrgDes,
            "Other Charge" to "₹${item.othCrg.to3FString()}",
            "CGST" to "₹${item.cgst.to3FString()}",
            "SGST" to "₹${item.sgst.to3FString()}",
            "IGST" to "₹${item.igst.to3FString()}",
            "Total Tax" to "₹${(item.cgst + item.sgst + item.igst).to3FString()}",
            "HUID" to item.huid,
            "Description Key" to item.addDesKey,
            "Description Value" to item.addDesValue,
            "Purchase Order ID" to item.purchaseOrderId,
            "Added Date" to SimpleDateFormat(
                "dd-MMM-yyyy HH:mm", Locale.getDefault()
            ).format(Date(item.addDate.time))
        )

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Item Details - ${item.itemId}</title>
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 10px; 
                        font-size: 12px;
                    }
                    .header { 
                        text-align: center; 
                        font-size: 16px; 
                        font-weight: bold; 
                        margin-bottom: 15px; 
                        border-bottom: 1px solid #333; 
                        padding-bottom: 5px; 
                    }
                    .item-row { 
                        display: flex; 
                        justify-content: space-between; 
                        margin-bottom: 3px; 
                        padding: 2px 0;
                    }
                    .item-label { 
                        font-weight: bold; 
                        min-width: 120px;
                    }
                    .item-value { 
                        text-align: right; 
                        flex: 1;
                    }
                    .footer { 
                        text-align: center; 
                        margin-top: 15px; 
                        font-size: 10px; 
                        color: #888; 
                        border-top: 1px solid #ccc;
                        padding-top: 5px;
                    }
                </style>
            </head>
            <body>
                <div class="header">Item Details Report</div>
                
                ${itemData.joinToString("") { (key, value) ->
                    """
                    <div class="item-row">
                        <span class="item-label">$key:</span>
                        <span class="item-value">$value</span>
                    </div>
                    """.trimIndent()
                }}
                
                <div class="footer">
                    Generated by JewelVault Mobile App
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Print thermal label using Bluetooth printer
     * @param context Application context
     * @param item Item entity
     * @param storeLogoBase64 Store logo in base64 format
     * @param onComplete Callback when print operation completes
     * @param onError Callback when print operation fails
     */
    fun printThermalLabelToBluetoothPrinter(
        context: Context,
        item: ItemEntity,
        storeLogoBase64: String? = null,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if printer is connected
                if (!PrinterUtils.isConnected()) {
                    onError("Printer is not connected. Please connect to a Bluetooth printer first.")
                    return@launch
                }
                
                // Generate thermal label HTML
                val htmlContent = generateThermalLabelHtml(context, item, storeLogoBase64)
                
                // Print HTML content
                PrinterUtils.printHtmlAsync(
                    htmlContent = htmlContent,
                    onSuccess = {
                        onComplete()
                    },
                    onError = { error ->
                        onError(error)
                    }
                )
            } catch (e: Exception) {
                onError("Error printing thermal label to Bluetooth printer: ${e.message}")
            }
        }
    }

    /**
     * Generate thermal label HTML content
     * @param item Item entity
     * @param storeLogoBase64 Store logo in base64 format
     * @return HTML content string
     */
    private fun generateThermalLabelHtml(context: Context, item: ItemEntity, storeLogoBase64: String?): String {
        // Load hallmark from drawable
        val hallmarkBase64 = try {
            val hallmarkDrawable = context.resources.getIdentifier("hallmark", "drawable", context.packageName)
            if (hallmarkDrawable != 0) {
                val bitmap = android.graphics.BitmapFactory.decodeResource(
                    context.resources,
                    hallmarkDrawable
                )
                bitmap?.let { bitmapToBase64(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            log("PrintUtils: Error loading hallmark drawable: ${e.message}")
            null
        }

        // Generate QR code for itemId (80px)
        val qrCodeBitmap = generateQRCode(item.itemId, 80)
        val qrCodeBase64 = qrCodeBitmap?.let { bitmapToBase64(it) } ?: ""

        // Format weights and purity
        val grossWeight = "${item.gsWt.to3FString()} gm"
        val fineWeight = "${item.fnWt.to3FString()} gm"
        val purity = item.purity ?: ""

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Thermal Label - ${item.itemId}</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                @page {
                    size: 100mm 13mm;
                    margin: 0;
                }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100mm;
                    height: 13mm;
                    background: white;
                    font-family: Arial, sans-serif;
                    -webkit-text-size-adjust: none;
                }
                .container {
                    width: 100mm;
                    height: 13mm;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                .printable-area {
                    width: 65mm;
                    height: 13mm;
                    display: flex;
                    box-sizing: border-box;
                }
                .left-section, .right-section {
                    height: 13mm;
                    box-sizing: border-box;
                }
                .left-section {
                    width: 32.5mm;
                    height: 13mm;
                    display: flex;
                    flex-direction: row;
                    align-items: center;
                    justify-content: space-between;
                    padding: 0.5mm;
                    flex-wrap: nowrap;
                    box-sizing: border-box;
                }
                .right-section {
                    width: 32.5mm;
                    height: 13mm;
                    display: flex;
                    flex-direction: row;
                    align-items: center;
                    justify-content: space-between;
                    padding: 0.5mm;
                    box-sizing: border-box;
                }
                .store-logo {
                    max-width: 13mm;
                    max-height: 8mm;
                    object-fit: contain;
                    display: block;
                }
                .hallmark-logo {
                    max-width: 10mm;
                    max-height: 6mm;
                    object-fit: contain;
                    display: block;
                }
                .qr-code {
                    width: 12mm;
                    height: 12mm;
                    object-fit: contain;
                    display: block;
                }
                .item-id {
                    font-size: 1.8mm; 
                    margin: 0 0.5mm;
                    text-align: center;
                    line-height: 1;
                    white-space: normal;
                    word-wrap: break-word;
                    max-width: 10mm;
                }
                .weight-info {
                    font-size: 2.5mm;
                    font-weight: bold;
                    margin: 0 0.5mm;
                    line-height: 1;
                    text-align: center;
                }
                .purity-info {
                    font-size: 3.0mm;
                    font-weight: bold;
                    margin: 0 0.5mm;
                    margin-top: 0.4mm;
                    text-align: center;
                }
                .store-name {
                    font-size: 2.2mm;
                    margin: 0;
                    text-align: left;
                    font-weight: 600;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="printable-area" aria-label="Printable area 65mm">
                     <div class="left-section">
                              ${
            if (!storeLogoBase64.isNullOrEmpty()) {
                """<img src="data:image/png;base64,$storeLogoBase64" class="store-logo" alt="Store Logo">"""
            } else {
                """<div style="width:13mm;height:8mm;background:#f0f0f0;display:flex;align-items:center;justify-content:center;font-size:2.2mm;color:#666;">STORE</div>"""
            }
        }
                    
                         ${
            if (qrCodeBase64.isNotEmpty()) {
                """<img src="data:image/png;base64,$qrCodeBase64" class="qr-code" alt="QR Code">"""
            } else {
                """<div style="width:12mm;height:12mm;background:#f0f0f0;display:flex;align-items:center;justify-content:center;font-size:2.2mm;color:#666;">QR</div>"""
            }
        }
         
              <p class="item-id">${item.itemId}</p>
                     </div>

                    <div class="right-section">
                        <div style="display:flex;flex-direction:column;justify-content:center;align-items:center;">
                            ${
            if (!hallmarkBase64.isNullOrEmpty()) {
                """<img src="data:image/png;base64,$hallmarkBase64" class="hallmark-logo" alt="Hallmark Logo">"""
            } else {
                """<div style="width:10mm;height:6mm;background:#f0f0f0;display:flex;align-items:center;justify-content:center;font-size:2.2mm;color:#666;">HALLMARK</div>"""
            }
        }
                            <p class="purity-info">$purity</p>
                        </div>
                        <div style="display:flex;flex-direction:column;justify-content:center;align-items:center;">
                            <p class="weight-info">GS: $grossWeight</p>
                            <p class="weight-info">FN: $fineWeight</p>
                        </div>
                    </div>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
    }


}
