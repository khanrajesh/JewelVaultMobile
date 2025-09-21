package com.velox.jewelvault.ui.screen.bluetooth

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.bluetooth.BluetoothService
import com.velox.jewelvault.data.bluetooth.ConnectionState
import com.velox.jewelvault.data.bluetooth.PrinterSettings
import com.velox.jewelvault.data.bluetooth.LabelSize
import com.velox.jewelvault.data.bluetooth.PrinterLanguage
import com.velox.jewelvault.data.bluetooth.LabelSizes
import com.velox.jewelvault.data.bluetooth.PrintResult
import com.velox.jewelvault.data.bluetooth.PrinterStatus
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for printer management and printing operations
 */
@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val bluetoothService: BluetoothService
) : ViewModel() {

    // Printer settings
    private val _printerSettings = MutableStateFlow(PrinterSettings())
    val printerSettings: StateFlow<PrinterSettings> = _printerSettings.asStateFlow()

    // Printer status
    private val _printerStatus = MutableStateFlow(PrinterStatus(false))
    val printerStatus: StateFlow<PrinterStatus> = _printerStatus.asStateFlow()

    // Print preview
    private val _printPreview = MutableStateFlow<Bitmap?>(null)
    val printPreview: StateFlow<Bitmap?> = _printPreview.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Loading states
    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    private val _isGeneratingPreview = MutableStateFlow(false)
    val isGeneratingPreview: StateFlow<Boolean> = _isGeneratingPreview.asStateFlow()

    init {
        observeRepository()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            // Observe connection status
            bluetoothService.connectionStatus.collect { status ->
                val connectedDevice = bluetoothService.connectedDevice.value
                _printerStatus.value = PrinterStatus(
                    isConnected = status == ConnectionState.CONNECTED,
                    deviceAddress = connectedDevice?.address,
                    deviceName = connectedDevice?.name
                )
            }
        }

        viewModelScope.launch {
            // Observe connected device
            bluetoothService.connectedDevice.collect { device ->
                if (device != null) {
                    _printerStatus.value = _printerStatus.value.copy(
                        deviceAddress = device.address,
                        deviceName = device.name
                    )
                }
            }
        }
    }

    fun updateLabelSize(labelSize: LabelSize) {
        _printerSettings.value = _printerSettings.value.copy(labelSize = labelSize)
        log("Label size updated to: ${labelSize.name}")
    }

    fun updateLanguage(language: PrinterLanguage) {
        _printerSettings.value = _printerSettings.value.copy(language = language)
        log("Printer language updated to: ${language.displayName}")
    }

    fun updateDensity(density: Int) {
        val clampedDensity = density.coerceIn(1, 8)
        _printerSettings.value = _printerSettings.value.copy(density = clampedDensity)
        log("Print density updated to: $clampedDensity")
    }

    fun updateAutoCut(autoCut: Boolean) {
        _printerSettings.value = _printerSettings.value.copy(autoCut = autoCut)
        log("Auto cut updated to: $autoCut")
    }

    fun updateAutoFeed(autoFeed: Boolean) {
        _printerSettings.value = _printerSettings.value.copy(autoFeed = autoFeed)
        log("Auto feed updated to: $autoFeed")
    }

    /**
     * Disconnect from current printer
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                val result = bluetoothService.disconnect()
                if (result.isSuccess) {
                    _printerStatus.value = PrinterStatus(false)
                    _errorMessage.value = null
                    log("Disconnected from printer")
                } else {
                    _errorMessage.value = "Failed to disconnect: ${result.exceptionOrNull()?.message}"
                    log("Failed to disconnect: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error disconnecting: ${e.message}"
                log("Error disconnecting: ${e.message}")
            }
        }
    }

    /**
     * Print HTML content to connected printer
     */
    fun printHtml(html: String, context: Context) {
        viewModelScope.launch {
            try {
                _isPrinting.value = true
                _errorMessage.value = null

                val settings = _printerSettings.value
                val result = printHtmlToPrinter(html, settings, context)
                
                if (result.success) {
                    _printerStatus.value = _printerStatus.value.copy(
                        lastPrintTime = System.currentTimeMillis(),
                        totalPrints = _printerStatus.value.totalPrints + 1
                    )
                    log("Successfully printed HTML content")
                } else {
                    _errorMessage.value = result.error ?: "Print failed"
                    log("Print failed: ${result.error}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error printing: ${e.message}"
                log("Error printing HTML: ${e.message}")
            } finally {
                _isPrinting.value = false
            }
        }
    }

    /**
     * Generate print preview for HTML content
     */
    fun generatePreview(html: String, context: Context) {
        viewModelScope.launch {
            try {
                _isGeneratingPreview.value = true
                _errorMessage.value = null

                val settings = _printerSettings.value
                val preview = renderHtmlToBitmap(html, settings.labelSize, context)
                _printPreview.value = preview
                
                log("Generated print preview")
            } catch (e: Exception) {
                _errorMessage.value = "Error generating preview: ${e.message}"
                log("Error generating preview: ${e.message}")
            } finally {
                _isGeneratingPreview.value = false
            }
        }
    }

    /**
     * Print test text
     */
    fun printTestText() {
        viewModelScope.launch {
            try {
                _isPrinting.value = true
                _errorMessage.value = null

                val testText = buildTestText()
                val result = bluetoothService.sendText(testText)
                
                if (result.isSuccess) {
                    _printerStatus.value = _printerStatus.value.copy(
                        lastPrintTime = System.currentTimeMillis(),
                        totalPrints = _printerStatus.value.totalPrints + 1
                    )
                    log("Successfully printed test text")
                } else {
                    _errorMessage.value = "Failed to print test text"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error printing test text: ${e.message}"
                log("Error printing test text: ${e.message}")
            } finally {
                _isPrinting.value = false
            }
        }
    }

    /**
     * Print test barcode
     */
    fun printTestBarcode() {
        viewModelScope.launch {
            try {
                _isPrinting.value = true
                _errorMessage.value = null

                val barcodeText = buildTestBarcode()
                val result = bluetoothService.sendText(barcodeText)
                
                if (result.isSuccess) {
                    _printerStatus.value = _printerStatus.value.copy(
                        lastPrintTime = System.currentTimeMillis(),
                        totalPrints = _printerStatus.value.totalPrints + 1
                    )
                    log("Successfully printed test barcode")
                } else {
                    _errorMessage.value = "Failed to print test barcode"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error printing test barcode: ${e.message}"
                log("Error printing test barcode: ${e.message}")
            } finally {
                _isPrinting.value = false
            }
        }
    }

    /**
     * Calibrate printer
     */
    fun calibratePrinter() {
        viewModelScope.launch {
            try {
                _isPrinting.value = true
                _errorMessage.value = null

                val calibrationCommands = buildCalibrationCommands()
                val result = bluetoothService.sendText(calibrationCommands)
                
                if (result.isSuccess) {
                    log("Printer calibration completed")
                } else {
                    _errorMessage.value = "Calibration failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error calibrating printer: ${e.message}"
                log("Error calibrating printer: ${e.message}")
            } finally {
                _isPrinting.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearPreview() {
        _printPreview.value = null
    }

    private suspend fun printHtmlToPrinter(html: String, settings: PrinterSettings, context: Context): PrintResult {
        return try {
            // Render HTML to bitmap
            val bitmap = renderHtmlToBitmap(html, settings.labelSize, context)
            
            // Convert bitmap to printer bytes
            val printerBytes = when (settings.language) {
                PrinterLanguage.ESC_POS -> bitmapToEscPosRaster(bitmap, settings)
                PrinterLanguage.ZPL -> bitmapToZplImage(bitmap, settings)
                PrinterLanguage.TSPL -> bitmapToTsplImage(bitmap, settings)
                PrinterLanguage.AUTO -> {
                    // Try to detect language from printer response or default to ESC/POS
                    bitmapToEscPosRaster(bitmap, settings)
                }
            }
            
            // Send to printer
            val sendResult = bluetoothService.sendData(printerBytes)
            if (sendResult.isSuccess) {
                PrintResult(true, printerBytes.size, null, bitmap)
            } else {
                PrintResult(false, 0, sendResult.exceptionOrNull()?.message, bitmap)
            }
        } catch (e: Exception) {
            PrintResult(false, 0, e.message, null)
        }
    }

    private suspend fun renderHtmlToBitmap(html: String, labelSize: LabelSize, context: Context): Bitmap {
        // This will be implemented in the HTML rendering utility
        // For now, return a placeholder
        return Bitmap.createBitmap(labelSize.widthPx, labelSize.heightPx, Bitmap.Config.ARGB_8888)
    }

    private fun bitmapToEscPosRaster(bitmap: Bitmap, settings: PrinterSettings): ByteArray {
        // This will be implemented in the ESC/POS conversion utility
        // For now, return placeholder
        return "ESC/POS raster data".toByteArray()
    }

    private fun bitmapToZplImage(bitmap: Bitmap, settings: PrinterSettings): ByteArray {
        // This will be implemented in the ZPL conversion utility
        // For now, return placeholder
        return "ZPL image data".toByteArray()
    }

    private fun bitmapToTsplImage(bitmap: Bitmap, settings: PrinterSettings): ByteArray {
        // This will be implemented in the TSPL conversion utility
        // For now, return placeholder
        return "TSPL image data".toByteArray()
    }

    private fun buildTestText(): String {
        val settings = _printerSettings.value
        return when (settings.language) {
            PrinterLanguage.ESC_POS -> buildEscPosTestText()
            PrinterLanguage.ZPL -> buildZplTestText()
            PrinterLanguage.TSPL -> buildTsplTestText()
            PrinterLanguage.AUTO -> buildEscPosTestText() // Default to ESC/POS
        }
    }

    private fun buildEscPosTestText(): String {
        return """
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.INIT}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.ALIGN_CENTER}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.DOUBLE_SIZE}
            TEST PRINT
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.FEED}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.ALIGN_LEFT}
            Printer Test Successful
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.FEED}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.FEED}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.CUT}
        """.trimIndent()
    }

    private fun buildZplTestText(): String {
        return """
            ^XA
            ^FO50,50^A0N,50,50^FDTEST PRINT^FS
            ^FO50,100^A0N,30,30^FDPrinter Test Successful^FS
            ^XZ
        """.trimIndent()
    }

    private fun buildTsplTestText(): String {
        return """
            SIZE 100 mm, 50 mm
            GAP 3 mm, 0 mm
            TEXT 50,50,"3",0,1,1,"TEST PRINT"
            TEXT 50,100,"3",0,1,1,"Printer Test Successful"
            PRINT 1,1
        """.trimIndent()
    }

    private fun buildTestBarcode(): String {
        val settings = _printerSettings.value
        return when (settings.language) {
            PrinterLanguage.ESC_POS -> buildEscPosTestBarcode()
            PrinterLanguage.ZPL -> buildZplTestBarcode()
            PrinterLanguage.TSPL -> buildTsplTestBarcode()
            PrinterLanguage.AUTO -> buildEscPosTestBarcode()
        }
    }

    private fun buildEscPosTestBarcode(): String {
        return """
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.INIT}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.ALIGN_CENTER}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.BARCODE_HEIGHT}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.BARCODE_HRI_BELOW}
            Test Barcode: 123456789
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.FEED}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.CUT}
        """.trimIndent()
    }

    private fun buildZplTestBarcode(): String {
        return """
            ^XA
            ^FO50,50^A0N,30,30^FDBarcode Test^FS
            ^FO50,100^BY3
            ^BCN,100,Y,N,N
            ^FD123456789^FS
            ^XZ
        """.trimIndent()
    }

    private fun buildTsplTestBarcode(): String {
        return """
            SIZE 100 mm, 50 mm
            GAP 3 mm, 0 mm
            TEXT 50,50,"3",0,1,1,"Barcode Test"
            BARCODE 50,100,"128",100,1,0,2,2,"123456789"
            PRINT 1,1
        """.trimIndent()
    }

    private fun buildCalibrationCommands(): String {
        val settings = _printerSettings.value
        return when (settings.language) {
            PrinterLanguage.ESC_POS -> buildEscPosCalibration()
            PrinterLanguage.ZPL -> buildZplCalibration()
            PrinterLanguage.TSPL -> buildTsplCalibration()
            PrinterLanguage.AUTO -> buildEscPosCalibration()
        }
    }

    private fun buildEscPosCalibration(): String {
        return """
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.INIT}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.ALIGN_CENTER}
            PRINTER CALIBRATION
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.FEED}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.FEED}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.FEED}
            ${com.velox.jewelvault.data.bluetooth.EscPosCommands.CUT}
        """.trimIndent()
    }

    private fun buildZplCalibration(): String {
        return """
            ^XA
            ^FO50,50^A0N,50,50^FDPRINTER CALIBRATION^FS
            ^XZ
        """.trimIndent()
    }

    private fun buildTsplCalibration(): String {
        return """
            SIZE 100 mm, 50 mm
            GAP 3 mm, 0 mm
            TEXT 50,50,"3",0,1,1,"PRINTER CALIBRATION"
            PRINT 1,1
        """.trimIndent()
    }
}
