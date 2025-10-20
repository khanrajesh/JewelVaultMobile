package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.velox.jewelvault.utils.log

object BleUtils {

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun logDevice(msg:String,device: BluetoothDevice?=null) {
    if (device == null) {
        log(msg)
        return
    }
    val name = device.name ?: "Unknown"
    val address = device.address
    val type = when (device.type) {
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> "CLASSIC"
        BluetoothDevice.DEVICE_TYPE_LE -> "LE"
        BluetoothDevice.DEVICE_TYPE_DUAL -> "DUAL"
        BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "UNKNOWN"
        else -> "UNKNOWN"
    }
    val bondState = when (device.bondState) {
        BluetoothDevice.BOND_NONE -> "NONE"
        BluetoothDevice.BOND_BONDING -> "BONDING"
        BluetoothDevice.BOND_BONDED -> "BONDED"
        else -> "UNKNOWN"
    }
    val bluetoothClassObj = device.bluetoothClass
    val deviceClass = bluetoothClassObj?.deviceClass ?: -1
    val majorDeviceClass = bluetoothClassObj?.majorDeviceClass ?: -1

    val deviceClassString = when (deviceClass) {
        // Audio/Video Devices
        BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED -> "AUDIO_VIDEO_UNCATEGORIZED"
        BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> "AUDIO_VIDEO_WEARABLE_HEADSET"
        BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> "AUDIO_VIDEO_HANDSFREE"
        BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE -> "AUDIO_VIDEO_MICROPHONE"
        BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> "AUDIO_VIDEO_LOUDSPEAKER"
        BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO -> "AUDIO_VIDEO_PORTABLE_AUDIO"
        BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> "AUDIO_VIDEO_HEADPHONES"
        BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> "AUDIO_VIDEO_CAR_AUDIO"
        BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> "AUDIO_VIDEO_SET_TOP_BOX"
        BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO -> "AUDIO_VIDEO_HIFI_AUDIO"
        BluetoothClass.Device.AUDIO_VIDEO_VCR -> "AUDIO_VIDEO_VCR"
        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA -> "AUDIO_VIDEO_VIDEO_CAMERA"
        BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER -> "AUDIO_VIDEO_CAMCORDER"
        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR -> "AUDIO_VIDEO_VIDEO_MONITOR"
        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER -> "AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER"
        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING -> "AUDIO_VIDEO_VIDEO_CONFERENCING"
        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> "AUDIO_VIDEO_VIDEO_GAMING_TOY"
        
        // Computer Devices
        BluetoothClass.Device.COMPUTER_UNCATEGORIZED -> "COMPUTER_UNCATEGORIZED"
        BluetoothClass.Device.COMPUTER_DESKTOP -> "COMPUTER_DESKTOP"
        BluetoothClass.Device.COMPUTER_SERVER -> "COMPUTER_SERVER"
        BluetoothClass.Device.COMPUTER_LAPTOP -> "COMPUTER_LAPTOP"
        BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA -> "COMPUTER_HANDHELD_PC_PDA"
        BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA -> "COMPUTER_PALM_SIZE_PC_PDA"
        BluetoothClass.Device.COMPUTER_WEARABLE -> "COMPUTER_WEARABLE"
        
        // Phone Devices
        BluetoothClass.Device.PHONE_UNCATEGORIZED -> "PHONE_UNCATEGORIZED"
        BluetoothClass.Device.PHONE_CELLULAR -> "PHONE_CELLULAR"
        BluetoothClass.Device.PHONE_CORDLESS -> "PHONE_CORDLESS"
        BluetoothClass.Device.PHONE_SMART -> "PHONE_SMART"
        BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY -> "PHONE_MODEM_OR_GATEWAY"
        BluetoothClass.Device.PHONE_ISDN -> "PHONE_ISDN"
        
        // Peripheral Devices
        BluetoothClass.Device.PERIPHERAL_NON_KEYBOARD_NON_POINTING -> "PERIPHERAL_NON_KEYBOARD_NON_POINTING"
        BluetoothClass.Device.PERIPHERAL_KEYBOARD -> "PERIPHERAL_KEYBOARD"
        BluetoothClass.Device.PERIPHERAL_POINTING -> "PERIPHERAL_POINTING"
        BluetoothClass.Device.PERIPHERAL_KEYBOARD_POINTING -> "PERIPHERAL_KEYBOARD_POINTING"
        
        // Wearable Devices
        BluetoothClass.Device.WEARABLE_UNCATEGORIZED -> "WEARABLE_UNCATEGORIZED"
        BluetoothClass.Device.WEARABLE_WRIST_WATCH -> "WEARABLE_WRIST_WATCH"
        BluetoothClass.Device.WEARABLE_PAGER -> "WEARABLE_PAGER"
        BluetoothClass.Device.WEARABLE_JACKET -> "WEARABLE_JACKET"
        BluetoothClass.Device.WEARABLE_HELMET -> "WEARABLE_HELMET"
        BluetoothClass.Device.WEARABLE_GLASSES -> "WEARABLE_GLASSES"
        
        // Toy Devices
        BluetoothClass.Device.TOY_UNCATEGORIZED -> "TOY_UNCATEGORIZED"
        BluetoothClass.Device.TOY_ROBOT -> "TOY_ROBOT"
        BluetoothClass.Device.TOY_VEHICLE -> "TOY_VEHICLE"
        BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE -> "TOY_DOLL_ACTION_FIGURE"
        BluetoothClass.Device.TOY_CONTROLLER -> "TOY_CONTROLLER"
        BluetoothClass.Device.TOY_GAME -> "TOY_GAME"
        
        // Health Devices
        BluetoothClass.Device.HEALTH_UNCATEGORIZED -> "HEALTH_UNCATEGORIZED"
        BluetoothClass.Device.HEALTH_BLOOD_PRESSURE -> "HEALTH_BLOOD_PRESSURE"
        BluetoothClass.Device.HEALTH_THERMOMETER -> "HEALTH_THERMOMETER"
        BluetoothClass.Device.HEALTH_WEIGHING -> "HEALTH_WEIGHING"
        BluetoothClass.Device.HEALTH_GLUCOSE -> "HEALTH_GLUCOSE"
        BluetoothClass.Device.HEALTH_PULSE_OXIMETER -> "HEALTH_PULSE_OXIMETER"
        BluetoothClass.Device.HEALTH_PULSE_RATE -> "HEALTH_PULSE_RATE"
        BluetoothClass.Device.HEALTH_DATA_DISPLAY -> "HEALTH_DATA_DISPLAY"
        
        else -> "UNKNOWN ($deviceClass)"
    }

    val majorDeviceClassString = when (majorDeviceClass) {
        BluetoothClass.Device.Major.AUDIO_VIDEO -> "AUDIO_VIDEO"
        BluetoothClass.Device.Major.COMPUTER -> "COMPUTER"
        BluetoothClass.Device.Major.PHONE -> "PHONE"
        BluetoothClass.Device.Major.PERIPHERAL -> "PERIPHERAL"
        BluetoothClass.Device.Major.HEALTH -> "HEALTH"
        BluetoothClass.Device.Major.IMAGING -> "IMAGING"
        BluetoothClass.Device.Major.MISC -> "MISC"
        BluetoothClass.Device.Major.NETWORKING -> "NETWORKING"
        BluetoothClass.Device.Major.TOY -> "TOY"
        BluetoothClass.Device.Major.UNCATEGORIZED -> "UNCATEGORIZED"
        BluetoothClass.Device.Major.WEARABLE -> "WEARABLE"
        else -> "UNKNOWN ($majorDeviceClass)"
    }

    // Service UUIDs handling
    val uuidsFromDeviceField = try {
        device.uuids?.joinToString(", ") { it.toString() } ?: "None"
    } catch (_: Exception) {
        "Unavailable"
    }

    // Check for specific services using BluetoothClass.Service
    val services = mutableListOf<String>()
    bluetoothClassObj?.let { bluetoothClass ->
        if (bluetoothClass.hasService(BluetoothClass.Service.AUDIO)) {
            services.add("AUDIO")
        }
        if (bluetoothClass.hasService(BluetoothClass.Service.CAPTURE)) {
            services.add("CAPTURE")
        }
        if (bluetoothClass.hasService(BluetoothClass.Service.INFORMATION)) {
            services.add("INFORMATION")
        }
        if (bluetoothClass.hasService(BluetoothClass.Service.LE_AUDIO)) {
            services.add("LE_AUDIO")
        }
        if (bluetoothClass.hasService(BluetoothClass.Service.LIMITED_DISCOVERABILITY)) {
            services.add("LIMITED_DISCOVERABILITY")
        }
        if (bluetoothClass.hasService(BluetoothClass.Service.NETWORKING)) {
            services.add("NETWORKING")
        }
        if (bluetoothClass.hasService(BluetoothClass.Service.OBJECT_TRANSFER)) {
            services.add("OBJECT_TRANSFER")
        }
        if (bluetoothClass.hasService(BluetoothClass.Service.POSITIONING)) {
            services.add("POSITIONING")
        }
        if (bluetoothClass.hasService(BluetoothClass.Service.RENDER)) {
            services.add("RENDER")
        }
        if (bluetoothClass.hasService(BluetoothClass.Service.TELEPHONY)) {
            services.add("TELEPHONY")
        }
    }
    val servicesString = if (services.isEmpty()) "None" else services.joinToString(", ")

    // Fetch UUIDs (service UUIDs) explicitly if possible (reflection if needed), will require async normally
    // For demonstration, call fetchUuidsWithSdp and show cached UUIDs:
    if (device.uuids == null) {
        try {
            device.fetchUuidsWithSdp()
            log("  Called fetchUuidsWithSdp to try to get more UUIDs (will need callback to receive them)")
        } catch (e: Exception) {
            log("  Error calling fetchUuidsWithSdp: ${e.message}")
        }
    }

    log(msg)
    log("BluetoothDevice Info:")
    log("  Name: $name")
    log("  Address: $address")
    log("  Type: $type")
    log("  Bond State: $bondState")
    log("  Bluetooth Class: $bluetoothClassObj")
    log("    Device Class: $deviceClassString")
    log("    Major Device Class: $majorDeviceClassString")
    log("    Raw Device Class Int: $deviceClass")
    log("    Raw Major Device Class Int: $majorDeviceClass")
    log("  Services Available: $servicesString")
    log("  UUIDs (from .uuids): $uuidsFromDeviceField")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        log("  Alias: ${device.alias}")
    }

}




}