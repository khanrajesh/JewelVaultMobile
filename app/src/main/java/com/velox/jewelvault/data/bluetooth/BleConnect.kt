package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.annotation.RequiresPermission
import com.velox.jewelvault.data.bluetooth.BleUtils.logDevice
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.permissions.hasConnectPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Class-based connector that encapsulates all dependencies via constructor.
 * Keep usage simple: create once, then call connect/tryClassicConnection.
 */
class BleConnect(
    private val context: Context,
    private val updateConnecting: (BluetoothDeviceDetails) -> Unit,
    private val removeConnecting: (String) -> Unit,
    private val removeConnected: (String) -> Unit,
    private val gattMap: MutableMap<String, BluetoothGatt>,
    private val manager: BleManager,
) {

    val rfcommSockets = mutableMapOf<String, BluetoothSocket>()
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private fun cLog(message: String) {
        log("BleConnect: $message")
    }

    // When true, lower-level connect functions must avoid auto restarting scans.
    // The orchestrator in connect() will decide when to restart scanning.
    private var suppressAutoRestartScan: Boolean = false
    private val attemptSignal: MutableMap<String, String?> = mutableMapOf()

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN])
    fun connect(
        address: String,
        onConnect: (BluetoothDeviceDetails) -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        // Cancel any existing connection job for this address
        manager.removeConnectionJob(address)
        
        // Launch new connection job and store it
        val connectionJob = manager.bleManagerScope.launch {
            try {
                performConnection(address, onConnect, onFailure)
            } finally {
                // Clean up job tracking when connection completes
                manager.removeConnectionJob(address)
            }
        }
        
        // Store the job for potential cancellation
        manager.storeConnectionJob(address, connectionJob)
    }
    
    private suspend fun performConnection(
        address: String,
        onConnect: (BluetoothDeviceDetails) -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        if (!hasConnectPermission(context)) {
            cLog("missing BLUETOOTH_CONNECT permission for $address")
            try { onFailure(SecurityException("Missing BLUETOOTH_CONNECT permission")) } catch (_: Throwable) {}
            return
        }

        manager.stopUnifiedScanning()

        val device = try {
            manager.bluetoothAdapter?.getRemoteDevice(address)
        } catch (t: Throwable) {
            cLog("resolve device failed for $address: ${t.message}")
            try { onFailure(t) } catch (_: Throwable) {}
            null
        } ?: return

        // Emit a generic CONNECTING state so UI can reflect immediately
        val connecting = manager.buildBluetoothDevice(device, address, "CONNECTING", device.name, null, null)
        updateConnecting(connecting)

        // Try all supported connection methods sequentially with per-step logging
        // This ensures we can connect to any type of device (BLE, Classic, Dual-mode)
        tryAllTransportsSequentially(device, onConnect, onFailure)
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN])
    private fun tryAllTransportsSequentially(
        device: BluetoothDevice,
        onConnect: (BluetoothDeviceDetails) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        logDevice("tryAllTransportsSequentially for ${device.address}",device)
        val address = device.address
        manager.bleManagerScope.launch {
            suppressAutoRestartScan = true
            try {
                val attempts: List<Pair<String, suspend () -> Unit>> = listOf(
                    "RFCOMM" to { connectViaRfcommInternal(device, onConnect, onFailure) },
//                    "GATT" to { connectGattInternal(device, onConnect, onFailure) }
                    // Removed other transport methods to avoid interference
                    // A2DP, HEADSET, HID are typically managed by the system
                )

                val total = attempts.size

                for ((idx, pair) in attempts.withIndex()) {
                    val name = pair.first

                    
                    // Stop immediately if device is already connected
                    if (manager.connectedDevices.value.any { it.address == address }) {
                        cLog("Device $address already connected; stopping further attempts")
                        removeConnecting(address)
                        manager.updateConnectedDevices()
                        return@launch
                    }
                    
                    // Clean up any existing connections before each attempt
                    cLog("Cleaning up existing connections before $name attempt for $address")
                    try {
                        // Clean up GATT
                        gattMap[address]?.let { gatt ->
                            try { gatt.disconnect() } catch (_: Throwable) {}
                            try { gatt.close() } catch (_: Throwable) {}
                            gattMap.remove(address)
                        }
                        
                        // Clean up RFCOMM
                        rfcommSockets[address]?.let { socket ->
                            try { socket.close() } catch (_: Throwable) {}
                            rfcommSockets.remove(address)
                        }
                        
                        // Remove from connecting devices
                        removeConnecting(address)
                    } catch (e: Throwable) {
                        cLog("Error during cleanup before $name attempt: ${e.message}")
                    }
                    
                    // Add small delay between attempts to allow cleanup
                    if (idx > 0) {
                        delay(500) // 500ms delay between attempts
                    }
                    
                    cLog("ATTEMPT_START $name for $address")
                    val startEvt = manager.buildBluetoothDevice(
                        device, address, "CONNECT_ATTEMPT_START_${name}", device.name, mapOf(
                            "connectionMethod" to name,
                            "methodLabel" to when (name) {
                                "RFCOMM" -> "Classic (RFCOMM)"
                                "GATT" -> "BLE (GATT)"
                                "A2DP" -> "Audio (A2DP)"
                                "HEADSET" -> "Headset"
                                "HID" -> "HID Host"
                                "LE_AUDIO" -> "LE Audio"
                                "MESH" -> "BLE Mesh"
                                "ADV_ONLY" -> "BLE Advertisement"
                                else -> name
                            },
                            "timeoutMs" to "30000",
                            "attempt" to (idx + 1).toString(),
                            "total" to total.toString()
                        ), null
                    )
                    updateConnecting(startEvt)

                    try {
                     pair.second()
                    } catch (t: Throwable) {
                        // Action invocation error shouldn't stop further attempts
                        cLog("ATTEMPT_ERROR $name for $address -> ${t.message}")
                        val excEvt = manager.buildBluetoothDevice(
                            device, address, "CONNECT_ATTEMPT_FAILED_${name}", device.name, mapOf(
                                "reason" to "exception",
                                "error" to (t.message ?: "unknown"),
                                "connectionMethod" to name,
                                "methodLabel" to when (name) {
                                    "RFCOMM" -> "Classic (RFCOMM)"
                                    "GATT" -> "BLE (GATT)"
                                    "A2DP" -> "Audio (A2DP)"
                                    "HEADSET" -> "Headset"
                                    "HID" -> "HID Host"
                                    "LE_AUDIO" -> "LE Audio"
                                    "MESH" -> "BLE Mesh"
                                    "ADV_ONLY" -> "BLE Advertisement"
                                    else -> name
                                },
                                "attempt" to (idx + 1).toString(),
                                "total" to total.toString()
                            ), null
                        )
                        updateConnecting(excEvt)
                    }

                    val timeoutMs = 30_000

                    var waited = 0
                    val step = 300
                    while (waited < timeoutMs) {
                        if (manager.connectedDevices.value.any { it.address == address }) break
                        if ((attemptSignal[address] ?: "").startsWith("error_")) break
                        delay(step.toLong())
                        waited += step
                    }

                    if (manager.connectedDevices.value.any { it.address == address }) {
                        cLog("ATTEMPT_SUCCESS $name for $address")
                        manager.buildBluetoothDevice(
                            device, address, "CONNECT_ATTEMPT_SUCCESS_${name}", device.name, mapOf(
                                "connectionMethod" to name, "methodLabel" to when (name) {
                                    "RFCOMM" -> "Classic (RFCOMM)"
                                    "GATT" -> "BLE (GATT)"
                                    else -> name
                                }, "attempt" to (idx + 1).toString(), "total" to total.toString()
                            ), null
                        )
                        removeConnecting(address)
                        manager.updateConnectedDevices()
                        
                        // IMPROVED: Better connection verification
                        try {
                            val connected = manager.connectedDevices.value.firstOrNull { it.address == address }
                            if (connected != null) {
                                cLog("Calling onConnect callback for verified connection: $address")
                                onConnect(connected)
                            } else {
                                cLog("WARNING: Device $address not found in connected devices after success")
                                // Try to find it in any device list
                                val anyDevice = manager.getDiscoveredClassicDevice(address)
                                    ?: manager.getDiscoveredLeDevice(address)
                                    ?: manager.getBondedDevice(address)
                                if (anyDevice != null) {
                                    cLog("Found device in other lists, calling onConnect")
                                    onConnect(anyDevice)
                                }
                            }
                        } catch (e: Throwable) {
                            cLog("Error calling onConnect callback: ${e.message}")
                        }
                        return@launch
                    } else {
                        cLog("ATTEMPT_FAILED $name for $address (timeout)")
                        val signal = attemptSignal[address]
                        
                        // Clean up failed attempt
                        cLog("Cleaning up failed $name attempt for $address")
                        try {
                            when (name) {
                                "RFCOMM" -> {
                                    rfcommSockets[address]?.let { socket ->
                                        try { socket.close() } catch (_: Throwable) {}
                                        rfcommSockets.remove(address)
                                    }
                                }
                                "GATT" -> {
                                    gattMap[address]?.let { gatt ->
                                        try { gatt.disconnect() } catch (_: Throwable) {}
                                        try { gatt.close() } catch (_: Throwable) {}
                                        gattMap.remove(address)
                                    }
                                }
                            }
                            removeConnecting(address)
                        } catch (e: Throwable) {
                            cLog("Error cleaning up failed $name attempt: ${e.message}")
                        }
                        
                        val failEvt = manager.buildBluetoothDevice(
                            device, address, "CONNECT_ATTEMPT_FAILED_${name}", device.name, mapOf(
                                "reason" to "timeout",
                                "connectionMethod" to name,
                                "methodLabel" to when (name) {
                                    "RFCOMM" -> "Classic (RFCOMM)"
                                    "GATT" -> "BLE (GATT)"
                                    "A2DP" -> "Audio (A2DP)"
                                    "HEADSET" -> "Headset"
                                    "HID" -> "HID Host"
                                    "LE_AUDIO" -> "LE Audio"
                                    "MESH" -> "BLE Mesh"
                                    "ADV_ONLY" -> "BLE Advertisement"
                                    else -> name
                                },
                                "timeoutMs" to "30000",
                                "attempt" to (idx + 1).toString(),
                                "total" to total.toString()
                            ).let { base ->
                                signal?.let { s -> base + ("signal" to s) } ?: base
                            }, null, "TIMEOUT"
                        )
                        updateConnecting(failEvt)
                    }
                }
            } finally {
                suppressAutoRestartScan = false
                if (!manager.connectedDevices.value.any { it.address == address }) {
                    removeConnecting(address)
                    manager.buildBluetoothDevice(
                        device, address, "CONNECT_FAILED_ALL", device.name, null, null
                    )
                    manager.updateConnectedDevices()
                    try { onFailure(Throwable("All connection attempts failed for $address")) } catch (_: Throwable) {}
                    try {
                        manager.startUnifiedScanning()
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    // Public unified disconnect that routes based on active transport
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect(address: String) {
        cLog("disconnect requested for $address")
        // Always try to disconnect all known transports in a safe order
        try {
            if (rfcommSockets.containsKey(address)) disconnectRfcommInternal(address)
        } catch (_: Throwable) {
        }
        try {
            if (gattMap.containsKey(address)) disconnectGattInternal(address)
        } catch (_: Throwable) {
        }
        try {
            disconnectA2dpInternal(address)
        } catch (_: Throwable) {
        }
        try {
            disconnectHeadsetInternal(address)
        } catch (_: Throwable) {
        }
        try {
            disconnectHidHostInternal(address)
        } catch (_: Throwable) {
        }
        // Ensure connecting/connected lists are cleared
        removeConnecting(address)
        removeConnected(address)
    }

    // ----------------- Classic RFCOMM (SPP) -----------------
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN])
    private fun connectViaRfcommInternal(
        device: BluetoothDevice, 
        onConnect: (BluetoothDeviceDetails) -> Unit,
        onFailure: (Throwable) -> Unit,
        uuid: UUID = SPP_UUID
    ) {
        if (!hasConnectPermission(context)) {
            log("RFCOMM: missing BLUETOOTH_CONNECT permission for ${device.address}")
            return
        }

        val address = device.address

        if (rfcommSockets.containsKey(address)) {
            log("RFCOMM: cleaning up existing socket for $address before new attempt")
            try {
                rfcommSockets[address]?.close()
            } catch (e: Throwable) {
                log("Error closing existing RFCOMM socket: ${e.message}")
            }
            rfcommSockets.remove(address)
        }

        manager.bleManagerScope.launch(Dispatchers.IO) {
            try {
                // If not bonded yet, initiate bonding and defer connect to bond callback
                val bonded = try { device.bondState == BluetoothDevice.BOND_BONDED } catch (_: Throwable) { false }
                if (!bonded) {
                    log("RFCOMM: device ${device.address} not bonded, initiating bonding and deferring RFCOMM connect")
                    // show connecting placeholder with method
                    updateConnecting(manager.buildBluetoothDevice(
                        device, address, "CONNECTING_RFCOMM_WAIT_BOND", device.name, mapOf(
                            "connectionMethod" to "RFCOMM",
                            "note" to "waiting_bond"
                        ), null, "CONNECTING"
                    ))
                    tryCreateBondIfNeeded(device)
                    return@launch
                }

                // Cancel discovery which slows down a connection
                try {
                    manager.bluetoothAdapter?.cancelDiscovery()
                } catch (_: Throwable) {
                }

                // Attempt secure RFCOMM first
                var connected = false
                var lastError: Throwable? = null
                try {
                    val socket = device.createRfcommSocketToServiceRecord(uuid)
                    log("RFCOMM: connecting SECURE socket to $address uuid=$uuid")
                    socket.connect()
                    rfcommSockets[address] = socket
                    connected = true
                } catch (t1: Throwable) {
                    lastError = t1
                    try { manager.bluetoothAdapter?.cancelDiscovery() } catch (_: Throwable) {}
                    // Attempt insecure RFCOMM
                    try {
                        val insecure = device.createInsecureRfcommSocketToServiceRecord(uuid)
                        log("RFCOMM: connecting INSECURE socket to $address uuid=$uuid")
                        insecure.connect()
                        rfcommSockets[address] = insecure
                        connected = true
                    } catch (t2: Throwable) {
                        lastError = t2
                        try { manager.bluetoothAdapter?.cancelDiscovery() } catch (_: Throwable) {}
                        // Reflection fallback on channel 1
                        try {
                            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
                            val refSock = m.invoke(device, 1) as android.bluetooth.BluetoothSocket
                            log("RFCOMM: connecting REFLECTION socket (channel 1) to $address")
                            refSock.connect()
                            rfcommSockets[address] = refSock
                            connected = true
                        } catch (t3: Throwable) {
                            lastError = t3
                        }
                    }
                }

                if (!connected) throw lastError ?: RuntimeException("RFCOMM connect failed")

                val evt = manager.buildBluetoothDevice(
                    device, address, "CLASSIC_RFCOMM_CONNECTED", device.name, mapOf(
                        "connectionMethod" to "RFCOMM",
                        "connectionTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth"
                    ), null, "CONNECTED_RFCOMM"
                )
                removeConnecting(address)
                manager.updateConnectedDevices(evt)
                
                log("RFCOMM: connected to $address")
                
                // CRITICAL FIX: Call onConnect callback for RFCOMM success
                try {
                    log("Calling onConnect callback for successful RFCOMM connection: $address")
                    onConnect(evt)
                } catch (e: Throwable) {
                    log("Error calling onConnect callback for RFCOMM: ${e.message}")
                }
                
                // Already bonded before connecting; no extra bonding here
            } catch (t: Throwable) {
                log("RFCOMM: connect failed for $address -> ${t.message}")
                try {
                    rfcommSockets.remove(address)?.close()
                } catch (_: Throwable) {
                }
                manager.buildBluetoothDevice(
                    device,
                    address,
                    "CLASSIC_RFCOMM_CONNECT_FAILED",
                    device.name,
                    mapOf(
                        "error" to (t.message ?: "unknown"),
                        "connectionMethod" to "RFCOMM",
                        "errorTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth"
                    ),
                    null,
                    "ERROR_RFCOMM"
                )
                if (!suppressAutoRestartScan) manager.startUnifiedScanning()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectRfcommInternal(address: String) {
        manager.bleManagerScope.launch(Dispatchers.IO) {
            try {
                rfcommSockets.remove(address)?.close()
                manager.buildBluetoothDevice(
                    null, address, "CLASSIC_RFCOMM_DISCONNECTED", null, mapOf(
                        "connectionMethod" to "RFCOMM",
                        "disconnectTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth"
                    ), null, "DISCONNECTED_RFCOMM"
                )
                removeConnected(address)
                manager.updateConnectedDevices()
            } catch (t: Throwable) {
                log("RFCOMM: disconnect error for $address -> ${t.message}")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectAllRfcommInternal() {
        val keys = rfcommSockets.keys.toList()
        keys.forEach { disconnectRfcommInternal(it) }
    }

    // ----------------- Classic A2DP (Audio) and Headset -----------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectA2dpInternal(device: BluetoothDevice) {
        withProfile(BluetoothProfile.A2DP) { proxy, dev ->
            val ok = tryInvokeConnect(proxy, dev)
            cLog("A2DP connect(${dev.address}) result=$ok")
            if (ok) {
                val evt = manager.buildBluetoothDevice(
                    dev, dev.address, "AUDIO_CONNECTED", dev.name, mapOf(
                        "connectionMethod" to "A2DP",
                        "connectionTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth Audio"
                    ), null, "CONNECTED_A2DP"
                )
                manager.updateConnectedDevices(evt)
            } else {
                manager.buildBluetoothDevice(
                    dev, dev.address, "A2DP_CONNECT_FAILED", dev.name, mapOf(
                        "connectionMethod" to "A2DP",
                        "errorTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth Audio"
                    ), null, "ERROR_A2DP"
                )
            }
        }(device.address)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectHeadsetInternal(device: BluetoothDevice) {
        withProfile(BluetoothProfile.HEADSET) { proxy, dev ->
            val ok = tryInvokeConnect(proxy, dev)
            cLog("HEADSET connect(${dev.address}) result=$ok")
            if (ok) {
                val evt = manager.buildBluetoothDevice(
                    dev, dev.address, "HEADSET_CONNECTED", dev.name, mapOf(
                        "connectionMethod" to "HEADSET",
                        "connectionTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth Headset"
                    ), null, "CONNECTED_HEADSET"
                )
                manager.updateConnectedDevices(evt)
            } else {
                manager.buildBluetoothDevice(
                    dev, dev.address, "HEADSET_CONNECT_FAILED", dev.name, mapOf(
                        "connectionMethod" to "HEADSET",
                        "errorTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth Headset"
                    ), null, "ERROR_HEADSET"
                )
            }
        }(device.address)
    }

    // Disconnections for classic audio profiles
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectA2dpInternal(address: String) {
        withProfile(BluetoothProfile.A2DP) { proxy, dev ->
            val ok = tryInvokeDisconnect(proxy, dev)
            cLog("A2DP disconnect(${dev.address}) result=$ok")
            if (ok) {
                manager.buildBluetoothDevice(
                    dev, address, "AUDIO_DISCONNECTED", dev.name, mapOf(
                        "connectionMethod" to "A2DP",
                        "disconnectTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth Audio"
                    ), null, "DISCONNECTED_A2DP"
                )
                removeConnected(address)
                manager.updateConnectedDevices()
            }
        }(address)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectHeadsetInternal(address: String) {
        withProfile(BluetoothProfile.HEADSET) { proxy, dev ->
            val ok = tryInvokeDisconnect(proxy, dev)
            cLog("HEADSET disconnect(${dev.address}) result=$ok")
            if (ok) {
                manager.buildBluetoothDevice(
                    dev, address, "HEADSET_DISCONNECTED", dev.name, mapOf(
                        "connectionMethod" to "HEADSET",
                        "disconnectTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth Headset"
                    ), null, "DISCONNECTED_HEADSET"
                )
                removeConnected(address)
                manager.updateConnectedDevices()
            }
        }(address)
    }

    // ----------------- Classic HID Host (keyboards/mice) -----------------
    // Note: Uses profile ID 4 which corresponds to HID Host on AOSP. This may be
    // restricted on some devices and may require system apps/privileges.
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectHidHostInternal(device: BluetoothDevice) {
        withProfile( /* HID_HOST */ 4) { proxy, dev ->
            val ok = tryInvokeConnect(proxy, dev)
            cLog("HID_HOST connect(${dev.address}) result=$ok")
            if (ok) {
                val evt = manager.buildBluetoothDevice(
                    dev, dev.address, "HID_HOST_CONNECTED", dev.name, mapOf(
                        "connectionMethod" to "HID",
                        "connectionTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth HID"
                    ), null, "CONNECTED_HID"
                )
                manager.updateConnectedDevices(evt)
            } else {
                manager.buildBluetoothDevice(
                    dev, dev.address, "HID_HOST_CONNECT_FAILED", dev.name, mapOf(
                        "connectionMethod" to "HID",
                        "errorTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth HID"
                    ), null, "ERROR_HID"
                )
            }
        }(device.address)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectHidHostInternal(address: String) {
        withProfile( /* HID_HOST */ 4) { proxy, dev ->
            val ok = tryInvokeDisconnect(proxy, dev)
            cLog("HID_HOST disconnect(${dev.address}) result=$ok")
            if (ok) {
                manager.buildBluetoothDevice(
                    dev, address, "HID_HOST_DISCONNECTED", dev.name, mapOf(
                        "connectionMethod" to "HID",
                        "disconnectTime" to System.currentTimeMillis().toString(),
                        "transportType" to "Classic Bluetooth HID"
                    ), null, "DISCONNECTED_HID"
                )
                removeConnected(address)
                manager.updateConnectedDevices()
            }
        }(address)
    }


    // ----------------- Helpers -----------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun withProfile(
        profileId: Int, block: (proxy: Any, device: BluetoothDevice) -> Unit
    ): (String) -> Unit = { address ->
        if (!hasConnectPermission(context)) {
            cLog("PROFILE:$profileId missing BLUETOOTH_CONNECT permission for $address")
        } else {
            val device = try {
                manager.bluetoothAdapter?.getRemoteDevice(address)
            } catch (_: Throwable) {
                null
            }
            if (device == null) {
                cLog("PROFILE:$profileId could not resolve device for $address")
            } else {
                try {
                    manager.bluetoothAdapter?.getProfileProxy(
                        context, object : BluetoothProfile.ServiceListener {
                            override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                                try {
                                    block(proxy, device)
                                } catch (t: Throwable) {
                                    cLog("PROFILE:$p block error -> ${t.message}")
                                } finally {
                                    try {
                                        manager.bluetoothAdapter?.closeProfileProxy(p, proxy)
                                    } catch (_: Throwable) {
                                    }
                                }
                            }

                            override fun onServiceDisconnected(p: Int) {
                                // no-op
                            }
                        }, profileId
                    )
                } catch (t: Throwable) {
                    cLog("PROFILE:$profileId getProfileProxy error -> ${t.message}")
                }
            }
        }
    }

    private fun tryInvokeConnect(proxy: Any, device: BluetoothDevice): Boolean {
        val methodNames = listOf("connect", "connectDevice")
        for (name in methodNames) {
            try {
                val m = proxy.javaClass.getMethod(name, BluetoothDevice::class.java)
                m.isAccessible = true
                val res = m.invoke(proxy, device)
                if (res is Boolean) return res
                return true
            } catch (_: NoSuchMethodException) {
                // try next
            } catch (t: Throwable) {
                cLog("REFLECT ${proxy.javaClass.simpleName}.$name failed -> ${t.message}")
            }
        }
        return false
    }

    private fun tryInvokeDisconnect(proxy: Any, device: BluetoothDevice): Boolean {
        val methodNames = listOf("disconnect", "disconnectDevice")
        for (name in methodNames) {
            try {
                val m = proxy.javaClass.getMethod(name, BluetoothDevice::class.java)
                m.isAccessible = true
                val res = m.invoke(proxy, device)
                if (res is Boolean) return res
                return true
            } catch (_: NoSuchMethodException) {
                // try next
            } catch (t: Throwable) {
                cLog("REFLECT ${proxy.javaClass.simpleName}.$name failed -> ${t.message}")
            }
        }
        return false
    }

    // GATT disconnection internal
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectGattInternal(address: String) {
        val gatt = gattMap[address]
        if (gatt != null) {
            try {
                gatt.disconnect()
            } catch (_: Throwable) {
            }
            try {
                gatt.close()
            } catch (_: Throwable) {
            }
            gattMap.remove(address)
            manager.buildBluetoothDevice(null, address, "GATT_DISCONNECTED", null, mapOf(
                "connectionMethod" to "GATT",
                "disconnectTime" to System.currentTimeMillis().toString(),
                "transportType" to "BLE (GATT)"
            ), null, "DISCONNECTED_GATT")
            removeConnected(address)
            removeConnecting(address)
            manager.updateConnectedDevices()
        }
    }

    // ----------------- Routing helpers -----------------

    private inline fun <T> safe(block: () -> T): T? = try {
        block()
    } catch (_: Throwable) {
        null
    }

    private fun gattStatusName(code: Int): String = when (code) {
        BluetoothGatt.GATT_SUCCESS -> "GATT_SUCCESS"
        0x01 -> "GATT_INVALID_HANDLE"
        0x02 -> "GATT_READ_NOT_PERMITTED"
        0x03 -> "GATT_WRITE_NOT_PERMITTED"
        0x04 -> "GATT_INVALID_PDU"
        0x05 -> "GATT_INSUFFICIENT_AUTHENTICATION"
        0x06 -> "GATT_REQUEST_NOT_SUPPORTED"
        0x07 -> "GATT_INVALID_OFFSET"
        0x08 -> "GATT_INSUFFICIENT_AUTHORIZATION"
        0x09 -> "GATT_PREPARE_QUEUE_FULL"
        0x0A -> "GATT_ATTRIBUTE_NOT_FOUND"
        0x0B -> "GATT_ATTRIBUTE_NOT_LONG"
        0x0C -> "GATT_INSUFFICIENT_ENCRYPTION_KEY_SIZE"
        0x0D -> "GATT_INVALID_ATTRIBUTE_VALUE_LENGTH"
        0x0E -> "GATT_UNLIKELY_ERROR"
        0x0F -> "GATT_INSUFFICIENT_ENCRYPTION"
        0x10 -> "GATT_UNSUPPORTED_GROUP_TYPE"
        0x11 -> "GATT_INSUFFICIENT_RESOURCES"
        0x80 -> "GATT_NO_RESOURCES"
        0x81 -> "GATT_INTERNAL_ERROR"
        0x85 -> "GATT_WRONG_STATE"
        0x87 -> "GATT_DB_FULL"
        0x88 -> "GATT_BUSY"
        0x89 -> "GATT_ERROR"
        0x8A -> "GATT_CMD_STARTED"
        0x8B -> "GATT_ILLEGAL_PARAMETER"
        0x8F -> "GATT_NO_ADV_IN_PROGRESS"
        0x90 -> "GATT_STACK_TIMEOUT"
        0x91 -> "GATT_ALREADY_OPEN"
        0x92 -> "GATT_CANCEL"
        0x101 -> "GATT_CONN_L2C_FAILURE"
        0x103 -> "GATT_CONN_TIMEOUT"
        0x104 -> "GATT_CONN_TERMINATE_PEER_USER"
        0x105 -> "GATT_CONN_TERMINATE_LOCAL_HOST"
        0x106 -> "GATT_CONN_FAIL_ESTABLISH"
        0x107 -> "GATT_CONN_LMP_TIMEOUT"
        0x109 -> "GATT_CONN_CANCEL"
        0x133 -> "GATT_CONN_ERROR"
        else -> "UNKNOWN_$code"
    }

    private fun gattStatusCategory(code: Int): String = when (code) {
        BluetoothGatt.GATT_SUCCESS -> "SUCCESS"
        0x88 /* GATT_BUSY */, 0x90 /* GATT_STACK_TIMEOUT */, 0x103 /* GATT_CONN_TIMEOUT */, 0x8A /* GATT_CMD_STARTED */ -> "TRANSIENT"
        0x05 /* INSUFFICIENT_AUTHENTICATION */, 0x0C /* INSUFFICIENT_ENCRYPTION_KEY_SIZE */, 0x0F /* INSUFFICIENT_ENCRYPTION */, 0x08 /* INSUFFICIENT_AUTHORIZATION */ -> "AUTH"
        0x02 /* READ_NOT_PERMITTED */, 0x03 /* WRITE_NOT_PERMITTED */, 0x06 /* REQ_NOT_SUPPORTED */ -> "NOT_SUPPORTED"
        0x81 /* INTERNAL_ERROR */, 0x101 /* L2C_FAILURE */, 0x104 /* TERM_PEER_USER */, 0x105 /* TERM_LOCAL_HOST */, 0x106 /* FAIL_ESTABLISH */ -> "LINK_ERROR"
        else -> "OTHER"
    }

    private fun gattStateName(state: Int): String = when (state) {
        BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
        BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
        BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
        else -> "STATE_$state"
    }

    // add this field to your class (top of the class)
    private val reconnectAttempts = mutableMapOf<String, Int>()

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    private fun connectGattInternal(
        device: BluetoothDevice,
        onConnect: (BluetoothDeviceDetails) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val address = device.address

        // mark as connecting for UI/logic (you already use updateConnecting/removeConnecting)
        cLog("GATT starting connection process for device: $address (${device.name})")
        val connectingEvt = manager.buildBluetoothDevice(
            device, address, "GATT_CONNECTING", device.name, mapOf(
                "connectionMethod" to "GATT",
                "transportType" to "BLE (GATT)",
                "timestamp" to System.currentTimeMillis().toString()
            ), null, "CONNECTING_GATT"
        )

        // Clean up any existing GATT connection before attempting new one
        val existingGatt = gattMap[address]
        if (existingGatt != null) {
            cLog("Cleaning up existing GATT connection for $address before new attempt")
            try {
                existingGatt.disconnect()
                existingGatt.close()
            } catch (e: Throwable) {
                cLog("Error cleaning up existing GATT: ${e.message}")
            }
            gattMap.remove(address)
        }
        
        // Remove from connecting devices to allow fresh attempt
        removeConnecting(address)



        updateConnecting(connectingEvt)

        // Determine the best transport mode based on device type
        val transportMode = when {
            // If device has BLE service UUIDs, prefer LE transport
            device.uuids?.any { uuid ->
                uuid?.uuid?.toString()?.let { uuidStr ->
                    uuidStr.contains("0000180", ignoreCase = true) || // BLE services
                    uuidStr.contains("df21fe2c", ignoreCase = true) // Custom BLE UUID
                } ?: false
            } == true -> {
                cLog("Device $address has BLE services, using TRANSPORT_LE")
                BluetoothDevice.TRANSPORT_LE
            }
            // For dual-mode or unknown devices, use AUTO to let system decide
            else -> {
                cLog("Device $address using TRANSPORT_AUTO for compatibility")
                BluetoothDevice.TRANSPORT_AUTO
            }
        }

        // call connectGatt with appropriate transport mode
        cLog("GATT attempting connection to $address with transport mode: $transportMode")
        val gatt = try {
            device.connectGatt(context, false, object : BluetoothGattCallback() {

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    super.onConnectionStateChange(gatt, status, newState)
                    val addr = gatt.device.address

                    val statusName = gattStatusName(status)
                    val statusCategory = gattStatusCategory(status)
                    val stateName = gattStateName(newState)
                    
                    cLog("=== GATT CONNECTION STATE CHANGE ===")
                    cLog("Device: $addr (${gatt.device.name})")
                    cLog("New State: $stateName ($newState)")
                    cLog("Status: $statusName ($status)")
                    cLog("Category: $statusCategory")
                    cLog("Transport Mode Used: $transportMode")
                    cLog("=====================================")

                    // Build state change event with comprehensive state info
                    val stateChangeEvt = manager.buildBluetoothDevice(
                        gatt.device, addr, "GATT_STATE_CHANGE", gatt.device.name, mapOf(
                            "stateCode" to newState.toString(),
                            "status" to statusName,
                            "statusCode" to status.toString(),
                            "category" to statusCategory,
                            "transportMode" to transportMode.toString(),
                            "timestamp" to System.currentTimeMillis().toString()
                        ), null, stateName
                    )
                    updateConnecting(stateChangeEvt)

                    // Handle success transitions first
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTING -> {
                                cLog("✅ GATT CONNECTING SUCCESS -> $addr")
                                val evt = manager.buildBluetoothDevice(
                                    gatt.device, addr, "GATT_CONNECTING", gatt.device.name, mapOf(
                                        "status" to statusName,
                                        "statusCode" to status.toString(),
                                        "transportMode" to transportMode.toString(),
                                        "timestamp" to System.currentTimeMillis().toString(),
                                        "connectionMethod" to "GATT",
                                        "transportType" to "BLE (GATT)"
                                    ), null, "CONNECTING_GATT"
                                )
                                updateConnecting(evt)
                            }

                            BluetoothProfile.STATE_CONNECTED -> {
                                cLog("🎉 GATT CONNECTED SUCCESS -> $addr")
                                cLog("Moving device from connecting to connected state")
                                
                                // Mark connected: move from connecting -> connected
                                gattMap[addr] = gatt
                                removeConnecting(addr)
                                
                                val evt = manager.buildBluetoothDevice(
                                    gatt.device, addr, "GATT_CONNECTED", gatt.device.name, mapOf(
                                        "status" to statusName,
                                        "statusCode" to status.toString(),
                                        "transportMode" to transportMode.toString(),
                                        "connectionTime" to System.currentTimeMillis().toString(),
                                        "servicesDiscovered" to "false",
                                        "connectionMethod" to "GATT",
                                        "transportType" to "BLE (GATT)"
                                    ), null, "CONNECTED_GATT"
                                )
                                manager.updateConnectedDevices(evt)
                                
                                // reset reconnect attempts on success
                                reconnectAttempts.remove(addr)
                                cLog("Reset reconnect attempts for $addr")

                                // CRITICAL FIX: Call onConnect callback immediately
                                try {
                                    cLog("Calling onConnect callback for successful GATT connection: $addr")
                                    onConnect(evt)
                                } catch (e: Throwable) {
                                    cLog("Error calling onConnect callback: ${e.message}")
                                }

                                // Initiate bonding if needed, and only then discover services in callback
                                if (hasConnectPermission(context)) {
                                    cLog("Initiating bonding process for $addr")
                                    tryCreateBondIfNeeded(gatt.device) {
                                        val ok = try { 
                                            gatt.discoverServices() 
                                            cLog("🔍 Starting service discovery for $addr")
                                            true
                                        } catch (e: Throwable) { 
                                            cLog("❌ Service discovery failed for $addr: ${e.message}")
                                            false 
                                        }
                                        cLog("GATT discoverServices ${if (ok) "started successfully" else "failed to start"} -> $addr")
                                    }
                                } else {
                                    cLog("⚠️ No connect permission, attempting service discovery anyway")
                                    // no permission: still try discoverServices to keep behavior consistent
                                    try { 
                                        gatt.discoverServices() 
                                        cLog("🔍 Service discovery started without permission for $addr")
                                    } catch (e: Throwable) { 
                                        cLog("❌ Service discovery failed due to permission for $addr: ${e.message}") 
                                    }
                                }
                            }

                            BluetoothProfile.STATE_DISCONNECTING -> {
                                cLog("🔄 GATT DISCONNECTING -> $addr")
                                val evt = manager.buildBluetoothDevice(
                                    gatt.device,
                                    addr,
                                    "GATT_DISCONNECTING",
                                    gatt.device.name,
                                    mapOf(
                                        "status" to statusName,
                                        "statusCode" to status.toString(),
                                        "transportMode" to transportMode.toString(),
                                        "timestamp" to System.currentTimeMillis().toString(),
                                        "connectionMethod" to "GATT",
                                        "transportType" to "BLE (GATT)"
                                    ),
                                    null,
                                    "DISCONNECTING_GATT"
                                )
                                updateConnecting(evt)
                            }

                            BluetoothProfile.STATE_DISCONNECTED -> {
                                cLog("❌ GATT DISCONNECTED -> $addr")
                                cLog("Cleaning up resources for disconnected device")
                                
                                try { gatt.close() } catch (e: Throwable) { cLog("Error closing GATT: ${e.message}") }
                                gattMap.remove(addr)
                                removeConnecting(addr)
                                removeConnected(addr)
                                attemptSignal[addr] = "error_$statusName"
                                
                                val evt = manager.buildBluetoothDevice(
                                    gatt.device, addr, "GATT_DISCONNECTED", gatt.device.name, mapOf(
                                        "status" to statusName,
                                        "statusCode" to status.toString(),
                                        "transportMode" to transportMode.toString(),
                                        "disconnectTime" to System.currentTimeMillis().toString(),
                                        "reason" to "normal_disconnect",
                                        "connectionMethod" to "GATT",
                                        "transportType" to "BLE (GATT)"
                                    ), null, "DISCONNECTED_GATT"
                                )
                                manager.updateConnectedDevices(evt)
                                cLog("Device $addr removed from all connection lists")
                            }
                        }
                        return
                    }

                    // Non-success statuses: handle aggressively for common codes (133 etc.)
                    cLog("❌ GATT CONNECTION FAILED - Status: $statusName ($status)")
                    when (status) {
                        133 -> { // GATT_ERROR: generic Android BLE stack error
                            cLog("🚨 CRITICAL: GATT_ERROR(133) for $addr")
                            cLog("This is a generic Android BLE stack error - cleaning up and scheduling reconnect")
                            
                            // Close & cleanup immediately
                            try { gatt.close() } catch (e: Throwable) { cLog("Error closing GATT during cleanup: ${e.message}") }
                            gattMap.remove(addr)
                            removeConnecting(addr)
                            removeConnected(addr)
                            attemptSignal[addr] = "error_${gattStatusName(status)}"
                            
                            val evt = manager.buildBluetoothDevice(
                                gatt.device, addr, "GATT_DISCONNECTED_ERROR", gatt.device.name,
                                mapOf(
                                    "status" to statusName,
                                    "statusCode" to status.toString(),
                                    "transportMode" to transportMode.toString(),
                                    "errorTime" to System.currentTimeMillis().toString(),
                                    "reason" to "gatt_error_133",
                                    "willRetry" to "true",
                                    "connectionMethod" to "GATT",
                                    "transportType" to "BLE (GATT)"
                                ), null, "ERROR_GATT"
                            )
                            manager.updateConnectedDevices(evt)

                            // Wait before retry to allow resources to be freed
                            manager.bleManagerScope.launch {
                                cLog("⏳ Waiting 1 second before retry for $addr")
                                delay(1000) // Wait 1 second before retry
                                scheduleReconnect(gatt.device)
                            }
                            return
                        }

                        BluetoothGatt.GATT_CONNECTION_TIMEOUT -> {
                            cLog("⏰ CONNECTION TIMEOUT for $addr (status=$statusName)")
                            cLog("Cleaning up and scheduling retry")
                            
                            try { gatt.close() } catch (e: Throwable) { cLog("Error closing GATT during timeout cleanup: ${e.message}") }
                            gattMap.remove(addr)
                            removeConnecting(addr)
                            removeConnected(addr)
                            attemptSignal[addr] = "error_$statusName"
                            
                            val evt = manager.buildBluetoothDevice(
                                gatt.device, addr, "GATT_CONNECTION_TIMEOUT", gatt.device.name,
                                mapOf(
                                    "status" to statusName,
                                    "statusCode" to status.toString(),
                                    "transportMode" to transportMode.toString(),
                                    "timeoutTime" to System.currentTimeMillis().toString(),
                                    "reason" to "connection_timeout",
                                    "willRetry" to "true",
                                    "connectionMethod" to "GATT",
                                    "transportType" to "BLE (GATT)"
                                ), null, "TIMEOUT_GATT"
                            )
                            manager.updateConnectedDevices(evt)
                            scheduleReconnect(gatt.device)
                            return
                        }

                        5, 0x0F -> { // INSUFFICIENT_AUTHENTICATION / INSUFFICIENT_ENCRYPTION
                            cLog("🔐 AUTHENTICATION/ENCRYPTION REQUIRED for $addr ($statusName)")
                            cLog("Triggering bonding process then retry")
                            
                            try { gatt.close() } catch (e: Throwable) { cLog("Error closing GATT during auth cleanup: ${e.message}") }
                            gattMap.remove(addr)
                            removeConnecting(addr)
                            removeConnected(addr)
                            attemptSignal[addr] = "error_$statusName"
                            
                            val evt = manager.buildBluetoothDevice(
                                gatt.device, addr, "GATT_AUTH_REQUIRED", gatt.device.name,
                                mapOf(
                                    "status" to statusName,
                                    "statusCode" to status.toString(),
                                    "transportMode" to transportMode.toString(),
                                    "authTime" to System.currentTimeMillis().toString(),
                                    "reason" to "insufficient_auth_encryption",
                                    "willRetry" to "true",
                                    "connectionMethod" to "GATT",
                                    "transportType" to "BLE (GATT)"
                                ), null, "AUTH_REQUIRED_GATT"
                            )
                            manager.updateConnectedDevices(evt)
                            
                            // trigger bonding flow and reconnect when bonded
                            tryCreateBondIfNeeded(gatt.device) {
                                scheduleReconnect(gatt.device)
                            }
                            return
                        }

                        62 -> { // GATT_CONN_FAIL_ESTABLISH
                            cLog("🔌 CONNECTION FAILED TO ESTABLISH for $addr (status=$statusName)")
                            cLog("Scheduling reconnect attempt")
                            
                            try { gatt.close() } catch (e: Throwable) { cLog("Error closing GATT during establish failure cleanup: ${e.message}") }
                            gattMap.remove(addr)
                            removeConnecting(addr)
                            removeConnected(addr)
                            attemptSignal[addr] = "error_$statusName"
                            
                            val evt = manager.buildBluetoothDevice(
                                gatt.device, addr, "GATT_CONN_FAIL_ESTABLISH", gatt.device.name,
                                mapOf(
                                    "status" to statusName,
                                    "statusCode" to status.toString(),
                                    "transportMode" to transportMode.toString(),
                                    "failTime" to System.currentTimeMillis().toString(),
                                    "reason" to "connection_failed_establish",
                                    "willRetry" to "true",
                                    "connectionMethod" to "GATT",
                                    "transportType" to "BLE (GATT)"
                                ), null, "CONNECTION_FAILED_GATT"
                            )
                            manager.updateConnectedDevices(evt)
                            scheduleReconnect(gatt.device)
                            return
                        }

                        else -> {
                            cLog("❓ UNKNOWN GATT ERROR for $addr: ${gattStatusName(status)} ($status)")
                            cLog("Cleaning up resources")
                            
                            try { gatt.close() } catch (e: Throwable) { cLog("Error closing GATT during unknown error cleanup: ${e.message}") }
                            gattMap.remove(addr)
                            removeConnecting(addr)
                            removeConnected(addr)
                            attemptSignal[addr] = "error_${gattStatusName(status)}"
                            
                            val evt = manager.buildBluetoothDevice(
                                gatt.device, addr, "GATT_UNKNOWN_ERROR", gatt.device.name,
                                mapOf(
                                    "status" to statusName,
                                    "statusCode" to status.toString(),
                                    "transportMode" to transportMode.toString(),
                                    "errorTime" to System.currentTimeMillis().toString(),
                                    "reason" to "unknown_gatt_error",
                                    "willRetry" to "false",
                                    "connectionMethod" to "GATT",
                                    "transportType" to "BLE (GATT)"
                                ), null, "UNKNOWN_ERROR_GATT"
                            )
                            manager.updateConnectedDevices(evt)

                            // Optional: retry for some other codes if you want (e.g., transient)
                            when (status) {
                                0x88, 0x90, 0x8A /* transient-like */ -> {
                                    cLog("🔄 Transient error detected, scheduling retry")
                                    scheduleReconnect(gatt.device)
                                }
                                else -> {
                                    cLog("❌ Non-transient error, no retry scheduled")
                                }
                            }
                            return
                        }
                    }
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    val addr = gatt.device.address
                    val serviceCount = gatt.services?.size ?: 0
                    val statusName = gattStatusName(status)
                    
                    cLog("🔍 GATT SERVICES DISCOVERED")
                    cLog("Device: $addr (${gatt.device.name})")
                    cLog("Status: $statusName ($status)")
                    cLog("Services Found: $serviceCount")
                    
                    if (serviceCount > 0) {
                        cLog("Available Services:")
                        gatt.services?.forEachIndexed { index, service ->
                            cLog("  [$index] ${service.uuid} - ${service.type}")
                        }
                    }
                    
                    val evt = manager.buildBluetoothDevice(
                        gatt.device, addr, "GATT_SERVICES_DISCOVERED", gatt.device.name, mapOf(
                            "status" to statusName,
                            "statusCode" to status.toString(),
                            "serviceCount" to serviceCount.toString(),
                            "discoveryTime" to System.currentTimeMillis().toString(),
                            "servicesAvailable" to if (serviceCount > 0) "true" else "false",
                            "connectionMethod" to "GATT",
                            "transportType" to "BLE (GATT)"
                        ), null, "SERVICES_DISCOVERED_GATT"
                    )
                    manager.updateConnectedDevices(evt)
                }

                override fun onPhyUpdate(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
                    super.onPhyUpdate(gatt, txPhy, rxPhy, status)
                    val addr = gatt.device.address
                    val statusName = gattStatusName(status)
                    
                    cLog("📡 GATT PHY UPDATE")
                    cLog("Device: $addr")
                    cLog("TX PHY: $txPhy, RX PHY: $rxPhy")
                    cLog("Status: $statusName ($status)")
                    
                    val evt = manager.buildBluetoothDevice(
                        gatt.device, addr, "GATT_PHY_UPDATE", gatt.device.name, mapOf(
                            "txPhy" to txPhy.toString(),
                            "rxPhy" to rxPhy.toString(),
                            "status" to statusName,
                            "statusCode" to status.toString(),
                            "updateTime" to System.currentTimeMillis().toString(),
                            "connectionMethod" to "GATT",
                            "transportType" to "BLE (GATT)"
                        ), null, "PHY_UPDATE_GATT"
                    )
                    manager.updateConnectedDevices(evt)
                }

                override fun onPhyRead(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
                    super.onPhyRead(gatt, txPhy, rxPhy, status)
                    val addr = gatt.device.address
                    val statusName = gattStatusName(status)
                    
                    cLog("📖 GATT PHY READ")
                    cLog("Device: $addr")
                    cLog("TX PHY: $txPhy, RX PHY: $rxPhy")
                    cLog("Status: $statusName ($status)")
                    
                    val evt = manager.buildBluetoothDevice(
                        gatt.device, addr, "GATT_PHY_READ", gatt.device.name, mapOf(
                            "txPhy" to txPhy.toString(),
                            "rxPhy" to rxPhy.toString(),
                            "status" to statusName,
                            "statusCode" to status.toString(),
                            "readTime" to System.currentTimeMillis().toString(),
                            "connectionMethod" to "GATT",
                            "transportType" to "BLE (GATT)"
                        ), null, "PHY_READ_GATT"
                    )
                    manager.updateConnectedDevices(evt)
                }

                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    super.onMtuChanged(gatt, mtu, status)
                    val addr = gatt.device.address
                    val statusName = gattStatusName(status)
                    
                    cLog("📏 GATT MTU CHANGED")
                    cLog("Device: $addr")
                    cLog("New MTU: $mtu")
                    cLog("Status: $statusName ($status)")
                    
                    val evt = manager.buildBluetoothDevice(
                        gatt.device, addr, "GATT_MTU_CHANGED", gatt.device.name, mapOf(
                            "mtu" to mtu.toString(),
                            "status" to statusName,
                            "statusCode" to status.toString(),
                            "changeTime" to System.currentTimeMillis().toString(),
                            "connectionMethod" to "GATT",
                            "transportType" to "BLE (GATT)"
                        ), null, "MTU_CHANGED_GATT"
                    )
                    manager.updateConnectedDevices(evt)
                }

                override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
                    super.onReliableWriteCompleted(gatt, status)
                    val addr = gatt.device.address
                    val statusName = gattStatusName(status)
                    
                    cLog("✍️ GATT RELIABLE WRITE COMPLETED")
                    cLog("Device: $addr")
                    cLog("Status: $statusName ($status)")
                    
                    val evt = manager.buildBluetoothDevice(
                        gatt.device, addr, "GATT_RELIABLE_WRITE_COMPLETED", gatt.device.name, mapOf(
                            "status" to statusName,
                            "statusCode" to status.toString(),
                            "completionTime" to System.currentTimeMillis().toString(),
                            "connectionMethod" to "GATT",
                            "transportType" to "BLE (GATT)"
                        ), null, "RELIABLE_WRITE_COMPLETED_GATT"
                    )
                    manager.updateConnectedDevices(evt)
                }
            }, transportMode)
        } catch (e: SecurityException) {
            cLog("🚫 SECURITY EXCEPTION: Missing BLUETOOTH_CONNECT permission for $address")
            cLog("Error details: ${e.message}")
            
            val evt = manager.buildBluetoothDevice(
                device, address, "GATT_PERMISSION_ERROR", device.name, mapOf(
                    "error" to "missing_bluetooth_connect_permission",
                    "errorMessage" to (e.message ?: "unknown"),
                    "errorTime" to System.currentTimeMillis().toString(),
                    "connectionMethod" to "GATT",
                    "transportType" to "BLE (GATT)"
                ), null, "PERMISSION_ERROR_GATT"
            )
            manager.updateConnectedDevices(evt)
            removeConnecting(address)
            return
        } catch (t: Throwable) {
            cLog("💥 EXCEPTION: connectGatt threw exception for $address")
            cLog("Exception type: ${t.javaClass.simpleName}")
            cLog("Exception message: ${t.message}")
            cLog("Stack trace: ${t.stackTrace.joinToString("\n")}")
            
            val evt = manager.buildBluetoothDevice(
                device, address, "GATT_CONNECT_EXCEPTION", device.name, mapOf(
                    "error" to "connectgatt_exception",
                    "errorType" to t.javaClass.simpleName,
                    "errorMessage" to (t.message ?: "unknown"),
                    "errorTime" to System.currentTimeMillis().toString(),
                    "connectionMethod" to "GATT",
                    "transportType" to "BLE (GATT)"
                ), null, "EXCEPTION_GATT"
            )
            manager.updateConnectedDevices(evt)
            removeConnecting(address)
            return
        }

        if (gatt == null) {
            cLog("❌ CRITICAL: connectGatt returned null for $address")
            cLog("This indicates a serious system-level issue")
            
            val evt = manager.buildBluetoothDevice(
                device, address, "GATT_CONNECT_NULL", device.name, mapOf(
                    "error" to "connectgatt_returned_null",
                    "errorTime" to System.currentTimeMillis().toString(),
                    "severity" to "critical",
                    "connectionMethod" to "GATT",
                    "transportType" to "BLE (GATT)"
                ), null, "NULL_GATT"
            )
            manager.updateConnectedDevices(evt)
            removeConnecting(address)
            return
        }

        cLog("✅ GATT object created successfully for $address")
        cLog("Starting 30-second connection timeout watchdog")

        manager.updateBondedDevices()

        // connect timeout watchdog (your original 30s behavior)
        manager.bleManagerScope.launch {
            delay(30_000)
            if (manager.connectedDevices.value.any { it.address == address }) {
                cLog("✅ Device $address connected successfully within timeout")
                return@launch
            }
            if (!manager.connectingDevices.value.any { it.address == address }) {
                cLog("ℹ️ Device $address no longer in connecting state, timeout watchdog cancelled")
                return@launch
            }
            
            cLog("⏰ TIMEOUT: GATT connection timeout reached for $address")
            cLog("Forcing disconnect and cleanup")
            
            try {
                // Try to get GATT from map first, if not there, use the local reference
                val current = gattMap[address] ?: gatt
                try { 
                    current?.disconnect() 
                    cLog("Disconnected GATT for timeout cleanup")
                } catch (e: Throwable) { 
                    cLog("Error disconnecting GATT during timeout: ${e.message}") 
                }
                try { 
                    current?.close() 
                    cLog("Closed GATT for timeout cleanup")
                } catch (e: Throwable) { 
                    cLog("Error closing GATT during timeout: ${e.message}") 
                }
            } catch (e: Throwable) {
                cLog("Exception during timeout cleanup: ${e.message}")
            }
            
            gattMap.remove(address)
            removeConnecting(address)
            removeConnected(address)
            
            val evt = manager.buildBluetoothDevice(
                device, address, "GATT_CONNECT_TIMEOUT", device.name, mapOf(
                    "timeoutDuration" to "30000",
                    "timeoutTime" to System.currentTimeMillis().toString(),
                    "reason" to "connection_timeout",
                    "connectionMethod" to "GATT",
                    "transportType" to "BLE (GATT)"
                ), null, "TIMEOUT_GATT"
            )
            manager.updateConnectedDevices(evt)
            
            if (!suppressAutoRestartScan) {
                cLog("Restarting unified scanning after timeout")
                manager.startUnifiedScanning()
            }
        }
    }

    /* -----------------------
       Small helpers used above
       ----------------------- */

    // Best-effort refresh using reflection — often clears cached service table on many devices
    private fun refreshGatt(gatt: BluetoothGatt): Boolean {
        return try {
            val refresh = gatt.javaClass.getMethod("refresh")
            val result = refresh.invoke(gatt) as? Boolean ?: false
            cLog("refreshGatt() -> $result for ${gatt.device.address}")
            result
        } catch (e: Exception) {
            cLog("refreshGatt() failed: ${e.message} for ${gatt.device.address}")
            false
        }
    }

    // Schedules reconnect attempts with small linear backoff. Limits attempts per-device.
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN])
    private fun scheduleReconnect(device: BluetoothDevice, maxAttempts: Int = 3) {
        val addr = device.address
        val prev = reconnectAttempts[addr] ?: 0
        val nextAttempt = prev + 1
        reconnectAttempts[addr] = nextAttempt

        if (nextAttempt > maxAttempts) {
            cLog("Max reconnect attempts ($maxAttempts) reached for $addr — giving up.")
            reconnectAttempts.remove(addr)
            return
        }

        val delayMs = 200L * nextAttempt // 200ms, 400ms, 600ms...
        manager.bleManagerScope.launch {
            cLog("Schedule reconnect attempt #$nextAttempt for $addr after ${delayMs}ms")
            delay(delayMs)
            // skip if already connecting/connected by another flow
            if (manager.connectedDevices.value.any { it.address == addr } || manager.connectingDevices.value.any { it.address == addr }) {
                cLog("Skipping reconnect for $addr because it is already connecting/connected")
                reconnectAttempts.remove(addr)
                return@launch
            }
            // TODO: Fix reconnect to use proper callbacks
            // connectGattInternal(device)
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun tryCreateBondIfNeeded(device: BluetoothDevice,startDiscovery:()->Unit={}) {
        try {
            if (!hasConnectPermission(context)) return
            when (device.bondState) {
                BluetoothDevice.BOND_NONE -> {
                    cLog("BOND creating for ${device.address}")
                    val ok = device.createBond()
                    cLog("BOND createBond() returned $ok for ${device.address}")
                }

                BluetoothDevice.BOND_BONDING -> {
                    manager.bleManagerScope.launch(Dispatchers.IO) {
                        var attempts = 0
                        while (attempts < 50) {
                            val bs = try { device.bondState } catch (_: Throwable) { BluetoothDevice.BOND_NONE }
                            if (bs != BluetoothDevice.BOND_BONDING) break
                            delay(200)
                            attempts++
                        }
                        val finalState = try { device.bondState } catch (_: Throwable) { BluetoothDevice.BOND_NONE }
                        val extraDelay = if (finalState == BluetoothDevice.BOND_BONDED) 1000L else 0L
                        if (extraDelay > 0) delay(extraDelay)
                        startDiscovery()
                    }
                }

                BluetoothDevice.BOND_BONDED -> {
                    cLog("BOND already bonded for ${device.address}")
                }
            }
        } catch (t: Throwable) {
            cLog("BOND error for ${device.address} -> ${t.message}")
        }
    }
}