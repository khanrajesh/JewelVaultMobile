package com.velox.jewelvault.ui.screen.bluetooth.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails

// Helper function to check if device is a printer
fun isPrinterDevice(device: BluetoothDeviceDetails, savedPrinters: List<com.velox.jewelvault.data.bluetooth.PrinterInfo> = emptyList()): Boolean {
    // First check if device is already saved as a printer
    if (savedPrinters.any { it.address == device.address }) {
        return true
    }
    
    // Then check based on device name or type
    return device.name?.contains("printer", ignoreCase = true) == true ||
            device.name?.contains("print", ignoreCase = true) == true ||
            device.bluetoothClass?.contains("printer", ignoreCase = true) == true
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

// Heuristic-based device type inference using name and bluetooth class hints
fun inferDeviceType(device: BluetoothDeviceDetails): DeviceType {
    if (isPrinterDevice(device)) return DeviceType.PRINTER

    val name = device.name?.lowercase() ?: ""
    val cls = device.bluetoothClass?.lowercase() ?: ""

    // Headphones / earbuds
    if (listOf("headset", "headphone", "earbud", "earbuds", "earphone", "airpods", "buds").any { hint -> name.contains(hint) || cls.contains(hint) }) {
        return DeviceType.HEADPHONE
    }

    // Speakers / soundbars
    if (listOf("speaker", "soundbar", "boom").any { hint -> name.contains(hint) || cls.contains(hint) }) {
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
    if (listOf("pc", "mac", "laptop", "notebook", "desktop", "imac", "macbook").any { hint -> name.contains(hint) || cls.contains(hint) }) {
        return DeviceType.COMPUTER
    }

    return DeviceType.OTHER
}


@Composable
fun TypeTags(device: BluetoothDeviceDetails) {
    val tags = buildList {
        if (isPrinterDevice(device)) add("Printer")
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
fun getDeviceIcon(device: BluetoothDeviceDetails): ImageVector {
    val name = device.name?.lowercase() ?: ""
    val cls = device.bluetoothClass?.lowercase() ?: ""
    return when {
        isPrinterDevice(device) -> Icons.Default.Print
        name.contains("headset") || name.contains("headphone") || cls.contains("headset") || cls.contains("headphone") -> Icons.Default.Headset
        name.contains("speaker") || cls.contains("speaker") -> Icons.Default.Speaker
        name.contains("keyboard") || cls.contains("keyboard") -> Icons.Default.Keyboard
        name.contains("mouse") || cls.contains("mouse") -> Icons.Default.Mouse
        name.contains("watch") || cls.contains("watch") -> Icons.Default.DeviceHub
        name.contains("phone") || name.contains("iphone") || cls.contains("phone") -> Icons.Default.Smartphone
        device.type == BluetoothDevice.DEVICE_TYPE_LE -> Icons.AutoMirrored.Filled.BluetoothSearching
        device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC -> Icons.Default.Bluetooth
        device.type == BluetoothDevice.DEVICE_TYPE_DUAL -> Icons.Default.Devices
        device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN -> Icons.Default.DeviceHub
        else -> Icons.Default.DeviceHub
    }
}

