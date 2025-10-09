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
    private val context: Context
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

    private val bleReceiver: BleBroadcastReceiver = BleBroadcastReceiver(context, this)

    val bleScanner = BleScanner(
        addLeDevice = { addOrUpdateDevice(leDiscoveredDevices, it) },
        classicDiscoveringState = isClassicDiscovering,
        leDiscoveringState = isLeDiscovering,
        manager = this,
        context = context
    )

    val isDiscovering: StateFlow<Boolean> = bleScanner.isDiscovering

    private val bluetoothConnect = BleConnect(
        context = context,
        updateConnecting = { addOrUpdateDevice(connectingDevices, it) },
        removeConnecting = { removeDevice(connectingDevices, it) },
        removeConnected = { removeDevice(connectedDevices, it) },
        gattMap = gattMap,
        isDeviceConnected = { addr -> connectedDevices.value.any { it.address == addr } },
        isDeviceConnecting = { addr -> connectingDevices.value.any { it.address == addr } },
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
     * Manually refreshes connected devices list
     */
    fun refreshConnectedDevices() {
        updateConnectedDevices()
        cLog("refreshConnectedDevices: Manually refreshed connected devices list")
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
     * Updates connected devices list from system and returns resulting list.
     *
     * NOTE: This function uses a CountDownLatch to synchronously wait for
     * profile binding callbacks. Call this off the main thread or convert to suspend.
     */
    fun updateConnectedDevices(div: BluetoothDeviceDetails?=null) {
        try {
            cLog("updateConnectedDevices: Updating connected devices list from system")
            val connectedList = mutableListOf<BluetoothDeviceDetails>()

            if (div != null) connectedList += listOf(div)

            // 1) GATT connected devices
            val gattConnected = try {
                bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            } catch (e: Exception) {
                cLog("updateConnectedDevices: Error getting GATT connected devices: ${e.message}")
                emptyList<BluetoothDevice>()
            }
            cLog("updateConnectedDevices: Found ${gattConnected.size} GATT connected devices")
            val gattDetails = gattConnected.map { device ->
                val d = buildBluetoothDevice(device, action = "CONNECTED_DEVICE")
                cLog("updateConnectedDevices: Added GATT connected device: ${device.address} (${device.name})")
                d
            }
            connectedList += gattDetails

            // 2) Bonded devices (reflection or best-effort)
            val bondedList = try {
                val bondedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
                cLog("updateConnectedDevices: Checking ${bondedDevices.size} bonded devices for connection status")
                bondedDevices.mapNotNull { device ->
                    try {
                        val connectionState = getDeviceConnectionState(device)
                        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                            val d = buildBluetoothDevice(device, action = "CONNECTED_DEVICE")
                            cLog("updateConnectedDevices: Added connected bonded device: ${device.address} (${device.name})")
                            d
                        } else null
                    } catch (e: Exception) {
                        cLog("updateConnectedDevices: Error checking connection state for ${device.address}: ${e.message}")
                        null
                    }
                }
            } catch (e: Exception) {
                cLog("updateConnectedDevices: Error checking bonded devices: ${e.message}")
                emptyList()
            }
            connectedList += bondedList

            // 3) Our own GATT connections (only actually connected ones)
            cLog("updateConnectedDevices: Checking our own GATT connections: ${gattMap.size} devices")
            val ourGatts = gattMap.keys.mapNotNull { addr ->
                try {
                    val dev = bluetoothAdapter?.getRemoteDevice(addr)
                    if (dev != null) {
                        val gatt = gattMap[addr]
                        val isActuallyConnected = gatt?.services?.isNotEmpty() == true
                        if (isActuallyConnected) {
                            val d = buildBluetoothDevice(dev, action = "CONNECTED_DEVICE")
                            cLog("updateConnectedDevices: Added our GATT connection: ${dev.address} (${dev.name})")
                            d
                        } else {
                            cLog("updateConnectedDevices: GATT device ${dev.address} is still connecting (services: ${gatt?.services?.size ?: 0}), not adding to connected list")
                            null
                        }
                    } else null
                } catch (e: Exception) {
                    cLog("updateConnectedDevices: Error getting our GATT device $addr: ${e.message}")
                    null
                }
            }
            connectedList += ourGatts

            // Helper: synchronously get profile-connected devices with timeout (returns BluetoothDevice list)
            fun getProfileConnectedDevicesSync(
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

            // 4) Merge profile-connected classic devices (A2DP, HEADSET, HID_HOST)
            fun mergeProfileDevices(profileId: Int, label: String): List<BluetoothDeviceDetails> {
                return try {
                    val devices = getProfileConnectedDevicesSync(profileId, timeoutMs = 500L)
                    cLog("mergeProfileDevices: Profile $label has ${devices.size} connected devices: $devices")
                    val additions = devices.map { dev ->
                        buildBluetoothDevice(dev, action = "CONNECTED_DEVICE")
                    }
                    additions
                } catch (t: Throwable) {
                    cLog("mergeProfileDevices: Error querying $label connected devices: ${t.message}")
                    emptyList()
                }
            }

            // add them
            connectedList += mergeProfileDevices(BluetoothProfile.A2DP, "A2DP")
            connectedList += mergeProfileDevices(BluetoothProfile.HEADSET, "HEADSET")
            connectedList +=  mergeProfileDevices(4, "HID_HOST")
            connectedList +=  mergeProfileDevices(BluetoothProfile.GATT_SERVER, "GATT_SERVER")

            connectedDevices.value = connectedList.distinctBy { it.address }

            cLog("updateConnectedDevices: Incrementally updated connected devices: ${connectedDevices.value.size} devices (added/updated=${connectedDevices.value.size})")


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
            uuids = uuidsStr
        )
    }

    /*   private fun emit(event: BluetoothDeviceDetails) {
           try {
               // cLog a short summary
               val summary = mutableListOf<String>()
               event.address.let { summary.add("addr=$it") }
               event.name?.let { summary.add("name=$it") }
               event.rssi?.let { summary.add("rssi=$it") }
               event.uuids?.let { summary.add("uuids=${it.take(120)}") }
               // enrich with transport state we track
               val transports = mutableListOf<String>()
               if (aclConnected.contains(event.address)) transports.add("ACL")
               if (gattMap.containsKey(event.address)) transports.add("GATT")
               if (a2dpConnected.contains(event.address)) transports.add("A2DP")
               if (headsetConnected.contains(event.address)) transports.add("HEADSET")
               if (hidConnected.contains(event.address)) transports.add("HID")
               if (transports.isNotEmpty()) summary.add("links=${transports.joinToString("+")}")
               cLog("${event.action} -> ${summary.joinToString(" | ")}")

               eventListener?.invoke(event)
           } catch (e: Exception) {

           }
       }*/


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
    val state: String? = null
)

/**
 * Data class for Bluetooth adapter state changes
 */
data class BluetoothStateChange(
    val currentState: Int, val previousState: Int
)