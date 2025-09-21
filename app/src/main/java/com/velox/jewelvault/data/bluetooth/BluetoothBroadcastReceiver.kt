package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.velox.jewelvault.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothBroadcastReceiver @Inject constructor(
    context: Context, private val scope: CoroutineScope
) : BroadcastReceiver() {
    // Use applicationContext to prevent leaks and ensure proper broadcast reception
    private val appContext: Context = context.applicationContext
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Device found events
    private val _deviceFound = MutableSharedFlow<DeviceFound>()
    val deviceFound: SharedFlow<DeviceFound> = _deviceFound.asSharedFlow()

    // Connection state changes
    private val _connectionStateChanged = MutableSharedFlow<ConnectionStateChange>()
    val connectionStateChanged: SharedFlow<ConnectionStateChange> =
        _connectionStateChanged.asSharedFlow()

    // Bluetooth adapter state changes
    private val _bluetoothStateChanged = MutableSharedFlow<BluetoothStateChange>()
    val bluetoothStateChanged: SharedFlow<BluetoothStateChange> =
        _bluetoothStateChanged.asSharedFlow()

    private var isRegistered = false
    private var hasRefreshedOnRegistration = false


    override fun onReceive(ctx: Context?, intent: Intent?) {
        val timestamp = System.currentTimeMillis()
        val action = intent?.action
        log("BluetoothBroadcastReceiver received action=$action at=$timestamp intent=$intent")

        if (intent == null || action == null) {
            log("BluetoothBroadcastReceiver: null intent or action -> returning")
            return
        }

        when (action) {


            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                log("BluetoothBroadcastReceiver: ACTION_DISCOVERY_STARTED received - setting _isDiscovering to true")
                _isDiscovering.value = true
                log("BluetoothBroadcastReceiver: Discovery started - _isDiscovering set to true")
            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                log("BluetoothBroadcastReceiver: ACTION_DISCOVERY_FINISHED received - setting _isDiscovering to false")
                _isDiscovering.value = false
                log("BluetoothBroadcastReceiver: Discovery finished - _isDiscovering set to false")
            }

            BluetoothDevice.ACTION_FOUND -> {
                log("BluetoothBroadcastReceiver: ACTION_FOUND received - processing device")
                // Permission guard for Android 12+ (S)
                if (requiresConnectPermission() && !hasBluetoothConnectPermission()) {
                    log("Skipping ACTION_FOUND: missing BLUETOOTH_CONNECT permission")
                    return
                }

                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val rssiRaw = intent.getIntExtra(BluetoothDevice.EXTRA_RSSI, Int.MIN_VALUE)
                val rssi = if (rssiRaw == Int.MIN_VALUE) null else rssiRaw

                if (device == null) {
                    log("ACTION_FOUND: device extra is null")
                    return
                }

                log("BluetoothBroadcastReceiver: Found device - Address: ${device.address}, Name: '${device.name}', RSSI: $rssi, Bond State: ${device.bondState}, Device Class: ${device.bluetoothClass?.deviceClass}")
                scope.launch { _deviceFound.emit(DeviceFound(device = device, rssi = rssi)) }
            }

            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                if (requiresConnectPermission() && !hasBluetoothConnectPermission()) {
                    log("Skipping ACTION_BOND_STATE_CHANGED: missing BLUETOOTH_CONNECT permission")
                    return
                }
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val bondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                val previousBondState = intent.getIntExtra(
                    BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE
                )
                if (device != null) {
                    log("Bond state changed ${device.address} $previousBondState -> $bondState")
                    scope.launch {
                        _connectionStateChanged.emit(
                            ConnectionStateChange(
                                device = device,
                                bondState = bondState,
                                previousBondState = previousBondState
                            )
                        )
                    }
                } else {
                    log("ACTION_BOND_STATE_CHANGED: device is null")
                }
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if (requiresConnectPermission() && !hasBluetoothConnectPermission()) {
                    log("Skipping ACTION_ACL_CONNECTED: missing BLUETOOTH_CONNECT permission")
                    return
                }
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    log("ACL connected ${it.address}")
                    scope.launch {
                        _connectionStateChanged.emit(
                            ConnectionStateChange(
                                device = it, connectionState = ConnectionState.CONNECTED
                            )
                        )
                    }
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                if (requiresConnectPermission() && !hasBluetoothConnectPermission()) {
                    log("Skipping ACTION_ACL_DISCONNECTED: missing BLUETOOTH_CONNECT permission")
                    return
                }
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    log("ACL disconnected ${it.address}")
                    scope.launch {
                        _connectionStateChanged.emit(
                            ConnectionStateChange(
                                device = it, connectionState = ConnectionState.DISCONNECTED
                            )
                        )
                    }
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                if (requiresConnectPermission() && !hasBluetoothConnectPermission()) {
                    log("Skipping ACTION_ACL_DISCONNECT_REQUESTED: missing BLUETOOTH_CONNECT permission")
                    return
                }
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    log("ACL disconnect requested ${it.address}")
                    scope.launch {
                        _connectionStateChanged.emit(
                            ConnectionStateChange(
                                device = it, connectionState = ConnectionState.DISCONNECTING
                            )
                        )
                    }
                }
            }

            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                if (requiresConnectPermission() && !hasBluetoothConnectPermission()) {
                    log("Skipping profile connection state change: missing BLUETOOTH_CONNECT permission")
                    return
                }
                val profileState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    val connectionState = when (profileState) {
                        BluetoothProfile.STATE_CONNECTING -> ConnectionState.CONNECTING
                        BluetoothProfile.STATE_CONNECTED -> ConnectionState.CONNECTED
                        BluetoothProfile.STATE_DISCONNECTING -> ConnectionState.DISCONNECTING
                        else -> ConnectionState.DISCONNECTED
                    }
                    log("Profile connection state changed ${device.address} -> $connectionState")
                    scope.launch {
                        _connectionStateChanged.emit(
                            ConnectionStateChange(
                                device = device, connectionState = connectionState
                            )
                        )
                    }
                } else {
                    log("Profile connection state change: device extra missing")
                }
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                val previousState = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR
                )
                log("Bluetooth adapter state changed: $previousState -> $state")
                scope.launch {
                    _bluetoothStateChanged.emit(
                        BluetoothStateChange(
                            currentState = state, previousState = previousState
                        )
                    )
                }
            }
        }
    }


    fun register() {
        if (isRegistered) {
            log("BluetoothBroadcastReceiver already registered")
            return
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }

        log(
            "BluetoothBroadcastReceiver: Attempting to register with filter: ${
                filter.actionsIterator().asSequence().toList()
            }"
        )

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

            // Refresh and sync with current system state only on first registration
            if (!hasRefreshedOnRegistration) {
                log("BluetoothBroadcastReceiver: First time registration - refreshing system state")
                refreshSystemState()
                hasRefreshedOnRegistration = true
            } else {
                log("BluetoothBroadcastReceiver: Already refreshed on previous registration - skipping")
            }
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

    fun getRegistrationStatus(): String = "BluetoothBroadcastReceiver - Registered: $isRegistered"

    fun isWorking(): Boolean = isRegistered

    /**
     * Refresh and sync with current system state when receiver registers
     */
    private fun refreshSystemState() {
        log("BluetoothBroadcastReceiver: Refreshing system state...")
        scope.launch {
            try {
                // Get current Bluetooth adapter state
                val bluetoothManager =
                    appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bluetoothManager?.adapter

                if (adapter != null) {
                    val currentState = adapter.state
                    log("BluetoothBroadcastReceiver: Current adapter state: $currentState")

                    // Emit current state to sync with system
                    _bluetoothStateChanged.emit(
                        BluetoothStateChange(
                            currentState = currentState,
                            previousState = currentState // Same as current since we're syncing
                        )
                    )

                    // Check if discovery is currently running
                    val isCurrentlyDiscovering = try {
                        if (hasBluetoothConnectPermission()) {
                            adapter.isDiscovering
                        } else {
                            false
                        }
                    } catch (e: SecurityException) {
                        log("BluetoothBroadcastReceiver: SecurityException checking discovery state: ${e.message}")
                        false
                    } catch (t: Throwable) {
                        log("BluetoothBroadcastReceiver: Error checking discovery state: ${t.message}")
                        false
                    }

                    log("BluetoothBroadcastReceiver: Current discovery state: $isCurrentlyDiscovering")
                    _isDiscovering.value = isCurrentlyDiscovering

                    // Refresh paired devices to sync current connections
                    refreshPairedDevices(adapter)
                } else {
                    log("BluetoothBroadcastReceiver: Bluetooth adapter not available")
                }
            } catch (t: Throwable) {
                log("BluetoothBroadcastReceiver: Error refreshing system state: ${t.message}")
            }
        }
    }

    /**
     * Refresh all available devices (paired and nearby)
     */
    private fun refreshPairedDevices(adapter: BluetoothAdapter) {
        try {
            if (!hasBluetoothConnectPermission()) {
                log("BluetoothBroadcastReceiver: Missing permissions for devices refresh")
                return
            }

            val pairedDevices = adapter.bondedDevices
            log("BluetoothBroadcastReceiver: Found ${pairedDevices.size} paired devices")

            // Add all paired devices to the device list
            pairedDevices.forEach { device ->
                log("BluetoothBroadcastReceiver: Refreshing paired device - ${device.name} (${device.address})")

                // Emit device found event to refresh the device list
                scope.launch {
                    _deviceFound.emit(DeviceFound(device = device, rssi = null))
                }

                // Check connection state for each paired device
                val connectionState = getDeviceConnectionState(device)
                if (connectionState != null) {
                    scope.launch {
                        _connectionStateChanged.emit(
                            ConnectionStateChange(
                                device = device,
                                connectionState = connectionState,
                                bondState = device.bondState
                            )
                        )
                    }
                }
            }
            
            // Start discovery to find nearby unpaired devices
            if (adapter.isEnabled && !adapter.isDiscovering) {
                log("BluetoothBroadcastReceiver: Starting discovery to find nearby devices")
                try {
                    adapter.startDiscovery()
                } catch (e: SecurityException) {
                    log("BluetoothBroadcastReceiver: SecurityException starting discovery: ${e.message}")
                } catch (t: Throwable) {
                    log("BluetoothBroadcastReceiver: Error starting discovery: ${t.message}")
                }
            }
        } catch (e: SecurityException) {
            log("BluetoothBroadcastReceiver: SecurityException refreshing devices: ${e.message}")
        } catch (t: Throwable) {
            log("BluetoothBroadcastReceiver: Error refreshing devices: ${t.message}")
        }
    }

    /**
     * Get current connection state for a device
     */
    private fun getDeviceConnectionState(device: BluetoothDevice): ConnectionState? {
        return try {
            if (!hasBluetoothConnectPermission()) {
                null
            } else {
                // For now, we'll assume paired devices are connected if they're bonded
                // In a real implementation, you might want to check actual connection state
                when (device.bondState) {
                    BluetoothDevice.BOND_BONDED -> ConnectionState.CONNECTED
                    else -> ConnectionState.DISCONNECTED
                }
            }
        } catch (e: SecurityException) {
            log("BluetoothBroadcastReceiver: SecurityException checking device connection state: ${e.message}")
            null
        } catch (t: Throwable) {
            log("BluetoothBroadcastReceiver: Error checking device connection state: ${t.message}")
            null
        }
    }

    // Helpers
    private fun requiresConnectPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private fun hasBluetoothConnectPermission(): Boolean {
        return try {
            val perm = Manifest.permission.BLUETOOTH_CONNECT
            appContext.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
        } catch (t: Throwable) {
            false
        }
    }
}
