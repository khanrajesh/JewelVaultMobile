# Bluetooth Printing Feature

This document describes the complete Bluetooth printing feature implementation for the JewelVault Mobile app.

## Overview

The Bluetooth printing feature provides comprehensive support for:
- Bluetooth Classic (RFCOMM) and BLE connections
- Real-time device discovery and connection management
- Multiple printer language support (ESC/POS, ZPL, TSPL, CPCL)
- HTML to printer format conversion
- Item label printing with QR codes
- Receipt printing
- Foreground service for connection persistence

## Architecture

### Core Components

1. **PrintModels.kt** - Data models for printing operations
2. **BluetoothManager.kt** - Bluetooth device discovery and connection management
3. **PrinterRepository.kt** - Core printing logic with `sendToPairedPrinter` function
4. **BluetoothPrintingService.kt** - Foreground service for connection persistence
5. **PrintingUtils.kt** - Utility functions for common printing tasks
6. **BluetoothPrintingIntegration.kt** - Integration helper for existing app

### UI Components

1. **BluetoothStartScreen.kt** - Main entry point for Bluetooth printing
2. **BluetoothScanConnectScreen.kt** - Device discovery and connection
3. **BluetoothManagePrintersScreen.kt** - Printer configuration and management

## Key Features

### 1. sendToPairedPrinter Function

The core function that handles all printing operations:

```kotlin
suspend fun sendToPairedPrinter(payload: PrintPayload): PrintResult
```

**Supported Payload Types:**
- `TextPayload` - Plain text printing
- `HtmlPayload` - HTML document printing
- `BitmapPayload` - Image printing
- `RawPayload` - Raw byte data
- `ItemLabelPayload` - Item label with QR code

**Return Types:**
- `PrintResult.Success` - Print successful
- `PrintResult.NoPairedPrinter` - No paired printer found
- `PrintResult.PrinterNotConnected` - Printer not connected
- `PrintResult.BluetoothDisabled` - Bluetooth disabled
- `PrintResult.Error` - Print error with message

### 2. Real-time Device Management

- Live device discovery (Classic and BLE)
- Real-time connection status updates
- RSSI monitoring for BLE devices
- Automatic printer detection using heuristics

### 3. Multiple Printer Language Support

- **ESC/POS** - Most thermal printers
- **ZPL** - Zebra printers
- **TSPL** - TSC printers
- **CPCL** - Citizen printers

### 4. Label Format Support

- Thermal 100mm x 13mm (default)
- Thermal 80mm x 80mm
- Thermal 58mm x 80mm
- Thermal 50mm x 80mm
- A4 and Letter paper

## Usage Examples

### Basic Text Printing

```kotlin
// Inject the integration helper
@Inject lateinit var printingIntegration: BluetoothPrintingIntegration

// Print simple text
val success = printingIntegration.printText("Hello World!")

// Print asynchronously
printingIntegration.printTextAsync("Hello World!") {
    // onSuccess
    println("Print successful")
} { error ->
    // onError
    println("Print failed: $error")
}
```

### Item Label Printing

```kotlin
// Print item label with QR code
val success = printingIntegration.printItemDetails(item)

// Print with custom settings
val result = printingUtils.printItemLabel(
    item = item,
    labelFormat = LabelFormat.THERMAL_100MM,
    includeQR = true,
    includeLogo = true
)
```

### Receipt Printing

```kotlin
// Print receipt for multiple items
val success = printingIntegration.printReceipt(
    items = itemList,
    storeName = "JewelVault Store",
    totalAmount = 15000.0,
    customerName = "John Doe"
)
```

### HTML Printing

```kotlin
// Print HTML document
val html = """
<!DOCTYPE html>
<html>
<head><title>Invoice</title></head>
<body>
    <h1>Invoice #12345</h1>
    <p>Total: ₹15,000</p>
</body>
</html>
"""

val result = printingUtils.printHtml(html, width = 384, height = 600)
```

### Using sendToPairedPrinter Directly

```kotlin
// Inject the repository
@Inject lateinit var printerRepository: PrinterRepository

// Print text
val textPayload = PrintPayload.TextPayload("Test Print")
val result = printerRepository.sendToPairedPrinter(textPayload)

when (result) {
    is PrintResult.Success -> println("Print successful")
    is PrintResult.NoPairedPrinter -> println("No printer found")
    is PrintResult.Error -> println("Error: ${result.message}")
    // ... handle other cases
}
```

## Integration with Existing App

### 1. Navigation Integration

The Bluetooth printing screens are integrated into the existing navigation:

```kotlin
// Navigate to Bluetooth printing
navController.navigate(Screens.BluetoothStart.route)

// Navigate to scan & connect
navController.navigate(Screens.BluetoothScanConnect.route)

// Navigate to manage printers
navController.navigate(Screens.BluetoothManagePrinters.route)
```

### 2. Permission Handling

The feature uses the existing permission system:

```kotlin
PermissionRequester(
    permissions = listOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION
    ),
    onAllPermissionsGranted = {
        // Start Bluetooth operations
    }
)
```

### 3. Dependency Injection

All components are properly integrated with Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager
    
    @Provides
    @Singleton
    fun providePrinterRepository(
        @ApplicationContext context: Context,
        bluetoothManager: BluetoothManager
    ): PrinterRepository
}
```

## UI Screens

### 1. Bluetooth Start Screen

- Shows Bluetooth status
- Lists paired printers
- Quick actions for scanning and management
- Test print functionality

### 2. Scan & Connect Screen

- Real-time device discovery
- Device type indicators (Classic/BLE)
- Connection status
- Pairing functionality

### 3. Printer Management Screen

- Configure printer settings
- Test print functionality
- Connection management
- Label format selection

## Foreground Service

The `BluetoothPrintingService` maintains printer connections when the app is backgrounded:

```kotlin
// Start service
val intent = Intent(context, BluetoothPrintingService::class.java).apply {
    action = BluetoothPrintingService.ACTION_START_SERVICE
}
context.startForegroundService(intent)

// Stop service
val intent = Intent(context, BluetoothPrintingService::class.java).apply {
    action = BluetoothPrintingService.ACTION_STOP_SERVICE
}
context.startService(intent)
```

## Testing

### Unit Tests

Comprehensive unit tests are provided for the `sendToPairedPrinter` function:

```kotlin
@Test
fun `sendToPairedPrinter should return Success when printer is connected`() = runTest {
    // Test implementation
}
```

### Compose Previews

All UI screens include Compose previews for development:

```kotlin
@Preview(showBackground = true)
@Composable
fun BluetoothStartScreenPreview() {
    JewelVaultTheme {
        BluetoothStartScreen()
    }
}
```

## Configuration

### Printer Configuration

```kotlin
val config = PrinterConfig(
    deviceAddress = "00:11:22:33:44:55",
    deviceName = "Thermal Printer",
    labelFormat = LabelFormat.THERMAL_100MM,
    printerLanguage = PrinterLanguage.ESC_POS,
    dpi = 203,
    autoConnect = true,
    connectionTimeout = 10000L,
    retryAttempts = 3
)
```

### Label Format Customization

```kotlin
enum class LabelFormat(
    val width: Int,
    val height: Int,
    val dpi: Int = 203,
    val description: String
) {
    THERMAL_100MM(384, 512, 203, "Thermal 100mm x 13mm"),
    THERMAL_80MM(384, 384, 203, "Thermal 80mm x 80mm"),
    // ... more formats
}
```

## Future Enhancements

### 1. Label Format Editor

The architecture supports adding a label format editor screen:

```kotlin
// Future implementation
composable(SubScreens.LabelFormatEditor.route) {
    LabelFormatEditorScreen()
}
```

### 2. Multiple Label Templates

Support for multiple label templates based on `ItemEntity`:

```kotlin
data class LabelTemplate(
    val id: String,
    val name: String,
    val format: LabelFormat,
    val template: String, // HTML template
    val itemTypes: List<String> // Applicable item types
)
```

### 3. Export/Import Templates

```kotlin
suspend fun exportLabelTemplate(template: LabelTemplate): ByteArray
suspend fun importLabelTemplate(data: ByteArray): LabelTemplate
```

## Troubleshooting

### Common Issues

1. **No Paired Printer Found**
   - Ensure Bluetooth is enabled
   - Check if printer is paired in system settings
   - Use Scan & Connect screen to discover devices

2. **Printer Not Connected**
   - Check printer power and Bluetooth status
   - Try reconnecting from Manage Printers screen
   - Verify printer is within range

3. **Print Quality Issues**
   - Adjust DPI settings in printer configuration
   - Check label format compatibility
   - Verify printer language settings

4. **Permission Issues**
   - Ensure all Bluetooth permissions are granted
   - Check location permissions for BLE scanning
   - Restart app after granting permissions

### Debug Logging

Enable debug logging to troubleshoot issues:

```kotlin
// Logs are automatically generated for:
// - Bluetooth state changes
// - Device discovery
// - Connection attempts
// - Print operations
// - Error conditions
```

## Migration Notes

### From Existing PrintUtils

Replace existing print calls:

```kotlin
// Old way
PrintUtils.printItemDirectly(context, item) { }

// New way
printingIntegration.printItemDetailsAsync(item) {
    // onSuccess
} { error ->
    // onError
}
```

### BroadcastReceiver Deprecation

The new implementation uses StateFlow/SharedFlow instead of BroadcastReceiver for real-time updates:

```kotlin
// Old way (deprecated)
val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle updates
    }
}

// New way
val devices by bluetoothManager.discoveredDevices.collectAsState()
val isConnected by bluetoothManager.isBluetoothEnabled.collectAsState()
```

## Performance Considerations

1. **Connection Pooling** - Connections are reused when possible
2. **Chunked Data Transfer** - Large data is sent in chunks for BLE
3. **Background Processing** - All printing operations run on IO dispatcher
4. **Memory Management** - Bitmaps are properly disposed after conversion
5. **Service Lifecycle** - Foreground service is stopped when no printers connected

## Security Considerations

1. **Permission Validation** - All operations check for required permissions
2. **Data Validation** - Input data is validated before processing
3. **Error Handling** - Sensitive information is not logged
4. **Connection Security** - Bluetooth connections use standard security protocols

This implementation provides a complete, production-ready Bluetooth printing solution that integrates seamlessly with the existing JewelVault Mobile app architecture.
