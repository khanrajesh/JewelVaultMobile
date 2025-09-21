package com.velox.jewelvault.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.webkit.WebView
import android.webkit.WebViewClient
import com.velox.jewelvault.data.bluetooth.LabelSize
import com.velox.jewelvault.data.bluetooth.PrinterLanguage
import com.velox.jewelvault.data.bluetooth.PrinterSettings
import com.velox.jewelvault.data.bluetooth.EscPosCommands
import com.velox.jewelvault.data.bluetooth.ZplCommands
import com.velox.jewelvault.data.bluetooth.TsplCommands
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utility functions for Bluetooth printing operations
 */

/**
 * Render HTML content to bitmap for printing
 */
suspend fun renderHtmlToBitmap(
    html: String,
    labelSize: LabelSize,
    context: Context
): Bitmap = suspendCancellableCoroutine { continuation ->
    try {
        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
            setSupportZoom(false)
        }

        // Set WebView size to match label dimensions
        webView.layoutParams = android.view.ViewGroup.LayoutParams(
            labelSize.widthPx,
            labelSize.heightPx
        )

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                try {
                    // Wait a bit for content to render
                    webView.postDelayed({
                        try {
                            val bitmap = captureWebView(webView, labelSize)
                            continuation.resume(bitmap)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }, 500) // 500ms delay for rendering
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                continuation.resumeWithException(Exception("WebView error: $description"))
            }
        }

        // Load HTML content
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        
        // Cleanup on cancellation
        continuation.invokeOnCancellation {
            webView.destroy()
        }
    } catch (e: Exception) {
        continuation.resumeWithException(e)
    }
}

/**
 * Capture WebView as bitmap
 */
private fun captureWebView(webView: WebView, labelSize: LabelSize): Bitmap {
    val bitmap = Bitmap.createBitmap(
        labelSize.widthPx,
        labelSize.heightPx,
        Bitmap.Config.ARGB_8888
    )
    
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    
    webView.draw(canvas)
    
    return bitmap
}

/**
 * Convert bitmap to ESC/POS raster format
 */
fun bitmapToEscPosRaster(bitmap: Bitmap, settings: PrinterSettings): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    
    // Convert to monochrome
    val monochromeBitmap = convertToMonochrome(bitmap)
    
    // Calculate bytes per line (8 pixels per byte)
    val bytesPerLine = (width + 7) / 8
    
    val result = StringBuilder()
    
    // ESC/POS initialization
    result.append(EscPosCommands.INIT)
    
    // Set print density
    result.append("${EscPosCommands.GS}8${settings.density.toChar()}")
    
    // Print each line
    for (y in 0 until height) {
        val lineData = extractLineData(monochromeBitmap, y, width, bytesPerLine)
        
        // ESC/POS raster command: GS v 0
        result.append("${EscPosCommands.GS}v0${(bytesPerLine and 0xFF).toChar()}${((bytesPerLine shr 8) and 0xFF).toChar()}")
        result.append(lineData)
    }
    
    // Feed and cut
    if (settings.autoFeed) {
        result.append(EscPosCommands.FEED)
    }
    if (settings.autoCut) {
        result.append(EscPosCommands.CUT)
    }
    
    return result.toString().toByteArray(Charsets.UTF_8)
}

/**
 * Convert bitmap to ZPL image format
 */
fun bitmapToZplImage(bitmap: Bitmap, settings: PrinterSettings): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    
    // Convert to monochrome
    val monochromeBitmap = convertToMonochrome(bitmap)
    
    // Convert to ZPL graphic format
    val zplData = convertToZplGraphic(monochromeBitmap, width, height)
    
    val result = StringBuilder()
    result.append(ZplCommands.START_FORMAT)
    result.append("${ZplCommands.FIELD_ORIGIN}0,0")
    result.append("${ZplCommands.GRAPHIC_FIELD}${zplData}")
    result.append(ZplCommands.FIELD_SEPARATOR)
    result.append(ZplCommands.END_FORMAT)
    
    return result.toString().toByteArray(Charsets.UTF_8)
}

/**
 * Convert bitmap to TSPL image format
 */
fun bitmapToTsplImage(bitmap: Bitmap, settings: PrinterSettings): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    
    // Convert to monochrome
    val monochromeBitmap = convertToMonochrome(bitmap)
    
    // Convert to TSPL graphic format
    val tsplData = convertToTsplGraphic(monochromeBitmap, width, height)
    
    val result = StringBuilder()
    result.append("${TsplCommands.SIZE}${settings.labelSize.widthMm} mm, ${settings.labelSize.heightMm} mm")
    result.append("\n")
    result.append("${TsplCommands.GAP}3 mm, 0 mm")
    result.append("\n")
    result.append("${TsplCommands.CLS}")
    result.append("\n")
    result.append(tsplData)
    result.append("\n")
    result.append("${TsplCommands.PRINT}1,1")
    
    return result.toString().toByteArray(Charsets.UTF_8)
}

/**
 * Convert bitmap to monochrome (black and white only)
 */
private fun convertToMonochrome(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val monochrome = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        
        // Convert to grayscale and then to black/white
        val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
        val bw = if (gray > 128) Color.WHITE else Color.BLACK
        pixels[i] = bw
    }
    
    monochrome.setPixels(pixels, 0, width, 0, 0, width, height)
    return monochrome
}

/**
 * Extract line data for ESC/POS raster format
 */
private fun extractLineData(bitmap: Bitmap, y: Int, width: Int, bytesPerLine: Int): String {
    val lineData = ByteArray(bytesPerLine)
    
    for (x in 0 until width) {
        val pixel = bitmap.getPixel(x, y)
        val isBlack = Color.red(pixel) < 128 // Black pixel
        
        if (isBlack) {
            val byteIndex = x / 8
            val bitIndex = 7 - (x % 8)
            lineData[byteIndex] = (lineData[byteIndex].toInt() or (1 shl bitIndex)).toByte()
        }
    }
    
    return String(lineData, Charsets.ISO_8859_1)
}

/**
 * Convert bitmap to ZPL graphic format
 */
private fun convertToZplGraphic(bitmap: Bitmap, width: Int, height: Int): String {
    val bytesPerRow = (width + 7) / 8
    val totalBytes = bytesPerRow * height
    
    val result = StringBuilder()
    result.append("${bytesPerRow},${height},")
    
    for (y in 0 until height) {
        for (x in 0 until width step 8) {
            var byte = 0
            for (bit in 0..7) {
                if (x + bit < width) {
                    val pixel = bitmap.getPixel(x + bit, y)
                    val isBlack = Color.red(pixel) < 128
                    if (isBlack) {
                        byte = byte or (1 shl (7 - bit))
                    }
                }
            }
            result.append(String.format("%02X", byte))
        }
    }
    
    return result.toString()
}

/**
 * Convert bitmap to TSPL graphic format
 */
private fun convertToTsplGraphic(bitmap: Bitmap, width: Int, height: Int): String {
    val result = StringBuilder()
    
    // TSPL uses a different format - convert to base64 or hex
    val bytesPerRow = (width + 7) / 8
    val totalBytes = bytesPerRow * height
    
    result.append("BITMAP 0,0,$width,$height,0,")
    
    for (y in 0 until height) {
        for (x in 0 until width step 8) {
            var byte = 0
            for (bit in 0..7) {
                if (x + bit < width) {
                    val pixel = bitmap.getPixel(x + bit, y)
                    val isBlack = Color.red(pixel) < 128
                    if (isBlack) {
                        byte = byte or (1 shl (7 - bit))
                    }
                }
            }
            result.append(String.format("%02X", byte))
        }
    }
    
    return result.toString()
}

/**
 * Create a test HTML for printing
 */
fun createTestHtml(labelSize: LabelSize, content: String = "Test Print"): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                @page {
                    size: ${labelSize.widthMm}mm ${labelSize.heightMm}mm;
                    margin: 0;
                }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: ${labelSize.widthMm}mm;
                    height: ${labelSize.heightMm}mm;
                    background: white;
                    font-family: Arial, sans-serif;
                    -webkit-text-size-adjust: none;
                    overflow: hidden;
                }
                .container {
                    width: 100%;
                    height: 100%;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    align-items: center;
                    text-align: center;
                    box-sizing: border-box;
                    padding: 2mm;
                }
                .title {
                    font-size: 8mm;
                    font-weight: bold;
                    margin-bottom: 2mm;
                }
                .content {
                    font-size: 4mm;
                    line-height: 1.2;
                }
                .barcode {
                    margin-top: 2mm;
                    font-family: monospace;
                    font-size: 3mm;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="title">$content</div>
                <div class="content">
                    Printer Test<br>
                    ${labelSize.name}<br>
                    ${System.currentTimeMillis()}
                </div>
                <div class="barcode">
                    ||| ||| ||| |||
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Create a sample jewelry label HTML
 */
fun createJewelryLabelHtml(
    labelSize: LabelSize,
    itemId: String,
    itemName: String,
    grossWeight: String,
    fineWeight: String,
    purity: String,
    storeName: String = "JewelVault"
): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                @page {
                    size: ${labelSize.widthMm}mm ${labelSize.heightMm}mm;
                    margin: 0;
                }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: ${labelSize.widthMm}mm;
                    height: ${labelSize.heightMm}mm;
                    background: white;
                    font-family: Arial, sans-serif;
                    -webkit-text-size-adjust: none;
                    overflow: hidden;
                }
                .container {
                    width: 100%;
                    height: 100%;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    box-sizing: border-box;
                    padding: 1mm;
                }
                .header {
                    text-align: center;
                    font-size: 6mm;
                    font-weight: bold;
                    margin-bottom: 1mm;
                }
                .content {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                }
                .item-info {
                    font-size: 4mm;
                    line-height: 1.3;
                    margin-bottom: 2mm;
                }
                .weight-info {
                    font-size: 5mm;
                    font-weight: bold;
                    text-align: center;
                    margin-bottom: 1mm;
                }
                .purity {
                    font-size: 6mm;
                    font-weight: bold;
                    text-align: center;
                    color: #333;
                }
                .footer {
                    font-size: 3mm;
                    text-align: center;
                    color: #666;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">$storeName</div>
                <div class="content">
                    <div class="item-info">
                        <div><strong>ID:</strong> $itemId</div>
                        <div><strong>Item:</strong> $itemName</div>
                    </div>
                    <div class="weight-info">
                        <div>GS: $grossWeight gm</div>
                        <div>FN: $fineWeight gm</div>
                    </div>
                    <div class="purity">$purity</div>
                </div>
                <div class="footer">
                    ${System.currentTimeMillis()}
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
}
