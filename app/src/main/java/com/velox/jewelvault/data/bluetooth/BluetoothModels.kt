package com.velox.jewelvault.data.bluetooth

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.Immutable

/**
 * Unified data models for Bluetooth device management
 */

/**
 * UI model for displaying Bluetooth devices with real-time updates
 */
@Immutable
data class DeviceUiModel(
    val name: String?,
    val address: String,
    val rssi: Int? = null,
    val bondState: Int = 0,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val lastSeenMillis: Long = System.currentTimeMillis(),
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val isPrinterCandidate: Boolean = false,
    val supportedServices: List<String> = emptyList(),
    val probeResult: ProbeResult? = null
)

/**
 * Types of Bluetooth devices
 */
enum class DeviceType {
    PHONE,
    TABLET,
    PRINTER,
    COMPUTER,
    LAPTOP,
    EARPHONE,
    HEADPHONE,
    SPEAKER,
    WATCH,
    KEYBOARD,
    MOUSE,
    GAMEPAD,
    CAMERA,
    SMART_TV,
    ROUTER,
    CAR,
    FITNESS_TRACKER,
    UNKNOWN
}



/**
 * Real-time connection state for devices
 */
enum class ConnectionState { 
    DISCONNECTED, 
    CONNECTING, 
    CONNECTED,
    DISCONNECTING
}

/**
 * Data class for connection state changes
 */
data class ConnectionStateChange(
    val device: BluetoothDevice,
    val connectionState: ConnectionState? = null,
    val bondState: Int? = null,
    val previousBondState: Int? = null
)

/**
 * Data class for Bluetooth adapter state changes
 */
data class BluetoothStateChange(
    val currentState: Int,
    val previousState: Int
)


/**
 * Result of printer probe attempt
 */
@Immutable
data class ProbeResult(
    val success: Boolean,
    val language: PrinterLanguage? = null,
    val error: String? = null,
    val responseTime: Long = 0L
)

/**
 * Printer language types
 */
enum class PrinterLanguage(val displayName: String, val commandPrefix: String) {
    ESC_POS("ESC/POS", "\u001B"),
    ZPL("ZPL", "^XA"),
    TSPL("TSPL", "SIZE"),
    AUTO("Auto Detect", "")
}

/**
 * Label size presets
 */
@Immutable
data class LabelSize(
    val name: String,
    val widthMm: Int,
    val heightMm: Int,
    val dpi: Int = 203
) {
    val widthPx: Int get() = (widthMm * dpi / 25.4).toInt()
    val heightPx: Int get() = (heightMm * dpi / 25.4).toInt()
}

/**
 * Printer settings
 */
@Immutable
data class PrinterSettings(
    val labelSize: LabelSize = LabelSize("4x6", 100, 150),
    val language: PrinterLanguage = PrinterLanguage.AUTO,
    val density: Int = 8, // 1-8 for ESC/POS
    val autoCut: Boolean = true,
    val autoFeed: Boolean = true
)

/**
 * Print job result
 */
@Immutable
data class PrintResult(
    val success: Boolean,
    val bytesSent: Int = 0,
    val error: String? = null,
    val previewBitmap: android.graphics.Bitmap? = null
)

/**
 * Device discovery event
 */
@Immutable
data class DeviceFound(
    val device: android.bluetooth.BluetoothDevice,
    val rssi: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Connection result
 */
@Immutable
data class ConnectionResult(
    val success: Boolean,
    val error: String? = null,
    val deviceAddress: String? = null
)

/**
 * Bluetooth disable result
 */
@Immutable
data class BluetoothDisableResult(
    val success: Boolean,
    val needsSettingsFallback: Boolean,
    val message: String
)

/**
 * Printer status information
 */
@Immutable
data class PrinterStatus(
    val isConnected: Boolean,
    val deviceAddress: String? = null,
    val deviceName: String? = null,
    val detectedLanguage: PrinterLanguage? = null,
    val lastPrintTime: Long? = null,
    val totalPrints: Int = 0
)

/**
 * Common label sizes
 */
object LabelSizes {
    val STANDARD_4X6 = LabelSize("4x6", 100, 150)
    val STANDARD_3X2 = LabelSize("3x2", 75, 50)
    val STANDARD_2X1 = LabelSize("2x1", 50, 25)
    val CUSTOM_100X50 = LabelSize("100x50mm", 100, 50)
    val CUSTOM_80X40 = LabelSize("80x40mm", 80, 40)
    
    val ALL_SIZES = listOf(
        STANDARD_4X6,
        STANDARD_3X2,
        STANDARD_2X1,
        CUSTOM_100X50,
        CUSTOM_80X40
    )
}

/**
 * Printer detection keywords
 */
object PrinterKeywords {
    val PRINTER_NAMES = listOf(
        "printer", "label", "thermal", "xprinter", "bzprinter", 
        "seznik", "shakti", "zebra", "tsc", "x-printer", "pos",
        "receipt", "thermal", "label", "barcode", "qrcode"
    )
    
    val PRINTER_CLASSES = listOf(
        "printer", "label", "thermal", "pos", "receipt"
    )
}

/**
 * ESC/POS command constants
 */
object EscPosCommands {
    const val ESC = "\u001B"
    const val GS = "\u001D"
    const val LF = "\n"
    const val CR = "\r"
    const val FF = "\u000C"
    const val CAN = "\u0018"
    
    // Common commands
    const val INIT = "$ESC@"
    const val CUT = "$GS V 0"
    const val FEED = "$LF"
    const val ALIGN_LEFT = "$ESC a 0"
    const val ALIGN_CENTER = "$ESC a 1"
    const val ALIGN_RIGHT = "$ESC a 2"
    
    // Text formatting
    const val BOLD_ON = "$ESC E 1"
    const val BOLD_OFF = "$ESC E 0"
    const val DOUBLE_HEIGHT = "$ESC ! 16"
    const val DOUBLE_WIDTH = "$ESC ! 32"
    const val DOUBLE_SIZE = "$ESC ! 48"
    
    // Barcode commands
    const val BARCODE_HEIGHT = "$GS h 64"
    const val BARCODE_WIDTH = "$GS w 2"
    const val BARCODE_HRI_ABOVE = "$GS H 2"
    const val BARCODE_HRI_BELOW = "$GS H 1"
}

/**
 * ZPL command constants
 */
object ZplCommands {
    const val START_FORMAT = "^XA"
    const val END_FORMAT = "^XZ"
    const val FIELD_ORIGIN = "^FO"
    const val FIELD_DATA = "^FD"
    const val FIELD_SEPARATOR = "^FS"
    const val TEXT_FONT = "^A0N"
    const val BARCODE_128 = "^BCN"
    const val QR_CODE = "^BQN"
    const val GRAPHIC_FIELD = "^GF"
    const val GRAPHIC_BOX = "^GB"
}

/**
 * TSPL command constants
 */
object TsplCommands {
    const val SIZE = "SIZE"
    const val GAP = "GAP"
    const val DIRECTION = "DIRECTION"
    const val REFERENCE = "REFERENCE"
    const val OFFSET = "OFFSET"
    const val SET = "SET"
    const val DENSITY = "DENSITY"
    const val SPEED = "SPEED"
    const val TEXT = "TEXT"
    const val BARCODE = "BARCODE"
    const val QRCODE = "QRCODE"
    const val PRINT = "PRINT"
    const val CLS = "CLS"
}



