package com.velox.jewelvault.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.bluetooth.BluetoothService
import com.velox.jewelvault.data.bluetooth.ConnectionState
import com.velox.jewelvault.data.bluetooth.LabelSizes
import com.velox.jewelvault.data.bluetooth.PrinterLanguage
import com.velox.jewelvault.data.bluetooth.PrinterSettings
import com.velox.jewelvault.ui.screen.bluetooth.PrinterViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// Import utility functions
import com.velox.jewelvault.utils.renderHtmlToBitmap
import com.velox.jewelvault.utils.bitmapToEscPosRaster

/**
 * Global printer manager that can be called from anywhere in the app
 * Provides a simple interface to check printer connection and print HTML content
 */
@Singleton
class GlobalPrinterManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothManager: BluetoothService
) {
    
    companion object {
        @Volatile
        private var INSTANCE: GlobalPrinterManager? = null
        
        fun getInstance(context: Context, bluetoothService: BluetoothService): GlobalPrinterManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GlobalPrinterManager(context, bluetoothService).also { INSTANCE = it }
            }
        }
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Check if printer is connected
     * @return true if printer is connected, false otherwise
     */
    suspend fun isPrinterConnected(): Boolean {
        return try {
            val connectionStatus = bluetoothManager.connectionStatus.first()
            connectionStatus == ConnectionState.CONNECTED
        } catch (e: Exception) {
            log("Error checking printer connection: ${e.message}")
            false
        }
    }
    
    /**
     * Print HTML content to connected printer
     * @param htmlContent HTML content to print
     * @param labelSize Label size for printing (default: 4x6)
     * @param onSuccess Callback when print is successful
     * @param onError Callback when print fails
     */
    suspend fun printHtml(
        htmlContent: String,
        labelSize: com.velox.jewelvault.data.bluetooth.LabelSize = LabelSizes.STANDARD_4X6,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        try {
            // Check if printer is connected
            if (!isPrinterConnected()) {
                onError?.invoke("Printer is not connected. Please connect to a printer first.")
                return
            }
            
            // Render HTML to bitmap
            val bitmap = renderHtmlToBitmap(htmlContent, labelSize, context)
            
            // Convert bitmap to printer bytes (ESC/POS by default)
            val printerBytes = bitmapToEscPosRaster(bitmap, PrinterSettings(labelSize = labelSize))
            
            // Send to printer
            val result = bluetoothManager.sendData(printerBytes)
            
            if (result.isSuccess) {
                log("Successfully printed HTML content (${printerBytes.size} bytes)")
                onSuccess?.invoke()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown print error"
                log("Failed to print HTML content: $error")
                onError?.invoke("Print failed: $error")
            }
        } catch (e: Exception) {
            log("Error printing HTML content: ${e.message}")
            onError?.invoke("Print error: ${e.message}")
        }
    }
    
    /**
     * Print HTML content asynchronously (non-blocking)
     * @param htmlContent HTML content to print
     * @param labelSize Label size for printing (default: 4x6)
     * @param onSuccess Callback when print is successful
     * @param onError Callback when print fails
     */
    fun printHtmlAsync(
        htmlContent: String,
        labelSize: com.velox.jewelvault.data.bluetooth.LabelSize = LabelSizes.STANDARD_4X6,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        coroutineScope.launch {
            printHtml(htmlContent, labelSize, onSuccess, onError)
        }
    }
    
    /**
     * Print simple text to connected printer
     * @param text Text to print
     * @param onSuccess Callback when print is successful
     * @param onError Callback when print fails
     */
    suspend fun printText(
        text: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        try {
            if (!isPrinterConnected()) {
                onError?.invoke("Printer is not connected. Please connect to a printer first.")
                return
            }
            
            val result = bluetoothManager.sendText(text)
            
            if (result.isSuccess) {
                log("Successfully printed text: $text")
                onSuccess?.invoke()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown print error"
                log("Failed to print text: $error")
                onError?.invoke("Print failed: $error")
            }
        } catch (e: Exception) {
            log("Error printing text: ${e.message}")
            onError?.invoke("Print error: ${e.message}")
        }
    }
    
    /**
     * Print simple text asynchronously (non-blocking)
     * @param text Text to print
     * @param onSuccess Callback when print is successful
     * @param onError Callback when print fails
     */
    fun printTextAsync(
        text: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        coroutineScope.launch {
            printText(text, onSuccess, onError)
        }
    }
    
    /**
     * Get current printer connection status
     * @return ConnectionStatus enum value
     */
    suspend fun getConnectionStatus(): ConnectionState {
        return try {
            bluetoothManager.connectionStatus.first()
        } catch (e: Exception) {
            log("Error getting connection status: ${e.message}")
            ConnectionState.DISCONNECTED
        }
    }
    
    /**
     * Get connected printer name
     * @return Printer name or null if not connected
     */
    suspend fun getConnectedPrinterName(): String? {
        return try {
            val device = bluetoothManager.connectedDevice.first()
            device?.name
        } catch (e: Exception) {
            log("Error getting printer name: ${e.message}")
            null
        }
    }
}

/**
 * Composable function to get GlobalPrinterManager instance
 * Use this in Compose screens
 */
@Composable
fun rememberGlobalPrinterManager(): GlobalPrinterManager {
    return remember {
        // This will be injected by Hilt in the actual implementation
        // For now, return a placeholder that will be replaced
        throw NotImplementedError("Use Hilt injection to get GlobalPrinterManager")
    }
}

/**
 * Extension function for easy access to printer manager
 * Usage: context.printHtml("Hello World")
 */
fun Context.printHtml(
    htmlContent: String,
    labelSize: com.velox.jewelvault.data.bluetooth.LabelSize = LabelSizes.STANDARD_4X6,
    onSuccess: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    // This would need to be implemented with proper dependency injection
    // For now, it's a placeholder
    log("Context.printHtml called - implement with proper DI")
}

/**
 * Extension function for easy access to printer manager
 * Usage: context.printText("Hello World")
 */
fun Context.printText(
    text: String,
    onSuccess: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    // This would need to be implemented with proper dependency injection
    // For now, it's a placeholder
    log("Context.printText called - implement with proper DI")
}

/**
 * Utility object for static access to printer functions
 * This can be used from anywhere in the app
 */
object PrinterUtils {
    
    private var globalPrinterManager: GlobalPrinterManager? = null
    
    fun initialize(printerManager: GlobalPrinterManager) {
        globalPrinterManager = printerManager
    }
    
    /**
     * Check if printer is connected
     * @return true if printer is connected, false otherwise
     */
    suspend fun isConnected(): Boolean {
        return globalPrinterManager?.isPrinterConnected() ?: false
    }
    
    /**
     * Print HTML content
     * @param htmlContent HTML content to print
     * @param labelSize Label size for printing
     * @param onSuccess Callback when print is successful
     * @param onError Callback when print fails
     */
    suspend fun printHtml(
        htmlContent: String,
        labelSize: com.velox.jewelvault.data.bluetooth.LabelSize = LabelSizes.STANDARD_4X6,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        globalPrinterManager?.printHtml(htmlContent, labelSize, onSuccess, onError)
            ?: onError?.invoke("Printer manager not initialized")
    }
    
    /**
     * Print HTML content asynchronously
     * @param htmlContent HTML content to print
     * @param labelSize Label size for printing
     * @param onSuccess Callback when print is successful
     * @param onError Callback when print fails
     */
    fun printHtmlAsync(
        htmlContent: String,
        labelSize: com.velox.jewelvault.data.bluetooth.LabelSize = LabelSizes.STANDARD_4X6,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        globalPrinterManager?.printHtmlAsync(htmlContent, labelSize, onSuccess, onError)
            ?: onError?.invoke("Printer manager not initialized")
    }
    
    /**
     * Print simple text
     * @param text Text to print
     * @param onSuccess Callback when print is successful
     * @param onError Callback when print fails
     */
    suspend fun printText(
        text: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        globalPrinterManager?.printText(text, onSuccess, onError)
            ?: onError?.invoke("Printer manager not initialized")
    }
    
    /**
     * Print simple text asynchronously
     * @param text Text to print
     * @param onSuccess Callback when print is successful
     * @param onError Callback when print fails
     */
    fun printTextAsync(
        text: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        globalPrinterManager?.printTextAsync(text, onSuccess, onError)
            ?: onError?.invoke("Printer manager not initialized")
    }
    
    /**
     * Get connection status
     * @return ConnectionStatus enum value
     */
    suspend fun getConnectionStatus(): ConnectionState? {
        return globalPrinterManager?.getConnectionStatus()
    }
    
    /**
     * Get connected printer name
     * @return Printer name or null if not connected
     */
    suspend fun getConnectedPrinterName(): String? {
        return globalPrinterManager?.getConnectedPrinterName()
    }
}
