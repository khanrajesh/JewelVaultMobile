package com.velox.jewelvault.data.printing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.velox.jewelvault.utils.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Bluetooth Classic and BLE connections for printing
 */
@Singleton
class BluetoothManager @Inject constructor(
    private val context: Context
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    // State flows for real-time updates
    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled ?: false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()
    private val _bondedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDevice>> = _bondedDevices.asStateFlow()
    
    // Active connections
    private val activeConnections = mutableMapOf<String, BluetoothConnection>()
    
    // Discovery state
    private var isDiscovering = false
    private var isBleScanning = false
    
    // Broadcast receiver for system events
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
                    if (state == BluetoothAdapter.STATE_ON) {
                        updateBondedDevices()
                    }
                    log("Bluetooth state changed: $state")
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    device?.let { handleDeviceFound(it, rssi) }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    device?.let { handleBondStateChanged(it, bondState) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
//                    Log.d("BT", "Discovery started")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                    Log.d("BT", "Discovery finished")
                }
            }
        }
    }
    
    // BLE scan callback
    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            handleBleDeviceFound(device, rssi)
        }
        
        override fun onScanFailed(errorCode: Int) {
            log("BLE scan failed with error: $errorCode")
            isBleScanning = false
        }
    }
    
    init {
        registerBluetoothReceiver()
    }
    
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }
    
    fun cleanup() {
        context.unregisterReceiver(bluetoothReceiver)
        stopDiscovery()
        stopBleScan()
        activeConnections.values.forEach { it.disconnect() }
        activeConnections.clear()
    }
    
    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }
    
    /**
     * Get bonded (paired) devices with real connection status
     */
    fun getBondedDevices(): List< BluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    private fun isDeviceActuallyConnected(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermission()) return false

        // 1. First, check if we have an active connection in our manager
        activeConnections[device.address]?.let {
            return it.isConnected()
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false

        try {
            // 2. Check Classic Bluetooth profiles (A2DP & HEADSET)
            val isClassicConnected = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET).any { profile ->
                if (bluetoothAdapter.getProfileConnectionState(profile) == BluetoothProfile.STATE_CONNECTED) {
                    val proxyDevices = mutableListOf<BluetoothDevice>()
                    val latch = java.util.concurrent.CountDownLatch(1)

                    bluetoothAdapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                            proxyDevices.addAll(proxy.connectedDevices)
                            bluetoothAdapter.closeProfileProxy(p, proxy)
                            latch.countDown()
                        }

                        override fun onServiceDisconnected(p: Int) {
                            latch.countDown()
                        }
                    }, profile)

                    latch.await() // Wait for async callback
                    proxyDevices.any { it.address == device.address }
                } else false
            }

            if (isClassicConnected) return true

            // 3. Check BLE connections using activeConnections (BLE GATT)
            if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                return activeConnections[device.address]?.isConnected() ?: false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return false
    }



    /**
     * Update bonded devices flow
     */
    private fun updateBondedDevices() {
        _bondedDevices.value = getBondedDevices()
    }

    /**
     * Check if device is connected via specific Bluetooth profiles (A2DP, HEADSET, etc.)
     * This is an asynchronous operation that uses callbacks
     */
    private fun checkProfileConnection(device: BluetoothDevice, profile: Int, callback: (Boolean) -> Unit) {
        if (!hasBluetoothPermission()) {
            callback(false)
            return
        }

        try {
            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val isConnected = proxy.connectedDevices.any { connectedDevice ->
                        connectedDevice.address == device.address
                    }
                    callback(isConnected)
                    // Close the proxy when done
                    bluetoothAdapter?.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {
                    callback(false)
                }
            }, profile)
        } catch (e: Exception) {
            callback(false)
        }
    }
    
    /**
     * Start device discovery
     */
    fun startDiscovery() {
        if (!hasBluetoothPermission() || isDiscovering) return
        
        bluetoothAdapter?.let { adapter ->
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            
            isDiscovering = true
            adapter.startDiscovery()
            log("Started Bluetooth discovery")
        }
    }
    
    /**
     * Stop device discovery
     */
    fun stopDiscovery() {
        if (!isDiscovering) return
        
        bluetoothAdapter?.let { adapter ->
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
        }
        isDiscovering = false
        log("Stopped Bluetooth discovery")
    }
    
    /**
     * Start BLE scan
     */
    fun startBleScan() {
        if (!hasBluetoothPermission() || isBleScanning) return
        
        bluetoothLeScanner?.let { scanner ->
            isBleScanning = true
            scanner.startScan(bleScanCallback)
            log("Started BLE scan")
        }
    }
    
    /**
     * Stop BLE scan
     */
    fun stopBleScan() {
        if (!isBleScanning) return
        
        bluetoothLeScanner?.let { scanner ->
            scanner.stopScan(bleScanCallback)
        }
        isBleScanning = false
        log("Stopped BLE scan")
    }
    
    /**
     * Connect to a device
     */
    suspend fun connectToDevice(deviceAddress: String): BluetoothConnection? {
        if (!hasBluetoothPermission()) return null
        
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return null
        
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_LE -> connectBleDevice(device)
            else -> connectClassicDevice(device)
        }
    }
    
    /**
     * Disconnect from a device
     */
    fun disconnectDevice(deviceAddress: String) {
        activeConnections[deviceAddress]?.disconnect()
        activeConnections.remove(deviceAddress)
        updateBondedDevices()
    }
    
    /**
     * Send data to a connected device
     */
    suspend fun sendData(deviceAddress: String, data: ByteArray): Boolean {
        val connection = activeConnections[deviceAddress] ?: return false
        return connection.sendData(data)
    }
    
    private fun handleDeviceFound(device: BluetoothDevice, rssi: Int) {
        if (!hasBluetoothPermission()) return

        
        updateDiscoveredDevices(device)
    }
    
    private fun handleBleDeviceFound(device: BluetoothDevice, rssi: Int) {
        if (!hasBluetoothPermission()) return

        
        updateDiscoveredDevices(device)
    }
    
    private fun handleBondStateChanged(device: BluetoothDevice, bondState: Int) {
        updateDiscoveredDevices(device)
    }
    
    private fun updateDiscoveredDevices(newDevice: BluetoothDevice) {
        val currentDevices = _discoveredDevices.value.toMutableList()
        
        if (newDevice != null) {
            val existingIndex = currentDevices.indexOfFirst { it.address == newDevice.address }
            if (existingIndex >= 0) {
                currentDevices[existingIndex] = newDevice
            } else {
                currentDevices.add(newDevice)
            }
        }
        
        _discoveredDevices.value = currentDevices.distinctBy { it.address }
    }
    
    
    fun isDeviceConnected(deviceAddress: String): Boolean {
        return activeConnections.containsKey(deviceAddress)
    }
    
    fun isPrinterDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.lowercase() ?: ""
        return name.contains("printer") || 
               name.contains("pos") || 
               name.contains("thermal") ||
               name.contains("receipt") ||
               name.contains("zebra") ||
               name.contains("epson") ||
               name.contains("star") ||
               name.contains("citizen")
    }
    
    fun detectPrinterLanguage(device: BluetoothDevice): PrinterLanguage? {
        val name = device.name?.lowercase() ?: ""
        return when {
            name.contains("zebra") -> PrinterLanguage.ZPL
            name.contains("tsc") -> PrinterLanguage.TSPL
            name.contains("citizen") -> PrinterLanguage.CPCL
            else -> PrinterLanguage.ESC_POS // Default for most thermal printers
        }
    }
    
    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private suspend fun connectClassicDevice(device: BluetoothDevice): BluetoothConnection? {
        return try {
            val socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            socket.connect()
            
            val connection = BluetoothClassicConnection(device, socket)
            activeConnections[device.address] = connection
            updateBondedDevices()
            
            log("Connected to classic device: ${device.name}")
            connection
        } catch (e: Exception) {
            log("Failed to connect to classic device: ${e.message}")
            null
        }
    }
    
    private suspend fun connectBleDevice(device: BluetoothDevice): BluetoothConnection? {
        return try {
            val gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        log("BLE device connected: ${device.name}")
                        gatt?.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        log("BLE device disconnected: ${device.name}")
                        activeConnections.remove(device.address)
                        updateBondedDevices()
                    }
                }
                
                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        gatt?.services?.forEach { service ->
                            log("BLE service discovered: ${service.uuid}")
                        }
                    }
                }
                
                override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                    // Handle BLE notifications/indications
                    characteristic?.let { char ->
                        val data = char.value
                        log("BLE characteristic changed: ${char.uuid}, data: ${data.joinToString { "%02x".format(it) }}")
                    }
                }
            })
            
            val connection = BluetoothBleConnection(device, gatt)
            activeConnections[device.address] = connection
            updateBondedDevices()
            
            log("Connected to BLE device: ${device.name}")
            connection
        } catch (e: Exception) {
            log("Failed to connect to BLE device: ${e.message}")
            null
        }
    }
}

/**
 * Abstract base class for Bluetooth connections
 */
abstract class BluetoothConnection(
    val device: BluetoothDevice,
) {
    abstract suspend fun sendData(data: ByteArray): Boolean
    abstract fun disconnect()
    abstract fun isConnected(): Boolean
}

/**
 * Bluetooth Classic (RFCOMM) connection
 */
class BluetoothClassicConnection(
    device: BluetoothDevice,
    private val socket: BluetoothSocket
) : BluetoothConnection(
    device = device,
) {
    private val outputStream: OutputStream? = socket.outputStream
    
    override suspend fun sendData(data: ByteArray): Boolean {
        return try {
            outputStream?.write(data)
            outputStream?.flush()
            true
        } catch (e: IOException) {
            log("Failed to send data via classic connection: ${e.message}")
            false
        }
    }
    
    override fun disconnect() {
        try {
            socket.close()
        } catch (e: IOException) {
            log("Error closing classic connection: ${e.message}")
        }
    }
    
    override fun isConnected(): Boolean {
        return socket.isConnected
    }
}

/**
 * Bluetooth Low Energy (BLE) connection
 */
class BluetoothBleConnection(
    device: BluetoothDevice,
    private val gatt: BluetoothGatt
) : BluetoothConnection(
    device = device,
) {
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    
    init {
        // Find write characteristic for printing
        gatt.services?.forEach { service ->
            service.characteristics?.forEach { characteristic ->
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                    writeCharacteristic = characteristic
                }
            }
        }
    }
    
    override suspend fun sendData(data: ByteArray): Boolean {
        return try {
            writeCharacteristic?.let { char ->
                // Split data into chunks if needed (BLE MTU is typically 20-244 bytes)
                val chunkSize = 20 // Conservative chunk size
                var offset = 0
                
                while (offset < data.size) {
                    val chunk = data.sliceArray(offset until minOf(offset + chunkSize, data.size))
                    char.value = chunk
                    gatt.writeCharacteristic(char)
                    
                    // Wait a bit between chunks
                    kotlinx.coroutines.delay(10)
                    offset += chunkSize
                }
                true
            } ?: false
        } catch (e: Exception) {
            log("Failed to send data via BLE connection: ${e.message}")
            false
        }
    }
    
    override fun disconnect() {
        try {
            gatt.disconnect()
            gatt.close()
        } catch (e: Exception) {
            log("Error closing BLE connection: ${e.message}")
        }
    }
    
    override fun isConnected(): Boolean {
        return gatt.device.bondState == BluetoothDevice.BOND_BONDED
    }
}
