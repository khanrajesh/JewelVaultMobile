package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.velox.jewelvault.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BluetoothReceiver @Inject constructor(
    private val context: Context
) : BroadcastReceiver() {

    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Device found events
    private val _deviceFound = MutableSharedFlow<com.velox.jewelvault.data.bluetooth.BluetoothDevice>()
    val deviceFound: SharedFlow<com.velox.jewelvault.data.bluetooth.BluetoothDevice> = _deviceFound.asSharedFlow()

    // Connection state changes
//    private val _connectionStateChanged = MutableSharedFlow<ConnectionStateChange>()
//    val connectionStateChanged: SharedFlow<ConnectionStateChange> =
//        _connectionStateChanged.asSharedFlow()

    // Bluetooth adapter state changes
    private val _bluetoothStateChanged = MutableStateFlow(
        BluetoothStateChange(BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_OFF)
    )
    val bluetoothStateChanged: StateFlow<BluetoothStateChange> = _bluetoothStateChanged.asStateFlow()


    private var isRegistered = false

    @SuppressLint("ServiceCast")
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    // Bluetooth adapter (guarded)
    val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            val mgr = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            mgr?.adapter
        } catch (t: Throwable) {
            log("Error obtaining BluetoothAdapter: ${t.message}")
            null
        }
    }
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    // Public listener for all emitted events
    var eventListener: ((com.velox.jewelvault.data.bluetooth.BluetoothDevice) -> Unit)? = null

    // Active GATT connections tracked by address
    private val gattMap = mutableMapOf<String, BluetoothGatt>()

    // Active BLE scan callback
    private var activeScanCallback: ScanCallback? = null


    // ----------------- Registration helpers -----------------
    fun registerReceiver() {
        if (isRegistered) {
            log("BluetoothBroadcastReceiver already registered")
            return
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(BluetoothDevice.ACTION_UUID)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        }
        try {
            // Use applicationContext to ensure proper broadcast reception
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // registerReceiver(receiver, filter, flags) overload available on API 33
                appContext.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
                log("BluetoothBroadcastReceiver registered with RECEIVER_NOT_EXPORTED")
            } else {
                appContext.registerReceiver(this, filter)
                log("BluetoothBroadcastReceiver registered (no flags)")
            }
            isRegistered = true
            log("BluetoothBroadcastReceiver registered successfully - isRegistered: $isRegistered")
            refreshSystemState()
        } catch (e: Exception) {
            log("Failed to register BluetoothBroadcastReceiver: ${e.message}")
            e.printStackTrace()
        }
    }

    fun unregister() {
        if (!isRegistered) {
            log("BluetoothBroadcastReceiver unregister called but not registered")
            return
        }
        try {
            appContext.unregisterReceiver(this)
            isRegistered = false
            log("BluetoothBroadcastReceiver unregistered")
        } catch (e: IllegalArgumentException) {
            log("Receiver not registered while trying to unregister: ${e.message}")
        } catch (e: Exception) {
            log("Error while unregistering bluetooth receiver: ${e.message}")
        }
    }

    fun isRegistered(): Boolean = isRegistered


    /**
     * Refresh and sync with current system state when receiver registers
     */
    fun refreshSystemState() {
        log("BluetoothReceiver: Refreshing system state...")
        scope.launch {
            try {
                // Get current Bluetooth adapter state
                val bluetoothManager =
                    appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bluetoothManager?.adapter

                if (adapter != null) {
                    val currentState = adapter.state
                    log("BluetoothReceiver: Current adapter state: $currentState")

                    // Emit current state to sync with system
                    _bluetoothStateChanged.value = BluetoothStateChange(
                        currentState = currentState, previousState = currentState
                    )
                    log("BluetoothReceiver: Emitted initial state: currentState=$currentState")

                    // Check if discovery is currently running
                    val isCurrentlyDiscovering = try {
                        if (hasConnectPermission()) {
                            adapter.isDiscovering
                        } else {
                            false
                        }
                    } catch (e: SecurityException) {
                        log("BluetoothReceiver: SecurityException checking discovery state: ${e.message}")
                        false
                    } catch (t: Throwable) {
                        log("BluetoothReceiver: Error checking discovery state: ${t.message}")
                        false
                    }

                    log("BluetoothReceiver: Current discovery state: $isCurrentlyDiscovering")
                    _isDiscovering.value = isCurrentlyDiscovering

                    // Refresh paired devices to sync current connections
//                    refreshPairedDevices(adapter)
                } else {
                    log("BluetoothReceiver: Bluetooth adapter not available")
                }
            } catch (t: Throwable) {
                log("BluetoothReceiver: Error refreshing system state: ${t.message}")
            }
        }
    }

    // ----------------- Permission helpers -----------------
    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun hasFineLocation(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    // ----------------- BroadcastReceiver -----------------
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        log("BluetoothReceiver: onReceive called with action: $action")
        try {
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    log("BluetoothReceiver: Processing ACTION_FOUND")
                    try {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                            .toInt()
                        val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: device?.name
                        log("BluetoothReceiver: Found device - name: $name, rssi: $rssi, address: ${device?.address}")
                        val event = buildEventFromDevice(device, action = "ACTION_FOUND").copy(
                            name = name, rssi = rssi
                        )
                        emit(event)
                        log("BluetoothReceiver: Emitted ACTION_FOUND event for device: ${device?.address}")
                    } catch (e: Exception) {
                        log("BluetoothReceiver: Exception in ACTION_FOUND: ${e.message}")
                    } catch (e: SecurityException) {
                        log("BluetoothReceiver: SecurityException in ACTION_FOUND: ${e.message}")
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                    log("BluetoothReceiver: Processing ACL action: $action")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    log("BluetoothReceiver: ACL device - address: ${device?.address}, name: ${device?.name}")
                    val event = buildEventFromDevice(device, action = action)
                    emit(event)
                    log("BluetoothReceiver: Emitted ACL event for device: ${device?.address}")
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    log("BluetoothReceiver: Processing ACTION_BOND_STATE_CHANGED")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                    log("BluetoothReceiver: Bond state changed - device: ${device?.address}, state: $state, prev: $prev")
                    val event =
                        buildEventFromDevice(device, action = "ACTION_BOND_STATE_CHANGED").copy(
                            extraInfo = mapOf(
                                "bondState" to state.toString(),
                                "prevBondState" to prev.toString()
                            )
                        )
                    emit(event)
                    log("BluetoothReceiver: Emitted BOND_STATE_CHANGED event for device: ${device?.address}")
                }

                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    log("BluetoothReceiver: Processing ACTION_PAIRING_REQUEST")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
                    val key = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, -1)
                    log("BluetoothReceiver: Pairing request - device: ${device?.address}, variant: $variant, key: $key")
                    val event =
                        buildEventFromDevice(device, action = "ACTION_PAIRING_REQUEST").copy(
                            extraInfo = mapOf(
                                "pairingVariant" to variant.toString(),
                                "pairingKey" to key.toString()
                            )
                        )
                    log("BluetoothReceiver: ACTION_PAIRING_REQUEST event: $event")
                    emit(event)
                    log("BluetoothReceiver: Emitted PAIRING_REQUEST event for device: ${device?.address}")
                }

                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    log("BluetoothReceiver: Processing ACTION_NAME_CHANGED")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                    log("BluetoothReceiver: Name changed - device: ${device?.address}, new name: $name")
                    val event = buildEventFromDevice(device, action = "ACTION_NAME_CHANGED").copy(
                        name = name ?: "<null>"
                    )
                    emit(event)
                    log("BluetoothReceiver: Emitted NAME_CHANGED event for device: ${device?.address}")
                }

                BluetoothDevice.ACTION_UUID -> {
                    log("BluetoothReceiver: Processing ACTION_UUID")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                    log("BluetoothReceiver: UUID action - device: ${device?.address}, uuids count: ${uuids?.size}")
                    val event = buildEventFromDevice(device, action = "ACTION_UUID").copy(
                        uuids = uuidsToString(uuids)
                    )
                    emit(event)
                    log("BluetoothReceiver: Emitted UUID event for device: ${device?.address}")
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    log("BluetoothReceiver: Discovery started")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    log("BluetoothReceiver: Discovery finished")
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    val prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1)
                    log("Adapter state changed: prev=$prev -> state=$state")
                    _bluetoothStateChanged.value = BluetoothStateChange(
                        currentState = state, previousState = prev
                    )
                    log("BluetoothBroadcastReceiver: Emitted state change: currentState=$state, previousState=$prev")
                }

                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    log("BluetoothReceiver: Processing ACTION_SCAN_MODE_CHANGED")
                    val mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
                    val prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1)
                    log("BluetoothReceiver: Scan mode changed: prev=$prev -> mode=$mode")
                }

                else -> {
                    log("BluetoothReceiver: Unhandled action: $action")
                }
            }
        } catch (t: Throwable) {
            log("BluetoothReceiver: Exception handling $action: ${t.message}")
            t.printStackTrace()
        } catch (se: SecurityException) {
            log("BluetoothReceiver: Security exception handling $action: ${se.message}")
            se.printStackTrace()
        }
        log("BluetoothReceiver: onReceive completed for action: $action")
    }

    // ----------------- BLE Scanning -----------------
    fun startBleScan(scanSettings: ScanSettings? = null, scanFilters: List<ScanFilter>? = null) {
        if (!hasScanPermission() || !hasFineLocation()) {
            log("Missing BLUETOOTH_SCAN or location permission; cannot start scan")
            return
        }
        val settings =
            scanSettings ?: ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        val scanner = bluetoothLeScanner
        if (scanner == null) {
            log("No BLE scanner available on this device")
            return
        }

        if (activeScanCallback != null) return

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    val device = result.device
                    val record = result.scanRecord

                    val manuMap = mutableMapOf<Int, ByteArray>()
                    record?.manufacturerSpecificData?.let { msd ->
                        for (i in 0 until msd.size()) {
                            manuMap[msd.keyAt(i)] = msd.valueAt(i)
                        }
                    }

                    val svcMap = mutableMapOf<UUID, ByteArray>()
                    record?.serviceData?.let { sd ->
                        sd.forEach { (k, v) -> svcMap[k.uuid] = v }
                    }

                    val event = buildEventFromDevice(device, action = "BLE_SCAN_RESULT").copy(
                        name = device.name ?: record?.deviceName,
                        rssi = result.rssi,
                        uuids = record?.serviceUuids?.joinToString(",") { it.uuid.toString() },
                        manufacturerData = if (manuMap.isEmpty()) null else manuMap,
                        serviceData = if (svcMap.isEmpty()) null else svcMap,
                        txPower = if (record?.txPowerLevel != Int.MIN_VALUE) record?.txPowerLevel else null,
                        extraInfo = mapOf("timestampNanos" to result.timestampNanos.toString())
                    )
//                    log("onScanResult: ${event.name} $event")
//                    _deviceFound.

//                    emit(event)
                } catch (e: Exception) {
                    log("onScanResult exception: ${e.message}")
                } catch (e: SecurityException) {
                    log("onScanResult security exception: ${e.message}")
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                log("BLE scan failed: $errorCode")
            }
        }

        activeScanCallback = callback
        try {
            log("BLE scan started")
            scanner.startScan(scanFilters, settings, callback)
        } catch (e: SecurityException) {
            log("onScanResult security exception: ${e.message}")
        }
    }

    fun stopBleScan() {
        val scanner = bluetoothLeScanner
        val cb = activeScanCallback ?: return
        try {
            scanner?.stopScan(cb)
        } catch (e: Exception) {
            log("stopBleScan exception: ${e.message}")
        } catch (e: SecurityException) {
            log("onScanResult security exception: ${e.message}")
        }
        activeScanCallback = null
        log("BLE scan stopped")
    }

    // ----------------- GATT Connect / Disconnect -----------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(address: String) {
        if (!hasConnectPermission()) {
            log("Missing BLUETOOTH_CONNECT permission; cannot connect")
            return
        }
        val device = try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (e: Exception) {
            null
        }
        if (device == null) {
            log("Device $address not found on adapter")
            return
        }
        if (gattMap.containsKey(address)) {
            log("Already connected/connecting to $address")
            return
        }

        val gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                try {
                    log("GATT state change for ${gatt.device.address}: status=$status newState=$newState")
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gattMap[gatt.device.address] = gatt
                        gatt.discoverServices()
                        emit(buildEventFromDevice(gatt.device, action = "GATT_CONNECTED"))
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        gattMap.remove(gatt.device.address)
                        emit(buildEventFromDevice(gatt.device, action = "GATT_DISCONNECTED"))
                        try {
                            gatt.close()
                        } catch (e: Exception) {
                        }
                    }
                } catch (e: SecurityException) {
                    log("onScanResult security exception: ${e.message}")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                log("Services discovered for ${gatt.device.address} (status=$status)")
                val infoMap = buildEventFromDevice(
                    gatt.device, action = "GATT_SERVICES_DISCOVERED"
                ).extraInfo.toMutableMap()
                val svcSummary = gatt.services.joinToString(",") { it.uuid.toString() }
                infoMap["gattServices"] = svcSummary

                // Attempt to read common DIS characteristics
                val dis = gatt.getService(UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb"))
                dis?.let { s ->
                    val chars = listOf(
                        UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"), // Manufacturer
                        UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb"), // Model
                        UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb"), // Serial
                        UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"), // Firmware
                        UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")  // HW rev
                    )
                    chars.forEach { uuid ->
                        s.getCharacteristic(uuid)?.let { ch ->
                            try {
                                gatt.readCharacteristic(ch)
                            } catch (e: Exception) {
                            } catch (e: SecurityException) {
                                log("onScanResult security exception: ${e.message}")
                            }
                        }
                    }
                }

                emit(
                    BluetoothDevice(
                        address = gatt.device.address,
                        device = gatt.device,
                        action = "GATT_SERVICES_DISCOVERED",
                        extraInfo = infoMap
                    )
                )
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val bytes = characteristic.value
                    val value = try {
                        bytes?.toString(Charsets.UTF_8) ?: bytes?.toHex() ?: "<empty>"
                    } catch (e: Exception) {
                        bytes?.toHex() ?: "<empty>"
                    }
                    val ev = buildEventFromDevice(gatt.device, action = "GATT_CHAR_READ").copy(
                        extraInfo = mapOf("char_${characteristic.uuid}" to value)
                    )
                    emit(ev)
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
            ) {
                val valHex = characteristic.value?.toHex() ?: "<empty>"
                val ev = buildEventFromDevice(gatt.device, action = "GATT_CHAR_CHANGED").copy(
                    extraInfo = mapOf("char_${characteristic.uuid}" to valHex)
                )
                emit(ev)
            }

            override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                val ev = buildEventFromDevice(gatt.device, action = "GATT_RSSI").copy(
                    rssi = rssi
                )
                emit(ev)
            }
        })

        // keep to prevent duplicates
        gattMap[address] = gatt
        log("Initiated GATT connect to $address")
    }

    fun disconnectDevice(address: String) {
        val gatt = gattMap[address]
        if (gatt != null) {
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                log("disconnectDevice exception: ${e.message}")
            } catch (e: SecurityException) {
                log("onScanResult security exception: ${e.message}")
            }
            gattMap.remove(address)
            log("Disconnected GATT for $address")
            emit(
                buildEventFromDevice(
                    null, action = "GATT_DISCONNECT_REQUESTED"
                ).copy(address = address)
            )
        } else {
            log("No GATT connection for $address")
        }
    }

    fun disconnectAll() {
        val keys = gattMap.keys.toList()
        keys.forEach { disconnectDevice(it) }
    }

    // ----------------- Utility getters -----------------
    fun getBondedDevicesDetailed(): List<com.velox.jewelvault.data.bluetooth.BluetoothDevice> {
        try {
            val set = bluetoothAdapter?.bondedDevices ?: emptySet()
            return set.map { buildEventFromDevice(it, action = "BONDED_DEVICE") }
        } catch (e: SecurityException) {
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun getConnectedDevicesDetailed(): List<com.velox.jewelvault.data.bluetooth.BluetoothDevice> {
        val out = mutableListOf<com.velox.jewelvault.data.bluetooth.BluetoothDevice>()
        try {
            val gattConnected = try {
                bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            } catch (e: Exception) {
                emptyList<BluetoothDevice>()
            }
            gattConnected.forEach { out.add(buildEventFromDevice(it, action = "CONNECTED_DEVICE")) }

            val profiles =
                listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET, BluetoothProfile.HEALTH)
            profiles.forEach { profile ->
                try {
                    val connected = bluetoothManager.getConnectedDevices(profile)
                    connected.forEach {
                        out.add(
                            buildEventFromDevice(
                                it, action = "CONNECTED_DEVICE"
                            )
                        )
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // include our own gatt connections
            gattMap.keys.forEach { addr ->
                try {
                    val dev = bluetoothAdapter?.getRemoteDevice(addr)
                    if (dev != null) out.add(buildEventFromDevice(dev, action = "CONNECTED_DEVICE"))
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            log("getConnectedDevicesDetailed exception: ${e.message}")
        } catch (e: SecurityException) {
            log("onScanResult security exception: ${e.message}")
        }
        return out
    }

    // ----------------- Classic discovery helpers -----------------
    fun startClassicDiscovery() {
        try {
            if (bluetoothAdapter == null) {
                log("No bluetooth adapter available")
                return
            }
            if (bluetoothAdapter!!.isDiscovering) return
            val ok = bluetoothAdapter!!.startDiscovery()
            log("startClassicDiscovery -> $ok")

        } catch (e: Exception) {
            log("startClassicDiscovery exception: ${e.message}")
        } catch (e: SecurityException) {

        }
    }

    fun cancelClassicDiscovery() {
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            log("cancelClassicDiscovery exception: ${e.message}")
        } catch (e: SecurityException) {
            log("onScanResult security exception: ${e.message}")
        }
    }

    // ----------------- Helpers -----------------
    private fun buildEventFromDevice(device: BluetoothDevice?, action: String): com.velox.jewelvault.data.bluetooth.BluetoothDevice {
        val address = try {
            device?.address ?: "<unknown>"
        } catch (se: SecurityException) {
            "<no-perm>"
        }
        val name = try {
            device?.name
        } catch (se: SecurityException) {
            null
        } catch (e: SecurityException) {
            null
        }
        val type = try {
            device?.type
        } catch (e: Exception) {
            null
        } catch (e: SecurityException) {
            null
        }
        val bond = try {
            device?.bondState
        } catch (e: Exception) {
            null
        } catch (e: SecurityException) {
            null
        }
        val btClass = try {
            device?.bluetoothClass?.toString()
        } catch (e: Exception) {
            null
        } catch (e: SecurityException) {
            null
        }
        var uuidsStr: String? = null
        try {
            val uuids = device?.uuids
            uuidsStr = uuids?.joinToString(",") { it?.uuid?.toString() ?: "<null>" }
            if (uuidsStr.isNullOrEmpty() && hasConnectPermission()) {
                try {
                    device?.fetchUuidsWithSdp()
                } catch (e: Exception) {
                }
            }
        } catch (se: SecurityException) {
            uuidsStr = "<no-perm>"
        }

        return BluetoothDevice(
            address = address,
            device = device,
            action = action,
            name = name,
            type = type,
            bondState = bond,
            bluetoothClass = btClass,
            uuids = uuidsStr
        )
    }

    private fun emit(event: com.velox.jewelvault.data.bluetooth.BluetoothDevice) {
        try {
            // log a short summary
            val summary = mutableListOf<String>()
            event.address.let { summary.add("addr=$it") }
            event.name?.let { summary.add("name=$it") }
            event.rssi?.let { summary.add("rssi=$it") }
            event.uuids?.let { summary.add("uuids=${it.take(120)}") }
            log("${event.action} -> ${summary.joinToString(" | ")}")

            eventListener?.invoke(event)
        } catch (e: Exception) {

        }
    }

    private fun uuidsToString(arr: Array<android.os.Parcelable>?): String {
        return arr?.joinToString(",") {
            try {
                (it as? ParcelUuid)?.uuid?.toString() ?: it.toString()
            } catch (e: Exception) {
                it.toString()
            }
        } ?: "<none>"
    }

    private fun ByteArray?.toHex(): String =
        this?.joinToString(separator = " ") { String.format("%02X", it) } ?: "<empty>"

}

// ----------------- Data model -----------------
data class BluetoothDevice(
    val address: String,
    val device: BluetoothDevice?,
    val action: String,
    val name: String? = null,
    val type: Int? = null,
    val bondState: Int? = null,
    val bluetoothClass: String? = null,
    val rssi: Int? = null,
    val uuids: String? = null,
    val manufacturerData: Map<Int, ByteArray>? = null,
    val serviceData: Map<UUID, ByteArray>? = null,
    val txPower: Int? = null,
    val extraInfo: Map<String, String> = emptyMap()
)

/**
 * Data class for Bluetooth adapter state changes
 */
data class BluetoothStateChange(
    val currentState: Int, val previousState: Int
)