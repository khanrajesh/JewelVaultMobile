package com.velox.jewelvault.ui.screen.bluetooth_new

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.bluetooth.InternalBluetoothManager
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails
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
    private val _Internal_bluetoothManager: InternalBluetoothManager,
    @Named("snackMessage") private val _snackBarState: MutableState<String>
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BluetoothScanConnectUiState())
    val uiState: StateFlow<BluetoothScanConnectUiState> = _uiState.asStateFlow()
    val bluetoothReceiver = _Internal_bluetoothManager

    // Device lists from InternalBluetoothManager
    val bondedDevices: StateFlow<List<BluetoothDeviceDetails>> = _Internal_bluetoothManager.bondedDevices
    val classicDiscoveredDevices: StateFlow<List<BluetoothDeviceDetails>> = _Internal_bluetoothManager.classicDiscoveredDevices
    val leDiscoveredDevices: StateFlow<List<BluetoothDeviceDetails>> = _Internal_bluetoothManager.leDiscoveredDevices
    val connectedDevices: StateFlow<List<BluetoothDeviceDetails>> = _Internal_bluetoothManager.connectedDevices
    val connectingDevices: StateFlow<List<BluetoothDeviceDetails>> = _Internal_bluetoothManager.connectingDevices
    
    // Combined discovered devices (classic + LE)
    val allDiscoveredDevices: StateFlow<List<BluetoothDeviceDetails>> = combine(
        _Internal_bluetoothManager.classicDiscoveredDevices,
        _Internal_bluetoothManager.leDiscoveredDevices
    ) { classic, le ->
        val merged = (classic + le).distinctBy { it.address }
        val (named, unnamed) = merged.partition { !it.name.isNullOrBlank() && it.name != "Unknown Device" }
        named.sortedBy { it.name?.lowercase() } + unnamed.sortedBy { it.name ?: "~" }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )
    
    // Separate bonded devices into connected and not connected
    val unconnectedPairedDevices: StateFlow<List<BluetoothDeviceDetails>> = combine(
        _Internal_bluetoothManager.bondedDevices,
        _Internal_bluetoothManager.connectedDevices,
        _Internal_bluetoothManager.connectingDevices
    ) { bonded, connected, connecting ->
        val connectedAddresses = connected.map { it.address }.toSet()
        val connectingAddresses = connecting.map { it.address }.toSet()
        bonded.filter { it.address !in connectedAddresses && it.address !in connectingAddresses }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )
    
    init {
        // Start scanning automatically when ViewModel is created
        startScanning()
    }
    
    fun startScanning() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = false)
                
                // Start unified scanning (both Classic and BLE)
                _Internal_bluetoothManager.startUnifiedScanning()
                
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
                _Internal_bluetoothManager.stopUnifiedScanning()
                
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
                _Internal_bluetoothManager.stopUnifiedScanning()
                
                // Small delay before restarting
                kotlinx.coroutines.delay(500)
                
                _Internal_bluetoothManager.startUnifiedScanning()
                
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

                _Internal_bluetoothManager.connect(deviceAddress)

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
                _Internal_bluetoothManager.disconnectDevice(deviceAddress)
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
                val device = _Internal_bluetoothManager.getDiscoveredDevice(deviceAddress)
                if (device != null) {
                    log("PAIR: Found device ${device.name} (${device.address})")
                    // Immediately reflect connecting placeholder in UI
                    _Internal_bluetoothManager.addConnectingPlaceholder(
                        address = device.address,
                        name = device.name,
                        extraInfo = mapOf("stage" to "pairing")
                    )
                    
                    // Check if already bonded
                    if (device.bondState == BluetoothDevice.BOND_BONDED) {
                        log("PAIR: Device $deviceAddress is already bonded, connecting directly")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        connectToDevice(deviceAddress)
                        return@launch
                    }
                    
                    // Attempt to create bond
                    val result = _Internal_bluetoothManager.createBond(deviceAddress)
                    if (result) {
                        log("PAIR: Bond creation initiated for $deviceAddress")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _snackBarState.value = "Pairing initiated for ${device.name ?: deviceAddress}."
                        
                        // Start monitoring for successful pairing, then auto-connect
                        startPairingMonitor(deviceAddress)
                    } else {
                        log("PAIR: Failed to initiate bond creation for $deviceAddress")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _snackBarState.value = "Failed to initiate pairing for ${device.name ?: deviceAddress}"
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

    // Convenience wrapper for UI: discover card should always pair then connect
    fun pairAndConnectDevice(deviceAddress: String) {
        pairDevice(deviceAddress)
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
                    val device = _Internal_bluetoothManager.getDiscoveredDevice(deviceAddress)
                    if (device != null && device.bondState == BluetoothDevice.BOND_BONDED) {
                        log("PAIR: Pairing completed for $deviceAddress, starting connection")
                        _snackBarState.value = "Pairing completed! Connecting to ${device.name ?: deviceAddress}..."
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
                _Internal_bluetoothManager.refreshConnectedDevices()
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
                _Internal_bluetoothManager.startContinuousScanning()
                log("Started continuous scanning")
            } catch (e: Exception) {
                log("Error starting continuous scanning: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        // Stop scanning when ViewModel is cleared
        _Internal_bluetoothManager.stopUnifiedScanning()
    }
}

data class BluetoothScanConnectUiState(
    val isLoading: Boolean = false,
    val message: String? = null
)
