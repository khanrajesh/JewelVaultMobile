package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class InternalBluetoothManager @Inject constructor(
    private val context: Context
) : BroadcastReceiver() {

    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Separate discovery states for Classic and LE scanning
    private val _isClassicDiscovering = MutableStateFlow(false)
    val isClassicDiscovering: StateFlow<Boolean> = _isClassicDiscovering.asStateFlow()

    private val _isLeDiscovering = MutableStateFlow(false)
    val isLeDiscovering: StateFlow<Boolean> = _isLeDiscovering.asStateFlow()

    // Combined discovery state (true if either classic or LE is discovering)

    // Device found events removed; UI relies on device lists instead

    // Device lists - similar to system Bluetooth list
    private val _bondedDevices = MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDeviceDetails>> = _bondedDevices.asStateFlow()

    private val _classicDiscoveredDevices =
        MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())
    val classicDiscoveredDevices: StateFlow<List<BluetoothDeviceDetails>> =
        _classicDiscoveredDevices.asStateFlow()

    private val _leDiscoveredDevices = MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())
    val leDiscoveredDevices: StateFlow<List<BluetoothDeviceDetails>> =
        _leDiscoveredDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())
    val connectedDevices: StateFlow<List<BluetoothDeviceDetails>> = _connectedDevices.asStateFlow()

    // Connecting devices (devices that are currently attempting to connect)
    private val _connectingDevices = MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())
    val connectingDevices: StateFlow<List<BluetoothDeviceDetails>> = _connectingDevices.asStateFlow()


    // Bluetooth adapter state changes
    private val _bluetoothStateChanged = MutableStateFlow(
        BluetoothStateChange(BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_OFF)
    )
    val bluetoothStateChanged: StateFlow<BluetoothStateChange> =
        _bluetoothStateChanged.asStateFlow()


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
    var eventListener: ((BluetoothDeviceDetails) -> Unit)? = null

    // Active GATT connections tracked by address
    private val gattMap = mutableMapOf<String, BluetoothGatt>()

    private val internalBluetoothScanner = InternalBluetoothScanner(
        bluetoothAdapter = bluetoothAdapter,
        bleScannerProvider = { bluetoothLeScanner },
        scope = scope,
        hasScanPermission = ::hasScanPermission,
        hasFineLocationPermission = ::hasFineLocation,
        emitEvent = ::emit,
        buildEvent = ::buildEventFromDevice,
        addLeDevice = { addOrUpdateDevice(_leDiscoveredDevices, it) },
        clearClassicDevices = { _classicDiscoveredDevices.value = emptyList() },
        clearLeDevices = { _leDiscoveredDevices.value = emptyList() },
        updateBondedDevices = ::updateBondedDevices,
        updateConnectedDevices = ::updateConnectedDevices,
        classicDiscoveringState = _isClassicDiscovering,
        leDiscoveringState = _isLeDiscovering,
        log = ::log
    )

    val isDiscovering: StateFlow<Boolean> = internalBluetoothScanner.isDiscovering

    private val bluetoothConnect = InternalBluetoothConnect(
            context = context,
            bluetoothAdapter = bluetoothAdapter,
            bluetoothManager = bluetoothManager,
            scope = scope,
            hasConnectPermission = ::hasConnectPermission,
            stopScanning = ::stopUnifiedScanning,
            startScanning = ::startUnifiedScanning,
            createDeviceEvent = ::buildEventFromDevice,
            updateBondedDevices = ::updateBondedDevices,
            updateConnectedDevices = ::updateConnectedDevices,
            addOrUpdateConnecting = { addOrUpdateDevice(_connectingDevices, it) },
            removeConnecting = { removeDevice(_connectingDevices, it) },
            addOrUpdateConnected = { addOrUpdateDevice(_connectedDevices, it) },
            removeConnected = { removeDevice(_connectedDevices, it) },
            emitEvent = ::emit,
            gattMap = gattMap,
            isDeviceConnected = { addr -> connectedDevices.value.any { it.address == addr } },
            isDeviceConnecting = { addr -> connectingDevices.value.any { it.address == addr } },
            log = ::log
        )


    init {
        internalBluetoothScanner.onStart()
    }

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

            // Start monitoring connected devices
            startConnectedDevicesMonitoring()
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
        log("InternalBluetoothManager: Refreshing system state...")
        scope.launch {
            try {
                // Get current Bluetooth adapter state
                val bluetoothManager =
                    appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bluetoothManager?.adapter

                if (adapter != null) {
                    val currentState = adapter.state
                    log("InternalBluetoothManager: Current adapter state: $currentState")

                    // Emit current state to sync with system
                    _bluetoothStateChanged.value = BluetoothStateChange(
                        currentState = currentState, previousState = currentState
                    )
                    log("InternalBluetoothManager: Emitted initial state: currentState=$currentState")

                    // Check if classic discovery is currently running
                    val isCurrentlyClassicDiscovering = try {
                        if (hasConnectPermission()) {
                            adapter.isDiscovering
                        } else {
                            false
                        }
                    } catch (e: SecurityException) {
                        log("InternalBluetoothManager: SecurityException checking classic discovery state: ${e.message}")
                        false
                    } catch (t: Throwable) {
                        log("InternalBluetoothManager: Error checking classic discovery state: ${t.message}")
                        false
                    }

                    log("InternalBluetoothManager: Current classic discovery state: $isCurrentlyClassicDiscovering")
                    _isClassicDiscovering.value = isCurrentlyClassicDiscovering

                    // Check if LE scanning is currently running
                    val isCurrentlyLeDiscovering = try {

                    } catch (t: Throwable) {
                        log("InternalBluetoothManager: Error checking LE discovery state: ${t.message}")
                        false
                    }

                    // Refresh device lists to sync current state
                    updateBondedDevices()
                    updateConnectedDevices()
                } else {
                    log("InternalBluetoothManager: Bluetooth adapter not available")
                }
            } catch (t: Throwable) {
                log("InternalBluetoothManager: Error refreshing system state: ${t.message}")
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
        log("InternalBluetoothManager: onReceive called with action: $action")
        try {
            when (action) {
                // ACTION_FOUND: Broadcast when a Bluetooth device is discovered during classic discovery
                // - Triggered by: BluetoothAdapter.startDiscovery()
                // - Purpose: Notify that a new device was found during scanning
                // - Frequency: One broadcast per discovered device
                // - Scope: Classic Bluetooth devices only (not BLE)
                BluetoothDevice.ACTION_FOUND -> {
                    log("InternalBluetoothManager: Processing ACTION_FOUND")
                    try {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                            .toInt()
                        val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: device?.name
                        log("InternalBluetoothManager: Found device - name: $name, rssi: $rssi, address: ${device?.address}")
                        val event = buildEventFromDevice(device, action = "ACTION_FOUND").copy(
                            name = name, rssi = rssi
                        )

                        // Add to classic discovered devices list
                        addOrUpdateDevice(_classicDiscoveredDevices, event)

                        emit(event)
                        log("InternalBluetoothManager: Emitted ACTION_FOUND event for device: ${device?.address}")
                    } catch (e: Exception) {
                        log("InternalBluetoothManager: Exception in ACTION_FOUND: ${e.message}")
                    } catch (e: SecurityException) {
                        log("InternalBluetoothManager: SecurityException in ACTION_FOUND: ${e.message}")
                    }
                }

                // ACTION_ACL_CONNECTED: Broadcast when ACL (Asynchronous Connection-Less) link is established
                // - Triggered by: Successful connection to a Bluetooth device
                // - Purpose: Notify that device is now connected and ready for communication
                // - Frequency: Once per successful connection
                // - Scope: Both Classic and BLE devices
                //
                // ACTION_ACL_DISCONNECTED: Broadcast when ACL link is lost/disconnected
                // - Triggered by: Device disconnection (manual or automatic)
                // - Purpose: Notify that device connection has been lost
                // - Frequency: Once per disconnection
                // - Scope: Both Classic and BLE devices
                //
                // ACTION_ACL_DISCONNECT_REQUESTED: Broadcast when disconnection is requested
                // - Triggered by: Disconnect request initiated (before actual disconnection)
                // - Purpose: Notify that disconnection process has started
                // - Frequency: Once per disconnect request
                // - Scope: Both Classic and BLE devices
                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                    log("InternalBluetoothManager: Processing ACL action: $action")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    log("InternalBluetoothManager: ACL device - address: ${device?.address}, name: ${device?.name}")
                    val event = buildEventFromDevice(device, action = action)

                    when (action) {
                        BluetoothDevice.ACTION_ACL_CONNECTED -> {
                            log("CONNECT: ACL connected - Adding device ${event.address} (${event.name}) to connected list")
                            // Add to connected devices list
                            addOrUpdateDevice(_connectedDevices, event)
                            // Also refresh the full connected devices list to catch any missed devices
                            updateConnectedDevices()
                        }

                        BluetoothDevice.ACTION_ACL_DISCONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                            log("CONNECT: ACL disconnected - Removing device ${event.address} (${event.name}) from connected list")
                            // Remove from connected devices list
                            removeDevice(_connectedDevices, event.address)
                            // Also refresh the full connected devices list to ensure accuracy
                            updateConnectedDevices()
                        }
                    }

                    emit(event)
                    log("InternalBluetoothManager: Emitted ACL event for device: ${device?.address}")
                }

                // ACTION_BOND_STATE_CHANGED: Broadcast when device bonding/pairing state changes
                // - Triggered by: Pairing process, bonding success/failure, unbonding
                // - Purpose: Track pairing status changes (bonded, not bonded, bonding)
                // - Frequency: Multiple times during pairing process
                // - Scope: Both Classic and BLE devices
                // - States: BOND_NONE(10), BOND_BONDING(11), BOND_BONDED(12)
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    log("InternalBluetoothManager: Processing ACTION_BOND_STATE_CHANGED")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                    log("InternalBluetoothManager: Bond state changed - device: ${device?.address}, state: $state, prev: $prev")
                    val event = buildEventFromDevice(
                        device, action = "ACTION_BOND_STATE_CHANGED", extraInfo = mapOf(
                            "bondState" to state.toString(), "prevBondState" to prev.toString()
                        )
                    )

                    // Update bonded devices list based on bond state
                    when (state) {
                        BluetoothDevice.BOND_BONDED -> {
                            addOrUpdateDevice(_bondedDevices, event)
                        }

                        BluetoothDevice.BOND_NONE -> {
                            removeDevice(_bondedDevices, event.address)
                        }
                    }

                    emit(event)
                    log("InternalBluetoothManager: Emitted BOND_STATE_CHANGED event for device: ${device?.address}")
                }

                // ACTION_PAIRING_REQUEST: Broadcast when device requests pairing/authentication
                // - Triggered by: Device initiating pairing process (PIN, passkey, etc.)
                // - Purpose: Handle pairing authentication requests from remote devices
                // - Frequency: Once per pairing attempt
                // - Scope: Both Classic and BLE devices
                // - Variants: PIN, PASSKEY, CONSENT, DISPLAY_PASSKEY, DISPLAY_PIN
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    log("InternalBluetoothManager: Processing ACTION_PAIRING_REQUEST")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
                    val key = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, -1)
                    log("InternalBluetoothManager: Pairing request - device: ${device?.address}, variant: $variant, key: $key")
                    val event =
                        buildEventFromDevice(device, action = "ACTION_PAIRING_REQUEST").copy(
                            extraInfo = mapOf(
                                "pairingVariant" to variant.toString(),
                                "pairingKey" to key.toString()
                            )
                        )
                    log("InternalBluetoothManager: ACTION_PAIRING_REQUEST event: $event")
                    emit(event)
                    log("InternalBluetoothManager: Emitted PAIRING_REQUEST event for device: ${device?.address}")
                }

                // ACTION_NAME_CHANGED: Broadcast when device name changes
                // - Triggered by: Device name update, SDP (Service Discovery Protocol) completion
                // - Purpose: Notify when device name becomes available or changes
                // - Frequency: Once when name is discovered/updated
                // - Scope: Both Classic and BLE devices
                // - Note: Some devices may not broadcast name changes
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    log("InternalBluetoothManager: Processing ACTION_NAME_CHANGED")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                    log("InternalBluetoothManager: Name changed - device: ${device?.address}, new name: $name")
                    val event = buildEventFromDevice(device, action = "ACTION_NAME_CHANGED").copy(
                        name = name ?: "<null>"
                    )
                    emit(event)
                    log("InternalBluetoothManager: Emitted NAME_CHANGED event for device: ${device?.address}")
                }

                // ACTION_UUID: Broadcast when device UUIDs are discovered/updated
                // - Triggered by: SDP (Service Discovery Protocol) completion, UUID fetch
                // - Purpose: Notify when device services/UUIDs become available
                // - Frequency: Once when UUIDs are discovered
                // - Scope: Both Classic and BLE devices
                // - Note: May be triggered by fetchUuidsWithSdp() call
                BluetoothDevice.ACTION_UUID -> {
                    log("InternalBluetoothManager: Processing ACTION_UUID")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                    log("InternalBluetoothManager: UUID action - device: ${device?.address}, uuids count: ${uuids?.size}")
                    val event = buildEventFromDevice(device, action = "ACTION_UUID").copy(
                        uuids = uuidsToString(uuids)
                    )
                    emit(event)
                    log("InternalBluetoothManager: Emitted UUID event for device: ${device?.address}")
                }

                // ACTION_DISCOVERY_STARTED: Broadcast when classic Bluetooth discovery begins
                // - Triggered by: BluetoothAdapter.startDiscovery()
                // - Purpose: Notify that device discovery has started
                // - Frequency: Once per discovery session
                // - Scope: Classic Bluetooth discovery only
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    internalBluetoothScanner.onClassicDiscoveryStarted()
                }

                // ACTION_DISCOVERY_FINISHED: Broadcast when classic Bluetooth discovery ends
                // - Triggered by: Discovery timeout (12s) or BluetoothAdapter.cancelDiscovery()
                // - Purpose: Notify that device discovery has completed
                // - Frequency: Once per discovery session
                // - Scope: Classic Bluetooth discovery only
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    internalBluetoothScanner.onClassicDiscoveryFinished()
                }

                // ACTION_STATE_CHANGED: Broadcast when Bluetooth adapter state changes
                // - Triggered by: Bluetooth on/off, adapter enable/disable
                // - Purpose: Track overall Bluetooth system state
                // - Frequency: Once per state change
                // - States: STATE_OFF(10), STATE_TURNING_ON(11), STATE_ON(12), STATE_TURNING_OFF(13)
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    val prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1)
                    log("Adapter state changed: prev=$prev -> state=$state")
                    _bluetoothStateChanged.value = BluetoothStateChange(
                        currentState = state, previousState = prev
                    )
                    log("BluetoothBroadcastReceiver: Emitted state change: currentState=$state, previousState=$prev")
                }

                // ACTION_SCAN_MODE_CHANGED: Broadcast when adapter scan mode changes
                // - Triggered by: Discoverability changes, adapter mode changes
                // - Purpose: Track device discoverability state
                // - Frequency: Once per mode change
                // - Modes: SCAN_MODE_NONE(20), SCAN_MODE_CONNECTABLE(21), SCAN_MODE_CONNECTABLE_DISCOVERABLE(23)
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    log("InternalBluetoothManager: Processing ACTION_SCAN_MODE_CHANGED")
                    val mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
                    val prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1)
                    log("InternalBluetoothManager: Scan mode changed: prev=$prev -> mode=$mode")
                }

                else -> {
                    log("InternalBluetoothManager: Unhandled action: $action")
                }
            }
        } catch (t: Throwable) {
            log("InternalBluetoothManager: Exception handling $action: ${t.message}")
            t.printStackTrace()
        } catch (se: SecurityException) {
            log("InternalBluetoothManager: Security exception handling $action: ${se.message}")
            se.printStackTrace()
        }
        log("InternalBluetoothManager: onReceive completed for action: $action")
    }

    // BLE scan controls are internalized in unified/continuous APIs below

    // ----------------- GATT Connect / Disconnect -----------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(address: String) {
        bluetoothConnect.connect(address)
    }

    fun disconnectDevice(address: String) {
        log("CONNECT: Starting disconnect process for device: $address")

        val gatt = gattMap[address]
        if (gatt != null) {
            log("CONNECT: Found GATT connection for $address, initiating disconnect")
            try {
                gatt.disconnect()
                log("CONNECT: Disconnect request sent for $address")
                gatt.close()
                log("CONNECT: GATT closed for $address")
            } catch (e: Exception) {
                log("CONNECT: ERROR - Exception during disconnect for $address: ${e.message}")
            } catch (e: SecurityException) {
                log("CONNECT: ERROR - Security exception during disconnect for $address: ${e.message}")
            }
            gattMap.remove(address)
            log("CONNECT: Device $address removed from GATT map")
            emit(
                buildEventFromDevice(
                    null, action = "GATT_DISCONNECT_REQUESTED"
                ).copy(address = address)
            )
            log("CONNECT: Disconnect event emitted for $address")
        } else {
            log("CONNECT: WARNING - No GATT connection found for $address")
        }
    }

    fun disconnectAll() {
        val keys = gattMap.keys.toList()
        keys.forEach { disconnectDevice(it) }
    }

    // ----------------- Device List Management Functions -----------------
    /**
     * Clears all discovered device lists (both classic and LE)
     */
    fun clearDiscoveredDevices() {
        _classicDiscoveredDevices.value = emptyList()
        _leDiscoveredDevices.value = emptyList()
        log("Cleared all discovered device lists")
    }

    /**
     * Clears only classic discovered devices list
     */
    fun clearClassicDiscoveredDevices() {
        _classicDiscoveredDevices.value = emptyList()
        log("Cleared classic discovered devices list")
    }

    /**
     * Clears only LE discovered devices list
     */
    fun clearLeDiscoveredDevices() {
        _leDiscoveredDevices.value = emptyList()
        log("Cleared LE discovered devices list")
    }

    /**
     * Refreshes all device lists from system state
     */
    fun refreshAllDeviceLists() {
        updateBondedDevices()
        updateConnectedDevices()
        log("CONNECT: Refreshed all device lists from system")
    }

    /**
     * Manually refreshes connected devices list
     */
    fun refreshConnectedDevices() {
        updateConnectedDevices()
        log("CONNECT: Manually refreshed connected devices list")
    }



    /**
     * Gets a discovered device by address from any of the discovered lists
     */
    fun getDiscoveredDevice(address: String): BluetoothDevice? {
        return getDiscoveredClassicDevice(address)?.device ?: getDiscoveredLeDevice(address)?.device
        ?: getBondedDevice(address)?.device
    }

    fun getDiscoveredClassicDevice(address: String): BluetoothDeviceDetails? {
        return _classicDiscoveredDevices.value.firstOrNull { it.address == address }
    }

    fun getDiscoveredLeDevice(address: String): BluetoothDeviceDetails? {
        return _leDiscoveredDevices.value.firstOrNull { it.address == address }
    }

    fun getBondedDevice(address: String): BluetoothDeviceDetails? {
        return _bondedDevices.value.firstOrNull { it.address == address }
    }

    

    /**
     * Creates a bond with the specified device
     */
    fun createBond(address: String): Boolean {
        return try {
            log("PAIR: Attempting to create bond with device: $address")
            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device != null) {
                log("PAIR: Device found: ${device.name} (${device.address})")
                log("PAIR: Current bond state: ${device.bondState}")

                if (device.bondState == BluetoothDevice.BOND_BONDED) {
                    log("PAIR: Device $address is already bonded")
                    return true
                }

                val result = device.createBond()
                log("PAIR: createBond() returned: $result")
                result
            } else {
                log("PAIR: ERROR - Could not get remote device for $address")
                false
            }
        } catch (e: SecurityException) {
            log("PAIR: SecurityException creating bond: ${e.message}")
            false
        } catch (e: Exception) {
            log("PAIR: Exception creating bond: ${e.message}")
            false
        }
    }

    

    

    /**
     * Starts periodic refresh of connected devices list
     */
    fun startConnectedDevicesMonitoring() {
        scope.launch {
            while (true) {
                delay(5000) // Refresh every 5 seconds
                updateConnectedDevices()
            }
        }
    }

    // ----------------- Utility getters removed (not used by UI) -----------------

    

    // ----------------- Classic discovery helpers -----------------
    fun startClassicDiscovery() {
        internalBluetoothScanner.startClassicDiscovery()
    }

    fun cancelClassicDiscovery() {
        internalBluetoothScanner.stopClassicDiscovery()
    }

    // ----------------- Unified scanning functions -----------------
    /**
     * Starts both LE (Low Energy) and classic Bluetooth scanning simultaneously.
     * Discovery states are managed by the system through broadcast receivers.
     * Scanning automatically stops after 60 seconds.
     *
     * @param scanSettings Optional BLE scan settings (defaults to low latency mode)
     * @param scanFilters Optional BLE scan filters
     */
    fun startUnifiedScanning(
        scanSettings: ScanSettings? = null, scanFilters: List<ScanFilter>? = null
    ) {
        internalBluetoothScanner.startUnifiedScan(scanSettings, scanFilters)
    }

    /**
     * Stops both LE (Low Energy) and classic Bluetooth scanning.
     * Discovery states are managed by the system through broadcast receivers.
     */
    fun stopUnifiedScanning() {
        internalBluetoothScanner.stopUnifiedScan()
    }

    fun startContinuousScanning(
        scanSettings: ScanSettings? = null, scanFilters: List<ScanFilter>? = null
    ) {
        internalBluetoothScanner.startContinuousScan(scanSettings, scanFilters)
    }

    fun restartScanning() {
        internalBluetoothScanner.restartScan()
    }

    // ----------------- Device List Management -----------------
    /**
     * Adds or updates a device in the specified list, ensuring no duplicates by address
     */
    private fun addOrUpdateDevice(
        deviceList: MutableStateFlow<List<BluetoothDeviceDetails>>,
        newDevice: BluetoothDeviceDetails
    ) {
        deviceList.value = deviceList.value.toMutableList().apply {
            // Remove existing device with same address
            removeAll { it.address == newDevice.address }
            // Add new device
            add(newDevice)
        }
    }

    /**
     * Removes a device from the specified list by address
     */
    private fun removeDevice(
        deviceList: MutableStateFlow<List<BluetoothDeviceDetails>>, address: String
    ) {
        deviceList.value = deviceList.value.filter { it.address != address }
    }

    /**
     * Updates bonded devices list from system
     */
    private fun updateBondedDevices() {
        try {
            val bondedSet = bluetoothAdapter?.bondedDevices ?: emptySet()
            val bondedList = bondedSet.map { buildEventFromDevice(it, action = "BONDED_DEVICE") }
            _bondedDevices.value = bondedList
            log("Updated bonded devices: ${bondedList.size} devices")
        } catch (e: SecurityException) {
            log("SecurityException updating bonded devices: ${e.message}")
        } catch (e: Exception) {
            log("Error updating bonded devices: ${e.message}")
        }
    }

    /**
     * Updates connected devices list from system
     */
    private fun updateConnectedDevices() {
        try {
            log("CONNECT: Updating connected devices list from system")
            val connectedList = mutableListOf<BluetoothDeviceDetails>()

            // Get GATT connected devices
            val gattConnected = try {
                bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            } catch (e: Exception) {
                log("CONNECT: Error getting GATT connected devices: ${e.message}")
                emptyList<BluetoothDevice>()
            }
            log("CONNECT: Found ${gattConnected.size} GATT connected devices")
            gattConnected.forEach { device ->
                val deviceDetails = buildEventFromDevice(device, action = "CONNECTED_DEVICE")
                connectedList.add(deviceDetails)
                log("CONNECT: Added GATT connected device: ${device.address} (${device.name})")
            }

            // Try to get connected devices using reflection (for devices that don't support profile queries)
            try {
                val bondedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
                log("CONNECT: Checking ${bondedDevices.size} bonded devices for connection status")

                bondedDevices.forEach { device ->
                    try {
                        // Try to get connection state using reflection
                        val connectionState = getDeviceConnectionState(device)
                        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                            val deviceDetails =
                                buildEventFromDevice(device, action = "CONNECTED_DEVICE")
                            connectedList.add(deviceDetails)
                            log("CONNECT: Added connected bonded device: ${device.address} (${device.name})")
                        }
                    } catch (e: Exception) {
                        log("CONNECT: Error checking connection state for ${device.address}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                log("CONNECT: Error checking bonded devices: ${e.message}")
            }

            // Include our own GATT connections (only actually connected ones)
            log("CONNECT: Checking our own GATT connections: ${gattMap.size} devices")
            gattMap.keys.forEach { addr ->
                try {
                    val dev = bluetoothAdapter?.getRemoteDevice(addr)
                    if (dev != null) {
                        val gatt = gattMap[addr]
                        // Only add to connected list if GATT has services (meaning connection is complete)
                        val isActuallyConnected = gatt?.services?.isNotEmpty() == true

                        if (isActuallyConnected) {
                            val deviceDetails =
                                buildEventFromDevice(dev, action = "CONNECTED_DEVICE")
                            connectedList.add(deviceDetails)
                            log("CONNECT: Added our GATT connection: ${dev.address} (${dev.name})")
                        } else {
                            log("CONNECT: GATT device ${dev.address} is still connecting (services: ${gatt?.services?.size ?: 0}), not adding to connected list")
                        }
                    }
                } catch (e: Exception) {
                    log("CONNECT: Error getting our GATT device $addr: ${e.message}")
                }
            }

            // Preserve existing classic connections that aren't detected by system
            val currentConnected = _connectedDevices.value
            val classicConnections = currentConnected.filter {
                it.action.contains("CLASSIC_CONNECTED") || it.action.contains("AUDIO_CONNECTED") || it.action.contains(
                    "REAL_CONNECTED"
                )
            }
            log("CONNECT: Preserving ${classicConnections.size} classic connections")

            // Add classic connections that aren't already in the list
            classicConnections.forEach { classicDevice ->
                if (connectedList.none { it.address == classicDevice.address }) {
                    connectedList.add(classicDevice)
                    log("CONNECT: Preserved classic connection: ${classicDevice.address} (${classicDevice.name})")
                }
            }

            // Remove duplicates and update the list
            val uniqueDevices = connectedList.distinctBy { it.address }
            _connectedDevices.value = uniqueDevices
            log("CONNECT: Updated connected devices list: ${uniqueDevices.size} unique devices")
            uniqueDevices.forEach { device ->
                log("CONNECT: Connected device: ${device.address} (${device.name})")
            }
        } catch (e: SecurityException) {
            log("CONNECT: ERROR - Security exception updating connected devices: ${e.message}")
        } catch (e: Exception) {
            log("CONNECT: ERROR - Error updating connected devices: ${e.message}")
        }
    }

    /**
     * Gets device connection state using reflection
     */
    private fun getDeviceConnectionState(device: BluetoothDevice): Int {
        return try {
            // Try to get connection state using reflection
            val method = device.javaClass.getMethod("getConnectionState")
            method.invoke(device) as Int
        } catch (e: Exception) {
            // If reflection fails, assume not connected
            BluetoothProfile.STATE_DISCONNECTED
        }
    }

    // ----------------- Helpers -----------------
    fun buildEventFromDevice(
        device: BluetoothDevice?,
        address: String? = null,
        action: String,
        name: String? = null,
        extraInfo: Map<String, String>? = null,
        uuids: String? = null
    ): BluetoothDeviceDetails {
        val address = address ?: try {
            device?.address ?: "<unknown>"
        } catch (se: SecurityException) {
            "<no-perm>"
        }
        val name = name ?: try {
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
        var uuidsStr: String? = uuids
        if (uuidsStr == null) {
            try {
                val u = device?.uuids
                uuidsStr = u?.joinToString(",") { it?.uuid?.toString() ?: "<null>" }
                if (uuidsStr.isNullOrEmpty() && hasConnectPermission()) {
                    try {
                        device?.fetchUuidsWithSdp()
                    } catch (e: Exception) {
                    }
                }
            } catch (se: SecurityException) {
                uuidsStr = "<no-perm>"
            }
        }

        return BluetoothDeviceDetails(
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

    private fun emit(event: BluetoothDeviceDetails) {
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
data class BluetoothDeviceDetails(
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