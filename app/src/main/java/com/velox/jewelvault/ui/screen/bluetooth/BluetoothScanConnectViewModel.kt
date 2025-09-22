package com.velox.jewelvault.ui.screen.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.printing.BluetoothManager
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothScanConnectViewModel @Inject constructor(
    private val _bluetoothManager: BluetoothManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BluetoothScanConnectUiState())
    val uiState: StateFlow<BluetoothScanConnectUiState> = _uiState.asStateFlow()
    val bluetoothManager = _bluetoothManager

    
    init {
        // Start scanning automatically when ViewModel is created
        startScanning()
    }
    
    fun startScanning() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isScanning = true, isLoading = false)
                
                // Start both Classic and BLE scanning
                _bluetoothManager.startDiscovery()
                _bluetoothManager.startBleScan()
                
                log("Started device scanning")
            } catch (e: Exception) {
                log("Error starting scan: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    isLoading = false,
                    message = "Failed to start scanning: ${e.message}"
                )
            }
        }
    }
    
    fun stopScanning() {
        viewModelScope.launch {
            try {
                _bluetoothManager.stopDiscovery()
                _bluetoothManager.stopBleScan()
                
                _uiState.value = _uiState.value.copy(isScanning = false)
                log("Stopped device scanning")
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
                
                // Stop and restart scanning to refresh
                _bluetoothManager.stopDiscovery()
                _bluetoothManager.stopBleScan()
                
                // Small delay before restarting
                kotlinx.coroutines.delay(500)
                
                _bluetoothManager.startDiscovery()
                _bluetoothManager.startBleScan()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isScanning = true,
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
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val connection = _bluetoothManager.connectToDevice(deviceAddress)
                if (connection != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Connected to device successfully"
                    )
                    log("Connected to device: $deviceAddress")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Failed to connect to device"
                    )
                }
            } catch (e: Exception) {
                log("Error connecting to device: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to connect: ${e.message}"
                )
            }
        }
    }
    
    fun disconnectDevice(deviceAddress: String) {
        viewModelScope.launch {
            try {
                _bluetoothManager.disconnectDevice(deviceAddress)
                _uiState.value = _uiState.value.copy(
                    message = "Disconnected from device"
                )
                log("Disconnected from device: $deviceAddress")
            } catch (e: Exception) {
                log("Error disconnecting from device: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    message = "Failed to disconnect: ${e.message}"
                )
            }
        }
    }
    
    fun pairDevice(deviceAddress: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Note: Actual pairing would require additional implementation
                // For now, we'll just show a message
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Pairing functionality requires additional implementation"
                )
                
                log("Pairing device: $deviceAddress")
            } catch (e: Exception) {
                log("Error pairing device: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to pair device: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        // Stop scanning when ViewModel is cleared
        _bluetoothManager.stopDiscovery()
        _bluetoothManager.stopBleScan()
    }
}

data class BluetoothScanConnectUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val message: String? = null
)
