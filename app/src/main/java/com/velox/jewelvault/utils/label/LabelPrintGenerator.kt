package com.velox.jewelvault.utils.label

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelElementEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import com.velox.jewelvault.utils.PrintUtils
import com.velox.jewelvault.utils.log
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
        val commands = StringBuilder()
        
        // Label setup
        commands.append("SIZE ${template.labelWidth} mm, ${template.labelHeight} mm\r\n")
        commands.append("GAP ${template.gapWidth} mm, ${template.gapHeight} mm\r\n")
        commands.append("DENSITY ${template.printDensity}\r\n")
        commands.append("SPEED 1\r\n")
        commands.append("DIRECTION ${if (template.orientation == "LANDSCAPE") 0 else 1}\r\n")
        commands.append("REFERENCE 0,0\r\n")
        commands.append("SET PEEL OFF\r\n")
        commands.append("SET CUTTER OFF\r\n")
        commands.append("SET PARTIAL_CUTTER OFF\r\n")
        commands.append("SET TEAR ON\r\n")
        commands.append("CLS\r\n")
        
        // Add elements in z-index order
        elements.sortedBy { it.zIndex }.forEach { element ->
            try {
                addTsplElement(commands, element, data)
            } catch (e: Exception) {
                log("LabelPrintGenerator: Error adding element ${element.elementId}: ${e.message}")
            }
        }
        
        commands.append("PRINT 1\r\n")
        return commands.toString().toByteArray(Charsets.UTF_8)
    }
    
    /**
     * Add a single element to TSPL commands
     */
    private fun addTsplElement(
        commands: StringBuilder,
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
        val xDots = (element.x * dotsPerMm).roundToInt()
        val yDots = (element.y * dotsPerMm).roundToInt()
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
        commands: StringBuilder,
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
        
        val fontSize = properties["fontSize"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 12f
        val isBold = properties["isBold"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val alignment = properties["alignment"]?.jsonPrimitive?.content ?: "START"
        
        // TSPL font size mapping (approximate)
        val tsplFontSize = when {
            fontSize <= 8f -> "1"
            fontSize <= 12f -> "2"
            fontSize <= 16f -> "3"
            fontSize <= 20f -> "4"
            else -> "5"
        }
        
        // TSPL alignment mapping
        val tsplAlignment = when (alignment.uppercase()) {
            "CENTER" -> 1
            "END" -> 2
            else -> 0 // START
        }
        
        commands.append("TEXT $xDots,$yDots,\"$tsplFontSize\",0,$tsplAlignment,${if (isBold) 1 else 0},\"$text\"\r\n")
    }
    
    private fun addTsplImageElement(
        commands: StringBuilder,
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
        
        if (imagePath != null || imageUri != null) {
            try {
                val bitmap = if (imagePath != null) {
                    BitmapFactory.decodeFile(imagePath)
                } else {
                    val uri = Uri.parse(imageUri)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
                
                if (bitmap != null) {
                    val targetSizeMm = kotlin.math.min(element.width, element.height).toInt()
                    val bitmapCommand = PrintUtils.buildTsplBitmapCommand(
                        source = bitmap,
                        xDots = xDots,
                        yDots = yDots,
                        targetSizeMm = targetSizeMm,
                        threshold = 140,
                        bitOrderMsbFirst = true,
                        blackIsOne = true,
                        dither = false
                    )
                    commands.append(bitmapCommand)
                    commands.append("\r\n")
                }
            } catch (e: Exception) {
                log("LabelPrintGenerator: Error processing image element: ${e.message}")
            }
        }
    }
    
    private fun addTsplQRElement(
        commands: StringBuilder,
        element: LabelElementEntity,
        properties: JsonObject,
        data: Map<String, Any>,
        xDots: Int,
        yDots: Int,
        widthDots: Int,
        heightDots: Int
    ) {
        val raw = properties["qrData"]?.jsonPrimitive?.content ?: ""
        val qrData = resolveDataBinding(raw, element.dataBinding, data).ifBlank { raw.ifBlank { " " } }
        
        val cellSize = properties["cellSize"]?.jsonPrimitive?.content?.toIntOrNull() ?: 4
        
        // Use TSPL native QRCODE command
        commands.append("QRCODE $xDots,$yDots,M,$cellSize,A,0,\"$qrData\"\r\n")
    }
    
    private fun addTsplBarcodeElement(
        commands: StringBuilder,
        element: LabelElementEntity,
        properties: JsonObject,
        data: Map<String, Any>,
        xDots: Int,
        yDots: Int,
        widthDots: Int,
        heightDots: Int
    ) {
        val raw = properties["barcodeData"]?.jsonPrimitive?.content ?: ""
        val barcodeData = resolveDataBinding(raw, element.dataBinding, data).ifBlank { raw.ifBlank { " " } }
        
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
        commands: StringBuilder,
        element: LabelElementEntity,
        properties: JsonObject,
        xDots: Int,
        yDots: Int,
        widthDots: Int,
        heightDots: Int
    ) {
        val thickness = properties["thickness"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
        val barHeight = if (heightDots > 0) heightDots else thickness
        val barWidth = if (widthDots > 0) widthDots else thickness
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
        val commands = StringBuilder()
        
        // CPCL setup
        val widthDots = (template.labelWidth * 8).roundToInt() // 203 DPI = 8 dots/mm
        val heightDots = (template.labelHeight * 8).roundToInt()
        
        commands.append("! 0 200 200 $heightDots 1\r\n")
        commands.append("PAGE-WIDTH $widthDots\r\n")
        
        // Add elements
        elements.sortedBy { it.zIndex }.forEach { element ->
            addCpclElement(commands, element, data)
        }
        
        commands.append("PRINT\r\n")
        return commands.toString().toByteArray(Charsets.UTF_8)
    }
    
    private fun addCpclElement(
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
        
        val xDots = (element.x * 8).roundToInt()
        val yDots = (element.y * 8).roundToInt()
        
        when (element.elementType) {
            "TEXT" -> {
                val text = resolveDataBinding(
                    properties["text"]?.jsonPrimitive?.content ?: "",
                    element.dataBinding,
                    data
                )
                val fontSize = properties["fontSize"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 12f
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
                val qrData = resolveDataBinding(
                    properties["qrData"]?.jsonPrimitive?.content ?: "",
                    element.dataBinding,
                    data
                )
                commands.append("QRCODE $xDots $yDots M 2 U 4\r\n")
                commands.append("DATA \"$qrData\"\r\n")
                commands.append("ENDQR\r\n")
            }
            // Add other element types as needed
        }
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
                        "itemId" -> item?.itemId ?: staticValue
                        "itemAddName" -> item?.itemAddName ?: staticValue
                        "gsWt" -> item?.gsWt?.toString() ?: staticValue
                        "fnWt" -> item?.fnWt?.toString() ?: staticValue
                        "purity" -> item?.purity ?: staticValue
                        "quantity" -> item?.quantity?.toString() ?: staticValue
                        "huid" -> item?.huid ?: staticValue
                        "addDesKey" -> item?.addDesKey ?: staticValue
                        "addDesValue" -> item?.addDesValue ?: staticValue
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
}
