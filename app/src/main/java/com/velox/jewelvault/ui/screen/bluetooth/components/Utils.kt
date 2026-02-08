package com.velox.jewelvault.ui.screen.bluetooth.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.BluetoothSearching
import androidx.compose.material.icons.twotone.Bluetooth
import androidx.compose.material.icons.twotone.DeviceHub
import androidx.compose.material.icons.twotone.Devices
import androidx.compose.material.icons.twotone.Headset
import androidx.compose.material.icons.twotone.Keyboard
import androidx.compose.material.icons.twotone.Mouse
import androidx.compose.material.icons.twotone.Print
import androidx.compose.material.icons.twotone.Smartphone
import androidx.compose.material.icons.twotone.Speaker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails
import com.velox.jewelvault.data.roomdb.entity.printer.PrinterEntity
import com.velox.jewelvault.ui.components.baseBackground9

/**
 * Unified function to determine if a device is a printer
 * Checks both saved printers and device characteristics
 */
fun isPrinterDevice(device: BluetoothDeviceDetails, savedPrinters: List<PrinterEntity> = emptyList()): Boolean {
    // First check if device is already saved as a printer
    val isSavedPrinter = savedPrinters.any { it.address == device.address }
    println("DEBUG: isPrinterDevice - Device: ${device.name} (${device.address})")
    println("DEBUG: isPrinterDevice - Saved printers count: ${savedPrinters.size}")
    println("DEBUG: isPrinterDevice - Is saved printer: $isSavedPrinter")
    
    if (isSavedPrinter) {
        println("DEBUG: isPrinterDevice - Device is saved as printer, returning true")
        return true
    }
    
    // Then check based on device characteristics
    val deviceType = getDeviceType(device)
    val isPrinterByType = deviceType == DeviceType.PRINTER
    println("DEBUG: isPrinterDevice - Device type: $deviceType")
    println("DEBUG: isPrinterDevice - Is printer by type: $isPrinterByType")
    
    return isPrinterByType
}


// Canonical device types for classification
enum class DeviceType(val label: String) {
    PRINTER("printer"),
    HEADPHONE("headphone"),
    SPEAKER("speaker"),
    KEYBOARD("keyboard"),
    MOUSE("mouse"),
    WATCH("watch"),
    SMARTPHONE("mobile"),
    TABLET("tablet"),
    TV("tv"),
    CAR("car"),
    WEARABLE("wearable"),
    GAMEPAD("gamepad"),
    CAMERA("camera"),
    COMPUTER("computer"),
    OTHER("device")
}

/**
 * Unified function to determine device type based on name, characteristics, and MAC address
 * This replaces the old inferDeviceType function and provides comprehensive device detection
 */
fun getDeviceType(device: BluetoothDeviceDetails): DeviceType {
    val name = device.name?.lowercase() ?: ""
    val address = device.address.lowercase()
    val cls = device.bluetoothClass?.lowercase() ?: ""
    
    println("DEBUG: getDeviceType - Device: ${device.name} (${device.address})")
    println("DEBUG: getDeviceType - Name: '$name', Address: '$address', Class: '$cls'")
    println("DEBUG: getDeviceType - Device type: ${device.type}")

    // Printer detection with comprehensive patterns
    val printerNamePatterns = listOf(
        "printer", "print", "thermal", "pos", "receipt", "label", "zebra",
        "epson", "canon", "hp", "brother", "citizen", "tsc", "bixolon",
        "star", "citizen", "dymo", "rollo", "munbyn", "phomemo"
    )
    
    val printerMacPrefixes = listOf(
        "00:11:22", "00:12:34", "00:15:83", "00:16:6c", "00:17:61",
        "00:18:39", "00:19:2f", "00:1a:92", "00:1b:63", "00:1c:42"
    )
    
    val hasPrinterName = printerNamePatterns.any { pattern -> name.contains(pattern) }
    val hasPrinterMac = printerMacPrefixes.any { prefix -> address.startsWith(prefix) }
    val isBlePrinter = (device.type == BluetoothDevice.DEVICE_TYPE_LE || device.type == BluetoothDevice.DEVICE_TYPE_DUAL) && (
        name.contains("printer") || name.contains("thermal") || name.contains("pos") ||
        device.extraInfo["serviceUuids"]?.toString()?.contains("printer") == true
    )
    
    println("DEBUG: getDeviceType - Printer name patterns check: $hasPrinterName")
    println("DEBUG: getDeviceType - Printer MAC prefixes check: $hasPrinterMac")
    println("DEBUG: getDeviceType - BLE printer check: $isBlePrinter")
    println("DEBUG: getDeviceType - Service UUIDs: ${device.extraInfo["serviceUuids"]}")
    
    if (hasPrinterName || hasPrinterMac || isBlePrinter) {
        println("DEBUG: getDeviceType - Detected as PRINTER")
        return DeviceType.PRINTER
    }

    // Headphones / earbuds
    if (listOf("headset", "headphone", "earbud", "earbuds", "earphone", "airpods", "buds").any { hint -> 
        name.contains(hint) || cls.contains(hint) 
    }) {
        return DeviceType.HEADPHONE
    }

    // Speakers / soundbars
    if (listOf("speaker", "soundbar", "boom").any { hint -> 
        name.contains(hint) || cls.contains(hint) 
    }) {
        return DeviceType.SPEAKER
    }

    // Input devices
    if (name.contains("keyboard") || cls.contains("keyboard")) return DeviceType.KEYBOARD
    if (name.contains("mouse") || cls.contains("mouse")) return DeviceType.MOUSE

    // Wearables
    if (name.contains("watch") || cls.contains("watch")) return DeviceType.WATCH
    if (name.contains("band") || name.contains("wear") || cls.contains("wear")) return DeviceType.WEARABLE

    // Phones / tablets
    if (name.contains("phone") || name.contains("iphone") || cls.contains("phone")) return DeviceType.SMARTPHONE
    if (name.contains("tablet") || name.contains("ipad") || cls.contains("tablet")) return DeviceType.TABLET

    // Entertainment / vehicles
    if (name.contains("tv") || cls.contains("tv")) return DeviceType.TV
    if (name.contains("car") || name.contains("auto") || cls.contains("car")) return DeviceType.CAR

    // Controllers / cameras / computers
    if (name.contains("gamepad") || name.contains("controller") || cls.contains("gamepad") || cls.contains("controller")) return DeviceType.GAMEPAD
    if (name.contains("camera") || cls.contains("camera")) return DeviceType.CAMERA
    if (listOf("pc", "mac", "laptop", "notebook", "desktop", "imac", "macbook").any { hint -> 
        name.contains(hint) || cls.contains(hint) 
    }) {
        println("DEBUG: getDeviceType - Detected as COMPUTER")
        return DeviceType.COMPUTER
    }

    println("DEBUG: getDeviceType - Detected as OTHER (default)")
    return DeviceType.OTHER
}


@Composable
fun TypeTags(device: BluetoothDeviceDetails, savedPrinters: List<PrinterEntity> = emptyList()) {
    val deviceType = getDeviceType(device)
    val tags = buildList {
        if (isPrinterDevice(device, savedPrinters)) add("Printer")
        when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> add("Classic")
            BluetoothDevice.DEVICE_TYPE_LE -> add("LE")
            BluetoothDevice.DEVICE_TYPE_DUAL -> add("LE + Classic")
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> add("Unknown")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .baseBackground9()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun getDeviceIcon(device: BluetoothDeviceDetails, savedPrinters: List<PrinterEntity> = emptyList()): ImageVector {
    val deviceType = getDeviceType(device)
    
    return when (deviceType) {
        DeviceType.PRINTER -> Icons.TwoTone.Print
        DeviceType.HEADPHONE -> Icons.TwoTone.Headset
        DeviceType.SPEAKER -> Icons.TwoTone.Speaker
        DeviceType.KEYBOARD -> Icons.TwoTone.Keyboard
        DeviceType.MOUSE -> Icons.TwoTone.Mouse
        DeviceType.WATCH -> Icons.TwoTone.DeviceHub
        DeviceType.SMARTPHONE -> Icons.TwoTone.Smartphone
        DeviceType.TABLET -> Icons.TwoTone.Smartphone
        DeviceType.TV -> Icons.TwoTone.DeviceHub
        DeviceType.CAR -> Icons.TwoTone.DeviceHub
        DeviceType.WEARABLE -> Icons.TwoTone.DeviceHub
        DeviceType.GAMEPAD -> Icons.TwoTone.DeviceHub
        DeviceType.CAMERA -> Icons.TwoTone.DeviceHub
        DeviceType.COMPUTER -> Icons.TwoTone.DeviceHub
        DeviceType.OTHER -> when (device.type) {
            BluetoothDevice.DEVICE_TYPE_LE -> Icons.AutoMirrored.TwoTone.BluetoothSearching
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> Icons.TwoTone.Bluetooth
            BluetoothDevice.DEVICE_TYPE_DUAL -> Icons.TwoTone.Devices
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> Icons.TwoTone.DeviceHub
            else -> Icons.TwoTone.DeviceHub
        }
    }
}

