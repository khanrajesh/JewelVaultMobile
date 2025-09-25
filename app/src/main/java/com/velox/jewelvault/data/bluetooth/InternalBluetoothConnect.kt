package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Class-based connector that encapsulates all dependencies via constructor.
 * Keep usage simple: create once, then call connect/tryClassicConnection.
 */
class InternalBluetoothConnect(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val bluetoothManager: BluetoothManager,
    private val scope: CoroutineScope,
    private val hasConnectPermission: () -> Boolean,
    private val stopScanning: () -> Unit,
    private val startScanning: () -> Unit,
    private val createDeviceEvent: (
            device: BluetoothDevice?,
            address: String?,
            action: String,
            name: String?,
            extraInfo: Map<String, String>?,
            uuids: String?
        ) -> BluetoothDeviceDetails,
    private val updateBondedDevices: () -> Unit,
    private val updateConnectedDevices: () -> Unit,
    private val addOrUpdateConnecting: (BluetoothDeviceDetails) -> Unit,
    private val removeConnecting: (String) -> Unit,
    private val addOrUpdateConnected: (BluetoothDeviceDetails) -> Unit,
    private val removeConnected: (String) -> Unit,
    private val emitEvent: (BluetoothDeviceDetails) -> Unit,
    private val gattMap: MutableMap<String, BluetoothGatt>,
    private val isDeviceConnected: (String) -> Boolean,
    private val isDeviceConnecting: (String) -> Boolean,
    private val log: (String) -> Unit,
) {

    private val rfcommSockets = mutableMapOf<String, BluetoothSocket>()
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private fun cLog(message: String) { log("CONNECT: $message") }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(address: String) {
        if (!hasConnectPermission()) {
            cLog("missing BLUETOOTH_CONNECT permission for $address")
            return
        }

        stopScanning()

        val device = try { bluetoothAdapter?.getRemoteDevice(address) } catch (t: Throwable) {
            cLog("resolve device failed for $address: ${t.message}")
            null
        } ?: return

        // Emit a generic CONNECTING state so UI can reflect immediately
        val connecting = createDeviceEvent(device, address, "CONNECTING", device.name, null, null)
        addOrUpdateConnecting(connecting)
        emitEvent(connecting)

        // Decide transport and delegate
        val chosen = decideTransport(device)
        when (chosen) {
            Transport.RFCOMM -> connectViaRfcommInternal(device)
            Transport.GATT -> connectGattInternal(device)
            Transport.A2DP -> connectA2dpInternal(device)
            Transport.HEADSET -> connectHeadsetInternal(device)
            Transport.HID -> connectHidHostInternal(device)
            Transport.ADV_ONLY -> prepareBleAdvertisementOnlyInternal(device)
            Transport.LE_AUDIO -> connectLeAudioInternal(device)
            Transport.MESH -> connectBleMeshInternal(device)
        }

        // Safety timeout to clear connecting if no success
        scope.launch {
            delay(20_000)
            if (!isDeviceConnected(address)) {
                removeConnecting(address)
                val failEvt = createDeviceEvent(device, address, "CONNECT_TIMEOUT", device.name, null, null)
                emitEvent(failEvt)
                updateConnectedDevices()
            }
        }
    }

    // Public unified disconnect that routes based on active transport
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect(address: String) {
        // Prefer RFCOMM if socket is open
        if (rfcommSockets.containsKey(address)) {
            disconnectRfcommInternal(address)
            return
        }
        // Try GATT map
        if (gattMap.containsKey(address)) {
            disconnectGattInternal(address)
            return
        }
        // Otherwise attempt profile-based disconnects (best-effort)
        disconnectA2dpInternal(address)
        disconnectHeadsetInternal(address)
        disconnectHidHostInternal(address)
    }

    // ----------------- Classic RFCOMM (SPP) -----------------
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectViaRfcommInternal(device: BluetoothDevice, uuid: UUID = SPP_UUID) {
        if (!hasConnectPermission()) {
            log("RFCOMM: missing BLUETOOTH_CONNECT permission for ${device.address}")
            return
        }

        val address = device.address

        if (rfcommSockets.containsKey(address)) {
            log("RFCOMM: socket already exists for $address")
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                // Cancel discovery which slows down a connection
                try { bluetoothAdapter?.cancelDiscovery() } catch (_: Throwable) {}

                val socket = device.createRfcommSocketToServiceRecord(uuid)
                log("RFCOMM: connecting socket to $address uuid=$uuid")
                socket.connect()
                rfcommSockets[address] = socket

                val evt = createDeviceEvent(device, address, "CLASSIC_RFCOMM_CONNECTED", device.name, null, null)
                addOrUpdateConnected(evt)
                removeConnecting(address)
                emitEvent(evt)
                updateConnectedDevices()
                log("RFCOMM: connected to $address")
            } catch (t: Throwable) {
                log("RFCOMM: connect failed for $address -> ${t.message}")
                try { rfcommSockets.remove(address)?.close() } catch (_: Throwable) {}
                val failEvt = createDeviceEvent(device, address, "CLASSIC_RFCOMM_CONNECT_FAILED", device.name, mapOf("error" to (t.message ?: "unknown")), null)
                emitEvent(failEvt)
            startScanning()
        }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectRfcommInternal(address: String) {
        scope.launch(Dispatchers.IO) {
            try {
                rfcommSockets.remove(address)?.close()
                val evt = createDeviceEvent(null, address, "CLASSIC_RFCOMM_DISCONNECTED", null, null, null)
                removeConnected(address)
                emitEvent(evt)
                updateConnectedDevices()
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
        withProfile( BluetoothProfile.A2DP ) { proxy, dev ->
            val ok = tryInvokeConnect(proxy, dev)
            cLog("A2DP connect(${dev.address}) result=$ok")
            if (ok) {
                val evt = createDeviceEvent(dev, dev.address, "AUDIO_CONNECTED", dev.name, null, null)
                addOrUpdateConnected(evt)
                emitEvent(evt)
                updateConnectedDevices()
            }
        }(device.address)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectHeadsetInternal(device: BluetoothDevice) {
        withProfile( BluetoothProfile.HEADSET ) { proxy, dev ->
            val ok = tryInvokeConnect(proxy, dev)
            cLog("HEADSET connect(${dev.address}) result=$ok")
            if (ok) {
                val evt = createDeviceEvent(dev, dev.address, "HEADSET_CONNECTED", dev.name, null, null)
                addOrUpdateConnected(evt)
                emitEvent(evt)
                updateConnectedDevices()
            }
        }(device.address)
    }

    // Disconnections for classic audio profiles
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectA2dpInternal(address: String) {
        withProfile( BluetoothProfile.A2DP ) { proxy, dev ->
            val ok = tryInvokeDisconnect(proxy, dev)
            cLog("A2DP disconnect(${dev.address}) result=$ok")
            if (ok) {
                val evt = createDeviceEvent(dev, address, "AUDIO_DISCONNECTED", dev.name, null, null)
                removeConnected(address)
                emitEvent(evt)
                updateConnectedDevices()
            }
        }(address)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectHeadsetInternal(address: String) {
        withProfile( BluetoothProfile.HEADSET ) { proxy, dev ->
            val ok = tryInvokeDisconnect(proxy, dev)
            cLog("HEADSET disconnect(${dev.address}) result=$ok")
            if (ok) {
                val evt = createDeviceEvent(dev, address, "HEADSET_DISCONNECTED", dev.name, null, null)
                removeConnected(address)
                emitEvent(evt)
                updateConnectedDevices()
            }
        }(address)
    }

    // ----------------- Classic HID Host (keyboards/mice) -----------------
    // Note: Uses profile ID 4 which corresponds to HID Host on AOSP. This may be
    // restricted on some devices and may require system apps/privileges.
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectHidHostInternal(device: BluetoothDevice) {
        withProfile( /* HID_HOST */ 4 ) { proxy, dev ->
            val ok = tryInvokeConnect(proxy, dev)
            cLog("HID_HOST connect(${dev.address}) result=$ok")
            if (ok) {
                val evt = createDeviceEvent(dev, dev.address, "HID_HOST_CONNECTED", dev.name, null, null)
                addOrUpdateConnected(evt)
                emitEvent(evt)
                updateConnectedDevices()
            }
        }(device.address)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectHidHostInternal(address: String) {
        withProfile( /* HID_HOST */ 4 ) { proxy, dev ->
            val ok = tryInvokeDisconnect(proxy, dev)
            cLog("HID_HOST disconnect(${dev.address}) result=$ok")
            if (ok) {
                val evt = createDeviceEvent(dev, address, "HID_HOST_DISCONNECTED", dev.name, null, null)
                removeConnected(address)
                emitEvent(evt)
                updateConnectedDevices()
            }
        }(address)
    }

    // ----------------- BLE Audio (LE Iso) placeholder -----------------
    private fun connectLeAudioInternal(device: BluetoothDevice) {
        cLog("LE_AUDIO not supported via public SDK. address=${device.address}")
    }

    // ----------------- BLE Mesh placeholder -----------------
    private fun connectBleMeshInternal(device: BluetoothDevice) {
        cLog("BLE_MESH requires vendor SDK; not implemented. address=${device.address}")
    }

    // ----------------- BLE Advertisement-only (no link to connect) -----------------
    private fun prepareBleAdvertisementOnlyInternal(device: BluetoothDevice) {
        cLog("BLE_ADV_ONLY no connection possible; ensure scanner is running. address=${device.address}")
    }

    // ----------------- Helpers -----------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun withProfile(profileId: Int, block: (proxy: Any, device: BluetoothDevice) -> Unit): (String) -> Unit = { address ->
        if (!hasConnectPermission()) {
            cLog("PROFILE:$profileId missing BLUETOOTH_CONNECT permission for $address")
        } else {
            val device = try { bluetoothAdapter?.getRemoteDevice(address) } catch (_: Throwable) { null }
            if (device == null) {
                cLog("PROFILE:$profileId could not resolve device for $address")
            } else {
                try {
                    bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                            try {
                                block(proxy, device)
                            } catch (t: Throwable) {
                                cLog("PROFILE:$p block error -> ${t.message}")
                            } finally {
                                try { bluetoothAdapter?.closeProfileProxy(p, proxy) } catch (_: Throwable) {}
                            }
                        }
                        override fun onServiceDisconnected(p: Int) {
                            // no-op
                        }
                    }, profileId)
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
            } catch (_: Throwable) {}
            try {
                gatt.close()
            } catch (_: Throwable) {}
            gattMap.remove(address)
            val evt = createDeviceEvent(null, address, "GATT_DISCONNECTED", null, null, null)
            removeConnected(address)
            removeConnecting(address)
            emitEvent(evt)
            updateConnectedDevices()
        }
    }

    // ----------------- Routing helpers -----------------
    private enum class Transport { RFCOMM, GATT, A2DP, HEADSET, HID, ADV_ONLY, LE_AUDIO, MESH }

    private fun decideTransport(device: BluetoothDevice): Transport {
        // Heuristics targeting label printers: prefer RFCOMM SPP when classic device class is IMAGING/PRINTER
        val name = safe { device.name }?.lowercase() ?: ""
        val uuids = safe { device.uuids?.mapNotNull { it?.uuid } } ?: emptyList()
        val isClassic = safe { device.type } == BluetoothDevice.DEVICE_TYPE_CLASSIC || safe { device.type } == BluetoothDevice.DEVICE_TYPE_DUAL

        val looksLikePrinter = listOf("printer", "label", "zebra", "sewnik", "seznik", "shakti").any { it in name }
        val hasSpp = uuids.any { it == SPP_UUID }

        if (isClassic && (hasSpp || looksLikePrinter)) return Transport.RFCOMM

        // If LE only, attempt GATT; otherwise fall back to ADV_ONLY
        return if (safe { device.type } == BluetoothDevice.DEVICE_TYPE_LE) Transport.GATT else Transport.ADV_ONLY
    }

    private inline fun <T> safe(block: () -> T): T? = try { block() } catch (_: Throwable) { null }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectGattInternal(device: BluetoothDevice) {
        val address = device.address
        if (gattMap.containsKey(address)) {
            cLog("GATT already connecting/connected $address")
            return
        }
        val gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                val addr = gatt.device.address
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        cLog("GATT connected -> $addr (status=$status)")
                        gattMap[addr] = gatt
                        val evt = createDeviceEvent(gatt.device, addr, "GATT_CONNECTED", gatt.device.name, null, null)
                        addOrUpdateConnected(evt)
                        removeConnecting(addr)
                        emitEvent(evt)
                        updateConnectedDevices()
                        try { gatt.discoverServices() } catch (_: Throwable) {}
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        cLog("GATT disconnected -> $addr (status=$status)")
                        try { gatt.close() } catch (_: Throwable) {}
                        gattMap.remove(addr)
                        removeConnecting(addr)
                        removeConnected(addr)
                        val evt = createDeviceEvent(gatt.device, addr, "GATT_DISCONNECTED", gatt.device.name, null, null)
                        emitEvent(evt)
        updateConnectedDevices()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                val addr = gatt.device.address
                cLog("GATT services discovered for $addr (status=$status, services=${gatt.services?.size ?: 0})")
                val evt = createDeviceEvent(gatt.device, addr, "GATT_SERVICES_DISCOVERED", gatt.device.name, mapOf("status" to status.toString()), null)
                emitEvent(evt)
                updateConnectedDevices()
            }
        })
        if (gatt == null) {
            cLog("GATT connectGatt returned null for $address")
            return
        }
        gattMap[address] = gatt
        val connecting = createDeviceEvent(device, address, "GATT_CONNECTING", device.name, null, null)
        addOrUpdateConnecting(connecting)
        emitEvent(connecting)
        updateBondedDevices()
        scope.launch {
            delay(20_000)
            if (isDeviceConnected(address)) return@launch
            if (!isDeviceConnecting(address)) return@launch
            try {
                gatt.disconnect()
                gatt.close()
            } catch (_: Throwable) {}
            gattMap.remove(address)
            removeConnecting(address)
            removeConnected(address)
        startScanning()
    }
    }
}