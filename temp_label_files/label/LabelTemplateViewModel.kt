package com.velox.jewelvault.ui.screen.label

import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.label.LabelElementEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelPrintJobEntity
import com.velox.jewelvault.data.model.label.LabelTemplate
import com.velox.jewelvault.data.model.label.LabelElement
import com.velox.jewelvault.data.model.label.LabelElementType
import com.velox.jewelvault.data.model.label.PrintJobData
import com.velox.jewelvault.data.model.label.ImageScaleType
import com.velox.jewelvault.data.model.label.QRErrorCorrectionLevel
import com.velox.jewelvault.data.model.label.BarcodeType
import com.velox.jewelvault.utils.generateId
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class LabelTemplateViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    @Named("snackMessage") private val _snackBarState: MutableState<String>
) : ViewModel() {

    // Saved templates from Room database
    val savedTemplates: StateFlow<List<LabelTemplateEntity>> =
        appDatabase.labelTemplateDao().getAllTemplates().stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    // Default template
    val defaultTemplate: StateFlow<LabelTemplateEntity?> =
        appDatabase.labelTemplateDao().getDefaultTemplate().stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    // Templates by type
    fun getTemplatesByType(templateType: String): StateFlow<List<LabelTemplateEntity>> =
        appDatabase.labelTemplateDao().getTemplatesByType(templateType).stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    // Search templates
    fun searchTemplates(query: String): StateFlow<List<LabelTemplateEntity>> =
        appDatabase.labelTemplateDao().searchTemplates(query).stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    /**
     * Create a new label template
     */
    fun createTemplate(
        templateName: String,
        templateType: String,
        labelWidth: Float,
        labelHeight: Float,
        gapWidth: Float = 3f,
        gapHeight: Float = 0f,
        printDensity: Int = 6,
        orientation: String = "PORTRAIT",
        printLanguage: String = "TSPL",
        description: String? = null
    ) {
        viewModelScope.launch {
            try {
                val templateId = generateId()
                val currentTime = System.currentTimeMillis()
                
                val template = LabelTemplateEntity(
                    templateId = templateId,
                    templateName = templateName,
                    templateType = templateType,
                    labelWidth = labelWidth,
                    labelHeight = labelHeight,
                    gapWidth = gapWidth,
                    gapHeight = gapHeight,
                    printDensity = printDensity,
                    orientation = orientation,
                    printLanguage = printLanguage,
                    createdAt = currentTime,
                    modifiedAt = currentTime,
                    isDefault = false,
                    description = description
                )
                
                appDatabase.labelTemplateDao().insertTemplate(template)
                _snackBarState.value = "Template '$templateName' created successfully"
                log("Created template: $templateId")
                
            } catch (e: Exception) {
                log("Error creating template: ${e.message}")
                _snackBarState.value = "Failed to create template: ${e.message}"
            }
        }
    }

    /**
     * Update an existing template
     */
    fun updateTemplate(template: LabelTemplateEntity) {
        viewModelScope.launch {
            try {
                val updatedTemplate = template.copy(modifiedAt = System.currentTimeMillis())
                appDatabase.labelTemplateDao().updateTemplate(updatedTemplate)
                _snackBarState.value = "Template '${template.templateName}' updated successfully"
                log("Updated template: ${template.templateId}")
                
            } catch (e: Exception) {
                log("Error updating template: ${e.message}")
                _snackBarState.value = "Failed to update template: ${e.message}"
            }
        }
    }

    /**
     * Delete a template and its elements
     */
    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                // Delete elements first (foreign key constraint)
                appDatabase.labelElementDao().deleteElementsByTemplateId(templateId)
                
                // Delete template
                appDatabase.labelTemplateDao().deleteTemplateById(templateId)
                
                _snackBarState.value = "Template deleted successfully"
                log("Deleted template: $templateId")
                
            } catch (e: Exception) {
                log("Error deleting template: ${e.message}")
                _snackBarState.value = "Failed to delete template: ${e.message}"
            }
        }
    }

    /**
     * Set a template as default
     */
    fun setDefaultTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                appDatabase.labelTemplateDao().clearAllDefaults()
                appDatabase.labelTemplateDao().setDefaultTemplate(templateId)
                _snackBarState.value = "Default template updated"
                log("Set default template: $templateId")
                
            } catch (e: Exception) {
                log("Error setting default template: ${e.message}")
                _snackBarState.value = "Failed to set default template: ${e.message}"
            }
        }
    }

    /**
     * Get template with its elements
     */
    suspend fun getTemplateWithElements(templateId: String): LabelTemplate? {
        return try {
            val template = appDatabase.labelTemplateDao().getTemplateById(templateId)
            if (template != null) {
                val elements = appDatabase.labelElementDao().getElementsByTemplateIdSync(templateId)
                LabelTemplate(template, elements.map { LabelElement(it, parseElementType(it)) })
            } else null
        } catch (e: Exception) {
            log("Error getting template with elements: ${e.message}")
            null
        }
    }

    /**
     * Add an element to a template
     */
    fun addElement(
        templateId: String,
        elementType: LabelElementType,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float = 0f,
        zIndex: Int = 0,
        dataBinding: String? = null
    ) {
        viewModelScope.launch {
            try {
                val elementId = generateId()
                val properties = serializeElementType(elementType)
                
                val element = LabelElementEntity(
                    elementId = elementId,
                    templateId = templateId,
                    elementType = getElementTypeName(elementType),
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    rotation = rotation,
                    zIndex = zIndex,
                    properties = properties,
                    dataBinding = dataBinding,
                    isVisible = true
                )
                
                appDatabase.labelElementDao().insertElement(element)
                
                // Update template modification time
                appDatabase.labelTemplateDao().updateModifiedAt(templateId, System.currentTimeMillis())
                
                log("Added element: $elementId to template: $templateId")
                
            } catch (e: Exception) {
                log("Error adding element: ${e.message}")
                _snackBarState.value = "Failed to add element: ${e.message}"
            }
        }
    }

    /**
     * Update an element
     */
    fun updateElement(element: LabelElementEntity) {
        viewModelScope.launch {
            try {
                appDatabase.labelElementDao().updateElement(element)
                
                // Update template modification time
                appDatabase.labelTemplateDao().updateModifiedAt(element.templateId, System.currentTimeMillis())
                
                log("Updated element: ${element.elementId}")
                
            } catch (e: Exception) {
                log("Error updating element: ${e.message}")
                _snackBarState.value = "Failed to update element: ${e.message}"
            }
        }
    }

    /**
     * Delete an element
     */
    fun deleteElement(elementId: String, templateId: String) {
        viewModelScope.launch {
            try {
                appDatabase.labelElementDao().deleteElementById(elementId)
                
                // Update template modification time
                appDatabase.labelTemplateDao().updateModifiedAt(templateId, System.currentTimeMillis())
                
                log("Deleted element: $elementId")
                
            } catch (e: Exception) {
                log("Error deleting element: ${e.message}")
                _snackBarState.value = "Failed to delete element: ${e.message}"
            }
        }
    }

    /**
     * Create a print job
     */
    fun createPrintJob(printJobData: PrintJobData) {
        viewModelScope.launch {
            try {
                val jobId = generateId()
                val currentTime = System.currentTimeMillis()
                
                val printJob = LabelPrintJobEntity(
                    jobId = jobId,
                    templateId = printJobData.templateId,
                    printerAddress = printJobData.printerAddress,
                    printLanguage = printJobData.printLanguage,
                    itemIds = printJobData.itemIds?.let { Json.encodeToString(ListSerializer(String.serializer()), it) },
                    customData = printJobData.customData?.let { 
                        Json.encodeToString(it)
                    },
                    copies = printJobData.copies,
                    status = "PENDING",
                    createdAt = currentTime,
                    completedAt = null,
                    errorMessage = null
                )
                
                appDatabase.labelPrintJobDao().insertPrintJob(printJob)
                log("Created print job: $jobId")
                
            } catch (e: Exception) {
                log("Error creating print job: ${e.message}")
                _snackBarState.value = "Failed to create print job: ${e.message}"
            }
        }
    }

    /**
     * Get print jobs by status
     */
    fun getPrintJobsByStatus(status: String): StateFlow<List<LabelPrintJobEntity>> =
        appDatabase.labelPrintJobDao().getPrintJobsByStatus(status).stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    /**
     * Update print job status
     */
    fun updatePrintJobStatus(jobId: String, status: String, errorMessage: String? = null) {
        viewModelScope.launch {
            try {
                if (status == "COMPLETED" || status == "FAILED") {
                    appDatabase.labelPrintJobDao().completePrintJob(
                        jobId = jobId,
                        status = status,
                        completedAt = System.currentTimeMillis(),
                        errorMessage = errorMessage
                    )
                } else {
                    appDatabase.labelPrintJobDao().updatePrintJobStatus(jobId, status)
                }
                log("Updated print job $jobId status to $status")
                
            } catch (e: Exception) {
                log("Error updating print job status: ${e.message}")
            }
        }
    }

    // Helper functions
    private fun getElementTypeName(elementType: LabelElementType): String {
        return when (elementType) {
            is LabelElementType.TextElement -> "TEXT"
            is LabelElementType.ImageElement -> "IMAGE"
            is LabelElementType.QRElement -> "QR_CODE"
            is LabelElementType.BarcodeElement -> "BARCODE"
        }
    }

    private fun serializeElementType(elementType: LabelElementType): String {
        val json = Json { ignoreUnknownKeys = true }
        return when (elementType) {
            is LabelElementType.TextElement -> {
                buildJsonObject {
                    put("text", elementType.text)
                    put("fontSize", elementType.fontSize)
                    put("fontFamily", elementType.fontFamily)
                    put("alignment", elementType.alignment.toString())
                    put("color", elementType.color)
                    put("isBold", elementType.isBold)
                    put("isItalic", elementType.isItalic)
                }.toString()
            }
            is LabelElementType.ImageElement -> {
                buildJsonObject {
                    put("imageUri", elementType.imageUri?.toString())
                    put("imagePath", elementType.imagePath)
                    put("scaleType", elementType.scaleType.name)
                }.toString()
            }
            is LabelElementType.QRElement -> {
                buildJsonObject {
                    put("qrData", elementType.qrData)
                    put("size", elementType.size)
                    put("errorCorrectionLevel", elementType.errorCorrectionLevel.name)
                }.toString()
            }
            is LabelElementType.BarcodeElement -> {
                buildJsonObject {
                    put("barcodeData", elementType.barcodeData)
                    put("barcodeType", elementType.barcodeType.name)
                    put("width", elementType.width)
                    put("height", elementType.height)
                }.toString()
            }
        }
    }

    private fun parseElementType(element: LabelElementEntity): LabelElementType {
        val json = Json { ignoreUnknownKeys = true }
        val properties = json.parseToJsonElement(element.properties).jsonObject
        
        return when (element.elementType) {
            "TEXT" -> {
                LabelElementType.TextElement(
                    text = properties["text"]?.jsonPrimitive?.content ?: "",
                    fontSize = properties["fontSize"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 12f,
                    fontFamily = properties["fontFamily"]?.jsonPrimitive?.content ?: "Arial",
                    alignment = when (properties["alignment"]?.jsonPrimitive?.content) {
                        "Center" -> androidx.compose.ui.text.style.TextAlign.Center
                        "End" -> androidx.compose.ui.text.style.TextAlign.End
                        else -> androidx.compose.ui.text.style.TextAlign.Start
                    },
                    color = properties["color"]?.jsonPrimitive?.content?.toIntOrNull() ?: android.graphics.Color.BLACK,
                    isBold = properties["isBold"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                    isItalic = properties["isItalic"]?.jsonPrimitive?.content?.toBoolean() ?: false
                )
            }
            "IMAGE" -> {
                LabelElementType.ImageElement(
                    imageUri = properties["imageUri"]?.jsonPrimitive?.content?.let { Uri.parse(it) },
                    imagePath = properties["imagePath"]?.jsonPrimitive?.content,
                    scaleType = when (properties["scaleType"]?.jsonPrimitive?.content) {
                        "CENTER_CROP" -> ImageScaleType.CENTER_CROP
                        "CENTER_INSIDE" -> ImageScaleType.CENTER_INSIDE
                        "FIT_XY" -> ImageScaleType.FIT_XY
                        else -> ImageScaleType.FIT_CENTER
                    }
                )
            }
            "QR_CODE" -> {
                LabelElementType.QRElement(
                    qrData = properties["qrData"]?.jsonPrimitive?.content ?: "",
                    size = properties["size"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 20f,
                    errorCorrectionLevel = when (properties["errorCorrectionLevel"]?.jsonPrimitive?.content) {
                        "LOW" -> QRErrorCorrectionLevel.LOW
                        "QUARTILE" -> QRErrorCorrectionLevel.QUARTILE
                        "HIGH" -> QRErrorCorrectionLevel.HIGH
                        else -> QRErrorCorrectionLevel.MEDIUM
                    }
                )
            }
            "BARCODE" -> {
                LabelElementType.BarcodeElement(
                    barcodeData = properties["barcodeData"]?.jsonPrimitive?.content ?: "",
                    barcodeType = when (properties["barcodeType"]?.jsonPrimitive?.content) {
                        "CODE39" -> BarcodeType.CODE39
                        "EAN13" -> BarcodeType.EAN13
                        "EAN8" -> BarcodeType.EAN8
                        "UPC_A" -> BarcodeType.UPC_A
                        "UPC_E" -> BarcodeType.UPC_E
                        "ITF" -> BarcodeType.ITF
                        else -> BarcodeType.CODE128
                    },
                    width = properties["width"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 50f,
                    height = properties["height"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 20f
                )
            }
            else -> {
                LabelElementType.TextElement(
                    text = "Unknown Element",
                    fontSize = 12f
                )
            }
        }
    }
}
