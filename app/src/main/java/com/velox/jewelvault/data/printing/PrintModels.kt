package com.velox.jewelvault.data.printing

import android.graphics.Bitmap
import com.velox.jewelvault.data.roomdb.entity.ItemEntity

/**
 * Data models for Bluetooth printing functionality
 */

/**
 * Represents different types of print payloads
 */
sealed class PrintPayload {
    data class TextPayload(
        val text: String,
        val encoding: String = "UTF-8"
    ) : PrintPayload()
    
    data class HtmlPayload(
        val html: String,
        val width: Int = 384, // Default thermal printer width
        val height: Int = 600
    ) : PrintPayload()
    
    data class BitmapPayload(
        val bitmap: Bitmap,
        val width: Int = 384,
        val height: Int = 600
    ) : PrintPayload()
    
    data class RawPayload(
        val data: ByteArray,
        val description: String = "Raw data"
    ) : PrintPayload() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as RawPayload
            if (!data.contentEquals(other.data)) return false
            if (description != other.description) return false
            return true
        }
        
        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + description.hashCode()
            return result
        }
    }
    
    data class ItemLabelPayload(
        val item: ItemEntity,
        val labelFormat: LabelFormat = LabelFormat.THERMAL_100MM,
        val includeQR: Boolean = true,
        val includeLogo: Boolean = true
    ) : PrintPayload()
}

/**
 * Represents the result of a print operation
 */
sealed class PrintResult {
    object Success : PrintResult()
    object NoPairedPrinter : PrintResult()
    object PrinterNotConnected : PrintResult()
    object PrinterNotSupported : PrintResult()
    object PermissionDenied : PrintResult()
    object BluetoothDisabled : PrintResult()
    data class Error(val message: String, val throwable: Throwable? = null) : PrintResult()
    data class PartialSuccess(val printedBytes: Int, val totalBytes: Int) : PrintResult()
}

/**
 * Represents different label formats for printing
 */
enum class LabelFormat(
    val width: Int,
    val height: Int,
    val dpi: Int = 203,
    val description: String
) {
    THERMAL_100MM(384, 512, 203, "Thermal 100mm x 13mm"),
    THERMAL_80MM(384, 384, 203, "Thermal 80mm x 80mm"),
    THERMAL_58MM(384, 384, 203, "Thermal 58mm x 80mm"),
    THERMAL_50MM(384, 384, 203, "Thermal 50mm x 80mm"),
    A4(2480, 3508, 300, "A4 Paper"),
    LETTER(2550, 3300, 300, "Letter Paper")
}

/**
 * Represents different printer languages/protocols
 */
enum class PrinterLanguage(
    val commandPrefix: String,
    val description: String
) {
    ESC_POS("ESC/POS", "ESC/POS Command Set"),
    ZPL("ZPL", "Zebra Programming Language"),
    TSPL("TSPL", "TSC Printer Language"),
    CPCL("CPCL", "Citizen Printer Command Language")
}

/**
 * Represents printer connection state
 */
enum class PrinterConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * Represents a Bluetooth device with printer capabilities
 */
data class BluetoothDeviceInfo(
    val address: String,
    val name: String?,
    val isPaired: Boolean,
    val isConnected: Boolean,
    val deviceType: String,
    val rssi: Int? = null,
    val batteryLevel: Int? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val isPrinter: Boolean = false,
    val printerLanguage: PrinterLanguage? = null,
    val supportedFormats: List<LabelFormat> = emptyList()
)



/**
 * Represents printer configuration
 */
data class PrinterConfig(
    val deviceAddress: String,
    val deviceName: String,
    val labelFormat: LabelFormat = LabelFormat.THERMAL_100MM,
    val printerLanguage: PrinterLanguage = PrinterLanguage.ESC_POS,
    val dpi: Int = 203,
    val autoConnect: Boolean = true,
    val connectionTimeout: Long = 10000L, // 10 seconds
    val retryAttempts: Int = 3
)

/**
 * Represents print job progress
 */
data class PrintJobProgress(
    val jobId: String,
    val totalBytes: Int,
    val printedBytes: Int,
    val status: PrintJobStatus,
    val error: String? = null
) {
    val progressPercentage: Float
        get() = if (totalBytes > 0) (printedBytes.toFloat() / totalBytes) * 100f else 0f
}

/**
 * Represents print job status
 */
enum class PrintJobStatus {
    QUEUED,
    PRINTING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Represents BLE telemetry data
 */
data class BleTelemetryData(
    val deviceAddress: String,
    val rssi: Int,
    val batteryLevel: Int?,
    val connectionState: PrinterConnectionState,
    val timestamp: Long = System.currentTimeMillis()
)
