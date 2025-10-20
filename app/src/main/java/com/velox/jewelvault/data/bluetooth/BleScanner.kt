package com.velox.jewelvault.data.bluetooth

import android.Manifest
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.RequiresPermission
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.permissions.hasFineLocation
import com.velox.jewelvault.utils.permissions.hasScanPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.core.util.size
import com.velox.jewelvault.data.bluetooth.BleUtils.logDevice

/**
 * Handles Bluetooth scanning logic (classic + BLE) so the receiver can delegate scanning concerns
 * and keep connection responsibilities separate.
 */
class BleScanner(
    private val addLeDevice: (BluetoothDeviceDetails) -> Unit,
    private val classicDiscoveringState: MutableStateFlow<Boolean>,
    private val leDiscoveringState: MutableStateFlow<Boolean>,
    private val manager: BleManager,
    private val context: Context,
) {
    private var activeScanCallback: ScanCallback? = null

    private val bleScannerProvider: BluetoothLeScanner? = manager.bluetoothAdapter?.bluetoothLeScanner

    val isDiscovering = MutableStateFlow(false)

    fun onClassicDiscoveryStarted() {
        log("SCAN: Classic discovery started")
        classicDiscoveringState.value = true
        manager.classicDiscoveredDevices.value = emptyList()
        recomputeIsDiscovering()
    }

    fun onClassicDiscoveryFinished() {
        log("SCAN: Classic discovery finished")
        classicDiscoveringState.value = false
        recomputeIsDiscovering()
    }

    fun onLeScanFailed(errorCode: Int) {
        log("SCAN: BLE scan failed $errorCode")
        leDiscoveringState.value = false
        recomputeIsDiscovering()
    }

    fun onStart() {
        recomputeIsDiscovering()
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN])
    fun startBleScan(scanSettings: ScanSettings? = null, scanFilters: List<ScanFilter>? = null) {
        if (!hasScanPermission(context) || !hasFineLocation(context)) {
            log("SCAN: Missing permissions; cannot start BLE scan")
            return
        }

        val scanner = bleScannerProvider ?: run {
            log("SCAN: No BLE scanner available")
            return
        }

        if (activeScanCallback != null) return

        val settings = scanSettings ?: ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L) //if this is not set to 0 the android will call onBatchScanResult not onScanResult
            .build()

        val callback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleBleResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::handleBleResult)
            }

            override fun onScanFailed(errorCode: Int) {
                onLeScanFailed(errorCode)
            }
        }

        activeScanCallback = callback
        try {
            log("SCAN: BLE scan started")
            scanner.startScan(scanFilters, settings, callback)
            leDiscoveringState.value = true
            manager.leDiscoveredDevices.value = emptyList()
        } catch (t: Throwable) {
            log("SCAN: Error starting BLE scan ${t.message}")
            activeScanCallback = null
        }
        recomputeIsDiscovering()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopBleScan() {
        val scanner = bleScannerProvider
        val callback = activeScanCallback ?: return
        try {
            scanner?.stopScan(callback)
            log("SCAN: BLE scan stopped")
        } catch (t: Throwable) {
            log("SCAN: Error stopping BLE scan ${t.message}")
        }
        activeScanCallback = null
        leDiscoveringState.value = false
        recomputeIsDiscovering()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startClassicDiscovery() {
        try {
            if (manager.bluetoothAdapter.isDiscovering) {
                classicDiscoveringState.value = true
                log("SCAN: Classic discovery already running")
                return
            }
            val ok = manager.bluetoothAdapter.startDiscovery()
            log("SCAN: Classic discovery start result -> $ok")
            if (ok || manager.bluetoothAdapter.isDiscovering) {
                classicDiscoveringState.value = true
                manager.classicDiscoveredDevices.value = emptyList()
            }
        } catch (t: Throwable) {
            log("SCAN: Error starting classic discovery ${t.message}")
        }
        recomputeIsDiscovering()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopClassicDiscovery() {
        try {
            manager.bluetoothAdapter.cancelDiscovery()
            log("SCAN: Classic discovery cancelled")
        } catch (t: Throwable) {
            log("SCAN: Error cancelling classic discovery ${t.message}")
        }
        classicDiscoveringState.value = false
        recomputeIsDiscovering()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startUnifiedScan(scanSettings: ScanSettings? = null, scanFilters: List<ScanFilter>? = null) {
        startBleScan(scanSettings, scanFilters)
        startClassicDiscovery()
        manager.updateBondedDevices()
        manager.updateConnectedDevices()
        manager.bleManagerScope.launch {
            delay(60_000)
            log("SCAN: Auto-stop after 60s")
            stopUnifiedScan()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopUnifiedScan() {
        stopBleScan()
        stopClassicDiscovery()
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleBleResult(result: ScanResult) {
        try {
            val device = result.device
            val record = result.scanRecord

            val manuMap = mutableMapOf<Int, ByteArray>()
            record?.manufacturerSpecificData?.let { msd ->
                for (i in 0 until msd.size) {
                    manuMap[msd.keyAt(i)] = msd.valueAt(i)
                }
            }

            val svcMap = mutableMapOf<UUID, ByteArray>()
            record?.serviceData?.forEach { (uuid, bytes) ->
                svcMap[uuid.uuid] = bytes
            }

            logDevice("record: $record, manuMap: $manuMap, svcMap: $svcMap, result: $result",device)
            val details = manager.buildBluetoothDevice(
                device,
                device?.address,
                "BLE_SCAN_RESULT",
                device?.name ?: record?.deviceName,
                mapOf("timestampNanos" to result.timestampNanos.toString()),
                record?.serviceUuids?.joinToString(",") { it.uuid.toString() }
            ).copy(
                rssi = result.rssi,
                manufacturerData = if (manuMap.isEmpty()) null else manuMap,
                serviceData = if (svcMap.isEmpty()) null else svcMap,
                txPower = if (record?.txPowerLevel != Int.MIN_VALUE) record?.txPowerLevel else null
            )

            addLeDevice(details)
        } catch (t: Throwable) {
            log("SCAN: Error processing BLE result ${t.message}")
        }
    }

    private fun recomputeIsDiscovering() {
        isDiscovering.value = classicDiscoveringState.value || leDiscoveringState.value
    }
}
