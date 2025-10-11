package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.RequiresPermission
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.permissions.hasConnectPermission
import com.velox.jewelvault.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BleManager @Inject constructor(
    private val context: Context,
    private val dataStoreManager: DataStoreManager
) {

    val showLog = true
    fun cLog(msg: String) {
        if (showLog) log(msg)
    }

    private val appContext: Context = context.applicationContext
    val bleManagerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Separate discovery states for Classic and LE scanning
    val isClassicDiscovering = MutableStateFlow(false)
    val isLeDiscovering = MutableStateFlow(false)


    // Device lists - similar to system Bluetooth list
    val bondedDevices = MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())
    val classicDiscoveredDevices = MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())
    val leDiscoveredDevices = MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())
    val connectedDevices = MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())
    val connectingDevices = MutableStateFlow<List<BluetoothDeviceDetails>>(emptyList())



    // Bluetooth adapter state changes
    val bluetoothStateChanged = MutableStateFlow(
        BluetoothStateChange(
            currentState = BluetoothAdapter.STATE_OFF, previousState = BluetoothAdapter.STATE_OFF
        )
    )

    private val bluetoothManager: BluetoothManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val bluetoothAdapter: BluetoothAdapter by lazy { bluetoothManager.adapter }


    // Active GATT connections tracked by address
    private val gattMap = mutableMapOf<String, BluetoothGatt>()

    // Live transport state tracking for enrichment
    val aclConnected = mutableSetOf<String>()
//    private val rfcommConnected = mutableSetOf<String>()
//    private val a2dpConnected = mutableSetOf<String>()
//    private val headsetConnected = mutableSetOf<String>()
//    private val hidConnected = mutableSetOf<String>()

    // Screen-specific monitoring
    private var isScanConnectScreenActive = false
    private var monitoringJob: kotlinx.coroutines.Job? = null

    // Connection success callbacks for printer saving
    private val connectionSuccessCallbacks = mutableMapOf<String, (String) -> Unit>()

    private val bleReceiver: BleBroadcastReceiver = BleBroadcastReceiver(context, this)

    val bleScanner = BleScanner(
        addLeDevice = { addOrUpdateDevice(leDiscoveredDevices, it) },
        classicDiscoveringState = isClassicDiscovering,
        leDiscoveringState = isLeDiscovering,
        manager = this,
        context = context
    )

    val isDiscovering: StateFlow<Boolean> = bleScanner.isDiscovering

    val bluetoothConnect = BleConnect(
        context = context,
        updateConnecting = { addOrUpdateDevice(connectingDevices, it) },
        removeConnecting = { removeDevice(connectingDevices, it) },
        removeConnected = { removeDevice(connectedDevices, it) },
        gattMap = gattMap,
//        isDeviceConnected = { addr -> connectedDevices.value.any { it.address == addr } },
//        isDeviceConnecting = { addr -> connectingDevices.value.any { it.address == addr } },
        manager = this
    )


    init {
        bleScanner.onStart()
    }

    // ----------------- Registration helpers -----------------
    fun registerReceiver() {
        bleReceiver.registerBleReceiver(onComplete = {
            refreshSystemState()
            startConnectedDevicesMonitoring()
        })
    }

    fun unregister() {
        bleReceiver.unregisterBleReceiver()
    }

    fun isRegistered(): Boolean = bleReceiver.isReceiverRegistered


    /**
     * Refresh and sync with current system state when receiver registers
     */
    fun refreshSystemState() {
        cLog("BleManager: Refreshing system state...")
        bleManagerScope.launch {
            try {
                // Get current Bluetooth adapter state
                val bluetoothManager =
                    appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bluetoothManager?.adapter

                if (adapter != null) {
                    val currentState = adapter.state
                    cLog("BleManager: Current adapter state: $currentState")

                    // Emit current state to sync with system
                    bluetoothStateChanged.value = BluetoothStateChange(
                        currentState = currentState, previousState = currentState
                    )
                    cLog("BleManager: Emitted initial state: currentState=$currentState")

                    // Check if classic discovery is currently running
                    val isCurrentlyClassicDiscovering = try {
                        if (hasConnectPermission(context)) {
                            adapter.isDiscovering
                        } else {
                            false
                        }
                    } catch (e: SecurityException) {
                        cLog("BleManager: SecurityException checking classic discovery state: ${e.message}")
                        false
                    } catch (t: Throwable) {
                        cLog("BleManager: Error checking classic discovery state: ${t.message}")
                        false
                    }

                    cLog("BleManager: Current classic discovery state: $isCurrentlyClassicDiscovering")
                    isClassicDiscovering.value = isCurrentlyClassicDiscovering

                    // Check if LE scanning is currently running
                    try {

                    } catch (t: Throwable) {
                        cLog("BleManager: Error checking LE discovery state: ${t.message}")
                        false
                    }

                    // Refresh device lists to sync current state
                    updateBondedDevices()
                    updateConnectedDevices()
                } else {
                    cLog("BleManager: Bluetooth adapter not available")
                }
            } catch (t: Throwable) {
                cLog("BleManager: Error refreshing system state: ${t.message}")
            }
        }
    }


    // BLE scan controls are internalized in unified/continuous APIs below

    // ----------------- GATT Connect / Disconnect -----------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(address: String) {
        bluetoothConnect.connect(address)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnectDevice(address: String) {
        cLog("BleManager: disconnectDevice: Starting disconnect process for device: $address")

        // Delegate to connector to handle RFCOMM/GATT/profiles end-to-end
        try {
            bluetoothConnect.disconnect(address)
        } catch (t: Throwable) {
            cLog("BleManager: disconnectDevice: ERROR - disconnect delegation failed for $address -> ${t.message}")
        }

        // Best-effort GATT cleanup in case delegate couldn't find it
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
        }

        // Remove from our connected/connecting lists immediately; periodic refresh will reconcile
        removeDevice(connectedDevices, address)
        removeDevice(connectingDevices, address)

        // Trigger an immediate refresh to pull latest system state
        updateConnectedDevices()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnectAll() {
        val keys = gattMap.keys.toList()
        keys.forEach { disconnectDevice(it) }
    }

    // ----------------- Device List Management Functions -----------------
    /**
     * Clears all discovered device lists (both classic and LE)
     */
    fun clearDiscoveredDevices() {
        classicDiscoveredDevices.value = emptyList()
        leDiscoveredDevices.value = emptyList()
        cLog("Cleared all discovered device lists")
    }

    /**
     * Clears only classic discovered devices list
     */
    fun clearClassicDiscoveredDevices() {
        classicDiscoveredDevices.value = emptyList()
        cLog("Cleared classic discovered devices list")
    }

    /**
     * Clears only LE discovered devices list
     */
    fun clearLeDiscoveredDevices() {
        leDiscoveredDevices.value = emptyList()
        cLog("Cleared LE discovered devices list")
    }

    /**
     * Refreshes all device lists from system state
     */
    fun refreshAllDeviceLists() {
        updateBondedDevices()
        updateConnectedDevices()
        cLog("CONNECT: Refreshed all device lists from system")
    }



    /**
     * Gets a discovered device by address from any of the discovered lists
     */
    fun getDiscoveredDevice(address: String): BluetoothDevice? {
        return getDiscoveredClassicDevice(address)?.device ?: getDiscoveredLeDevice(address)?.device
        ?: getBondedDevice(address)?.device
    }

    fun getDiscoveredClassicDevice(address: String): BluetoothDeviceDetails? {
        return classicDiscoveredDevices.value.firstOrNull { it.address == address }
    }

    fun getDiscoveredLeDevice(address: String): BluetoothDeviceDetails? {
        return leDiscoveredDevices.value.firstOrNull { it.address == address }
    }

    fun getBondedDevice(address: String): BluetoothDeviceDetails? {
        return bondedDevices.value.firstOrNull { it.address == address }
    }


    /**
     * Creates a bond with the specified device
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun createBond(address: String): Boolean {
        return try {
            cLog("PAIR: Attempting to create bond with device: $address")
            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device != null) {
                cLog("PAIR: Device found: ${device.name} (${device.address})")
                cLog("PAIR: Current bond state: ${device.bondState}")

                if (device.bondState == BluetoothDevice.BOND_BONDED) {
                    cLog("PAIR: Device $address is already bonded")
                    return true
                }

                val result = device.createBond()
                cLog("PAIR: createBond() returned: $result")
                result
            } else {
                cLog("PAIR: ERROR - Could not get remote device for $address")
                false
            }
        } catch (e: SecurityException) {
            cLog("PAIR: SecurityException creating bond: ${e.message}")
            false
        } catch (e: Exception) {
            cLog("PAIR: Exception creating bond: ${e.message}")
            false
        }
    }

    /**
     * Removes a bond with the specified device
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun removeBond(address: String): Boolean {
        return try {
            cLog("PAIR: Attempting to remove bond with device: $address")
            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device != null) {
                cLog("PAIR: Device found: ${device.name} (${device.address})")
                cLog("PAIR: Current bond state: ${device.bondState}")

                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    cLog("PAIR: Device $address is already not bonded")
                    return true
                }

                val method = device.javaClass.getMethod("removeBond")
                val result = method.invoke(device) as? Boolean ?: false

                cLog("PAIR: removeBond() returned: $result")
                result
            } else {
                cLog("PAIR: ERROR - Could not get remote device for $address")
                false
            }
        } catch (e: SecurityException) {
            cLog("PAIR: SecurityException remove bond: ${e.message}")
            false
        } catch (e: Exception) {
            cLog("PAIR: Exception remove bond: ${e.message}")
            false
        }
    }


    /**
     * Starts periodic refresh of connected devices list
     */
    fun startConnectedDevicesMonitoring() {
        bleManagerScope.launch {
            var fastUntil = 0L
            while (true) {
                val now = System.currentTimeMillis()
                val interval = if (now < fastUntil) 1000L else 5000L
                delay(interval)
                updateConnectedDevices()
                // if any connecting devices exist, temporarily increase cadence
                if (connectingDevices.value.isNotEmpty()) {
                    fastUntil = now + 15_000
                }
            }
        }
    }

    /**
     * Sets ScanConnectScreen as active and starts 5-second monitoring
     */
    fun setScanConnectScreenActive(active: Boolean) {
        isScanConnectScreenActive = active
        if (active) {
            startScanConnectScreenMonitoring()
        } else {
            stopScanConnectScreenMonitoring()
        }
    }

    /**
     * Starts 5-second monitoring specifically for ScanConnectScreen
     */
    private fun startScanConnectScreenMonitoring() {
        // Cancel existing monitoring if any
        monitoringJob?.cancel()
        
        monitoringJob = bleManagerScope.launch {
            while (isScanConnectScreenActive) {
                delay(5000) // 5 seconds
                if (isScanConnectScreenActive) {
                    cLog("ScanConnectScreen: Updating connected devices (5-second interval)")
                    updateConnectedDevices()
                }
            }
        }
    }

    /**
     * Stops ScanConnectScreen-specific monitoring
     */
    private fun stopScanConnectScreenMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        cLog("ScanConnectScreen: Stopped 5-second monitoring")
    }

    // ----------------- Utility getters removed (not used by UI) -----------------


    // ----------------- Classic discovery helpers -----------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startClassicDiscovery() {
        bleScanner.startClassicDiscovery()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun cancelClassicDiscovery() {
        bleScanner.stopClassicDiscovery()
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
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startUnifiedScanning(
        scanSettings: ScanSettings? = null, scanFilters: List<ScanFilter>? = null
    ) {
        bleScanner.startUnifiedScan(scanSettings, scanFilters)
    }

    /**
     * Stops both LE (Low Energy) and classic Bluetooth scanning.
     * Discovery states are managed by the system through broadcast receivers.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopUnifiedScanning() {
        bleScanner.stopUnifiedScan()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startContinuousScanning(
        scanSettings: ScanSettings? = null, scanFilters: List<ScanFilter>? = null
    ) {
        bleScanner.startContinuousScan(scanSettings, scanFilters)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun restartScanning() {
        bleScanner.restartScan()
    }


    fun addOrUpdateConnectedDeviceList( devices: List<BluetoothDeviceDetails>){
        connectedDevices.value = connectedDevices.value.toMutableList().apply {
            devices.forEach {device->
                removeAll { it.address == device.address }
                add(device)
            }
        }
    }


    // ----------------- Device List Management -----------------
    /**
     * Adds or updates a device in the specified list, ensuring no duplicates by address
     */
    fun addOrUpdateDevice(
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
    fun removeDevice(
        deviceList: MutableStateFlow<List<BluetoothDeviceDetails>>, address: String
    ) {
        deviceList.value = deviceList.value.filter { it.address != address }
    }

    // Expose a minimal helper so UI can reflect a connecting/paring placeholder immediately
    val classicTimestamps = mutableMapOf<String, Long>()

    fun addConnectingPlaceholder(
        address: String, name: String? = null, extraInfo: Map<String, String>? = null
    ) {
        val evt = buildBluetoothDevice(
            device = null,
            address = address,
            action = "CONNECTING",
            name = name,
            extraInfo = extraInfo,
            uuids = null
        )
        addOrUpdateDevice(connectingDevices, evt)
    }


    /**
     * Updates bonded devices list from system
     */
    fun updateBondedDevices() {
        try {
            val bondedSet = bluetoothAdapter?.bondedDevices ?: emptySet()
            val bondedList = bondedSet.map { buildBluetoothDevice(it, action = "BONDED_DEVICE") }
            bondedDevices.value = bondedList
            cLog("Updated bonded devices: ${bondedList.size} devices")
        } catch (e: SecurityException) {
            cLog("SecurityException updating bonded devices: ${e.message}")
        } catch (e: Exception) {
            cLog("Error updating bonded devices: ${e.message}")
        }
    }

    /**
     * Gets GATT connected devices from system
     */
    fun getGattConnectedDevices(): List<BluetoothDeviceDetails> {
        return try {
            val gattConnected = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            cLog("getGattConnectedDevices: Found ${gattConnected.size} GATT connected devices")
            gattConnected.map { device ->
                val d = buildBluetoothDevice(device, action = "CONNECTED_DEVICE")
                cLog("getGattConnectedDevices: Added GATT connected device: ${device.address} (${device.name})")
                d
            }
        } catch (e: Exception) {
            cLog("getGattConnectedDevices: Error getting GATT connected devices: ${e.message}")
            emptyList()
        }
    }

    /**
     * Gets connected bonded devices using reflection
     */
    fun getConnectedBondedDevices(): List<BluetoothDeviceDetails> {
        return try {
                val bondedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
            cLog("getConnectedBondedDevices: Checking ${bondedDevices.size} bonded devices for connection status")
                bondedDevices.mapNotNull { device ->
                    try {
                        val connectionState = getDeviceConnectionState(device)
                        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                            val d = buildBluetoothDevice(device, action = "CONNECTED_DEVICE")
                        cLog("getConnectedBondedDevices: Added connected bonded device: ${device.address} (${device.name})")
                            d
                        } else null
                    } catch (e: Exception) {
                    cLog("getConnectedBondedDevices: Error checking connection state for ${device.address}: ${e.message}")
                        null
                    }
                }
            } catch (e: Exception) {
            cLog("getConnectedBondedDevices: Error checking bonded devices: ${e.message}")
                emptyList()
            }
    }

    /**
     * Gets our own GATT connections (only actually connected ones)
     * Also cleans up disconnected GATT connections
     */
    fun getOurGattConnections(): List<BluetoothDeviceDetails> {
        cLog("getOurGattConnections: Checking our own GATT connections: ${gattMap.size} devices")
        
        val connectedDevices = mutableListOf<BluetoothDeviceDetails>()
        val disconnectedAddresses = mutableListOf<String>()
        
        gattMap.keys.forEach { addr ->
                try {
                    val dev = bluetoothAdapter?.getRemoteDevice(addr)
                    if (dev != null) {
                        val gatt = gattMap[addr]
                        val isActuallyConnected = gatt?.services?.isNotEmpty() == true
                    
                        if (isActuallyConnected) {
                            val d = buildBluetoothDevice(dev, action = "CONNECTED_DEVICE")
                        connectedDevices.add(d)
                        cLog("getOurGattConnections: Added our GATT connection: ${dev.address} (${dev.name})")
                        } else {
                        cLog("getOurGattConnections: GATT device ${dev.address} is still connecting (services: ${gatt?.services?.size ?: 0}), marking for cleanup")
                        disconnectedAddresses.add(addr)
                        }
                } else {
                    cLog("getOurGattConnections: GATT device $addr not found in adapter, marking for cleanup")
                    disconnectedAddresses.add(addr)
                }
                } catch (e: Exception) {
                cLog("getOurGattConnections: Error checking our GATT device $addr: ${e.message}, marking for cleanup")
                disconnectedAddresses.add(addr)
            }
        }
        
        // Clean up disconnected GATT connections
        if (disconnectedAddresses.isNotEmpty()) {
            cLog("getOurGattConnections: Cleaning up ${disconnectedAddresses.size} disconnected GATT connections")
            disconnectedAddresses.forEach { addr ->
                try {
                    gattMap[addr]?.close()
                    gattMap.remove(addr)
                    cLog("getOurGattConnections: Cleaned up disconnected GATT connection for $addr")
                } catch (e: Exception) {
                    cLog("getOurGattConnections: Error cleaning up GATT connection for $addr: ${e.message}")
                }
            }
        }
        
        return connectedDevices
    }

    /**
     * Enhanced RFComm connection check with multiple validation methods
     */
    fun isRfcommDeviceConnected(address: String): Boolean {
        return try {
            val socket = bluetoothConnect.rfcommSockets[address]
            if (socket == null) {
                cLog("isRfcommDeviceConnected: No socket found for $address")
                return false
            }

            // Method 1: Basic socket state check
            val basicCheck = socket.isConnected
            cLog("isRfcommDeviceConnected: Basic check for $address: $basicCheck")

            if (!basicCheck) {
                return false
            }

            // Method 2: Try to read socket state (more reliable)
            val advancedCheck = try {
                // Try to get socket input stream - this will fail if disconnected
                socket.inputStream.available() >= 0
            } catch (e: Exception) {
                cLog("isRfcommDeviceConnected: Advanced check failed for $address: ${e.message}")
                false
            }

            // Method 3: Check if device is still in bonded devices
            val deviceCheck = try {
                val device = bluetoothAdapter?.getRemoteDevice(address)
                device != null && device.bondState == BluetoothDevice.BOND_BONDED
            } catch (e: Exception) {
                cLog("isRfcommDeviceConnected: Device check failed for $address: ${e.message}")
                false
            }

            val isConnected = basicCheck && advancedCheck && deviceCheck
            cLog("isRfcommDeviceConnected: Final result for $address: $isConnected (basic: $basicCheck, advanced: $advancedCheck, device: $deviceCheck)")
            
            isConnected

        } catch (e: Exception) {
            cLog("isRfcommDeviceConnected: Error checking RFComm device $address: ${e.message}")
            false
        }
    }

    /**
     * Test RFComm connection by sending a small test packet
     */
    fun testRfcommConnection(address: String): Boolean {
        return try {
            val socket = bluetoothConnect.rfcommSockets[address]
            if (socket == null) {
                cLog("testRfcommConnection: No socket found for $address")
                return false
            }

            // Try to write a small test packet
            val testData = byteArrayOf(0x00) // Null byte test
            socket.outputStream.write(testData)
            socket.outputStream.flush()
            
            cLog("testRfcommConnection: Test packet sent successfully to $address")
            true

        } catch (e: Exception) {
            cLog("testRfcommConnection: Test packet failed for $address: ${e.message}")
            false
        }
    }

    /**
     * Gets RFComm connected devices (Classic Bluetooth SPP connections)
     * Also cleans up disconnected sockets
     */
    fun getRfcommConnectedDevices(): List<BluetoothDeviceDetails> {
        cLog("getRfcommConnectedDevices: Checking RFComm connections: ${bluetoothConnect.rfcommSockets.size} devices")
        
        val connectedDevices = mutableListOf<BluetoothDeviceDetails>()
        val disconnectedAddresses = mutableListOf<String>()
        
        bluetoothConnect.rfcommSockets.keys.forEach { addr ->
            try {
                val dev = bluetoothAdapter?.getRemoteDevice(addr)
                if (dev != null) {
                    // Use enhanced connection check instead of basic socket.isConnected
                    val isActuallyConnected = isRfcommDeviceConnected(addr)
                    
                    if (isActuallyConnected) {
                        val d = buildBluetoothDevice(dev, action = "CONNECTED_DEVICE")
                        connectedDevices.add(d)
                        cLog("getRfcommConnectedDevices: Added RFComm connected device: ${dev.address} (${dev.name})")
                    } else {
                        cLog("getRfcommConnectedDevices: RFComm device ${dev.address} failed enhanced connection check, marking for cleanup")
                        disconnectedAddresses.add(addr)
                    }
                } else {
                    cLog("getRfcommConnectedDevices: RFComm device $addr not found in adapter, marking for cleanup")
                    disconnectedAddresses.add(addr)
                }
            } catch (e: Exception) {
                cLog("getRfcommConnectedDevices: Error checking RFComm device $addr: ${e.message}, marking for cleanup")
                disconnectedAddresses.add(addr)
            }
        }
        
        // Clean up disconnected sockets
        if (disconnectedAddresses.isNotEmpty()) {
            cLog("getRfcommConnectedDevices: Cleaning up ${disconnectedAddresses.size} disconnected RFComm sockets")
            disconnectedAddresses.forEach { addr ->
                try {
                    bluetoothConnect.rfcommSockets[addr]?.close()
                    bluetoothConnect.rfcommSockets.remove(addr)
                    cLog("getRfcommConnectedDevices: Cleaned up disconnected socket for $addr")
                } catch (e: Exception) {
                    cLog("getRfcommConnectedDevices: Error cleaning up socket for $addr: ${e.message}")
                }
            }
        }
        
        return connectedDevices
    }

    /**
     * Helper: synchronously get profile-connected devices with timeout (returns BluetoothDevice list)
     */
    private fun getProfileConnectedDevicesSync(
                profileId: Int,
                timeoutMs: Long = 500L
            ): List<BluetoothDevice> {
                val latch = java.util.concurrent.CountDownLatch(1)
                val result = mutableListOf<BluetoothDevice>()
                try {
                    bluetoothAdapter?.getProfileProxy(
                        appContext,
                        object : BluetoothProfile.ServiceListener {
                            override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                                try {
                                    val devices = try {
                                        proxy.connectedDevices
                                    } catch (e: Exception) {
                                        emptyList<BluetoothDevice>()
                                    }
                                    if (devices.isNotEmpty()) result += devices
                                } catch (t: Throwable) {
                                    cLog("getProfileConnectedDevicesSync: error reading devices for profile $profileId: ${t.message}")
                                } finally {
                                    try {
                                        bluetoothAdapter?.closeProfileProxy(p, proxy)
                                    } catch (_: Throwable) {
                                    }
                                    latch.countDown()
                                }
                            }

                            override fun onServiceDisconnected(p: Int) {
                                latch.countDown()
                            }
                        },
                        profileId
                    )
                    // wait up to timeoutMs
                    latch.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                } catch (t: Throwable) {
                    cLog("getProfileConnectedDevicesSync: Error querying profile $profileId: ${t.message}")
                }
                return result
            }

    /**
     * Gets profile-connected devices for a specific profile
     */
    fun getProfileConnectedDevices(profileId: Int, label: String): List<BluetoothDeviceDetails> {
                return try {
                    val devices = getProfileConnectedDevicesSync(profileId, timeoutMs = 500L)
            cLog("getProfileConnectedDevices: Profile $label has ${devices.size} connected devices: $devices")
            devices.map { dev ->
                        buildBluetoothDevice(dev, action = "CONNECTED_DEVICE")
                    }
                } catch (t: Throwable) {
            cLog("getProfileConnectedDevices: Error querying $label connected devices: ${t.message}")
                    emptyList()
                }
            }

    /**
     * Gets all profile-connected devices (A2DP, HEADSET, HID_HOST, GATT_SERVER)
     */
    fun getAllProfileConnectedDevices(): List<BluetoothDeviceDetails> {
        val profileDevices = mutableListOf<BluetoothDeviceDetails>()
        profileDevices += getProfileConnectedDevices(BluetoothProfile.A2DP, "A2DP")
        profileDevices += getProfileConnectedDevices(BluetoothProfile.HEADSET, "HEADSET")
        profileDevices += getProfileConnectedDevices(4, "HID_HOST")
        profileDevices += getProfileConnectedDevices(BluetoothProfile.GATT_SERVER, "GATT_SERVER")
        return profileDevices
    }

    /**
     * Determines the connection method for a device based on its current connection state
     */
    fun getDeviceConnectionMethod(address: String): String? {
        try {
            // Check GATT connections first
            val gattDevices = getGattConnectedDevices()
            if (gattDevices.any { it.address == address }) {
                return "GATT"
            }

            // Check our own GATT connections
            val ourGattDevices = getOurGattConnections()
            if (ourGattDevices.any { it.address == address }) {
                return "GATT"
            }

            // Check RFComm connections
            val rfcommDevices = getRfcommConnectedDevices()
            if (rfcommDevices.any { it.address == address }) {
                return "RFComm"
            }

            // Check profile connections
            val a2dpDevices = getProfileConnectedDevices(BluetoothProfile.A2DP, "A2DP")
            if (a2dpDevices.any { it.address == address }) {
                return "A2DP"
            }

            val headsetDevices = getProfileConnectedDevices(BluetoothProfile.HEADSET, "HEADSET")
            if (headsetDevices.any { it.address == address }) {
                return "HEADSET"
            }

            val hidDevices = getProfileConnectedDevices(4, "HID_HOST")
            if (hidDevices.any { it.address == address }) {
                return "HID_HOST"
            }

            val gattServerDevices = getProfileConnectedDevices(BluetoothProfile.GATT_SERVER, "GATT_SERVER")
            if (gattServerDevices.any { it.address == address }) {
                return "GATT_SERVER"
            }

            // Check bonded devices with reflection
            val bondedDevices = getConnectedBondedDevices()
            if (bondedDevices.any { it.address == address }) {
                return "BONDED"
            }

            cLog("getDeviceConnectionMethod: No connection method found for device $address")
            return null

        } catch (e: Exception) {
            cLog("getDeviceConnectionMethod: Error determining connection method for $address: ${e.message}")
            return null
        }
    }

    /**
     * Gets the saved connection method for a printer from DataStore
     */
    fun getSavedPrinterConnectionMethod(address: String): String? {
        return try {
            val savedPrinters = dataStoreManager.getSavedPrinters()
            val printer = savedPrinters.find { it.address == address }
            printer?.connectionMethod
        } catch (e: Exception) {
            cLog("getSavedPrinterConnectionMethod: Error getting saved connection method for $address: ${e.message}")
            null
        }
    }

    /**
     * Checks if a printer is connected using its saved connection method
     */
    fun isPrinterConnected(address: String): Boolean {
        return try {
            val savedMethod = getSavedPrinterConnectionMethod(address)
            if (savedMethod == null) {
                cLog("isPrinterConnected: No saved connection method for $address, checking all methods")
                return getDeviceConnectionMethod(address) != null
            }

            cLog("isPrinterConnected: Checking $address using saved method: $savedMethod")
            
            when (savedMethod) {
                "GATT" -> {
                    val gattDevices = getGattConnectedDevices()
                    val ourGattDevices = getOurGattConnections()
                    gattDevices.any { it.address == address } || ourGattDevices.any { it.address == address }
                }
                "RFComm" -> {
                    val rfcommDevices = getRfcommConnectedDevices()
                    rfcommDevices.any { it.address == address }
                }
                "A2DP" -> {
                    val a2dpDevices = getProfileConnectedDevices(BluetoothProfile.A2DP, "A2DP")
                    a2dpDevices.any { it.address == address }
                }
                "HEADSET" -> {
                    val headsetDevices = getProfileConnectedDevices(BluetoothProfile.HEADSET, "HEADSET")
                    headsetDevices.any { it.address == address }
                }
                "HID_HOST" -> {
                    val hidDevices = getProfileConnectedDevices(4, "HID_HOST")
                    hidDevices.any { it.address == address }
                }
                "GATT_SERVER" -> {
                    val gattServerDevices = getProfileConnectedDevices(BluetoothProfile.GATT_SERVER, "GATT_SERVER")
                    gattServerDevices.any { it.address == address }
                }
                "BONDED" -> {
                    val bondedDevices = getConnectedBondedDevices()
                    bondedDevices.any { it.address == address }
                }
                else -> {
                    cLog("isPrinterConnected: Unknown connection method $savedMethod for $address, checking all methods")
                    getDeviceConnectionMethod(address) != null
                }
            }
        } catch (e: Exception) {
            cLog("isPrinterConnected: Error checking connection for $address: ${e.message}")
            false
        }
    }

    /**
     * Connects to a printer using its saved connection method
     */
    fun connectToPrinterUsingSavedMethod(address: String): Boolean {
        return try {
            val savedMethod = getSavedPrinterConnectionMethod(address)
            if (savedMethod == null) {
                cLog("connectToPrinterUsingSavedMethod: No saved connection method for $address, using default connect")
                connect(address)
                return true
            }

            cLog("connectToPrinterUsingSavedMethod: Connecting to $address using saved method: $savedMethod")
            
            when (savedMethod) {
                "GATT", "GATT_SERVER" -> {
                    // Use GATT connection
                    connect(address)
                }
                "RFComm" -> {
                    // Use RFComm connection
                    connect(address)
                }
                "A2DP", "HEADSET", "HID_HOST" -> {
                    // These are typically managed by the system, just use default connect
                    connect(address)
                }
                "BONDED" -> {
                    // Device is already bonded, just connect
                    connect(address)
                }
                else -> {
                    cLog("connectToPrinterUsingSavedMethod: Unknown connection method $savedMethod for $address, using default connect")
                    connect(address)
                }
            }
            true
        } catch (e: Exception) {
            cLog("connectToPrinterUsingSavedMethod: Error connecting to $address: ${e.message}")
            false
        }
    }

    /**
     * Registers a callback to be called when a device successfully connects
     */
    fun registerConnectionSuccessCallback(address: String, callback: (String) -> Unit) {
        connectionSuccessCallbacks[address] = callback
        cLog("registerConnectionSuccessCallback: Registered callback for $address")
    }

    /**
     * Unregisters a connection success callback
     */
    fun unregisterConnectionSuccessCallback(address: String) {
        connectionSuccessCallbacks.remove(address)
        cLog("unregisterConnectionSuccessCallback: Unregistered callback for $address")
    }

    /**
     * Triggers connection success callbacks for a device
     */
    fun triggerConnectionSuccessCallback(address: String, connectionMethod: String) {
        connectionSuccessCallbacks[address]?.let { callback ->
            cLog("triggerConnectionSuccessCallback: Triggering callback for $address with method: $connectionMethod")
            callback(connectionMethod)
            // Remove callback after triggering to avoid multiple calls
            connectionSuccessCallbacks.remove(address)
        }
    }

    /**
     * Updates connected devices list from system and returns resulting list.
     *
     * NOTE: This function uses a CountDownLatch to synchronously wait for
     * profile binding callbacks. Call this off the main thread or convert to suspend.
     */
    fun updateConnectedDevices(div: BluetoothDeviceDetails?=null) {
        try {
            cLog("updateConnectedDevices: Updating connected devices list from system")
            
            val currentConnectedDevices = connectedDevices.value.toMutableList()
            val newConnectedDevices = mutableListOf<BluetoothDeviceDetails>()

            if (div != null) newConnectedDevices += listOf(div)

            // 1) GATT connected devices
            newConnectedDevices += getGattConnectedDevices()

            // 2) Bonded devices (reflection or best-effort)
            newConnectedDevices += getConnectedBondedDevices()

            // 3) Our own GATT connections (only actually connected ones)
            newConnectedDevices += getOurGattConnections()

            // 4) RFComm connected devices (Classic Bluetooth SPP connections)
            newConnectedDevices += getRfcommConnectedDevices()

            // 5) Profile-connected classic devices (A2DP, HEADSET, HID_HOST, GATT_SERVER)
            newConnectedDevices += getAllProfileConnectedDevices()

            val finalConnectedDevices = newConnectedDevices.distinctBy { it.address }
            
            // Only update if the list has actually changed to prevent UI blinking
            val currentAddresses = currentConnectedDevices.map { it.address }.toSet()
            val newAddresses = finalConnectedDevices.map { it.address }.toSet()
            
            if (currentAddresses != newAddresses) {
                connectedDevices.value = finalConnectedDevices
                cLog("updateConnectedDevices: Updated connected devices: ${finalConnectedDevices.size} devices (changed)")
            } else {
                cLog("updateConnectedDevices: Connected devices list unchanged: ${finalConnectedDevices.size} devices")
            }

        } catch (e: SecurityException) {
            cLog("updateConnectedDevices: ERROR - Security exception updating connected devices: ${e.message}")

        } catch (e: Exception) {
            cLog("updateConnectedDevices: ERROR - Error updating connected devices: ${e.message}")
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
    fun buildBluetoothDevice(
        device: BluetoothDevice?,
        address: String? = null,
        action: String,
        name: String? = null,
        extraInfo: Map<String, String>? = null,
        uuids: String? = null,
        state: String? = null
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
                if (uuidsStr.isNullOrEmpty() && hasConnectPermission(context)) {
                    try {
                        device?.fetchUuidsWithSdp()
                    } catch (e: Exception) {
                    }
                }
            } catch (se: SecurityException) {
                uuidsStr = "<no-perm> error : ${se.message}"
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
            uuids = uuidsStr,
            extraInfo = extraInfo ?: emptyMap(),
            state = state
        )
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
    val extraInfo: Map<String, String> = emptyMap(),
    val state: String? = "-"
)

/**
 * Data class for Bluetooth adapter state changes
 */
data class BluetoothStateChange(
    val currentState: Int, val previousState: Int
)