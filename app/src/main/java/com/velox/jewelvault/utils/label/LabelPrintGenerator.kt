package com.velox.jewelvault.utils.label

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelElementEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import com.velox.jewelvault.utils.FileManager
import com.velox.jewelvault.utils.PrintUtils
import com.velox.jewelvault.utils.log
import java.io.ByteArrayOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

/**
 * Generates print data for label templates in various printer languages
 */
class LabelPrintGenerator(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Generate print data for a label template
     */
    fun generatePrintData(
        template: LabelTemplateEntity,
        elements: List<LabelElementEntity>,
        data: Map<String, Any> = emptyMap(),
        language: String = template.printLanguage
    ): ByteArray {
        log("LabelPrintGenerator: Generating print data for template ${template.templateId} in $language")
        
        return when (language.uppercase()) {
            "TSPL" -> generateTsplLabel(template, elements, data)
            "CPCL" -> generateCpclLabel(template, elements, data)
            "PPLB" -> generatePplbLabel(template, elements, data)
            "ESC" -> generateEscLabel(template, elements, data)
            else -> {
                log("LabelPrintGenerator: Unknown language $language, defaulting to TSPL")
                generateTsplLabel(template, elements, data)
            }
        }
    }
    
    /**
     * Generate TSPL (TSC Printer Language) commands
     */
    private fun generateTsplLabel(
        template: LabelTemplateEntity,
        elements: List<LabelElementEntity>,
        data: Map<String, Any>
    ): ByteArray {
        val commands = TsplCommandBuilder()
        
        // Label setup
        commands.append("SIZE ${template.labelWidth} mm, ${template.labelHeight} mm\r\n")
        commands.append("GAP ${template.gapWidth} mm, ${template.gapHeight} mm\r\n")
        commands.append("DENSITY ${template.printDensity}\r\n")
        commands.append("SPEED ${template.printSpeed}\r\n")
        commands.append("DIRECTION ${template.printDirection}\r\n")
        val referenceX = (template.referenceX * 8).roundToInt()
        val referenceY = (template.referenceY * 8).roundToInt()
        commands.append("REFERENCE $referenceX,$referenceY\r\n")
        commands.append("SET PEEL OFF\r\n")
        commands.append("SET CUTTER OFF\r\n")
        commands.append("SET PARTIAL_CUTTER OFF\r\n")
        commands.append("SET TEAR ON\r\n")
        commands.append("CLS\r\n")
        
        // Add elements in z-index order
        elements.sortedBy { it.zIndex }.forEach { element ->
            try {
                addTsplElement(commands, template, element, data)
            } catch (e: Exception) {
                log("LabelPrintGenerator: Error adding element ${element.elementId}: ${e.message}")
            }
        }
        
        commands.append("PRINT 1\r\n")
        return commands.toByteArray()
    }
    
    /**
     * Add a single element to TSPL commands
     */
    private fun addTsplElement(
        commands: TsplCommandBuilder,
        template: LabelTemplateEntity,
        element: LabelElementEntity,
        data: Map<String, Any>
    ) {
        if (!element.isVisible) return
        
        val properties = try {
            json.parseToJsonElement(element.properties).jsonObject
        } catch (e: Exception) {
            log("LabelPrintGenerator: Error parsing element properties: ${e.message}")
            JsonObject(emptyMap())
        }
        
        // Convert mm to dots (assuming 203 DPI = 8 dots per mm)
        val dotsPerMm = 8.0
        val paddingDots = (template.labelPadding * dotsPerMm).roundToInt()
        val xDots = (paddingDots + element.x * dotsPerMm).roundToInt()
        val yDots = (paddingDots + element.y * dotsPerMm).roundToInt()
        val widthDots = (element.width * dotsPerMm).roundToInt()
        val heightDots = (element.height * dotsPerMm).roundToInt()
        
        when (element.elementType) {
            "TEXT" -> addTsplTextElement(commands, element, properties, data, xDots, yDots, widthDots, heightDots)
            "IMAGE" -> addTsplImageElement(commands, element, properties, data, xDots, yDots, widthDots, heightDots)
            "QR_CODE" -> addTsplQRElement(commands, element, properties, data, xDots, yDots, widthDots, heightDots)
            "BARCODE" -> addTsplBarcodeElement(commands, element, properties, data, xDots, yDots, widthDots, heightDots)
            "LINE" -> addTsplLineElement(commands, element, properties, xDots, yDots, widthDots, heightDots)
        }
    }
    
    private fun addTsplTextElement(
        commands: TsplCommandBuilder,
        element: LabelElementEntity,
        properties: JsonObject,
        data: Map<String, Any>,
        xDots: Int,
        yDots: Int,
        widthDots: Int,
        heightDots: Int
    ) {
        val rawText = properties["text"]?.jsonPrimitive?.content ?: ""
        val text = resolveDataBinding(rawText, element.dataBinding, data).ifBlank { rawText.ifBlank { " " } }
        
        val fontSize = properties["fontSize"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 6f
        val isBold = properties["isBold"]?.jsonPrimitive?.content?.toBoolean() ?: false
        
        // TSPL font size mapping (approximate)
        val tsplFontSize = when {
            fontSize <= 8f -> "1"
            fontSize <= 12f -> "2"
            fontSize <= 16f -> "3"
            fontSize <= 20f -> "4"
            else -> "5"
        }
        val scale = when {
            fontSize <= 10f -> 1
            fontSize <= 16f -> 2
            else -> 3
        }

        commands.append("TEXT $xDots,$yDots,\"$tsplFontSize\",0,$scale,$scale,\"$text\"\r\n")
        if (isBold) {
            // Simple bold effect: overprint once with 1 dot shift
            commands.append("TEXT ${xDots + 1},$yDots,\"$tsplFontSize\",0,$scale,$scale,\"$text\"\r\n")
        }
    }
    
    private fun addTsplImageElement(
        commands: TsplCommandBuilder,
        element: LabelElementEntity,
        properties: JsonObject,
        data: Map<String, Any>,
        xDots: Int,
        yDots: Int,
        widthDots: Int,
        heightDots: Int
    ) {
        val imagePath = properties["imagePath"]?.jsonPrimitive?.content
        val imageUri = properties["imageUri"]?.jsonPrimitive?.content
        val invert = properties["invert"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        val defaultThreshold = if (element.dataBinding == "store.logo" || element.dataBinding == "store.bsiMark") 170 else 150
        val threshold = properties["threshold"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(40, 240) ?: defaultThreshold
        val ditherMode = properties["ditherMode"]?.jsonPrimitive?.content ?: "floyd"
        val useDither = ditherMode != "none"
        val imageMode = properties["mode"]?.jsonPrimitive?.content?.lowercase() ?: "bitmap"
        val imageDpi = properties["dpi"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(150, 600) ?: 203

        val boundBitmap = when (element.dataBinding) {
            "store.logo" -> loadStoreLogoBitmap()
            "store.bsiMark" -> loadBsiMarkBitmap()
            else -> null
        }

        val bitmap = boundBitmap ?: try {
            when {
                imagePath != null -> BitmapFactory.decodeFile(imagePath)
                imageUri != null -> {
                    val uri = Uri.parse(imageUri)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            log("LabelPrintGenerator: Error processing image element: ${e.message}")
            null
        }

        if (bitmap != null) {
            val targetSizeMm = kotlin.math.min(element.width, element.height).toInt()
            val isLogoBinding = element.dataBinding == "store.logo" || element.dataBinding == "store.bsiMark"
            log("LABEL_PRINT_IMG mode=$imageMode dpi=$imageDpi th=$threshold dither=$ditherMode invert=$invert binding=${element.dataBinding} target=${targetSizeMm}mm")
            if (imageMode == "download") {
                val downloadName = "G" + element.elementId.takeLast(3).padStart(3, '0')
                val downloadCommand = PrintUtils.buildTsplDownloadBmpCommandBytes(
                    source = bitmap,
                    xDots = xDots,
                    yDots = yDots,
                    name = "$downloadName.BMP",
                    targetSizeMm = targetSizeMm,
                    threshold = threshold,
                    dpi = imageDpi,
                    dither = useDither,
                    cropToSquare = true,
                    invert = invert
                )
                commands.appendBytes(downloadCommand)
            } else {
                // Use BITMAP with dithering and tuned threshold to reduce banding while keeping simplicity
                val bitmapCommand = PrintUtils.buildTsplBitmapCommand(
                    source = bitmap,
                    xDots = xDots,
                    yDots = yDots,
                    targetSizeMm = targetSizeMm,
                    cropToSquare = true,
                    threshold = threshold,
                    bitOrderMsbFirst = true,
                    blackIsOne = true,
                    dither = useDither,
                    ditherMode = ditherMode,
                    invert = invert
                )
                commands.append(bitmapCommand)
                commands.append("\r\n")
            }
        }
    }
    
    private fun addTsplQRElement(
        commands: TsplCommandBuilder,
        element: LabelElementEntity,
        properties: JsonObject,
        data: Map<String, Any>,
        xDots: Int,
        yDots: Int,
        widthDots: Int,
        heightDots: Int
    ) {
        val raw = properties["qrData"]?.jsonPrimitive?.content ?: ""
        val bindingToUse = element.dataBinding?.takeUnless { it.isBlank() } ?: "item.qrPayload"
        val qrData = resolveDataBinding(raw, bindingToUse, data).ifBlank { raw.ifBlank { " " } }
        
        // Keep QR square and bounded: derive a conservative cell size from the smaller dimension.
        val minSideDots = kotlin.math.min(widthDots, heightDots).coerceAtLeast(16)
        val derivedCell = (minSideDots / 40).coerceIn(1, 10) // assume up to ~40 modules for our compact payload
        val cellSize = properties["cellSize"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(1, 10) ?: derivedCell
        
        // Use TSPL native QRCODE command
        commands.append("QRCODE $xDots,$yDots,M,$cellSize,A,0,\"$qrData\"\r\n")
    }
    
    private fun addTsplBarcodeElement(
        commands: TsplCommandBuilder,
        element: LabelElementEntity,
        properties: JsonObject,
        data: Map<String, Any>,
        xDots: Int,
        yDots: Int,
        widthDots: Int,
        heightDots: Int
    ) {
        val raw = properties["barcodeData"]?.jsonPrimitive?.content ?: ""
        val bindingToUse = element.dataBinding?.takeUnless { it.isBlank() } ?: "item.itemId"
        val barcodeData = resolveDataBinding(raw, bindingToUse, data).ifBlank { raw.ifBlank { " " } }
        
        val barcodeType = properties["barcodeType"]?.jsonPrimitive?.content ?: "CODE128"
        
        // TSPL barcode type mapping
        val tsplBarcodeType = when (barcodeType.uppercase()) {
            "CODE128" -> "128"
            "CODE39" -> "39"
            "EAN13" -> "EAN13"
            "EAN8" -> "EAN8"
            "UPC_A" -> "UPC-A"
            "UPC_E" -> "UPC-E"
            "ITF" -> "ITF"
            else -> "128"
        }
        
        commands.append("BARCODE $xDots,$yDots,\"$tsplBarcodeType\",$heightDots,1,0,2,2,\"$barcodeData\"\r\n")
    }
    
    private fun addTsplLineElement(
        commands: TsplCommandBuilder,
        element: LabelElementEntity,
        properties: JsonObject,
        xDots: Int,
        yDots: Int,
        widthDots: Int,
        heightDots: Int
    ) {
        val thicknessDots = 2 // fixed ~0.2mm at 203dpi
        val isHorizontal = widthDots >= heightDots
        val length = if (isHorizontal) widthDots else heightDots
        val barWidth = if (isHorizontal) length.coerceAtLeast(1) else thicknessDots
        val barHeight = if (isHorizontal) thicknessDots else length.coerceAtLeast(1)
        commands.append("BAR $xDots,$yDots,$barWidth,$barHeight\r\n")
    }
    
    /**
     * Generate CPCL (Citizen Printer Command Language) commands
     */
    private fun generateCpclLabel(
        template: LabelTemplateEntity,
        elements: List<LabelElementEntity>,
        data: Map<String, Any>
    ): ByteArray {
        val commands = TsplCommandBuilder()
        
        // CPCL setup
        val widthDots = (template.labelWidth * 8).roundToInt() // 203 DPI = 8 dots/mm
        val heightDots = (template.labelHeight * 8).roundToInt()
        
        commands.append("! 0 200 200 $heightDots 1\r\n")
        commands.append("PAGE-WIDTH $widthDots\r\n")
        
        // Add elements
        elements.sortedBy { it.zIndex }.forEach { element ->
            addCpclElement(commands, template, element, data)
        }
        
        commands.append("PRINT\r\n")
        return commands.toByteArray()
    }
    
    private fun addCpclElement(
        commands: TsplCommandBuilder,
        template: LabelTemplateEntity,
        element: LabelElementEntity,
        data: Map<String, Any>
    ) {
        if (!element.isVisible) return
        
        val properties = try {
            json.parseToJsonElement(element.properties).jsonObject
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
        
        val paddingDots = (template.labelPadding * 8).roundToInt()
        val xDots = (paddingDots + element.x * 8).roundToInt()
        val yDots = (paddingDots + element.y * 8).roundToInt()
        
        when (element.elementType) {
            "TEXT" -> {
                val text = resolveDataBinding(
                    properties["text"]?.jsonPrimitive?.content ?: "",
                    element.dataBinding,
                    data
                )
                val fontSize = properties["fontSize"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 6f
                val cpclFontSize = when {
                    fontSize <= 8f -> 1
                    fontSize <= 12f -> 2
                    fontSize <= 16f -> 3
                    fontSize <= 20f -> 4
                    else -> 5
                }
                commands.append("TEXT $cpclFontSize 0 $xDots $yDots $text\r\n")
            }
            "QR_CODE" -> {
                val bindingToUse = element.dataBinding?.takeUnless { it.isBlank() } ?: "item.qrPayload"
                val qrData = resolveDataBinding(
                    properties["qrData"]?.jsonPrimitive?.content ?: "",
                    bindingToUse,
                    data
                )
                // Keep QR square and bounded
                val minSideDots = kotlin.math.min(
                    (element.width * 8).roundToInt(),
                    (element.height * 8).roundToInt()
                ).coerceAtLeast(16)
                val derivedCell = (minSideDots / 40).coerceIn(1, 10)
                val cellSize = properties["cellSize"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(1, 10) ?: derivedCell
                commands.append("QRCODE $xDots $yDots M 2 U $cellSize\r\n")
                commands.append("DATA \"$qrData\"\r\n")
                commands.append("ENDQR\r\n")
            }
            "IMAGE" -> {
                val imagePath = properties["imagePath"]?.jsonPrimitive?.content
                val imageUri = properties["imageUri"]?.jsonPrimitive?.content
                val invert = properties["invert"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                val defaultThreshold = if (element.dataBinding == "store.logo" || element.dataBinding == "store.bsiMark") 170 else 150
                val threshold = properties["threshold"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(40, 240)
                    ?: defaultThreshold
                val ditherMode = properties["ditherMode"]?.jsonPrimitive?.content ?: "floyd"
                val useDither = ditherMode != "none"
                val imageMode = properties["mode"]?.jsonPrimitive?.content?.lowercase() ?: "bitmap"
                val imageDpi = properties["dpi"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(150, 600) ?: 203

                val boundBitmap = when (element.dataBinding) {
                    "store.logo" -> loadStoreLogoBitmap()
                    "store.bsiMark" -> loadBsiMarkBitmap()
                    else -> null
                }

                val bitmap = boundBitmap ?: try {
                    when {
                        imagePath != null -> BitmapFactory.decodeFile(imagePath)
                        imageUri != null -> {
                            val uri = Uri.parse(imageUri)
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream)
                            }
                        }
                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }

                if (bitmap != null) {
                    val targetSizeMm = kotlin.math.min(element.width, element.height).toInt()
                    log("LABEL_PRINT_IMG_CPCL mode=$imageMode dpi=$imageDpi th=$threshold dither=$ditherMode invert=$invert binding=${element.dataBinding} target=${targetSizeMm}mm")
                    if (imageMode == "download") {
                        val downloadName = "G" + element.elementId.takeLast(3).padStart(3, '0') + ".BMP"
                        val bytes = PrintUtils.buildTsplDownloadBmpCommandBytes(
                            source = bitmap,
                            xDots = xDots,
                            yDots = yDots,
                            name = downloadName,
                            targetSizeMm = targetSizeMm,
                            threshold = threshold,
                            dpi = imageDpi,
                            dither = useDither,
                            cropToSquare = true,
                            invert = invert
                        )
                        commands.appendBytes(bytes)
                    } else {
                        val bmpCmd = PrintUtils.buildTsplBitmapCommand(
                            source = bitmap,
                            xDots = xDots,
                            yDots = yDots,
                            targetSizeMm = targetSizeMm,
                            cropToSquare = true,
                            threshold = threshold,
                            bitOrderMsbFirst = true,
                            blackIsOne = true,
                            dither = useDither,
                            ditherMode = ditherMode,
                            invert = invert
                        )
                        commands.append(bmpCmd)
                        commands.append("\r\n")
                    }
                }
            }
            "BARCODE" -> {
                val raw = properties["barcodeData"]?.jsonPrimitive?.content ?: ""
                val bindingToUse = element.dataBinding?.takeUnless { it.isBlank() } ?: "item.itemId"
                val barcodeData = resolveDataBinding(raw, bindingToUse, data).ifBlank { raw.ifBlank { " " } }
                val barcodeType = properties["barcodeType"]?.jsonPrimitive?.content ?: "128"
                // CPCL: BARCODE type x y rotation narrow wide height human-readable data
                val height = (element.height * 8).roundToInt().coerceAtLeast(24)
                commands.append("BARCODE $barcodeType $xDots $yDots 0 2 4 $height $barcodeData\r\n")
            }
            "LINE" -> {
                val thicknessDots = 2
                val isHorizontal = element.width >= element.height
                val lengthDots = ((if (isHorizontal) element.width else element.height) * 8).roundToInt().coerceAtLeast(1)
                val barWidth = if (isHorizontal) lengthDots else thicknessDots
                val barHeight = if (isHorizontal) thicknessDots else lengthDots
                commands.append("BOX $xDots $yDots ${xDots + barWidth} ${yDots + barHeight} $thicknessDots\r\n")
            }
        }
    }

    /**
     * Generate PPLB commands (similar layout to CPCL)
     */
    private fun generatePplbLabel(
        template: LabelTemplateEntity,
        elements: List<LabelElementEntity>,
        data: Map<String, Any>
    ): ByteArray {
        val commands = TsplCommandBuilder()

        val widthDots = (template.labelWidth * 8).roundToInt()
        val heightDots = (template.labelHeight * 8).roundToInt()
        commands.append("! 0 200 200 $heightDots 1\r\n")
        commands.append("PAGE-WIDTH $widthDots\r\n")

        elements.sortedBy { it.zIndex }.forEach { element ->
            addCpclElement(commands, template, element, data)
        }

        commands.append("PRINT\r\n")
        return commands.toByteArray()
    }
    
    /**
     * Generate ESC/POS commands
     */
    private fun generateEscLabel(
        template: LabelTemplateEntity,
        elements: List<LabelElementEntity>,
        data: Map<String, Any>
    ): ByteArray {
        val commands = StringBuilder()
        
        // ESC/POS setup
        commands.append("${27.toChar()}@") // Initialize printer
        commands.append("${27.toChar()}a1") // Center alignment
        
        // Add elements
        elements.sortedBy { it.zIndex }.forEach { element ->
            addEscElement(commands, element, data)
        }
        
        commands.append("${27.toChar()}d3") // Feed paper
        commands.append("${10.toChar()}") // Line feed
        
        return commands.toString().toByteArray(Charsets.UTF_8)
    }
    
    private fun addEscElement(
        commands: StringBuilder,
        element: LabelElementEntity,
        data: Map<String, Any>
    ) {
        if (!element.isVisible) return
        
        val properties = try {
            json.parseToJsonElement(element.properties).jsonObject
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
        
        when (element.elementType) {
            "TEXT" -> {
                val text = resolveDataBinding(
                    properties["text"]?.jsonPrimitive?.content ?: "",
                    element.dataBinding,
                    data
                )
                commands.append(text)
                commands.append("\r\n")
            }
            // Add other element types as needed
        }
    }

    private fun loadStoreLogoBitmap(): Bitmap? {
        return try {
            val logoUri = FileManager.getLogoFileUri(context)
            if (logoUri != null) {
                context.contentResolver.openInputStream(logoUri)?.use { BitmapFactory.decodeStream(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            log("LabelPrintGenerator: Error loading store logo: ${e.message}")
            null
        }
    }

    private fun loadBsiMarkBitmap(): Bitmap? {
        return try {
            val hallmarkDrawable =
                context.resources.getIdentifier("hallmark", "drawable", context.packageName)
            if (hallmarkDrawable != 0) {
                BitmapFactory.decodeResource(context.resources, hallmarkDrawable)
            } else {
                null
            }
        } catch (e: Exception) {
            log("LabelPrintGenerator: Error loading BSI mark: ${e.message}")
            null
        }
    }
    
    /**
     * Resolve data binding for dynamic content
     */
    private fun resolveDataBinding(
        staticValue: String,
        dataBinding: String?,
        data: Map<String, Any>
    ): String {
        if (dataBinding.isNullOrBlank()) return staticValue
        
        return try {
            when {
                dataBinding.startsWith("item.") -> {
                    val field = dataBinding.removePrefix("item.")
                    val item = data["item"] as? ItemEntity
                    when (field) {
                        "qrPayload" -> item?.let { PrintUtils.buildItemQrPayload(it) } ?: staticValue
                        "itemId" -> item?.itemId ?: staticValue
                        "itemAddName" -> item?.itemAddName ?: staticValue
                        "catName" -> item?.catName ?: staticValue
                        "catId" -> item?.catId ?: staticValue
                        "subCatName" -> item?.subCatName ?: staticValue
                        "subCatId" -> item?.subCatId ?: staticValue
                        "entryType" -> item?.entryType ?: staticValue
                        "gsWt" -> item?.gsWt?.toString() ?: staticValue
                        "ntWt" -> item?.ntWt?.toString() ?: staticValue
                        "fnWt" -> item?.fnWt?.toString() ?: staticValue
                        "purity" -> item?.purity ?: staticValue
                        "quantity" -> item?.quantity?.toString() ?: staticValue
                        "huid" -> item?.huid ?: staticValue
                        "unit" -> item?.unit ?: staticValue
                        "crgType" -> item?.crgType ?: staticValue
                        "crg" -> item?.crg?.toString() ?: staticValue
                        "othCrgDes" -> item?.othCrgDes ?: staticValue
                        "othCrg" -> item?.othCrg?.toString() ?: staticValue
                        "cgst" -> item?.cgst?.toString() ?: staticValue
                        "sgst" -> item?.sgst?.toString() ?: staticValue
                        "igst" -> item?.igst?.toString() ?: staticValue
                        "addDesKey" -> item?.addDesKey ?: staticValue
                        "addDesValue" -> item?.addDesValue ?: staticValue
                        "purchaseOrderId" -> item?.purchaseOrderId ?: staticValue
                        else -> staticValue
                    }
                }
                dataBinding.startsWith("store.") -> {
                    val field = dataBinding.removePrefix("store.")
                    val store = data["store"] as? com.velox.jewelvault.data.roomdb.entity.StoreEntity
                    when (field) {
                        "storeId" -> store?.storeId ?: staticValue
                        "name" -> store?.name ?: staticValue
                        "proprietor" -> store?.proprietor ?: staticValue
                        "email" -> store?.email ?: staticValue
                        "phone" -> store?.phone ?: staticValue
                        "address" -> store?.address ?: staticValue
                        "registrationNo" -> store?.registrationNo ?: staticValue
                        "gstinNo" -> store?.gstinNo ?: staticValue
                        "panNo" -> store?.panNo ?: staticValue
                        "upiId" -> store?.upiId ?: staticValue
                        else -> staticValue
                    }
                }
                dataBinding.startsWith("custom.") -> {
                    val field = dataBinding.removePrefix("custom.")
                    data[field]?.toString() ?: staticValue
                }
                else -> staticValue
            }
        } catch (e: Exception) {
            log("LabelPrintGenerator: Error resolving data binding '$dataBinding': ${e.message}")
            staticValue
        }
    }

    private class TsplCommandBuilder {
        private val out = ByteArrayOutputStream()
        fun append(text: String) {
            out.write(text.toByteArray(Charsets.UTF_8))
        }
        fun appendBytes(bytes: ByteArray) {
            out.write(bytes)
        }
        fun toByteArray(): ByteArray = out.toByteArray()
    }
}
