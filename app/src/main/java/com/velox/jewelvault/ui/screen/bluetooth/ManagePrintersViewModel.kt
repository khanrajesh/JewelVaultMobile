package com.velox.jewelvault.ui.screen.bluetooth

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.bluetooth.BleManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.printer.PrinterEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelElementEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import com.velox.jewelvault.utils.FileManager
import com.velox.jewelvault.utils.PrintUtils
import com.velox.jewelvault.utils.label.LabelPrintGenerator
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ManagePrintersViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val appDatabase: AppDatabase,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
    @Named("snackMessage") private val _snackBarState: MutableState<String>
) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState

    // Saved printers from Room database
    val savedPrinters: StateFlow<List<PrinterEntity>> =
        appDatabase.printerDao().getAllPrinters().stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    // Default printer
    val defaultPrinter: StateFlow<PrinterEntity?> =
        appDatabase.printerDao().getDefaultPrinter().stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    // Printer connection statuses
    val printerStatuses: StateFlow<Map<String, Boolean>> = combine(
        savedPrinters, bleManager.connectedDevices
    ) { printers, connected ->
        printers.associate { printer ->
            printer.address to connected.any { it.address == printer.address }
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
        initialValue = emptyMap()
    )

    /**
     * Test print with different protocols
     */
    fun testPrint(address: String, protocol: String) {
        viewModelScope.launch {
            try {
                val printerName =
                    savedPrinters.value.find { it.address == address }?.name ?: address
                _snackBarState.value = "Testing $protocol print on $printerName..."

                val testData = generateTestPrintData(protocol)
                val success = bleManager.sendPrintData(address, testData)

                if (success) {
                    _snackBarState.value =
                        "✅ Test print sent successfully to $printerName ($protocol)"
                    log("Test print successful for $address with protocol $protocol")
                } else {
                    _snackBarState.value = "❌ Failed to send test print to $printerName ($protocol)"
                    log("Test print failed for $address with protocol $protocol")
                }
            } catch (e: Exception) {
                log("Error testing print: ${e.message}")
                _snackBarState.value = "❌ Error testing print: ${e.message}"
            }
        }
    }


    /**
     * Print item label to the currently connected printer.
     */
    fun printItemLabel(
        item: ItemEntity,
        context1: Context,
        qrUri: Uri? = null,
        logoUri: Uri? = null
    ) {
        viewModelScope.launch {
            try {
                val connected = bleManager.connectedDevices.value.firstOrNull()
                if (connected == null) {
                    _snackBarState.value = "No connected printer found"
                    return@launch
                }
                val address = connected.address
                val printerName =
                    savedPrinters.value.find { it.address == address }?.name ?: address
                _snackBarState.value = "Printing item label on $printerName..."

//                val tspl = generateTsplItemLabel(item, context1, qrUri, logoUri)
                val tspl = generateTsplItemLabel(item, context1, qrUri, logoUri)
                val success = bleManager.sendPrintData(address, tspl)
                if (success) {
                    _snackBarState.value = "✅ Item label sent to $printerName"
                } else {
                    _snackBarState.value = "❌ Failed to send item label to $printerName"
                }
            } catch (e: Exception) {
                log("Error printing item label: ${e.message}")
                _snackBarState.value = "❌ Error printing label: ${e.message}"
            }
        }
    }

    private fun generateTsplItemLabel(
        item: ItemEntity,
        context1: Context,
        qrUri: Uri? = null,
        logoUri: Uri? = null
    ): ByteArray {
        // Approx. conversion for TSPL coordinates: 203 dpi ≈ 8 dots per mm
        fun mm(v: Int): Int = v * 8

        mm(0)
        mm(0)

        // Text start (use left gap of 45mm as requested earlier)
        val leftGapMm = 45
        mm(leftGapMm)
        mm(2)
        mm(4) // larger line gap for bigger font

        String.format("%.3f", item.fnWt)
        String.format("%.3f", item.gsWt)
        item.addDesKey
        item.addDesValue

        val context = context1.applicationContext
        // Build TSPL content: prefer native QRCODE when QR is provided
        val qrCommand = try {
            log("generateTsplItemLabel: logo uri=$qrUri")

            if (qrUri != null) {
                val inputStream = context.contentResolver.openInputStream(qrUri)
                val src = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (src != null) {
                    log("generateTsplItemLabel: logo decoded: ${src.width}x${src.height}")

                    // Detect QR by name prefix
                    val isQr = try {
                        qrUri.lastPathSegment?.startsWith("qr_") == true
                    } catch (_: Throwable) {
                        false
                    }

                    // If QR, use native TSPL QRCODE (avoids image distortion/line issues)
                    if (isQr) {
                        val qrX = 340
                        val qrY = mm(1)
                        // Use widely-supported TSPL syntax: QRCODE x,y,M,cell,A,rotation,"data"
                        val cell = 4
                        val qrCmd = "QRCODE $qrX,$qrY,M,$cell,A,0,\"${item.itemId}\""
                        log("generateTsplItemLabel: using native QRCODE command: ${qrCmd.length} chars")
                        qrCmd
                    } else {
                        val qrX = 340
                        val qrY = mm(1)
                        val cmd = PrintUtils.buildTsplImageViaDownload(
                            source = src,
                            xDots = qrX,
                            yDots = qrY,
                            name = "GQR",
                            targetSizeMm = 8,
                            threshold = 140
                        )
                        log("generateTsplItemLabel: qr bitmap fallback cmd length=${cmd.length}")
                        cmd
                    }
                } else ""
            } else ""
        } catch (t: Throwable) {
            log("generateTsplItemLabel: logo error=${t.message}")
            ""
        }

        // Build rotated logo (90°) to mitigate vertical banding
        val logoCommand: String = try {
            val localLogoUri =
                logoUri ?: FileManager.getLogoFileUri(context)
            log("generateTsplItemLabel: store logo uri=$localLogoUri")
            if (localLogoUri != null) {
                val inputStream = context.contentResolver.openInputStream(localLogoUri)
                val src = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (src != null) {
                    // Final pass: non-rotated, solid threshold (no dithering) to avoid dotted edges
                    val x = 0
                    val y = mm(1)
                    PrintUtils.buildTsplBitmapCommand(
                        source = src,
                        xDots = x,
                        yDots = y,
                        targetSizeMm = 6,
                        cropToSquare = true,
                        threshold = 120,
                        bitOrderMsbFirst = true,
                        blackIsOne = true,
                        dither = false
                    )
                } else ""
            } else ""
        } catch (t: Throwable) {
            log("generateTsplItemLabel: store logo error=${t.message}")
            ""
        }

        // Assemble final ByteArray with ASCII commands
        val baos = java.io.ByteArrayOutputStream()
        fun w(s: String) = baos.write((s).toByteArray(Charsets.UTF_8))

        w("SIZE 100 mm, 13 mm\r\n")
        w("GAP 3 mm, 0 mm\r\n")
        w("DENSITY 6\r\n")
        w("SPEED 1\r\n")
        w("DIRECTION 1\r\n")
        w("REFERENCE 0,0\r\n")
        w("SET PEEL OFF\r\n")
        w("SET CUTTER OFF\r\n")
        w("SET PARTIAL_CUTTER OFF\r\n")
        w("SET TEAR ON\r\n")
        w("CLS\r\n")

        if (logoCommand.isNotEmpty()) {
            w(logoCommand)
            w("\r\n")
        }

//        if (qrCommand.isNotEmpty()) {
//            w(qrCommand)
//            w("\r\n")
//        }

        w("PRINT 1\r\n")
        return baos.toByteArray()
    }

    /**
     * Handle test result confirmation from user
     */
    fun handleTestResult(address: String, protocol: String, passed: Boolean) {
        viewModelScope.launch {
            try {
                val printerName =
                    savedPrinters.value.find { it.address == address }?.name ?: address

                if (passed) {
                    // Add protocol as supported language for this printer
                    addSupportedLanguage(address, protocol)
                    _snackBarState.value =
                        "✅ $protocol added as supported language for $printerName"
                    log("Added $protocol as supported language for $address")
                } else {
                    _snackBarState.value = "❌ $protocol test failed for $printerName"
                    log("$protocol test failed for $address")
                }
            } catch (e: Exception) {
                log("Error handling test result: ${e.message}")
                _snackBarState.value = "Failed to save test result: ${e.message}"
            }
        }
    }

    /**
     * Generate test print data for different protocols
     */
    private fun generateTestPrintData(protocol: String): ByteArray {
        return when (protocol.uppercase()) {
            "CPCL" -> generateCpclTestData()
            "TSPL" -> generateTsplTestData()
            "ESC1" -> generateEsc1TestData()
            "ESC2" -> generateEsc2TestData()
            "PPLB" -> generatePplbTestData()
            else -> generateGenericTestData()
        }
    }

    private fun generateCpclTestData(): ByteArray {
        val cpclCommands = """
            ! 0 200 200 210 1
            TEXT 4 0 30 40 JewelVault Test Print
            TEXT 4 0 30 80 Protocol: CPCL
            TEXT 4 0 30 120 Date: ${
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())
        }
            TEXT 4 0 30 160 Status: Connected
            TEXT 4 0 30 200 This is a test print
            TEXT 4 0 30 240 from JewelVault Mobile App
            PRINT
        """.trimIndent()
        return cpclCommands.toByteArray(Charsets.UTF_8)
    }

    private fun generateTsplTestData(): ByteArray {
        val tsplCommands = """
            SIZE 100 mm, 50 mm
            GAP 3 mm, 0 mm
            DIRECTION 1
            REFERENCE 0,0
            SET PEEL OFF
            SET CUTTER OFF
            SET PARTIAL_CUTTER OFF
            SET TEAR ON
            CLS
            TEXT 100,100,"3",0,1,1,"JewelVault Test Print"
            TEXT 100,150,"3",0,1,1,"Protocol: TSPL"
            TEXT 100,200,"3",0,1,1,"Date: ${
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())
        }"
            TEXT 100,250,"3",0,1,1,"Status: Connected"
            TEXT 100,300,"3",0,1,1,"This is a test print"
            TEXT 100,350,"3",0,1,1,"from JewelVault Mobile App"
            PRINT 1
        """.trimIndent()
        return tsplCommands.toByteArray(Charsets.UTF_8)
    }

    private fun generateEsc1TestData(): ByteArray {
        val esc1Commands = """
            ${27.toChar()}@
            ${27.toChar()}a1
            JewelVault Test Print
            ${27.toChar()}a0
            Protocol: ESC1
            Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
            Status: Connected
            This is a test print
            from JewelVault Mobile App
            ${27.toChar()}d1
            ${10.toChar()}
            ${10.toChar()}
            ${10.toChar()}
        """.trimIndent()
        return esc1Commands.toByteArray(Charsets.UTF_8)
    }

    private fun generateEsc2TestData(): ByteArray {
        val esc2Commands = """
            ${27.toChar()}@
            ${27.toChar()}!${8.toChar()}
            JewelVault Test Print
            ${27.toChar()}!${0.toChar()}
            Protocol: ESC2
            Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
            Status: Connected
            This is a test print
            from JewelVault Mobile App
            ${27.toChar()}d${2.toChar()}
            ${10.toChar()}
            ${10.toChar()}
            ${10.toChar()}
        """.trimIndent()
        return esc2Commands.toByteArray(Charsets.UTF_8)
    }

    private fun generatePplbTestData(): ByteArray {
        val pplbCommands = """
            ! 0 200 200 210 1
            PAGE-WIDTH 384
            TEXT 4 0 30 40 JewelVault Test Print
            TEXT 4 0 30 80 Protocol: PPLB
            TEXT 4 0 30 120 Date: ${
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())
        }
            TEXT 4 0 30 160 Status: Connected
            TEXT 4 0 30 200 This is a test print
            TEXT 4 0 30 240 from JewelVault Mobile App
            PRINT
        """.trimIndent()
        return pplbCommands.toByteArray(Charsets.UTF_8)
    }

    private fun generateGenericTestData(): ByteArray {
        val genericCommands = """
            JewelVault Test Print
            Protocol: Generic
            Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
            Status: Connected
            This is a test print
            from JewelVault Mobile App
            
            
            
        """.trimIndent()
        return genericCommands.toByteArray(Charsets.UTF_8)
    }

    /**
     * Set a printer as default
     */
    fun setDefaultPrinter(address: String) {
        viewModelScope.launch {
            try {
                appDatabase.printerDao().clearAllDefaults()
                appDatabase.printerDao().setDefaultPrinter(address)
                val printerName =
                    savedPrinters.value.find { it.address == address }?.name ?: address
                _snackBarState.value = "Set $printerName as default printer"
                log("Set default printer: $address")
            } catch (e: Exception) {
                log("Error setting default printer: ${e.message}")
                _snackBarState.value = "Failed to set default printer: ${e.message}"
            }
        }
    }

    /**
     * Remove a printer from saved list
     */
    fun removePrinter(address: String) {
        viewModelScope.launch {
            try {
                val printerName =
                    savedPrinters.value.find { it.address == address }?.name ?: address
                appDatabase.printerDao().deletePrinterByAddress(address)
                _snackBarState.value = "Removed $printerName from saved printers"
                log("Removed printer: $address")
            } catch (e: Exception) {
                log("Error removing printer: ${e.message}")
                _snackBarState.value = "Failed to remove printer: ${e.message}"
            }
        }
    }

    /**
     * Check connection liveness for a printer
     */
    fun checkConnection(address: String, method: String) {
        viewModelScope.launch {
            try {
                val isAlive = bleManager.checkPrinterConnectionLiveness(address, method)
                val printerName =
                    savedPrinters.value.find { it.address == address }?.name ?: address
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
     * Connect to a printer using its saved method
     */
    fun connectToPrinter(address: String, method: String) {
        viewModelScope.launch {
            try {
                val printerName =
                    savedPrinters.value.find { it.address == address }?.name ?: address
                _snackBarState.value = "Connecting to $printerName..."

                bleManager.connectToPrinterUsingSavedMethod(
                    address = address,
                    savedMethod = method,
                    onConnect = { device ->
                        _snackBarState.value = "Connected to $printerName"
                        log("Connected to printer: $address")
                    },
                    onFailure = { error ->
                        _snackBarState.value = "Failed to connect to $printerName: ${error.message}"
                        log("Failed to connect to printer: $address - ${error.message}")
                    })
            } catch (e: Exception) {
                log("Error connecting to printer: ${e.message}")
                _snackBarState.value = "Failed to connect to printer: ${e.message}"
            }
        }
    }

    /**
     * Disconnect a printer
     */
    fun disconnectPrinter(address: String) {
        viewModelScope.launch {
            try {
                val printerName =
                    savedPrinters.value.find { it.address == address }?.name ?: address
                bleManager.disconnectDevice(address)
                _snackBarState.value = "Disconnected from $printerName"
                log("Disconnected printer: $address")
            } catch (e: Exception) {
                log("Error disconnecting printer: ${e.message}")
                _snackBarState.value = "Failed to disconnect printer: ${e.message}"
            }
        }
    }

    /**
     * Print label using a template
     */
    fun printLabelWithTemplate(
        template: LabelTemplateEntity,
        elements: List<LabelElementEntity>,
        item: ItemEntity,
        context1: Context
    ) {
        viewModelScope.launch {
            try {
                val connected = bleManager.connectedDevices.value.firstOrNull()
                if (connected == null) {
                    _snackBarState.value = "No connected printer found"
                    return@launch
                }
                val address = connected.address
                val printerName = savedPrinters.value.find { it.address == address }?.name ?: address
                _snackBarState.value = "Printing label on $printerName..."

                val printGenerator = LabelPrintGenerator(context1.applicationContext)

                // Determine best language for this printer
                val preferred = resolvePrinterLanguage(address, template.printLanguage)
                val candidates = linkedSetOf(
                    preferred.uppercase(),
                    template.printLanguage.uppercase(),
                    "TSPL", "CPCL", "ESC"
                )

                var sent = false
                var usedLanguage: String? = null
                // Load a store for bindings (first store if any)
                val store = try { appDatabase.storeDao().getAllStores().firstOrNull() } catch (_: Exception) { null }

                for (lang in candidates) {
                    val data = printGenerator.generatePrintData(
                        template = template,
                        elements = elements,
                        data = buildMap<String, Any> {
                            put("item", item)
                            if (store != null) put("store", store)
                        },
                        language = lang
                    )
                    val ok = bleManager.sendPrintData(address, data)
                    if (ok) {
                        sent = true
                        usedLanguage = lang
                        break
                    }
                }

                if (sent && usedLanguage != null) {
                    // Remember working language for this printer
                    try {
                        addSupportedLanguage(address, usedLanguage)
                        appDatabase.printerDao().updateCurrentLanguage(address, usedLanguage)
                    } catch (_: Exception) {}
                    _snackBarState.value = "✅ Label sent to $printerName ($usedLanguage)"
                } else {
                    _snackBarState.value = "❌ Failed to print on $printerName. Try another language."
                }
            } catch (e: Exception) {
                log("Error printing label: ${e.message}")
                _snackBarState.value = "❌ Error printing label: ${e.message}"
            }
        }
    }

    private suspend fun resolvePrinterLanguage(address: String, templateLanguage: String): String {
        val printer = appDatabase.printerDao().getPrinterByAddress(address)
        printer?.currentLanguage?.let { return it }
        val supported = try {
            printer?.supportedLanguages?.let {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(it)
            }
        } catch (_: Exception) { null } ?: emptyList()

        val normalized = supported.map { it.uppercase() }
        val templateLang = templateLanguage.uppercase()
        return when {
            normalized.contains(templateLang) -> templateLang
            normalized.contains("TSPL") -> "TSPL"
            normalized.contains("CPCL") -> "CPCL"
            normalized.contains("ESC") -> "ESC"
            else -> templateLang
        }
    }

    /**
     * Print item using the default template. If only one template exists, treat it as default.
     */
    fun printItemWithDefaultTemplate(item: ItemEntity, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val templates = appDatabase.labelTemplateDao().getAllTemplates().first()
                if (templates.isEmpty()) {
                    _snackBarState.value = "No label templates found"
                    return@launch
                }
                val chosen = templates.firstOrNull { it.isDefault } ?: if (templates.size == 1) templates[0] else null
                if (chosen == null) {
                    _snackBarState.value = "Select a default template first"
                    return@launch
                }
                val elements = appDatabase.labelElementDao().getElementsByTemplateIdSync(chosen.templateId)
                if (elements.isEmpty()) {
                    _snackBarState.value = "Template has no elements"
                    return@launch
                }
                printLabelWithTemplate(chosen, elements, item, context)
            } catch (e: Exception) {
                log("Error printing with default template: ${e.message}")
                _snackBarState.value = "Failed to print: ${e.message}"
            }
        }
    }

    /**
     * Add a supported language to a printer
     */
    private suspend fun addSupportedLanguage(printerAddress: String, language: String) {
        val printer = appDatabase.printerDao().getPrinterByAddress(printerAddress)
        if (printer != null) {
            val currentLanguages = printer.supportedLanguages?.let {
                try {
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(it)
                        .toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } ?: mutableListOf()

            if (!currentLanguages.contains(language)) {
                currentLanguages.add(language)
                val updatedLanguages = kotlinx.serialization.json.Json.encodeToString(
                    ListSerializer(String.serializer()),
                    currentLanguages
                )
                appDatabase.printerDao().updateSupportedLanguages(printerAddress, updatedLanguages)
            }
        }
    }
}
