package com.velox.jewelvault.ui.screen.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.printing.BluetoothManager
import com.velox.jewelvault.data.printing.PrintResult
import com.velox.jewelvault.data.printing.PrinterConfig
import com.velox.jewelvault.data.printing.PrinterRepository
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothManagePrintersViewModel @Inject constructor(
    private val bluetoothManager: BluetoothManager,
    private val printerRepository: PrinterRepository
) : ViewModel() {
  /*
    private val _uiState = MutableStateFlow(BluetoothManagePrintersUiState())
    val uiState: StateFlow<BluetoothManagePrintersUiState> = _uiState.asStateFlow()
    
    val isBluetoothEnabled = bluetoothManager.isBluetoothEnabled
    val bondedDevices = bluetoothManager.bondedDevices
    
    init {
        loadConnectedPrinters()
    }
    
    fun loadConnectedPrinters() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Get connected printers from bonded devices
                val printers = bluetoothManager.bondedDevices.value.filter { it.isPrinter && it.isConnected }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectedPrinters = printers
                )
                
                log("Loaded ${printers.size} connected printers")
            } catch (e: Exception) {
                log("Error loading connected printers: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to load printers: ${e.message}"
                )
            }
        }
    }
    
    fun updatePrinterConfig(deviceAddress: String, config: PrinterConfig) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Save printer configuration
                // Note: This would require additional implementation to persist config
                // For now, we'll just show a success message
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Printer configuration updated successfully"
                )
                
                log("Updated printer config for $deviceAddress: $config")
            } catch (e: Exception) {
                log("Error updating printer config: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to update configuration: ${e.message}"
                )
            }
        }
    }
    
    fun testPrint(deviceAddress: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = printerRepository.testPrint(deviceAddress)
                when (result) {
                    is PrintResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Test print sent successfully"
                        )
                    }
                    is PrintResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Test print failed: ${result.message}"
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Test print failed: ${result::class.simpleName}"
                        )
                    }
                }
            } catch (e: Exception) {
                log("Error sending test print: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to send test print: ${e.message}"
                )
            }
        }
    }
    
    fun disconnectPrinter(deviceAddress: String) {
        viewModelScope.launch {
            try {
                bluetoothManager.disconnectDevice(deviceAddress)
                _uiState.value = _uiState.value.copy(
                    message = "Disconnected from printer"
                )
                log("Disconnected from printer: $deviceAddress")
            } catch (e: Exception) {
                log("Error disconnecting from printer: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    message = "Failed to disconnect: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }*/
}

data class BluetoothManagePrintersUiState(
    val isLoading: Boolean = false,
    val connectedPrinters: List<com.velox.jewelvault.data.printing.BluetoothDeviceInfo> = emptyList(),
    val message: String? = null
)
