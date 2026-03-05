package com.velox.jewelvault.ui.screen.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.bluetooth.BleManager
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.printer.PrinterEntity
import com.velox.jewelvault.ui.screen.bluetooth.components.isPrinterDevice
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ScanConnectViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val appDatabase: AppDatabase,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>
) : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothScanConnectUiState())
    val uiState: StateFlow<BluetoothScanConnectUiState> = _uiState.asStateFlow()
    val bluetoothReceiver = bleManager
    val currentScreenHeadingState = _currentScreenHeadingState

    val connectedDevices: StateFlow<List<BluetoothDeviceDetails>> = bleManager.connectedDevices.asStateFlow()
    val connectingDevices: StateFlow<List<BluetoothDeviceDetails>> = bleManager.connectingDevices.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    
    // Saved printers from Room database
    val savedPrinters: StateFlow<List<PrinterEntity>> = appDatabase.printerDao().getAllPrinters().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )

    // Saved printers with connection status
    val savedPrintersWithStatus: StateFlow<List<Pair<PrinterEntity, Boolean>>> = combine(
        savedPrinters,
        connectedDevices
    ) { saved, connected ->
        saved.map { printer ->
            val isConnected = connected.any { it.address == printer.address }
            printer to isConnected
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )

    // Combined discovered devices (classic + LE)
    val allDiscoveredDevices: StateFlow<List<BluetoothDeviceDetails>> = combine(
        bleManager.classicDiscoveredDevices,
        bleManager.leDiscoveredDevices
    ) { classic, le ->
        val merged = (classic + le).distinctBy { it.address }

        fun isUnknownName(device: BluetoothDeviceDetails): Boolean {
            val name = device.name?.trim()
            return name.isNullOrBlank() || name == "Unknown Device"
        }

        fun nameKey(device: BluetoothDeviceDetails): String {
            val name = device.name?.trim()
            return if (!name.isNullOrBlank() && name != "Unknown Device") {
                name.lowercase()
            } else {
                "~"
            }
        }

        fun macClusterKey(address: String): String {
            val clean = address.uppercase().replace(":", "")
            // Use first 5 bytes to keep near-matching MACs close (dual Classic/LE variants).
            return if (clean.length >= 10) clean.substring(0, 10) else clean
        }

        val named = merged
            .filterNot { isUnknownName(it) }
            .sortedWith(compareBy<BluetoothDeviceDetails>({ nameKey(it) }, { it.address.lowercase() }))

        val unknown = merged.filter { isUnknownName(it) }
        val unknownByMac = unknown
            .groupBy { macClusterKey(it.address) }
            .mapValues { entry ->
                entry.value.sortedBy { it.address.lowercase() }
            }
            .toMutableMap()

        val ordered = mutableListOf<BluetoothDeviceDetails>()

        for (device in named) {
            ordered.add(device)
            val key = macClusterKey(device.address)
            val related = unknownByMac.remove(key)
            if (!related.isNullOrEmpty()) {
                ordered.addAll(related)
            }
        }

        val remainingUnknown = unknownByMac.values.flatten().sortedBy { it.address.lowercase() }
        ordered + remainingUnknown
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )

    // Separate bonded devices into connected and not connected, with printers prioritized
    val unconnectedPairedDevices: StateFlow<List<BluetoothDeviceDetails>> = combine(
        bleManager.bondedDevices,
        bleManager.connectedDevices,
        bleManager.connectingDevices,
        savedPrinters
    ) { bonded, connected, connecting, savedPrintersList ->
        val connectedAddresses = connected.map { it.address }.toSet()
        val connectingAddresses = connecting.map { it.address }.toSet()
        val unconnected = bonded.filter { it.address !in connectedAddresses && it.address !in connectingAddresses }

        // Separate printers and non-printers, then prioritize printers
        val printers = unconnected.filter { isPrinterDevice(it, savedPrintersList) }
        val nonPrinters = unconnected.filterNot { isPrinterDevice(it, savedPrintersList) }

        // Sort printers and non-printers alphabetically by name
        val sortedPrinters = printers.sortedBy { it.name?.lowercase() ?: "~" }
        val sortedNonPrinters = nonPrinters.sortedBy { it.name?.lowercase() ?: "~" }

        sortedPrinters + sortedNonPrinters
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )

    init {
        // Set screen heading when ViewModel is created
        currentScreenHeadingState.value = "Bluetooth Devices"
    }

    fun startScanning() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = false)

                // Start unified scanning (both Classic and BLE)
                bleManager.startUnifiedScanning()

                log("Started unified device scanning")
            } catch (e: Exception) {
                log("Error starting scan: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to start scanning: ${e.message}"
                )
            }
        }
    }

    fun stopScanning() {
        viewModelScope.launch {
            try {
                // Stop unified scanning (both Classic and BLE)
                bleManager.stopUnifiedScanning()

                log("Stopped unified device scanning")
            } catch (e: Exception) {
                log("Error stopping scan: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    message = "Failed to stop scanning: ${e.message}"
                )
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String, isPrinter: Boolean) {
        viewModelScope.launch  {
            try {
                log("CONNECT_VIEWMODEL: Starting connection to device: $deviceAddress")
                _uiState.value = _uiState.value.copy(isLoading = true)
                _snackBarState.value = if (isPrinter) "Connecting to printer..." else "Connecting to device..."

                // Add timeout mechanism to prevent infinite waiting
                var connectionCompleted = false
                val timeoutJob = launch {
                    delay(45000) // 45 second timeout
                    if (!connectionCompleted) {
                        log("CONNECT_VIEWMODEL: Connection timeout for $deviceAddress")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _snackBarState.value = "Connection timeout. Please try again."
                        connectionCompleted = true
                    }
                }

                bleManager.connect(
                    address = deviceAddress,
                    onConnect = { device ->
                        if (!connectionCompleted) {
                            connectionCompleted = true
                            timeoutJob.cancel()
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            _snackBarState.value = if (isPrinter) "Connected to printer: ${device.name ?: device.address}" else "Connected to ${device.name ?: device.address}"
                            log("CONNECT_VIEWMODEL: Connected to device: ${device.address}")
                            
                            // Save printer info if it's a printer
                            if (isPrinter) {
                                savePrinterInfo(device)
                            }
                        }
                    },
                    onFailure = { t ->
                        if (!connectionCompleted) {
                            connectionCompleted = true
                            timeoutJob.cancel()
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            _snackBarState.value = "Failed to connect: ${t.message ?: "Unknown error"}"
                            log("CONNECT_VIEWMODEL: Connection failed: ${t.message}")
                            bleManager.removeDevice(bleManager.connectingDevices, deviceAddress)
                        }
                    }
                )
            } catch (e: Exception) {
                log("CONNECT_VIEWMODEL: Error connecting to device: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                _snackBarState.value = "Failed to connect: ${e.message}"
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectByMacAddress(deviceAddress: String, isPrinter: Boolean) {
        viewModelScope.launch {
            try {
                log("MAC_CONNECT: Starting MAC connect for $deviceAddress")
                _uiState.value = _uiState.value.copy(isLoading = true)

                val deviceDetails = bleManager.getDiscoveredClassicDevice(deviceAddress)
                    ?: bleManager.getDiscoveredLeDevice(deviceAddress)
                    ?: bleManager.getBondedDevice(deviceAddress)

                val isBonded = deviceDetails?.bondState == BluetoothDevice.BOND_BONDED
                if (isBonded) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    connectToDevice(deviceAddress, isPrinter)
                    return@launch
                }

                bleManager.addConnectingPlaceholder(
                    address = deviceAddress,
                    name = deviceDetails?.name,
                    extraInfo = mapOf("stage" to "pairing", "source" to "manual_mac")
                )

                val pairingInitiated = bleManager.createBond(deviceAddress)
                _uiState.value = _uiState.value.copy(isLoading = false)

                if (pairingInitiated) {
                    _snackBarState.value = "Pairing initiated for ${deviceDetails?.name ?: deviceAddress}"
                    startPairingMonitor(deviceAddress, isPrinter)
                } else {
                    _snackBarState.value = "Pairing failed. Trying direct connect..."
                    connectToDevice(deviceAddress, isPrinter)
                }
            } catch (e: Exception) {
                log("MAC_CONNECT: Error connecting by MAC: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                _snackBarState.value = "MAC connect failed: ${e.message}"
            }
        }
    }

    fun disconnectDevice(deviceAddress: String) {
        viewModelScope.launch {
            try {
                bleManager.disconnectDevice(deviceAddress)
                _snackBarState.value = "Disconnected from device"
                log("Disconnected from device: $deviceAddress")
            } catch (e: Exception) {
                log("Error disconnecting from device: ${e.message}")
                _snackBarState.value = "Failed to disconnect: ${e.message}"
            }
        }
    }

    fun cancelConnection(deviceAddress: String) {
        viewModelScope.launch {
            try {
                log("Canceling connection to device: $deviceAddress")
                bleManager.cancelConnection(deviceAddress) // This will cancel the connection job
                _snackBarState.value = "Connection canceled"
            } catch (e: Exception) {
                log("Error canceling connection: ${e.message}")
                _snackBarState.value = "Failed to cancel connection: ${e.message}"
            }
        }
    }

    fun pairDevice(deviceAddress: String, isPrinter: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                log("PAIR: Starting pair-then-connect process for device: $deviceAddress")
                _snackBarState.value = "Pairing and connecting..."

                // Get the device from discovered devices
                val deviceDetails = bleManager.getDiscoveredClassicDevice(deviceAddress)
                    ?: bleManager.getDiscoveredLeDevice(deviceAddress)
                    ?: bleManager.getBondedDevice(deviceAddress)
                if (deviceDetails != null) {
                    log("PAIR: Found device ${deviceDetails.name} (${deviceDetails.address})")
                    // Immediately reflect connecting placeholder in UI
                    bleManager.addConnectingPlaceholder(
                        address = deviceDetails.address,
                        name = deviceDetails.name,
                        extraInfo = mapOf("stage" to "pairing")
                    )

                    // Check if already bonded
                    if (deviceDetails.bondState == BluetoothDevice.BOND_BONDED) {
                        log("PAIR: Device $deviceAddress is already bonded, connecting directly")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        connectToDevice(deviceAddress, isPrinter)
                        return@launch
                    }

                    // Attempt to create bond
                    val result = bleManager.createBond(deviceAddress)
                    if (result) {
                        log("PAIR: Bond creation initiated for $deviceAddress")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _snackBarState.value = "Pairing initiated for ${deviceDetails.name ?: deviceAddress}."

                        // Start monitoring for successful pairing, then auto-connect
                        startPairingMonitor(deviceAddress, isPrinter)
                    } else {
                        log("PAIR: Failed to initiate bond creation for $deviceAddress")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _snackBarState.value = "Failed to initiate pairing for ${deviceDetails.name ?: deviceAddress}"
                    }
                } else {
                    log("PAIR: Device $deviceAddress not found in discovered devices")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _snackBarState.value = "Device not found. Please refresh and try again."
                }

            } catch (e: Exception) {
                log("PAIR: Error pairing device: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                _snackBarState.value = "Failed to pair device: ${e.message}"
            }
        }
    }



    /**
     * Monitors pairing progress and automatically connects when pairing is complete
     */
    private fun startPairingMonitor(deviceAddress: String, isPrinter: Boolean) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 60 // Monitor for 60 seconds (1 second intervals)
            var bondingStarted = false
            var retryCount = 0
            val maxRetries = 1

            while (attempts < maxAttempts) {
                delay(1000) // Check every second
                attempts++

                try {
                    val deviceDetails = bleManager.getDiscoveredClassicDevice(deviceAddress)
                        ?: bleManager.getDiscoveredLeDevice(deviceAddress)
                        ?: bleManager.getBondedDevice(deviceAddress)
                    val bondState = deviceDetails?.bondState

                    if (bondState == BluetoothDevice.BOND_BONDING) {
                        bondingStarted = true
                    }

                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        log("PAIR: Pairing completed for $deviceAddress, starting connection")
                        _snackBarState.value = "Pairing completed! Connecting to ${deviceDetails?.name ?: deviceAddress}..."
                        connectToDevice(deviceAddress, isPrinter)
                        return@launch
                    }

            if (bondingStarted && bondState == BluetoothDevice.BOND_NONE) {
                        if (retryCount < maxRetries) {
                            retryCount += 1
                            bondingStarted = false
                            log("PAIR: Pairing failed for $deviceAddress, retrying ($retryCount/$maxRetries)")
                            _snackBarState.value = "Pairing failed. Please enter PIN again."
                            try {
                                bleManager.removeBond(deviceAddress)
                            } catch (_: Exception) {
                            }
                            delay(800)
                            val retry = bleManager.createBond(deviceAddress)
                            if (retry) {
                                _snackBarState.value = "Retrying pairing... Please enter PIN."
                            } else {
                                _snackBarState.value = "Pairing retry failed. Please try again."
                                bleManager.removeDevice(bleManager.connectingDevices, deviceAddress)
                                return@launch
                            }
                        } else {
                            _snackBarState.value = "Pairing failed. Please try again."
                            bleManager.removeDevice(bleManager.connectingDevices, deviceAddress)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    log("PAIR: Error monitoring pairing for $deviceAddress: ${e.message}")
                }
            }

            log("PAIR: Pairing monitor timeout for $deviceAddress")
            _snackBarState.value = "Pairing timeout. Please try connecting manually."
            bleManager.removeDevice(bleManager.connectingDevices, deviceAddress)
        }
    }

    fun refreshAllDeviceLists() {
        viewModelScope.launch {
            try {
                bleManager.refreshAllDeviceLists()
                log("Refreshed connected devices list")
            } catch (e: Exception) {
                log("Error refreshing connected devices: ${e.message}")
            }
        }
    }

    fun connectToPrinterUsingSavedMethod(address: String, savedMethod: String) {
        viewModelScope.launch {
            try {
                log("CONNECT_PRINTER: Starting connection to printer: $address using method: $savedMethod")
                _uiState.value = _uiState.value.copy(isLoading = true, message = "Connecting to printer using saved method: $savedMethod...")

                // Add timeout mechanism to prevent infinite waiting
                var connectionCompleted = false
                val timeoutJob = launch {
                    delay(45000) // 45 second timeout
                    if (!connectionCompleted) {
                        log("CONNECT_PRINTER: Connection timeout for $address")
                        _uiState.value = _uiState.value.copy(isLoading = false, message = "Connection timeout. Please try again.")
                        connectionCompleted = true
                    }
                }

                bleManager.connectToPrinterUsingSavedMethod(
                    address = address,
                    savedMethod = savedMethod,
                    onConnect = { device ->
                        if (!connectionCompleted) {
                            connectionCompleted = true
                            timeoutJob.cancel()
                            _uiState.value = _uiState.value.copy(isLoading = false, message = "Connected to printer: ${device.name ?: device.address}")
                            log("CONNECT_PRINTER: Connected to printer: ${device.address}")
                            // Refresh device lists to update UI
                            bleManager.refreshAllDeviceLists()
                        }
                    },
                    onFailure = { t ->
                        if (!connectionCompleted) {
                            connectionCompleted = true
                            timeoutJob.cancel()
                            _uiState.value = _uiState.value.copy(isLoading = false, message = "Failed to connect to printer: ${t.message ?: "Unknown error"}")
                            log("CONNECT_PRINTER: Connection failed: ${t.message}")
                            // Refresh device lists even on failure to update UI
                            bleManager.refreshAllDeviceLists()
                            bleManager.removeDevice(bleManager.connectingDevices, address)
                        }
                    }
                )
            } catch (e: Exception) {
                log("CONNECT_PRINTER: Error connecting to printer: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, message = "Failed to connect to printer: ${e.message}")
            }
        }
    }


    fun forgetDevice(deviceAddress: String) {
        viewModelScope.launch {
            try {
                log("FORGET: Starting forget process for device: $deviceAddress")
                _uiState.value = _uiState.value.copy(isLoading = true)

                // First disconnect if connected
                val connectedDevice = bleManager.connectedDevices.value.find { it.address == deviceAddress }
                if (connectedDevice != null) {
                    bleManager.disconnectDevice(deviceAddress)
                    delay(1000) // Give time for disconnect
                }

                // Remove the bond/unpair the device
                val success = bleManager.removeBond(deviceAddress)

                if (success) {
                    _snackBarState.value = "Device forgotten successfully"
                    log("FORGET: Successfully forgot device: $deviceAddress")

                    // Give the system a moment to process the bond removal
                    delay(500)

                    // Immediately update bonded devices list after successful forget
                    bleManager.updateBondedDevices()

                    // Also refresh all device lists to ensure UI consistency
                    bleManager.refreshAllDeviceLists()
                } else {
                    _snackBarState.value = "Failed to forget device"
                    log("FORGET: Failed to forget device: $deviceAddress")
                }

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                log("FORGET: Error forgetting device: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                _snackBarState.value = "Failed to forget device: ${e.message}"
            }
        }
    }

    /**
     * Add a connected device as a printer manually
     */
    fun addDeviceAsPrinter(device: BluetoothDeviceDetails) {
        viewModelScope.launch {
            try {
                log("addDeviceAsPrinter: Adding device ${device.address} (${device.name}) as printer")
                
                // Check if this is the first printer being saved
                val existingPrinters = appDatabase.printerDao().getAllPrinters().first()
                val isFirstPrinter = existingPrinters.isEmpty()
                
                val printerEntity = PrinterEntity(
                    name = device.name,
                    address = device.address,
                    method = device.extraInfo["connectionMethod"] ?: "UNKNOWN",
                    isDefault = isFirstPrinter, // Set as default if first printer
                    lastConnectedAt = System.currentTimeMillis(),
                    supportedLanguages = null, // Will be set later when testing protocols
                    currentLanguage = null
                )
                
                appDatabase.printerDao().insertPrinter(printerEntity)
                
                if (isFirstPrinter) {
                    log("addDeviceAsPrinter: Set ${device.address} as default printer (first printer)")
                    _snackBarState.value = "Added ${device.name ?: device.address} as printer and set as default"
                } else {
                    _snackBarState.value = "Added ${device.name ?: device.address} as printer"
                }
                
                log("addDeviceAsPrinter: Successfully added printer ${device.address}")
            } catch (e: Exception) {
                log("addDeviceAsPrinter: Error adding printer ${device.address}: ${e.message}")
                _snackBarState.value = "Failed to add printer: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun showNotPrinterMessage() {
        _uiState.value = _uiState.value.copy(message = "This device is not a saved printer")
    }

    /**
     * Check connection liveness for a saved printer
     */
    fun checkPrinterConnection(address: String, method: String) {
        viewModelScope.launch {
            try {
                val isAlive = bleManager.checkPrinterConnectionLiveness(address, method)
                val printerName = savedPrinters.value.find { it.address == address }?.name ?: address
                _snackBarState.value = if (isAlive) {
                    "Printer $printerName is connected and responsive"
                } else {
                    "Printer $printerName is not responding"
                }
                log("Connection check for $address ($method): $isAlive")
            } catch (e: Exception) {
                log("Error checking printer connection: ${e.message}")
                _snackBarState.value = "Failed to check printer connection: ${e.message}"
            }
        }
    }

    /**
     * Save printer info to Room database when a printer connects
     */
    private fun savePrinterInfo(device: BluetoothDeviceDetails) {
        viewModelScope.launch {
            try {
                log("savePrinterInfo: Saving printer ${device.address} (${device.name})")
                
                // Check if this is the first printer being saved
                val existingPrinters = appDatabase.printerDao().getAllPrinters().first()
                val isFirstPrinter = existingPrinters.isEmpty()
                
                val printerEntity = PrinterEntity(
                    name = device.name,
                    address = device.address,
                    method = device.extraInfo["connectionMethod"] ?: "UNKNOWN",
                    isDefault = isFirstPrinter, // Set as default if first printer
                    lastConnectedAt = System.currentTimeMillis(),
                    supportedLanguages = null, // Will be set later when testing protocols
                    currentLanguage = null
                )
                
                appDatabase.printerDao().insertPrinter(printerEntity)
                
                if (isFirstPrinter) {
                    log("savePrinterInfo: Set ${device.address} as default printer (first printer)")
                }
                
            } catch (e: Exception) {
                log("savePrinterInfo: Error saving printer ${device.address}: ${e.message}")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onCleared() {
        super.onCleared()
        // Stop scanning when ViewModel is cleared
        bleManager.stopUnifiedScanning()
    }
}

data class BluetoothScanConnectUiState(
    val isLoading: Boolean = false,
    val message: String? = null
)
