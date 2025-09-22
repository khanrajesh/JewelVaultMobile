package com.velox.jewelvault.data.printing

import android.content.Context
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.sql.Timestamp
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrinterRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockBluetoothManager: BluetoothManager
    private lateinit var printerRepository: PrinterRepository

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockBluetoothManager = mockk(relaxed = true)
        printerRepository = PrinterRepository(mockContext, mockBluetoothManager)
    }

    @Test
    fun `sendToPairedPrinter should return BluetoothDisabled when Bluetooth is not available`() = runTest {
        // Given
        every { mockBluetoothManager.isBluetoothAvailable() } returns false
        val payload = PrintPayload.TextPayload("Test text")

        // When
        val result = printerRepository.sendToPairedPrinter(payload)

        // Then
        assertTrue(result is PrintResult.BluetoothDisabled)
    }

    @Test
    fun `sendToPairedPrinter should return NoPairedPrinter when no printers are available`() = runTest {
        // Given
        every { mockBluetoothManager.isBluetoothAvailable() } returns true
        every { mockBluetoothManager.getBondedDevices() } returns emptyList()
        val payload = PrintPayload.TextPayload("Test text")

        // When
        val result = printerRepository.sendToPairedPrinter(payload)

        // Then
        assertTrue(result is PrintResult.NoPairedPrinter)
    }

    @Test
    fun `sendToPairedPrinter should return Success when printer is already connected`() = runTest {
        // Given
        val printerDevice = BluetoothDeviceInfo(
            address = "00:11:22:33:44:55",
            name = "Test Printer",
            isPaired = true,
            isConnected = true,
            deviceType = BluetoothDeviceType.CLASSIC,
            isPrinter = true
        )
        
        every { mockBluetoothManager.isBluetoothAvailable() } returns true
        every { mockBluetoothManager.getBondedDevices() } returns listOf(printerDevice)
        every { mockBluetoothManager.sendData(any(), any()) } returns true
        
        val payload = PrintPayload.TextPayload("Test text")

        // When
        val result = printerRepository.sendToPairedPrinter(payload)

        // Then
        assertTrue(result is PrintResult.Success)
        verify { mockBluetoothManager.sendData(printerDevice.address, any()) }
    }

    @Test
    fun `sendToPairedPrinter should return Success when connecting to paired printer`() = runTest {
        // Given
        val printerDevice = BluetoothDeviceInfo(
            address = "00:11:22:33:44:55",
            name = "Test Printer",
            isPaired = true,
            isConnected = false,
            deviceType = BluetoothDeviceType.CLASSIC,
            isPrinter = true
        )
        
        val mockConnection = mockk<BluetoothConnection>(relaxed = true)
        
        every { mockBluetoothManager.isBluetoothAvailable() } returns true
        every { mockBluetoothManager.getBondedDevices() } returns listOf(printerDevice)
        every { mockBluetoothManager.connectToDevice(any()) } returns mockConnection
        every { mockBluetoothManager.sendData(any(), any()) } returns true
        
        val payload = PrintPayload.TextPayload("Test text")

        // When
        val result = printerRepository.sendToPairedPrinter(payload)

        // Then
        assertTrue(result is PrintResult.Success)
        verify { mockBluetoothManager.connectToDevice(printerDevice.address) }
        verify { mockBluetoothManager.sendData(printerDevice.address, any()) }
    }

    @Test
    fun `sendToPairedPrinter should return PrinterNotConnected when connection fails`() = runTest {
        // Given
        val printerDevice = BluetoothDeviceInfo(
            address = "00:11:22:33:44:55",
            name = "Test Printer",
            isPaired = true,
            isConnected = false,
            deviceType = BluetoothDeviceType.CLASSIC,
            isPrinter = true
        )
        
        every { mockBluetoothManager.isBluetoothAvailable() } returns true
        every { mockBluetoothManager.getBondedDevices() } returns listOf(printerDevice)
        every { mockBluetoothManager.connectToDevice(any()) } returns null
        
        val payload = PrintPayload.TextPayload("Test text")

        // When
        val result = printerRepository.sendToPairedPrinter(payload)

        // Then
        assertTrue(result is PrintResult.PrinterNotConnected)
    }

    @Test
    fun `sendToPairedPrinter should return Error when sendData fails`() = runTest {
        // Given
        val printerDevice = BluetoothDeviceInfo(
            address = "00:11:22:33:44:55",
            name = "Test Printer",
            isPaired = true,
            isConnected = true,
            deviceType = BluetoothDeviceType.CLASSIC,
            isPrinter = true
        )
        
        every { mockBluetoothManager.isBluetoothAvailable() } returns true
        every { mockBluetoothManager.getBondedDevices() } returns listOf(printerDevice)
        every { mockBluetoothManager.sendData(any(), any()) } returns false
        
        val payload = PrintPayload.TextPayload("Test text")

        // When
        val result = printerRepository.sendToPairedPrinter(payload)

        // Then
        assertTrue(result is PrintResult.Error)
        assertEquals("Failed to send data to printer", (result as PrintResult.Error).message)
    }

    @Test
    fun `sendToPairedPrinter should handle ItemLabelPayload correctly`() = runTest {
        // Given
        val printerDevice = BluetoothDeviceInfo(
            address = "00:11:22:33:44:55",
            name = "Test Printer",
            isPaired = true,
            isConnected = true,
            deviceType = BluetoothDeviceType.CLASSIC,
            isPrinter = true
        )
        
        val testItem = ItemEntity(
            itemId = "TEST001",
            itemAddName = "Test Item",
            catId = "CAT001",
            userId = "USER001",
            storeId = "STORE001",
            catName = "Test Category",
            subCatId = "SUBCAT001",
            subCatName = "Test SubCategory",
            entryType = "Purchase",
            quantity = 1,
            gsWt = 10.5,
            ntWt = 10.0,
            fnWt = 9.5,
            purity = "22K",
            crgType = "Fixed",
            crg = 100.0,
            othCrgDes = "Other charges",
            othCrg = 50.0,
            cgst = 15.0,
            sgst = 15.0,
            igst = 0.0,
            huid = "HUID123",
            addDesKey = "Description",
            addDesValue = "Test description",
            addDate = Timestamp(System.currentTimeMillis()),
            modifiedDate = Timestamp(System.currentTimeMillis()),
            sellerFirmId = "FIRM001",
            purchaseOrderId = "PO001",
            purchaseItemId = "PI001"
        )
        
        every { mockBluetoothManager.isBluetoothAvailable() } returns true
        every { mockBluetoothManager.getBondedDevices() } returns listOf(printerDevice)
        every { mockBluetoothManager.sendData(any(), any()) } returns true
        
        val payload = PrintPayload.ItemLabelPayload(
            item = testItem,
            labelFormat = LabelFormat.THERMAL_100MM,
            includeQR = true,
            includeLogo = false
        )

        // When
        val result = printerRepository.sendToPairedPrinter(payload)

        // Then
        assertTrue(result is PrintResult.Success)
        verify { mockBluetoothManager.sendData(printerDevice.address, any()) }
    }

    @Test
    fun `getAvailablePrinters should return only printer devices`() = runTest {
        // Given
        val devices = listOf(
            BluetoothDeviceInfo(
                address = "00:11:22:33:44:55",
                name = "Test Printer",
                isPaired = true,
                isConnected = true,
                deviceType = BluetoothDeviceType.CLASSIC,
                isPrinter = true
            ),
            BluetoothDeviceInfo(
                address = "00:11:22:33:44:56",
                name = "Test Phone",
                isPaired = true,
                isConnected = true,
                deviceType = BluetoothDeviceType.CLASSIC,
                isPrinter = false
            )
        )
        
        every { mockBluetoothManager.getBondedDevices() } returns devices

        // When
        val result = printerRepository.getAvailablePrinters()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test Printer", result.first().name)
        assertTrue(result.first().isPrinter)
    }

    @Test
    fun `testPrint should return Success when test print succeeds`() = runTest {
        // Given
        val deviceAddress = "00:11:22:33:44:55"
        every { mockBluetoothManager.sendData(any(), any()) } returns true

        // When
        val result = printerRepository.testPrint(deviceAddress)

        // Then
        assertTrue(result is PrintResult.Success)
        verify { mockBluetoothManager.sendData(deviceAddress, any()) }
    }

    @Test
    fun `testPrint should return Error when test print fails`() = runTest {
        // Given
        val deviceAddress = "00:11:22:33:44:55"
        every { mockBluetoothManager.sendData(any(), any()) } returns false

        // When
        val result = printerRepository.testPrint(deviceAddress)

        // Then
        assertTrue(result is PrintResult.Error)
        assertEquals("Failed to send data to printer", (result as PrintResult.Error).message)
    }
}
