package com.velox.jewelvault.ui.screen.bluetooth

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.bluetooth.BleManager
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.PrinterInfo
import com.velox.jewelvault.ui.screen.bluetooth.components.isPrinterDevice
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ScanConnectViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val dataStoreManager: DataStoreManager,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BluetoothScanConnectUiState())
    val uiState: StateFlow<BluetoothScanConnectUiState> = _uiState.asStateFlow()
    val bluetoothReceiver = bleManager
    val currentScreenHeadingState = _currentScreenHeadingState

    // Device lists from BleManager
    val bondedDevices: StateFlow<List<BluetoothDeviceDetails>> = bleManager.bondedDevices
    val classicDiscoveredDevices: StateFlow<List<BluetoothDeviceDetails>> = bleManager.classicDiscoveredDevices
    val leDiscoveredDevices: StateFlow<List<BluetoothDeviceDetails>> = bleManager.leDiscoveredDevices
    val connectedDevices: StateFlow<List<BluetoothDeviceDetails>> = bleManager.connectedDevices.asStateFlow()
    val connectingDevices: StateFlow<List<BluetoothDeviceDetails>> = bleManager.connectingDevices.asStateFlow()
    
    // Combined discovered devices (classic + LE)
    val allDiscoveredDevices: StateFlow<List<BluetoothDeviceDetails>> = combine(
        bleManager.classicDiscoveredDevices,
        bleManager.leDiscoveredDevices
    ) { classic, le ->
        val merged = (classic + le).distinctBy { it.address }
        val (named, unnamed) = merged.partition { !it.name.isNullOrBlank() && it.name != "Unknown Device" }
        named.sortedBy { it.name?.lowercase() } + unnamed.sortedBy { it.name ?: "~" }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )
    
    // Separate bonded devices into connected and not connected, with printers prioritized
    val unconnectedPairedDevices: StateFlow<List<BluetoothDeviceDetails>> = combine(
        bleManager.bondedDevices,
        bleManager.connectedDevices,
        bleManager.connectingDevices
    ) { bonded, connected, connecting ->
        val connectedAddresses = connected.map { it.address }.toSet()
        val connectingAddresses = connecting.map { it.address }.toSet()
        val unconnected = bonded.filter { it.address !in connectedAddresses && it.address !in connectingAddresses }
        
        // Separate printers and non-printers, then prioritize printers
        val printers = unconnected.filter { isPrinterDevice(it) }
        val nonPrinters = unconnected.filterNot { isPrinterDevice(it) }
        
        // Sort printers by connection time (most recent first) and non-printers alphabetically
        val sortedPrinters = printers.sortedByDescending { 
            dataStoreManager.getSavedPrinters().find { saved -> saved.address == it.address }?.connectionTime ?: 0L
        }
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
    
    fun refreshDevices() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Stop and restart unified scanning to refresh
                bleManager.stopUnifiedScanning()
                
                // Small delay before restarting
                kotlinx.coroutines.delay(500)
                
                bleManager.startUnifiedScanning()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Refreshing device list..."
                )
                
                log("Refreshed device list")
            } catch (e: Exception) {
                log("Error refreshing devices: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to refresh devices: ${e.message}"
                )
            }
        }
    }
    
    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            try {
                log("CONNECT_VIEWMODEL: Starting connection to device: $deviceAddress")
                _uiState.value = _uiState.value.copy(isLoading = true)

                bleManager.connect(deviceAddress)

                // Register callback to save printer info when connection succeeds
                val deviceDetails = bleManager.getDiscoveredClassicDevice(deviceAddress) 
                    ?: bleManager.getDiscoveredLeDevice(deviceAddress)
                    ?: bleManager.getBondedDevice(deviceAddress)
                if (deviceDetails != null && isPrinterDevice(deviceDetails)) {
                    bleManager.registerConnectionSuccessCallback(deviceAddress) { connectionMethod ->
                        viewModelScope.launch {
                            val printerInfo = PrinterInfo(
                                address = deviceDetails.address,
                                name = deviceDetails.name ?: "Unknown Printer",
                                connectionMethod = connectionMethod,
                                connectionTime = System.currentTimeMillis()
                            )
                            dataStoreManager.savePrinter(printerInfo)
                            log("PRINTER: Saved printer info for ${deviceDetails.name} (${deviceDetails.address}) with method: $connectionMethod")
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
                _snackBarState.value = "Connecting to device..."
                log("CONNECT_VIEWMODEL: Connection initiated for device: $deviceAddress")
            } catch (e: Exception) {
                log("CONNECT_VIEWMODEL: Error connecting to device: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                _snackBarState.value = "Failed to connect: ${e.message}"
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
    
    fun pairDevice(deviceAddress: String) {
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
                        connectToDevice(deviceAddress)
                        return@launch
                    }
                    
                    // Attempt to create bond
                    val result = bleManager.createBond(deviceAddress)
                    if (result) {
                        log("PAIR: Bond creation initiated for $deviceAddress")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _snackBarState.value = "Pairing initiated for ${deviceDetails.name ?: deviceAddress}."
                        
                        // Start monitoring for successful pairing, then auto-connect
                        startPairingMonitor(deviceAddress)
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
    private fun startPairingMonitor(deviceAddress: String) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 30 // Monitor for 30 seconds (1 second intervals)
            
            while (attempts < maxAttempts) {
                delay(1000) // Check every second
                attempts++
                
                try {
                    val deviceDetails = bleManager.getDiscoveredClassicDevice(deviceAddress) 
                        ?: bleManager.getDiscoveredLeDevice(deviceAddress)
                        ?: bleManager.getBondedDevice(deviceAddress)
                    if (deviceDetails != null && deviceDetails.bondState == BluetoothDevice.BOND_BONDED) {
                        log("PAIR: Pairing completed for $deviceAddress, starting connection")
                        _snackBarState.value = "Pairing completed! Connecting to ${deviceDetails.name ?: deviceAddress}..."
                        
                        // Register callback to save printer info when connection succeeds
                        if (isPrinterDevice(deviceDetails)) {
                            bleManager.registerConnectionSuccessCallback(deviceAddress) { connectionMethod ->
                                viewModelScope.launch {
                                    val printerInfo = PrinterInfo(
                                        address = deviceDetails.address,
                                        name = deviceDetails.name ?: "Unknown Printer",
                                        connectionMethod = connectionMethod,
                                        connectionTime = System.currentTimeMillis()
                                    )
                                    dataStoreManager.savePrinter(printerInfo)
                                    log("PRINTER: Saved printer info for ${deviceDetails.name} (${deviceDetails.address}) during pairing with method: $connectionMethod")
                                }
                            }
                        }
                        
                        connectToDevice(deviceAddress)
                        return@launch
                    }
                } catch (e: Exception) {
                    log("PAIR: Error monitoring pairing for $deviceAddress: ${e.message}")
                }
            }
            
            log("PAIR: Pairing monitor timeout for $deviceAddress")
            _snackBarState.value = "Pairing timeout. Please try connecting manually."
        }
    }
    
    fun refreshConnectedDevices() {
        viewModelScope.launch {
            try {
                bleManager.refreshConnectedDevices()
                log("Refreshed connected devices list")
            } catch (e: Exception) {
                log("Error refreshing connected devices: ${e.message}")
            }
        }
    }

    fun restartScanning() {
        viewModelScope.launch {
            try {
//                _Internal_bluetoothManager.restartScanning()
                log("Restarted scanning")
            } catch (e: Exception) {
                log("Error restarting scanning: ${e.message}")
            }
        }
    }

    fun startContinuousScanning() {
        viewModelScope.launch {
            try {
                bleManager.startContinuousScanning()
                log("Started continuous scanning")
            } catch (e: Exception) {
                log("Error starting continuous scanning: ${e.message}")
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

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
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
