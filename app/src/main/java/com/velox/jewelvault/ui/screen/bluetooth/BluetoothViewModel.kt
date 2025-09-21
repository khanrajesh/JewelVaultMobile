package com.velox.jewelvault.ui.screen.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.bluetooth.BluetoothService
import com.velox.jewelvault.data.bluetooth.ConnectionResult
import com.velox.jewelvault.data.bluetooth.ConnectionState
import com.velox.jewelvault.data.bluetooth.DeviceUiModel
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Unified ViewModel for all Bluetooth operations
 * Handles device scanning, connection, and real-time monitoring
 */
@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothService: BluetoothService
) : ViewModel() {

    // Real-time device monitoring
    val devices: StateFlow<List<DeviceUiModel>> = bluetoothService.devices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val isDiscovering: StateFlow<Boolean> = bluetoothService.isDiscovering.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    val connectionStatus: StateFlow<ConnectionState> = bluetoothService.connectionStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConnectionState.DISCONNECTED
    )

    val connectedDevice = bluetoothService.connectedDevice

    // Bluetooth adapter state
    val bluetoothAdapterState = bluetoothService.bluetoothAdapterState
    val bluetoothStateChanged = bluetoothService.bluetoothStateChanged


    // UI State
    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    // Selected device
    private val _selectedDevice = MutableStateFlow<DeviceUiModel?>(null)
    val selectedDevice: StateFlow<DeviceUiModel?> = _selectedDevice.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        observeBluetoothService()
        startMonitoring()
    }

    private fun observeBluetoothService() {
        viewModelScope.launch {
            // Observe connection results
            bluetoothService.connectionResult.collect { result ->
                handleConnectionResult(result)
            }
        }

        viewModelScope.launch {
            // Observe device discovery events
            bluetoothService.deviceFound.collect { deviceFound ->
                log("Device found: ${deviceFound.device.name ?: "Unknown"} (${deviceFound.device.address})")
            }
        }

        viewModelScope.launch {
            // Observe Bluetooth state changes
            bluetoothService.bluetoothStateChanged.collect { stateChange ->
                log("Bluetooth state changed in ViewModel: ${stateChange.previousState} -> ${stateChange.currentState}")
                // You can add UI-specific logic here, like showing snackbars or updating UI state
                when (stateChange.currentState) {
                    android.bluetooth.BluetoothAdapter.STATE_ON -> {
                        log("Bluetooth turned ON - UI can react")
                    }

                    android.bluetooth.BluetoothAdapter.STATE_OFF -> {
                        log("Bluetooth turned OFF - UI can react")
                    }
                }
            }
        }
        
        viewModelScope.launch {
            // Observe discovery state changes
            bluetoothService.isDiscovering.collect { discovering ->
                log("BluetoothViewModel: Discovery state changed to: $discovering")
            }
        }
    }

    private fun handleConnectionResult(result: ConnectionResult) {
        if (result.success) {
            _errorMessage.value = null
            log("Successfully connected to device: ${result.deviceAddress}")
        } else {
            _errorMessage.value = result.error ?: "Connection failed"
            log("Connection failed: ${result.error}")
        }
    }

    // Monitoring functions
    fun startMonitoring() {
        log("BluetoothViewModel starting monitoring...")
        bluetoothService.start()
        log("BluetoothViewModel monitoring started")
    }

    fun stopMonitoring() = bluetoothService.stop()

    // Discovery functions
    fun startDiscovery() {
        viewModelScope.launch {
            try {
                val result = bluetoothService.startDiscovery()
                if (result.isFailure) {
                    _errorMessage.value =
                        result.exceptionOrNull()?.message ?: "Failed to start discovery"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error starting discovery: ${e.message}"
            }
        }
    }

    fun stopDiscovery() {
        viewModelScope.launch {
            try {
                val result = bluetoothService.stopDiscovery()
                if (result.isFailure) {
                    _errorMessage.value =
                        result.exceptionOrNull()?.message ?: "Failed to stop discovery"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error stopping discovery: ${e.message}"
            }
        }
    }

    // Connection functions
    fun connect(deviceAddress: String) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val result = bluetoothService.connect(deviceAddress)
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Connection failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error connecting to device: ${e.message}"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                val result = bluetoothService.disconnect()
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Disconnect failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error disconnecting: ${e.message}"
            }
        }
    }

    // Device management
    fun selectDevice(device: DeviceUiModel) {
        _selectedDevice.value = device
    }

    fun probeDevice(device: DeviceUiModel) {
        viewModelScope.launch {
            try {
                val bluetoothDevice =
                    bluetoothService.bluetoothAdapter?.getRemoteDevice(device.address)
                if (bluetoothDevice != null) {
                    val probeResult = bluetoothService.probeDevice(bluetoothDevice)
                    log("Probe result for ${device.name}: success=${probeResult.success}, language=${probeResult.language}")
                }
            } catch (e: Exception) {
                log("Error probing device: ${e.message}")
                _errorMessage.value = "Error probing device: ${e.message}"
            }
        }
    }

    // Utility functions
    fun isBluetoothAvailable() = bluetoothService.isBluetoothAvailable()
    fun hasBluetoothPermissions() = bluetoothService.hasBluetoothPermissions()

    fun clearError() {
        _errorMessage.value = null
    }




    // Data transmission
    fun sendData(data: ByteArray) {
        viewModelScope.launch {
            try {
                val result = bluetoothService.sendData(data)
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to send data"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error sending data: ${e.message}"
            }
        }
    }

    fun sendText(text: String) {
        viewModelScope.launch {
            try {
                val result = bluetoothService.sendText(text)
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to send text"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error sending text: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothService.cleanup()
    }
}

/**
 * UI state for Bluetooth operations
 */
data class BluetoothUiState(
    val isScanning: Boolean = false,
    val hasBluetoothPermission: Boolean = false,
    val isBluetoothEnabled: Boolean = false
)
