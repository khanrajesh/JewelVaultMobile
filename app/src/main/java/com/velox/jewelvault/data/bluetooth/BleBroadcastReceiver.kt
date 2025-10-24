package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.velox.jewelvault.data.bluetooth.BleUtils.logDevice
import com.velox.jewelvault.utils.log
import kotlin.collections.set


class BleBroadcastReceiver(context: Context, private val manager: BleManager) :
    BroadcastReceiver() {

    private val appContext = context.applicationContext
    private fun cLog(msg: String) = log(msg)


    var isReceiverRegistered = false


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context?, intent: Intent?) {

        manager.cLog("BleBroadcastReceiver: onReceive called with action: ${intent?.action}, name: ${intent?.getDevice()?.name}, address: ${intent?.getDevice()?.address}, ${intent?.getDevice()}")
        val action = intent?.action ?: return

        try {
            when (action) {

                //For Bluetooth state change like on/off
                BluetoothAdapter.ACTION_STATE_CHANGED, BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    handleBleStateChange(intent)
                }

                //For Classic Bluetooth discovery
                BluetoothDevice.ACTION_FOUND, BluetoothAdapter.ACTION_DISCOVERY_STARTED, BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    handleDiscoveryStateChange(intent)
                }

                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                    handleAclConnectedDeviceStateChange(intent)
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED, BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    handleBondingStateChange(intent)
                }

                BluetoothDevice.ACTION_NAME_CHANGED -> handleDeviceNameChange(intent)


                // ACTION_UUID: Broadcast when device UUIDs are discovered/updated
                // - Triggered by: SDP (Service Discovery Protocol) completion, UUID fetch
                // - Purpose: Notify when device services/UUIDs become available
                // - Frequency: Once when UUIDs are discovered
                // - Scope: Both Classic and BLE devices
                // - Note: May be triggered by fetchUuidsWithSdp() call
                BluetoothDevice.ACTION_UUID -> {
                    cLog("BleManager: onReceive: Processing ACTION_UUID")
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                    cLog("BleManager: onReceive: UUID action - device: ${device?.address}, uuids count: ${uuids?.size}")
                    val event = manager.buildBluetoothDevice(device, action = "ACTION_UUID").copy(
                        uuids = uuidsToString(uuids)
                    )
                    cLog("BleManager: onReceive: Emitted UUID event for device: ${device?.address}")
                }


                // Profile connection states: A2DP / HEADSET / HID Host
                "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED",
                "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED",
                "android.bluetooth.hidhost.profile.action.CONNECTION_STATE_CHANGED" -> {
                    val device = intent.getDevice()
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                    val profileAction = action.substringAfterLast('.')
                    cLog("BleManager: onReceive: PROFILE $profileAction state=$state dev=${device?.address}")

                    val event = manager.buildBluetoothDevice(
                        device, action = when (action) {
                            "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" -> if (state == BluetoothProfile.STATE_CONNECTED) "AUDIO_CONNECTED" else "AUDIO_DISCONNECTED"
                            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" -> if (state == BluetoothProfile.STATE_CONNECTED) "HEADSET_CONNECTED" else "HEADSET_DISCONNECTED"
                            else -> if (state == BluetoothProfile.STATE_CONNECTED) "HID_HOST_CONNECTED" else "HID_HOST_DISCONNECTED"
                        }
                    )

                    // Update lists immediately
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        manager.addOrUpdateConnectedDeviceList(listOf(event))
                        manager.classicTimestamps[event.address] = System.currentTimeMillis()
//                        when (action) {
//                            "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" -> a2dpConnected.add(
//                                event.address
//                            )
//
//                            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" -> headsetConnected.add(
//                                event.address
//                            )
//
//                            else -> hidConnected.add(event.address)
//                        }
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
//                        removeDevice(_connectedDevices, event.address)
//                        classicTimestamps.remove(event.address)
//                        when (action) {
//                            "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" -> a2dpConnected.remove(
//                                event.address
//                            )
//
//                            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" -> headsetConnected.remove(
//                                event.address
//                            )
//
//                            else -> hidConnected.remove(event.address)
//                        }
                    }
                    // Immediate refresh to reconcile
                    manager.updateConnectedDevices()
                }

                else -> {
                    manager.cLog("BleManager: onReceive: Unhandled action: $action")
                }
            }
        } catch (t: Throwable) {
            cLog("BleManager: onReceive: Exception handling $action: ${t.message}")
            t.printStackTrace()
        } catch (se: SecurityException) {
            cLog("BleManager: onReceive: Security exception handling $action: ${se.message}")
            se.printStackTrace()
        }
        cLog("BleManager: onReceive completed for action: $action")



    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun handleBleStateChange(intent: Intent) {

        when (intent.action) {
            // ACTION_STATE_CHANGED: Broadcast when Bluetooth adapter state changes
            // - Triggered by: Bluetooth on/off, adapter enable/disable
            // - Purpose: Track overall Bluetooth system state
            // - Frequency: Once per state change
            // - States: STATE_OFF(10), STATE_TURNING_ON(11), STATE_ON(12), STATE_TURNING_OFF(13)
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                val prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1)
                cLog("BleManager: onReceive: Adapter state changed: prev=$prev -> state=$state")
                manager.bluetoothStateChanged.value = BluetoothStateChange(  state,prev )
                cLog("BleManager: onReceive: Emitted state change: currentState=$state, previousState=$prev")

                // Stop scanning if Bluetooth is turning off or already off
                if (state == BluetoothAdapter.STATE_OFF ||
                    state == BluetoothAdapter.STATE_TURNING_OFF ||
                    state == BluetoothAdapter.STATE_DISCONNECTED // optional; not always used for adapter
                ) {
                    manager.bleScanner.stopUnifiedScan()
                    cLog("BleManager: Stopped unified scan due to Bluetooth off/turning off")
                }
            }

            // ACTION_SCAN_MODE_CHANGED: Broadcast when adapter scan mode changes
            // - Triggered by: Discoverability changes, adapter mode changes
            // - Purpose: Track device discoverability state
            // - Frequency: Once per mode change
            // - Modes: SCAN_MODE_NONE(20), SCAN_MODE_CONNECTABLE(21), SCAN_MODE_CONNECTABLE_DISCOVERABLE(23)
            BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                cLog("BleManager: onReceive: Processing ACTION_SCAN_MODE_CHANGED")
                val mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
                val prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1)
                cLog("BleManager: onReceive: Scan mode changed: prev=$prev -> mode=$mode")
            }
        }

    }

    private fun handleDiscoveryStateChange(intent: Intent) {
        when (intent.action) {
            // ACTION_FOUND: Broadcast when a Bluetooth device is discovered during classic discovery
            // - Triggered by: BluetoothAdapter.startDiscovery()
            // - Purpose: Notify that a new device was found during scanning
            // - Frequency: One broadcast per discovered device
            // - Scope: Classic Bluetooth devices only (not BLE)
            BluetoothDevice.ACTION_FOUND -> {
                cLog("BleManager: onReceive: Processing ACTION_FOUND")
                try {
                    val device = intent.getDevice()
                    val rssi =
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: device?.name
                    cLog("")
                    logDevice("BleManager: onReceive: Found device - name: $name, rssi: $rssi, address: ${device?.address}",device)

                    val event = manager.buildBluetoothDevice(device, action = "ACTION_FOUND").copy(name = name, rssi = rssi)

                    // Add to classic discovered devices list
                    manager.addOrUpdateDevice(manager.classicDiscoveredDevices, event)
                } catch (e: Exception) {
                    cLog("BleManager: onReceive: Exception in ACTION_FOUND: ${e.message}")
                } catch (e: SecurityException) {
                    cLog("BleManager: onReceive: SecurityException in ACTION_FOUND: ${e.message}")
                }
            }

            // ACTION_DISCOVERY_STARTED: Broadcast when classic Bluetooth discovery begins
            // - Triggered by: BluetoothAdapter.startDiscovery()
            // - Purpose: Notify that device discovery has started
            // - Frequency: Once per discovery session
            // - Scope: Classic Bluetooth discovery only
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                cLog("BleManager: onReceive: Processing ACTION_DISCOVERY_STARTED")
                manager.bleScanner.onClassicDiscoveryStarted()
            }

            // ACTION_DISCOVERY_FINISHED: Broadcast when classic Bluetooth discovery ends
            // - Triggered by: Discovery timeout (12s) or BluetoothAdapter.cancelDiscovery()
            // - Purpose: Notify that device discovery has completed
            // - Frequency: Once per discovery session
            // - Scope: Classic Bluetooth discovery only
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                cLog("BleManager: onReceive: Processing ACTION_DISCOVERY_FINISHED")
                manager.bleScanner.onClassicDiscoveryFinished()
            }


//
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleAclConnectedDeviceStateChange(intent: Intent) {
        when (intent.action) {
            // ACTION_ACL_CONNECTED: Broadcast when ACL (Asynchronous Connection-Less) link is established
            // - Triggered by: Successful connection to a Bluetooth device
            // - Purpose: Notify that device is now connected and ready for communication
            // - Frequency: Once per successful connection
            // - Scope: Both Classic and BLE devices
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = intent.getDevice()
                cLog("BleManager: onReceive: ACL device - address: ${device?.address}, name: ${device?.name}")
                val event = manager.buildBluetoothDevice(device, action = intent.action?:"NA")

                device?.address?.let { manager.aclConnected.add(it) }
                cLog("BleManager: onReceive: ACL connected - Adding device ${event.address} (${event.name}) to connected list")
                // Add to connected devices list
//                manager.addOrUpdateConnectedDeviceList(listOf(event))
                // Also refresh the full connected devices list to catch any missed devices
                manager.updateConnectedDevices(event)
            }

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
            BluetoothDevice.ACTION_ACL_DISCONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                val device = intent.getDevice()

                logDevice("BleManager: onReceive: ACL device - address: ${device?.address}, name: ${device?.name}",device)
                val event = manager.buildBluetoothDevice(device, action = intent.action?:"NA")
                //todo do proper impl
                device?.address?.let { manager.aclConnected.remove(it) }
//                cLog("BleManager: onReceive: ACL disconnected - Removing device ${event.address} (${event.name}) from connected list")
//                // Remove from connected devices list
                manager.removeDevice(manager.connectedDevices, event.address)
//                // Also refresh the full connected devices list to ensure accuracy
                manager.updateConnectedDevices()
            }

            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED->{

                val device = intent.getDevice()
                cLog("BleManager: onReceive: Action state change device - address: ${device?.address}, name: ${device?.name}")
                manager.updateConnectedDevices()
            }



//
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleBondingStateChange(intent: Intent) {
        when (intent.action) {
            // ACTION_BOND_STATE_CHANGED: Broadcast when device bonding/pairing state changes
            // - Triggered by: Pairing process, bonding success/failure, unbonding
            // - Purpose: Track pairing status changes (bonded, not bonded, bonding)
            // - Frequency: Multiple times during pairing process
            // - Scope: Both Classic and BLE devices
            // - States: BOND_NONE(10), BOND_BONDING(11), BOND_BONDED(12)
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                cLog("BleManager: onReceive: Processing ACTION_BOND_STATE_CHANGED")
                val device = intent.getDevice()
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                val prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                cLog("BleManager: onReceive: Bond state changed - device: ${device?.address}, state: $state, prev: $prev")
                val event = manager.buildBluetoothDevice(
                    device, action = "ACTION_BOND_STATE_CHANGED", extraInfo = mapOf(
                        "bondState" to state.toString(), "prevBondState" to prev.toString()
                    )
                )

                // Update bonded devices list based on bond state
                when (state) {
                    BluetoothDevice.BOND_BONDED -> {
                        manager.addOrUpdateDevice(manager.bondedDevices, event)
                        // Auto-connect via RFCOMM once bonded
                        try {
                            val addr = device?.address
                            if (!addr.isNullOrBlank()) {
                                manager.connect(
                                    address = addr,
                                    onConnect = { dev ->
                                        val method = dev.extraInfo["connectionMethod"] ?: "UNKNOWN"
                                        manager.cLog("Auto-connect after bonding succeeded for " + addr + " via " + method)
                                    },
                                    onFailure = { err ->
                                        manager.cLog("Auto-connect after bonding failed for " + addr + ": " + (err.message ?: "unknown"))
                                    }
                                )
                            }
                        } catch (t: Throwable) {
                            manager.cLog("Auto-connect after bonding error: ${'$'}{t.message}")
                        }
                    }

                    BluetoothDevice.BOND_NONE -> {
                        manager.removeDevice(manager.bondedDevices, event.address)
                    }
                }

                cLog("BleManager: onReceive: Emitted BOND_STATE_CHANGED event for device: ${device?.address}")
            }

            // ACTION_PAIRING_REQUEST: Broadcast when device requests pairing/authentication
            // - Triggered by: Device initiating pairing process (PIN, passkey, etc.)
            // - Purpose: Handle pairing authentication requests from remote devices
            // - Frequency: Once per pairing attempt
            // - Scope: Both Classic and BLE devices
            // - Variants: PIN, PASSKEY, CONSENT, DISPLAY_PASSKEY, DISPLAY_PIN
            BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                cLog("BleManager: onReceive: Processing ACTION_PAIRING_REQUEST")
                val device = intent.getDevice()
                val variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
                val key = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, -1)
                cLog("BleManager: onReceive: Pairing request - device: ${device?.address}, variant: $variant, key: $key")
                val event =
                    manager.buildBluetoothDevice(device, action = "ACTION_PAIRING_REQUEST").copy(
                        extraInfo = mapOf(
                            "pairingVariant" to variant.toString(), "pairingKey" to key.toString()
                        )
                    )
                logDevice("BleManager: onReceive: ACTION_PAIRING_REQUEST event: $event",device)
                //todo current doing nothing with this event
            }
        }
    }

    private fun handleDeviceNameChange(intent: Intent) {
        when (intent.action) {
            // ACTION_NAME_CHANGED: Broadcast when device name changes
            // - Triggered by: Device name update, SDP (Service Discovery Protocol) completion
            // - Purpose: Notify when device name becomes available or changes
            // - Frequency: Once when name is discovered/updated
            // - Scope: Both Classic and BLE devices
            // - Note: Some devices may not broadcast name changes
            BluetoothDevice.ACTION_NAME_CHANGED -> {
                cLog("BleManager: onReceive: Processing ACTION_NAME_CHANGED")
                val device = intent.getDevice()
                val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                cLog("BleManager: onReceive: Name changed - device: ${device?.address}, new name: $name")
                manager.buildBluetoothDevice(device, action = "ACTION_NAME_CHANGED").copy(
                    name = name ?: "<null>"
                )
                cLog("BleManager: onReceive: Emitted NAME_CHANGED event for device: ${device?.address}")
            }
        }
    }


    fun registerBleReceiver(onComplete: () -> Unit = {}, onError: (String?) -> Unit = {}) {
        if (isReceiverRegistered) {
            manager.cLog("BluetoothBroadcastReceiver already registered")
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
            // Profile connection state broadcasts
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.hidhost.profile.action.CONNECTION_STATE_CHANGED")
        }
        try {
            // Use applicationContext to ensure proper broadcast reception
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // registerReceiver(receiver, filter, flags) overload available on API 33
                appContext.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
                manager.cLog("BluetoothBroadcastReceiver registered with RECEIVER_NOT_EXPORTED")
            } else {
                ContextCompat.registerReceiver(
                    appContext, this, filter, ContextCompat.RECEIVER_NOT_EXPORTED
                )
                manager.cLog("BluetoothBroadcastReceiver registered (no flags)")
            }
            isReceiverRegistered = true
            manager.cLog("BluetoothBroadcastReceiver registered successfully - isRegistered")

            onComplete()
        } catch (e: Exception) {
            manager.cLog("Failed to register BluetoothBroadcastReceiver: ${e.message}")
            onError(e.message)
        }
    }

    fun unregisterBleReceiver(onComplete: () -> Unit = {}, onError: (String?) -> Unit = {}) {
        if (!isReceiverRegistered) {
            manager.cLog("BluetoothBroadcastReceiver unregister called but not registered")
            return
        }
        try {
            appContext.unregisterReceiver(this)
            isReceiverRegistered = false
            onComplete()
            manager.cLog("BluetoothBroadcastReceiver unregistered")
        } catch (e: IllegalArgumentException) {
            manager.cLog("Receiver not registered while trying to unregister: ${e.message}")
            onError(e.message)
        } catch (e: Exception) {
            manager.cLog("Error while unregistering bluetooth receiver: ${e.message}")
            onError(e.message)
        }
    }

    @Suppress("DEPRECATION") // we handle it manually
    fun Intent.getDevice(): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            //old device
            this.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
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

}