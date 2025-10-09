package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.annotation.RequiresPermission
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
    private val isDeviceConnected: (String) -> Boolean,
    private val isDeviceConnecting: (String) -> Boolean,
    private val manager: BleManager,
) {

    private val rfcommSockets = mutableMapOf<String, BluetoothSocket>()
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private fun cLog(message: String) {
        log("BleConnect: $message")
    }

    // When true, lower-level connect functions must avoid auto restarting scans.
    // The orchestrator in connect() will decide when to restart scanning.
    private var suppressAutoRestartScan: Boolean = false
    private val attemptSignal: MutableMap<String, String?> = mutableMapOf()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(address: String) {

        if (!hasConnectPermission(context)) {
            cLog("missing BLUETOOTH_CONNECT permission for $address")
            return
        }

        manager.stopUnifiedScanning()

        val device = try {
            manager.bluetoothAdapter?.getRemoteDevice(address)
        } catch (t: Throwable) {
            cLog("resolve device failed for $address: ${t.message}")
            null
        } ?: return

        // Emit a generic CONNECTING state so UI can reflect immediately
        val connecting =
            manager.buildBluetoothDevice(device, address, "CONNECTING", device.name, null, null)
        updateConnecting(connecting)

        // Attempt all supported connection methods sequentially with per-step logging
//        tryAllTransportsSequentially(device)
        connectGattInternal(device)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun tryAllTransportsSequentially(device: BluetoothDevice) {
        val address = device.address
        manager.bleManagerScope.launch {
            suppressAutoRestartScan = true
            try {
                val attempts: List<Pair<String, suspend () -> Unit>> = listOf(
                    "RFCOMM" to { connectViaRfcommInternal(device) },
                    "GATT" to { connectGattInternal(device) },
                    "A2DP" to { connectA2dpInternal(device) },
                    "HEADSET" to { connectHeadsetInternal(device) },
                    "HID" to { connectHidHostInternal(device) },
                    "LE_AUDIO" to {
                        cLog("LE_AUDIO not supported via public SDK. address=${device.address}")
                        manager.buildBluetoothDevice(
                            device,
                            device.address,
                            "LE_AUDIO_NOT_SUPPORTED",
                            device.name,
                            mapOf("reason" to "public_sdk_unavailable"),
                            null
                        )
                    },
                    "MESH" to {
                        cLog("BLE_MESH requires vendor SDK; not implemented. address=${device.address}")
                        manager.buildBluetoothDevice(
                            device,
                            device.address,
                            "BLE_MESH_NOT_IMPLEMENTED",
                            device.name,
                            mapOf("reason" to "vendor_sdk_required"),
                            null
                        )
                    },
                    "ADV_ONLY" to {
                        cLog("BLE_ADV_ONLY no connection possible; ensure scanner is running. address=${device.address}")
                        manager.buildBluetoothDevice(
                            device,
                            device.address,
                            "BLE_ADV_ONLY_NO_CONNECTION",
                            device.name,
                            mapOf("hint" to "scanner_running"),
                            null
                        )
                    })

                val total = attempts.size
                for ((idx, pair) in attempts.withIndex()) {
                    val name = pair.first
                    val action = pair.second
                    // Stop immediately if device is already connected
                    if (isDeviceConnected(address)) {
                        cLog("Device $address already connected; stopping further attempts")
                        removeConnecting(address)
                        manager.updateConnectedDevices()
                        return@launch
                    }
                    cLog("ATTEMPT_START $name for $address")
                    val startEvt = manager.buildBluetoothDevice(
                        device, address, "CONNECT_ATTEMPT_START_${name}", device.name, mapOf(
                            "method" to name,
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
                        action()
                    } catch (t: Throwable) {
                        // Action invocation error shouldn't stop further attempts
                        cLog("ATTEMPT_ERROR $name for $address -> ${t.message}")
                        val excEvt = manager.buildBluetoothDevice(
                            device, address, "CONNECT_ATTEMPT_FAILED_${name}", device.name, mapOf(
                                "reason" to "exception",
                                "error" to (t.message ?: "unknown"),
                                "method" to name,
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
                        if (isDeviceConnected(address)) break
                        if ((attemptSignal[address] ?: "").startsWith("error_")) break
                        delay(step.toLong())
                        waited += step
                    }

                    if (isDeviceConnected(address)) {
                        cLog("ATTEMPT_SUCCESS $name for $address")
                        manager.buildBluetoothDevice(
                            device, address, "CONNECT_ATTEMPT_SUCCESS_${name}", device.name, mapOf(
                                "method" to name, "methodLabel" to when (name) {
                                    "RFCOMM" -> "Classic (RFCOMM)"
                                    "GATT" -> "BLE (GATT)"
                                    "A2DP" -> "Audio (A2DP)"
                                    "HEADSET" -> "Headset"
                                    "HID" -> "HID Host"
                                    "LE_AUDIO" -> "LE Audio"
                                    "MESH" -> "BLE Mesh"
                                    "ADV_ONLY" -> "BLE Advertisement"
                                    else -> name
                                }, "attempt" to (idx + 1).toString(), "total" to total.toString()
                            ), null
                        )
                        removeConnecting(address)
                        manager.updateConnectedDevices()
                        return@launch
                    } else {
                        cLog("ATTEMPT_FAILED $name for $address (timeout)")
                        val signal = attemptSignal[address]
                        val failEvt = manager.buildBluetoothDevice(
                            device, address, "CONNECT_ATTEMPT_FAILED_${name}", device.name, mapOf(
                                "reason" to "timeout",
                                "method" to name,
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
                            }, null
                        )
                        updateConnecting(failEvt)
                    }
                }
            } finally {
                suppressAutoRestartScan = false
                if (!isDeviceConnected(address)) {
                    removeConnecting(address)
                    manager.buildBluetoothDevice(
                        device, address, "CONNECT_FAILED_ALL", device.name, null, null
                    )
//                    emitEvent(failEvt)
                    manager.updateConnectedDevices()
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
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectViaRfcommInternal(device: BluetoothDevice, uuid: UUID = SPP_UUID) {
        if (!hasConnectPermission(context)) {
            log("RFCOMM: missing BLUETOOTH_CONNECT permission for ${device.address}")
            return
        }

        val address = device.address

        if (rfcommSockets.containsKey(address)) {
            log("RFCOMM: socket already exists for $address")
            return
        }

        manager.bleManagerScope.launch(Dispatchers.IO) {
            try {
                // Cancel discovery which slows down a connection
                try {
                    manager.bluetoothAdapter?.cancelDiscovery()
                } catch (_: Throwable) {
                }

                val socket = device.createRfcommSocketToServiceRecord(uuid)
                log("RFCOMM: connecting socket to $address uuid=$uuid")
                socket.connect()
                rfcommSockets[address] = socket

                val evt = manager.buildBluetoothDevice(
                    device, address, "CLASSIC_RFCOMM_CONNECTED", device.name, null, null
                )
                removeConnecting(address)
                manager.updateConnectedDevices(evt)
                log("RFCOMM: connected to $address")
                // Initiate bonding if needed immediately after classic connect
                if (hasConnectPermission(context)) {
                    tryCreateBondIfNeeded(device)
                }
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
                    mapOf("error" to (t.message ?: "unknown")),
                    null
                )
//                emitEvent(failEvt)
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
                    null, address, "CLASSIC_RFCOMM_DISCONNECTED", null, null, null
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
                    dev, dev.address, "AUDIO_CONNECTED", dev.name, null, null
                )
                manager.updateConnectedDevices(evt)
            } else {
                manager.buildBluetoothDevice(
                    dev, dev.address, "A2DP_CONNECT_FAILED", dev.name, null, null
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
                    dev, dev.address, "HEADSET_CONNECTED", dev.name, null, null
                )
                manager.updateConnectedDevices(evt)
            } else {
                manager.buildBluetoothDevice(
                    dev, dev.address, "HEADSET_CONNECT_FAILED", dev.name, null, null
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
                    dev, address, "AUDIO_DISCONNECTED", dev.name, null, null
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
                    dev, address, "HEADSET_DISCONNECTED", dev.name, null, null
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
                    dev, dev.address, "HID_HOST_CONNECTED", dev.name, null, null
                )
                manager.updateConnectedDevices(evt)
            } else {
                manager.buildBluetoothDevice(
                    dev, dev.address, "HID_HOST_CONNECT_FAILED", dev.name, null, null
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
                    dev, address, "HID_HOST_DISCONNECTED", dev.name, null, null
                )
                removeConnected(address)
                manager.updateConnectedDevices()
            }
        }(address)
    }

    // ----------------- BLE Audio (LE Iso) placeholder -----------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectLeAudioInternal(device: BluetoothDevice) {

//        emitEvent(evt)
    }

    // ----------------- BLE Mesh placeholder -----------------
    private fun connectBleMeshInternal(device: BluetoothDevice) {

//        emitEvent(evt)
    }

    // ----------------- BLE Advertisement-only (no link to connect) -----------------
    private fun prepareBleAdvertisementOnlyInternal(device: BluetoothDevice) {

//        emitEvent(evt)
        if (!suppressAutoRestartScan) {
            try {
                manager.startUnifiedScanning()
            } catch (_: Throwable) {
            }
        }
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
            manager.buildBluetoothDevice(null, address, "GATT_DISCONNECTED", null, null, null)
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectGattInternal(device: BluetoothDevice) {
        val address = device.address
        if (gattMap.containsKey(address)) {
            cLog("GATT already connecting/connected $address")
            return
        }

        // Emit GATT connecting state
        val connectingEvt = manager.buildBluetoothDevice(
            device, address, "GATT_CONNECTING", device.name, null, null
        )
        updateConnecting(connectingEvt)
//        emitEvent(connectingEvt)

        val gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                val addr = gatt.device.address

                val statusName = gattStatusName(status)
                val statusCategory = gattStatusCategory(status)
                val stateName = gattStateName(newState)
                cLog("GATT onConnectionStateChange addr=$addr state=$stateName($newState) status=$statusName($status) cat=$statusCategory")
                // Explicit mapping log like: GATT_SUCCESS -> CONNECTED/CONNECTING/... per current transition
                cLog("GATT MAP $statusName -> $stateName [$statusCategory]")
                manager.buildBluetoothDevice(
                    gatt.device, addr, "GATT_STATE_CHANGE", gatt.device.name, mapOf(
                        "state" to newState.toString(),
                        "stateName" to stateName,
                        "status" to status.toString(),
                        "statusName" to statusName,
                        "category" to statusCategory
                    ), null
                )
//                emitEvent(stateEvt)
                // Emit a compact status->state route event for UI/analytics
//                emitEvent(
//                    ibm.buildBluetoothDevice(
//                        gatt.device,
//                        addr,
//                        "GATT_STATUS_ROUTE",
//                        gatt.device.name,
//                        mapOf("from" to statusName, "to" to stateName, "category" to statusCategory),
//                        null
//                    )
//                )

                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTING -> {
                                cLog("GATT CONNECTING -> $addr ($statusName)")
                                val evt = manager.buildBluetoothDevice(
                                    gatt.device, addr, "GATT_CONNECTING", gatt.device.name, mapOf(
                                        "status" to status.toString(), "statusName" to statusName
                                    ), null
                                )
                                updateConnecting(evt)
                            }

                            BluetoothProfile.STATE_CONNECTED -> {
                                cLog("GATT CONNECTED -> $addr ($statusName)")
                                gattMap[addr] = gatt
                                val evt = manager.buildBluetoothDevice(
                                    gatt.device, addr, "GATT_CONNECTED", gatt.device.name, mapOf(
                                        "status" to status.toString(), "statusName" to statusName
                                    ), null
                                )
                                removeConnecting(addr)
                                manager.updateConnectedDevices(evt)

                                // Initiate bonding if needed right after successful GATT connect
                                if (hasConnectPermission(context)) {
                                    tryCreateBondIfNeeded(gatt.device,){
                                        val ok = try { gatt.discoverServices() } catch (_: Throwable) { false }
                                        cLog("GATT discoverServices after bonding ${if (ok) "started" else "failed to start"} -> ${device.address}")
                                    }
                                }

                            }

                            BluetoothProfile.STATE_DISCONNECTING -> {
                                cLog("GATT DISCONNECTING -> $addr ($statusName)")
                                manager.buildBluetoothDevice(
                                    gatt.device,
                                    addr,
                                    "GATT_DISCONNECTING",
                                    gatt.device.name,
                                    mapOf(
                                        "status" to status.toString(), "statusName" to statusName
                                    ),
                                    null
                                )
                            }

                            BluetoothProfile.STATE_DISCONNECTED -> {
                                cLog("GATT DISCONNECTED -> $addr ($statusName)")
                                try {
                                    gatt.close()
                                } catch (_: Throwable) {
                                }
                                gattMap.remove(addr)
                                removeConnecting(addr)
                                removeConnected(addr)
                                attemptSignal[addr] = "error_$statusName"
                               val evt =  manager.buildBluetoothDevice(
                                    gatt.device, addr, "GATT_DISCONNECTED", gatt.device.name, mapOf(
                                        "status" to status.toString(), "statusName" to statusName
                                    ), null
                                )
                                manager.updateConnectedDevices(evt)
                            }


                        }
                    }

                    BluetoothGatt.GATT_CONNECTION_TIMEOUT -> {
                        cLog("Connection attempt timed out for $addr")
                        // Potentially trigger retry logic here if newState is STATE_DISCONNECTED
                    }

                    // Add cases for other specific statuses you want to handle or log differently
                    else -> {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            cLog("GATT operation failed with status: ${gattStatusName(status)} for $addr")
                        }
                    }
                }


            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                val addr = gatt.device.address
                cLog("GATT services discovered for $addr (status=$status, services=${gatt.services?.size ?: 0})")
                manager.buildBluetoothDevice(
                    gatt.device, addr, "GATT_SERVICES_DISCOVERED", gatt.device.name, mapOf(
                        "status" to status.toString(),
                        "statusName" to gattStatusName(status),
                        "serviceCount" to (gatt.services?.size ?: 0).toString()
                    ), null
                )
                manager.updateConnectedDevices()
            }

            override fun onPhyUpdate(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status)
                cLog(
                    "GATT PHY_UPDATE addr=${gatt.device.address} tx=$txPhy rx=$rxPhy status=${
                        gattStatusName(
                            status
                        )
                    }($status)"
                )
//                emitEvent(
//                    ibm.buildBluetoothDevice(
//                        gatt.device,
//                        gatt.device.address,
//                        "GATT_PHY_UPDATE",
//                        gatt.device.name,
//                        mapOf("txPhy" to txPhy.toString(), "rxPhy" to rxPhy.toString(), "status" to status.toString(), "statusName" to gattStatusName(status)),
//                        null
//                    )
//                )
            }

            override fun onPhyRead(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
                super.onPhyRead(gatt, txPhy, rxPhy, status)
                cLog(
                    "GATT PHY_READ addr=${gatt.device.address} tx=$txPhy rx=$rxPhy status=${
                        gattStatusName(
                            status
                        )
                    }($status)"
                )
//                emitEvent(
//                    ibm.buildBluetoothDevice(
//                        gatt.device,
//                        gatt.device.address,
//                        "GATT_PHY_READ",
//                        gatt.device.name,
//                        mapOf("txPhy" to txPhy.toString(), "rxPhy" to rxPhy.toString(), "status" to status.toString(), "statusName" to gattStatusName(status)),
//                        null
//                    )
//                )
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                cLog(
                    "GATT MTU_CHANGED addr=${gatt.device.address} mtu=$mtu status=${
                        gattStatusName(
                            status
                        )
                    }($status)"
                )
//                emitEvent(
//                    ibm.buildBluetoothDevice(
//                        gatt.device,
//                        gatt.device.address,
//                        "GATT_MTU_CHANGED",
//                        gatt.device.name,
//                        mapOf("mtu" to mtu.toString(), "status" to status.toString(), "statusName" to gattStatusName(status)),
//                        null
//                    )
//                )
            }

            override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
                super.onReliableWriteCompleted(gatt, status)
                cLog(
                    "GATT RELIABLE_WRITE_COMPLETED addr=${gatt.device.address} status=${
                        gattStatusName(
                            status
                        )
                    }($status)"
                )
//                emitEvent(
//                    ibm.buildBluetoothDevice(
//                        gatt.device,
//                        gatt.device.address,
//                        "GATT_RELIABLE_WRITE_COMPLETED",
//                        gatt.device.name,
//                        mapOf("status" to status.toString(), "statusName" to gattStatusName(status)),
//                        null
//                    )
//                )
            }
        }, BluetoothDevice.TRANSPORT_AUTO)
        if (gatt == null) {
            cLog("GATT connectGatt returned null for $address")
            manager.buildBluetoothDevice(
                device,
                address,
                "GATT_CONNECT_FAILED",
                device.name,
                mapOf("error" to "connectGatt_returned_null"),
                null
            )
            removeConnecting(address)
            return
        }
        gattMap[address] = gatt
        // Note: connecting event already emitted above
        manager.updateBondedDevices()
        manager.bleManagerScope.launch {
            delay(30_000)
            if (isDeviceConnected(address)) return@launch
            if (!isDeviceConnecting(address)) return@launch
            try {
                gatt.disconnect()
                gatt.close()
            } catch (_: Throwable) {
            }
            gattMap.remove(address)
            removeConnecting(address)
            removeConnected(address)
            manager.buildBluetoothDevice(
                device, address, "GATT_CONNECT_TIMEOUT", device.name, null, null
            )
            if (!suppressAutoRestartScan) manager.startUnifiedScanning()
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