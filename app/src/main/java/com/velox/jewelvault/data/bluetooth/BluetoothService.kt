package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.velox.jewelvault.utils.log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class BluetoothService @Inject constructor(
    context: Context,
    private val broadcastReceiver: BluetoothBroadcastReceiver
) {
    companion object {
        private const val TAG = "BluetoothService"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CONNECTION_TIMEOUT = 10_000L // 10s
    }

    // prefer applicationContext to avoid leaking Activity contexts
    private val appContext: Context = context.applicationContext

    // dedicated scope so we can cancel when fully disposing manager
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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

    // internal map and exposed list flow
    private val _devicesMap = MutableStateFlow<Map<String, DeviceUiModel>>(emptyMap())
    private val _devicesList = MutableStateFlow<List<DeviceUiModel>>(emptyList())
    val devices: StateFlow<List<DeviceUiModel>> = _devicesList.asStateFlow()

    // mirror broadcastReceiver flows
    val isDiscovering: StateFlow<Boolean> = broadcastReceiver.isDiscovering
    val deviceFound: SharedFlow<DeviceFound> = broadcastReceiver.deviceFound
    val bluetoothStateChanged: SharedFlow<BluetoothStateChange> = broadcastReceiver.bluetoothStateChanged

    // connection monitoring
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionState> = _connectionStatus.asStateFlow()

    private val _bluetoothAdapterState = MutableStateFlow(BluetoothAdapter.STATE_OFF)
    val bluetoothAdapterState: StateFlow<Int> = _bluetoothAdapterState.asStateFlow()

    private val _connectionResult = MutableSharedFlow<ConnectionResult>(replay = 1)
    val connectionResult: SharedFlow<ConnectionResult> = _connectionResult.asSharedFlow()

    // stable paired devices flow
    private val _pairedDevicesSnapshot = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private var hasRefreshedOnInit = false

    // socket + streams
    private var currentSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    init {
        // keep _devicesList in sync with _devicesMap
        scope.launch {
            _devicesMap.collect { map ->
                val list = map.values.sortedByDescending { it.lastSeenMillis }
                _devicesList.value = list
            }
        }

        observeBroadcastReceiver()
        seedPairedDevicesSnapshot()
        
        // Register broadcast receiver immediately in init
        if (!broadcastReceiver.isRegistered()) {
            log("BluetoothService init: Registering broadcast receiver...")
            broadcastReceiver.register()
            log("BluetoothService init: Broadcast receiver registration completed - isRegistered: ${broadcastReceiver.isRegistered()}")
        }
    }

    private fun seedPairedDevicesSnapshot() {
        scope.launch {
            val paired = try {
                if (!hasBluetoothPermissions()) {
                    log("Missing Bluetooth permissions for bonded devices")
                    emptyList()
                } else {
                    bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
                }
            } catch (e: SecurityException) {
                log("SecurityException reading bonded devices: ${e.message}")
                emptyList()
            } catch (t: Throwable) {
                log("Error reading bonded devices: ${t.message}")
                emptyList()
            }
            _pairedDevicesSnapshot.value = paired
            // also seed devices map
            paired.forEach { d ->
                updateDevice(d, null, d.bondState, ConnectionState.DISCONNECTED)
            }
        }
    }

    private fun observeBroadcastReceiver() {
        log("BluetoothService: observing broadcastReceiver")
        
        // Observe discovery state changes
        scope.launch {
            isDiscovering.collect { discovering ->
                log("BluetoothService: Discovery state changed to: $discovering")
            }
        }
        
        // device found
        scope.launch {
            deviceFound.collect { df ->
                log("BluetoothService: Processing found device - Address: ${df.device.address}, Name: '${df.device.name}', RSSI: ${df.rssi}, Bond State: ${df.device.bondState}")
                updateDevice(df.device, df.rssi, null, null)
                log("BluetoothService: Device added to map - Total devices: ${_devicesMap.value.size}")
            }
        }
        // connection state changes
        scope.launch {
            broadcastReceiver.connectionStateChanged.collect { change ->
                log("Connection state change for ${change.device.address} state=${change.connectionState} bond=${change.bondState}")
                updateDevice(change.device, null, change.bondState, change.connectionState)
                if (change.connectionState == ConnectionState.DISCONNECTED) {
                    // if this was the connected device, clear it
                    if (_connectedDevice.value?.address == change.device.address) {
                        _connectedDevice.value = null
                        _connectionStatus.value = ConnectionState.DISCONNECTED
                    }
                }
            }
        }
        // bluetooth adapter changes
        scope.launch {
            bluetoothStateChanged.collect { st ->
                log("Bluetooth adapter ${st.previousState} -> ${st.currentState}")
                _bluetoothAdapterState.value = st.currentState
                if (st.currentState == BluetoothAdapter.STATE_OFF) {
                    _connectedDevice.value = null
                    _connectionStatus.value = ConnectionState.DISCONNECTED
                }
            }
        }
    }

    /**
     * Update device info (debounced update of the map & list)
     */
    private fun updateDevice(device: BluetoothDevice, rssi: Int?, bondState: Int?, connectionState: ConnectionState?) {
        val addr = device.address ?: return
        val now = System.currentTimeMillis()
        val existing = _devicesMap.value[addr]

        log("BluetoothService: updateDevice called - Address: $addr, Name: '${device.name}', RSSI: $rssi, Bond: $bondState, Connection: $connectionState")

        // debounce quick duplicates
        if (existing != null && now - existing.lastSeenMillis < 200) {
            if (rssi != null && existing.rssi != rssi) {
                val updated = existing.copy(rssi = rssi, lastSeenMillis = now)
                _devicesMap.value = _devicesMap.value.toMutableMap().apply { put(addr, updated) }
                log("BluetoothService: Updated existing device RSSI - $addr")
            }
            return
        }

        val newModel = DeviceUiModel(
            name = device.name,
            address = addr,
            rssi = rssi ?: existing?.rssi,
            bondState = bondState ?: existing?.bondState ?: device.bondState,
            connectionState = connectionState ?: existing?.connectionState ?: ConnectionState.DISCONNECTED,
            lastSeenMillis = now,
            deviceType = getDeviceType(device),
            isPrinterCandidate = isPrinterCandidate(device)
        )

        _devicesMap.value = _devicesMap.value.toMutableMap().apply { put(addr, newModel) }
        log("BluetoothService: Device model created/updated - Type: ${newModel.deviceType}, Printer Candidate: ${newModel.isPrinterCandidate}, Total devices: ${_devicesMap.value.size}")
    }

    // region lifecycle / start / stop
    fun start() {
        log("BluetoothService start — receiver status: ${broadcastReceiver.getRegistrationStatus()}")
        // Broadcast receiver is already registered in init
        // seeding already done in init; you can start discovery externally
        
        // Refresh system state only on first start
        if (!hasRefreshedOnInit) {
            log("BluetoothService: First time start - refreshing system state")
            refreshSystemState()
            hasRefreshedOnInit = true
        } else {
            log("BluetoothService: Already refreshed on previous start - skipping")
        }
    }

    /**
     * Stop ongoing activities but keep manager usable.
     * Use close() to fully cancel internal coroutine scope.
     */
    fun stop() {
        log("BluetoothService stop: cancelling running tasks (but not closing scope)")
        // don't cancel scope here — just stop discovery and disconnect in background
        scope.launch {
            try { stopDiscovery() } catch (t: Throwable) { log("stop: stopDiscovery err ${t.message}") }
            try { disconnect() } catch (t: Throwable) { log("stop: disconnect err ${t.message}") }
        }
        // Unregister broadcast receiver
        if (broadcastReceiver.isRegistered()) {
            broadcastReceiver.unregister()
            log("BluetoothService: Unregistered broadcast receiver")
        }
    }

    /**
     * Close the manager permanently (cancels scope)
     */
    fun close() {
        log("BluetoothService close: cancelling scope")
        scope.cancel()
    }
    // endregion

    // region permissions & availability
    fun isBluetoothAvailable(): Boolean {
        return try { 
            if (!hasBluetoothPermissions()) {
                log("Missing Bluetooth permissions for availability check")
                false
            } else {
                bluetoothAdapter != null && bluetoothAdapter!!.isEnabled
            }
        } catch (e: SecurityException) {
            log("SecurityException checking Bluetooth availability: ${e.message}")
            false
        } catch (t: Throwable) { 
            log("Error checking Bluetooth availability: ${t.message}")
            false 
        }
    }

    fun requestBluetoothEnableIntent(): Intent {
        return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    }

    fun openBluetoothSettingsIntent(): Intent {
        return Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    }

    fun hasBluetoothPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
            permissions += Manifest.permission.BLUETOOTH_SCAN
            // ACCESS_FINE_LOCATION may still be needed for older target behaviour
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        } else {
            permissions += Manifest.permission.BLUETOOTH
            permissions += Manifest.permission.BLUETOOTH_ADMIN
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        return permissions.all { ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED }
    }
    // endregion

    // region paired devices helpers
    /**
     * Snapshot list of paired devices (stable reference)
     */
    fun getPairedDevicesSnapshot(): List<BluetoothDevice> = _pairedDevicesSnapshot.value

    /**
     * Flow of paired devices snapshot (updates when reseeded)
     */
    fun getPairedDevicesFlow(): StateFlow<List<BluetoothDevice>> = _pairedDevicesSnapshot.asStateFlow()
    // endregion

    // region discovery
    suspend fun startDiscovery(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            log("BluetoothService: startDiscovery called")
            
            if (!isBluetoothAvailable()) {
                log("BluetoothService: startDiscovery failed - Bluetooth not available")
                return@withContext Result.failure(Exception("Bluetooth not available"))
            }
            if (!hasBluetoothPermissions()) {
                log("BluetoothService: startDiscovery failed - Missing Bluetooth permissions")
                return@withContext Result.failure(Exception("Missing Bluetooth permissions"))
            }
            if (isDiscovering.value) {
                log("BluetoothService: startDiscovery skipped - already discovering")
                return@withContext Result.success(Unit)
            }

            val started = try {
                if (hasBluetoothPermissions()) {
                    log("BluetoothService: Starting discovery...")
                    bluetoothAdapter?.startDiscovery() ?: false
                } else {
                    log("Missing permissions for startDiscovery")
                    false
                }
            } catch (e: SecurityException) {
                log("SecurityException starting discovery: ${e.message}")
                false
            } catch (t: Throwable) { 
                log("startDiscovery err ${t.message}")
                false 
            }

            if (started) {
                log("BluetoothService: startDiscovery succeeded")
                Result.success(Unit)
            } else {
                log("BluetoothService: startDiscovery failed - returned false")
                Result.failure(Exception("startDiscovery returned false"))
            }
        } catch (t: Throwable) {
            log("BluetoothService: startDiscovery exception: ${t.message}")
            Result.failure(t)
        }
    }

    suspend fun stopDiscovery(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try { 
                if (hasBluetoothPermissions()) {
                    bluetoothAdapter?.cancelDiscovery()
                } else {
                    log("Missing permissions for stopDiscovery")
                }
            } catch (e: SecurityException) {
                log("SecurityException stopping discovery: ${e.message}")
            } catch (t: Throwable) { 
                log("stopDiscovery err ${t.message}") 
            }
            
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
    // endregion

    // region connect / disconnect
    suspend fun connect(deviceAddress: String): Result<ConnectionResult> = withContext(Dispatchers.IO) {
        try {
            if (!isBluetoothAvailable()) return@withContext Result.failure(Exception("Bluetooth not available"))
            stopDiscovery()

            if (!hasBluetoothPermissions()) return@withContext Result.failure(Exception("Missing Bluetooth permissions"))

            val device = try { 
                if (hasBluetoothPermissions()) {
                    bluetoothAdapter?.getRemoteDevice(deviceAddress)
                } else {
                    log("Missing permissions for getRemoteDevice")
                    null
                }
            } catch (e: SecurityException) {
                log("SecurityException getting remote device: ${e.message}")
                null
            } catch (t: Throwable) { 
                log("Error getting remote device: ${t.message}")
                null 
            } ?: return@withContext Result.failure(Exception("Device not found: $deviceAddress"))

            _connectionStatus.value = ConnectionState.CONNECTING

            val result = tryConnectWithFallbacks(device)

            if (result.success) {
                _connectedDevice.value = device
                _connectionStatus.value = ConnectionState.CONNECTED
                log("Connected to ${device.address}")
            } else {
                _connectionStatus.value = ConnectionState.DISCONNECTED
                log("Connect failed ${device.address} reason=${result.error}")
            }

            _connectionResult.emit(result)
            Result.success(result)
        } catch (t: Throwable) {
            _connectionStatus.value = ConnectionState.DISCONNECTED
            val res = ConnectionResult(false, t.message, deviceAddress)
            _connectionResult.emit(res)
            Result.failure(t)
        }
    }

    private suspend fun tryConnectWithFallbacks(device: BluetoothDevice): ConnectionResult {
        listOf(::tryInsecureConnection, ::trySecureConnection, ::tryReflectionConnection).forEach { fn ->
            try {
                val r = fn(device)
                if (r.success) return r
            } catch (t: Throwable) {
                log("connect fallback ${fn.name} failed: ${t.message}")
            }
        }
        return ConnectionResult(false, "All methods failed", device.address)
    }

    private suspend fun tryInsecureConnection(device: BluetoothDevice): ConnectionResult = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermissions()) return@withContext ConnectionResult(false, "Missing permissions", device.address)
            val socket = try {
                device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: SecurityException) {
                return@withContext ConnectionResult(false, "SecurityException creating insecure socket: ${e.message}", device.address)
            }
            socket.connect()
            currentSocket = socket
            outputStream = socket.outputStream
            inputStream = socket.inputStream
            ConnectionResult(true, null, device.address)
        } catch (e: SecurityException) {
            ConnectionResult(false, "SecurityException in insecure connection: ${e.message}", device.address)
        } catch (t: Throwable) {
            ConnectionResult(false, "Insecure failed: ${t.message}", device.address)
        }
    }

    private suspend fun trySecureConnection(device: BluetoothDevice): ConnectionResult = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermissions()) return@withContext ConnectionResult(false, "Missing permissions", device.address)
            val socket = try {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: SecurityException) {
                return@withContext ConnectionResult(false, "SecurityException creating secure socket: ${e.message}", device.address)
            }
            socket.connect()
            currentSocket = socket
            outputStream = socket.outputStream
            inputStream = socket.inputStream
            ConnectionResult(true, null, device.address)
        } catch (e: SecurityException) {
            ConnectionResult(false, "SecurityException in secure connection: ${e.message}", device.address)
        } catch (t: Throwable) {
            ConnectionResult(false, "Secure failed: ${t.message}", device.address)
        }
    }

    private suspend fun tryReflectionConnection(device: BluetoothDevice): ConnectionResult = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermissions()) return@withContext ConnectionResult(false, "Missing permissions", device.address)

            val method: Method = try {
                device.javaClass.getMethod("createRfcommSocket", Integer.TYPE)
            } catch (e: SecurityException) {
                return@withContext ConnectionResult(false, "SecurityException getting reflection method: ${e.message}", device.address)
            }
            val socket = try {
                method.invoke(device, 1) as BluetoothSocket
            } catch (e: SecurityException) {
                return@withContext ConnectionResult(false, "SecurityException invoking reflection method: ${e.message}", device.address)
            }
            socket.connect()
            currentSocket = socket
            outputStream = socket.outputStream
            inputStream = socket.inputStream
            ConnectionResult(true, null, device.address)
        } catch (e: SecurityException) {
            ConnectionResult(false, "SecurityException in reflection connection: ${e.message}", device.address)
        } catch (t: Throwable) {
            ConnectionResult(false, "Reflection failed: ${t.message}", device.address)
        }
    }

    suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try { currentSocket?.close() } catch (_: Throwable) {}
            try { outputStream?.close() } catch (_: Throwable) {}
            try { inputStream?.close() } catch (_: Throwable) {}

            currentSocket = null
            outputStream = null
            inputStream = null
            _connectedDevice.value = null
            _connectionStatus.value = ConnectionState.DISCONNECTED
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
    // endregion

    // region send / probe
    suspend fun sendData(data: ByteArray): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val s = outputStream ?: return@withContext Result.failure(Exception("Not connected"))
            s.write(data)
            s.flush()
            Result.success(data.size)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun sendText(text: String): Result<Int> = withContext(Dispatchers.IO) {
        sendData(text.toByteArray(Charsets.UTF_8))
    }

    /**
     * Probe device: connects briefly and sends small probes.
     * Ensures socket/streams are closed in finally.
     */
    suspend fun probeDevice(device: BluetoothDevice): ProbeResult = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermissions()) return@withContext ProbeResult(false, null, "Missing permissions", 0)
        val start = System.currentTimeMillis()
        var tempSocket: BluetoothSocket? = null
        try {
            tempSocket = try {
                device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: SecurityException) {
                return@withContext ProbeResult(false, null, "SecurityException creating probe socket: ${e.message}", 0)
            }
            tempSocket.connect()
            val out = tempSocket.outputStream
            val probes = listOf(
                Pair("\u001B@", PrinterLanguage.ESC_POS),
                Pair("^XA^XZ", PrinterLanguage.ZPL),
                Pair("SIZE 100 mm,50 mm\n", PrinterLanguage.TSPL)
            )
            for ((cmd, lang) in probes) {
                try {
                    out.write(cmd.toByteArray())
                    out.flush()
                    val elapsed = System.currentTimeMillis() - start
                    return@withContext ProbeResult(true, lang, null, elapsed)
                } catch (e: SecurityException) {
                    return@withContext ProbeResult(false, null, "SecurityException during probe: ${e.message}", 0)
                } catch (t: Throwable) {
                    // try next probe
                }
            }
            val elapsed = System.currentTimeMillis() - start
            ProbeResult(false, null, "No probe succeeded", elapsed)
        } catch (e: SecurityException) {
            ProbeResult(false, null, "SecurityException in probe: ${e.message}", 0)
        } catch (t: Throwable) {
            ProbeResult(false, null, t.message, 0)
        } finally {
            try { tempSocket?.close() } catch (_: Throwable) {}
        }
    }
    // endregion

    // region detection helpers
    fun isPrinterCandidate(device: BluetoothDevice): Boolean {
        return try {
            if (!hasBluetoothPermissions()) {
                log("Missing permissions for isPrinterCandidate")
                false
            } else {
                val name = device.name?.lowercase() ?: ""
                val deviceClass = device.bluetoothClass?.deviceClass ?: 0
                val nameMatch = PrinterKeywords.PRINTER_NAMES.any { name.contains(it.lowercase()) }
                val classMatch = deviceClass == 0x1F00
                nameMatch || classMatch
            }
        } catch (e: SecurityException) {
            log("SecurityException in isPrinterCandidate: ${e.message}")
            false
        } catch (t: Throwable) {
            log("Error in isPrinterCandidate: ${t.message}")
            false
        }
    }

    fun getDeviceType(device: BluetoothDevice): DeviceType {
        return try {
            if (!hasBluetoothPermissions()) {
                log("Missing permissions for getDeviceType")
                DeviceType.UNKNOWN
            } else {
                val name = device.name?.lowercase() ?: ""
                when {
                    listOf("printer", "label", "thermal", "zebra", "tsc", "pos", "receipt").any { name.contains(it) } -> DeviceType.PRINTER
                    listOf("phone","mobile","galaxy","iphone","pixel").any { name.contains(it) } -> DeviceType.PHONE
                    listOf("tablet","ipad","tab").any { name.contains(it) } -> DeviceType.TABLET
                    listOf("laptop","macbook","thinkpad").any { name.contains(it) } -> DeviceType.LAPTOP
                    listOf("earphone","earbud","airpods").any { name.contains(it) } -> DeviceType.EARPHONE
                    listOf("headphone","headset").any { name.contains(it) } -> DeviceType.HEADPHONE
                    listOf("speaker","sound","audio").any { name.contains(it) } -> DeviceType.SPEAKER
                    else -> DeviceType.UNKNOWN
                }
            }
        } catch (e: SecurityException) {
            log("SecurityException in getDeviceType: ${e.message}")
            DeviceType.UNKNOWN
        } catch (t: Throwable) {
            log("Error in getDeviceType: ${t.message}")
            DeviceType.UNKNOWN
        }
    }
    // endregion

    /**
     * Best-effort cleanup: stops discovery, disconnects and closes scope.
     */
    fun cleanup() {
        try {
            runBlocking {
                try { stopDiscovery() } catch (_: Throwable) {}
                try { disconnect() } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            log("cleanup error: ${t.message}")
        } finally {
            // Unregister broadcast receiver
            if (broadcastReceiver.isRegistered()) {
                broadcastReceiver.unregister()
                log("BluetoothService: Unregistered broadcast receiver during cleanup")
            }
            close()
        }
    }


    /**
     * Refresh system state to sync with current Bluetooth connections
     */
    private fun refreshSystemState() {
        log("BluetoothService: Refreshing system state...")
        scope.launch {
            try {
                // Refresh paired devices to get current connection states
                val pairedDevices = try {
                    if (hasBluetoothPermissions()) {
                        bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
                    } else {
                        log("BluetoothService: Missing permissions for system state refresh")
                        emptyList()
                    }
                } catch (e: SecurityException) {
                    log("BluetoothService: SecurityException refreshing system state: ${e.message}")
                    emptyList()
                } catch (t: Throwable) {
                    log("BluetoothService: Error refreshing system state: ${t.message}")
                    emptyList()
                }
                
                log("BluetoothService: Found ${pairedDevices.size} paired devices for refresh")
                
                // Update each paired device with current state
                pairedDevices.forEach { device ->
                    log("BluetoothService: Refreshing device - ${device.name} (${device.address})")
                    updateDevice(device, null, device.bondState, ConnectionState.CONNECTED)
                }
                
                // Update paired devices snapshot
                _pairedDevicesSnapshot.value = pairedDevices
                
                // Start discovery to find nearby unpaired devices
                if (isBluetoothAvailable() && !isDiscovering.value) {
                    log("BluetoothService: Starting discovery to find nearby devices")
                    try {
                        val result = startDiscovery()
                        if (result.isSuccess) {
                            log("BluetoothService: Discovery started successfully")
                        } else {
                            log("BluetoothService: Failed to start discovery: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (t: Throwable) {
                        log("BluetoothService: Error starting discovery: ${t.message}")
                    }
                }
                
                log("BluetoothService: System state refresh completed")
            } catch (t: Throwable) {
                log("BluetoothService: Error during system state refresh: ${t.message}")
            }
        }
    }

}
