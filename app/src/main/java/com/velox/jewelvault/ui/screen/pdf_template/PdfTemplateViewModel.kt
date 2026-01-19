package com.velox.jewelvault.ui.screen.pdf_template

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.velox.jewelvault.data.model.pdf.PdfTemplateStatus
import com.velox.jewelvault.data.model.pdf.PdfTemplateType
import com.velox.jewelvault.data.model.pdf.PdfTemplateValidation
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.pdf.PdfElementEntity
import com.velox.jewelvault.data.roomdb.entity.pdf.PdfTemplateEntity
import com.velox.jewelvault.utils.PdfUtils
import com.velox.jewelvault.utils.generateId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PdfTemplateViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState

    val templates: StateFlow<List<PdfTemplateEntity>> =
        appDatabase.pdfTemplateDao().getAllTemplates().stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    private var defaultsEnsured = false

    fun ensureDefaults() {
        if (defaultsEnsured) return
        defaultsEnsured = true
        viewModelScope.launch(Dispatchers.IO) {
            PdfTemplateType.all.forEach { type ->
                val existingDefault =
                    appDatabase.pdfTemplateDao().getDefaultTemplateByTypeSync(type)
                val existingSystemDefault =
                    appDatabase.pdfTemplateDao().getSystemDefaultByTypeSync(type)
                if (existingSystemDefault == null) {
                    val systemTemplate = createSystemDefault(type, existingDefault == null)
                    appDatabase.withTransaction {
                        appDatabase.pdfTemplateDao().insertTemplate(systemTemplate)
                        val elements = createDefaultElements(
                            templateId = systemTemplate.templateId,
                            templateType = systemTemplate.templateType
                        )
                        if (elements.isNotEmpty()) {
                            appDatabase.pdfElementDao().insertElements(elements)
                        }
                    }
                } else if (existingDefault == null) {
                    appDatabase.pdfTemplateDao().clearDefaultForType(type)
                    appDatabase.pdfTemplateDao().setDefaultTemplate(existingSystemDefault.templateId)
                }
            }
        }
    }

    suspend fun getTemplateWithElements(
        templateId: String
    ): Triple<PdfTemplateEntity?, List<PdfElementEntity>, Exception?> {
        return try {
            val template = appDatabase.pdfTemplateDao().getTemplateById(templateId)
            val elements = appDatabase.pdfElementDao().getElementsByTemplateIdSync(templateId)
            Triple(template, elements, null)
        } catch (e: Exception) {
            Triple(null, emptyList(), e)
        }
    }

    fun updateTemplate(template: PdfTemplateEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = template.copy(modifiedAt = System.currentTimeMillis())
                appDatabase.pdfTemplateDao().updateTemplate(updated)
                _snackBarState.value = "Template saved"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to save template: ${e.message}"
            }
        }
    }

    fun saveElements(templateId: String, elements: List<PdfElementEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appDatabase.withTransaction {
                    appDatabase.pdfElementDao().deleteElementsByTemplateId(templateId)
                    if (elements.isNotEmpty()) {
                        appDatabase.pdfElementDao().insertElements(elements)
                    }
                }
                _snackBarState.value = "Elements saved"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to save elements: ${e.message}"
            }
        }
    }

    fun saveDraft(template: PdfTemplateEntity, elements: List<PdfElementEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = template.copy(
                    status = PdfTemplateStatus.DRAFT,
                    modifiedAt = System.currentTimeMillis(),
                    publishedAt = null
                )
                appDatabase.withTransaction {
                    appDatabase.pdfTemplateDao().updateTemplate(updated)
                    appDatabase.pdfElementDao().deleteElementsByTemplateId(template.templateId)
                    if (elements.isNotEmpty()) {
                        appDatabase.pdfElementDao().insertElements(elements)
                    }
                }
                _snackBarState.value = "Draft saved"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to save draft: ${e.message}"
            }
        }
    }

    fun saveAndPublish(template: PdfTemplateEntity, elements: List<PdfElementEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (template.isDefault || template.isSystemDefault) {
                    _snackBarState.value = "Default templates cannot be published directly"
                    return@launch
                }
                val missing = PdfTemplateValidation.missingBindings(template.templateType, elements)
                if (missing.isNotEmpty()) {
                    _snackBarState.value = "Missing fields: ${missing.joinToString()}"
                    return@launch
                }
                val now = System.currentTimeMillis()
                val updated = template.copy(
                    status = PdfTemplateStatus.PUBLISHED,
                    modifiedAt = now,
                    publishedAt = now
                )
                appDatabase.withTransaction {
                    appDatabase.pdfTemplateDao().updateTemplate(updated)
                    appDatabase.pdfElementDao().deleteElementsByTemplateId(template.templateId)
                    if (elements.isNotEmpty()) {
                        appDatabase.pdfElementDao().insertElements(elements)
                    }
                }
                _snackBarState.value = "Template published"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to publish template: ${e.message}"
            }
        }
    }

    fun createTemplate(
        templateType: String,
        templateName: String,
        startFromDefault: Boolean,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val baseTemplate = if (startFromDefault) {
                    appDatabase.pdfTemplateDao().getDefaultTemplateByTypeSync(templateType)
                } else {
                    null
                }
                val templateId = generateId()
                val resolvedName =
                    templateName.ifBlank { "${PdfTemplateType.displayName(templateType)} Draft" }
                val newTemplate = if (baseTemplate != null) {
                    baseTemplate.copy(
                        templateId = templateId,
                        templateName = resolvedName,
                        status = PdfTemplateStatus.DRAFT,
                        isDefault = false,
                        isSystemDefault = false,
                        baseTemplateId = baseTemplate.templateId,
                        createdAt = now,
                        modifiedAt = now,
                        publishedAt = null
                    )
                } else {
                    PdfTemplateEntity(
                        templateId = templateId,
                        templateName = resolvedName,
                        templateType = templateType,
                        pageWidth = PdfUtils.A4_PORTRAIT.first.toFloat(),
                        pageHeight = PdfUtils.A4_PORTRAIT.second.toFloat(),
                        orientation = "PORTRAIT",
                        marginLeft = 16f,
                        marginTop = 16f,
                        marginRight = 16f,
                        marginBottom = 16f,
                        status = PdfTemplateStatus.DRAFT,
                        isDefault = false,
                        isSystemDefault = false,
                        baseTemplateId = null,
                        createdAt = now,
                        modifiedAt = now,
                        publishedAt = null,
                        description = null
                    )
                }

                val baseElements = if (baseTemplate != null) {
                    appDatabase.pdfElementDao().getElementsByTemplateIdSync(baseTemplate.templateId)
                } else {
                    emptyList()
                }

                appDatabase.withTransaction {
                    appDatabase.pdfTemplateDao().insertTemplate(newTemplate)
                    if (baseElements.isNotEmpty()) {
                        val copiedElements = baseElements.map { element ->
                            element.copy(
                                elementId = generateId(),
                                templateId = templateId
                            )
                        }
                        appDatabase.pdfElementDao().insertElements(copiedElements)
                    }
                }

                val message = if (startFromDefault && baseTemplate == null) {
                    "Default template not found, created a blank template"
                } else {
                    "Template created"
                }
                _snackBarState.value = message
                viewModelScope.launch(Dispatchers.Main) {
                    onCreated(templateId)
                }
            } catch (e: Exception) {
                _snackBarState.value = "Failed to create template: ${e.message}"
            }
        }
    }

    fun publishTemplate(templateId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val template = appDatabase.pdfTemplateDao().getTemplateById(templateId)
                if (template == null) {
                    _snackBarState.value = "Template not found"
                    return@launch
                }
                if (template.isDefault || template.isSystemDefault) {
                    _snackBarState.value = "Default templates cannot be published directly"
                    return@launch
                }
                if (template.status == PdfTemplateStatus.PUBLISHED) {
                    _snackBarState.value = "Template is already published"
                    return@launch
                }
                val elements = appDatabase.pdfElementDao().getElementsByTemplateIdSync(templateId)
                val missing = PdfTemplateValidation.missingBindings(template.templateType, elements)
                if (missing.isNotEmpty()) {
                    _snackBarState.value = "Missing fields: ${missing.joinToString()}"
                    return@launch
                }
                val updated = template.copy(
                    status = PdfTemplateStatus.PUBLISHED,
                    modifiedAt = System.currentTimeMillis(),
                    publishedAt = System.currentTimeMillis()
                )
                appDatabase.pdfTemplateDao().updateTemplate(updated)
                _snackBarState.value = "Template published"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to publish template: ${e.message}"
            }
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val template = appDatabase.pdfTemplateDao().getTemplateById(templateId)
                if (template == null) {
                    _snackBarState.value = "Template not found"
                    return@launch
                }
                if (template.isDefault || template.isSystemDefault) {
                    _snackBarState.value = "Default templates cannot be deleted"
                    return@launch
                }
                appDatabase.withTransaction {
                    appDatabase.pdfElementDao().deleteElementsByTemplateId(templateId)
                    appDatabase.pdfTemplateDao().deleteTemplateById(templateId)
                }
                _snackBarState.value = "Template deleted"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to delete template: ${e.message}"
            }
        }
    }

    fun setDefaultTemplate(templateId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val template = appDatabase.pdfTemplateDao().getTemplateById(templateId)
                if (template == null) {
                    _snackBarState.value = "Template not found"
                    return@launch
                }
                if (template.status != PdfTemplateStatus.PUBLISHED) {
                    _snackBarState.value = "Publish the template before setting default"
                    return@launch
                }
                appDatabase.withTransaction {
                    appDatabase.pdfTemplateDao().clearDefaultForType(template.templateType)
                    appDatabase.pdfTemplateDao().setDefaultTemplate(templateId)
                }
                _snackBarState.value = "Default template updated"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to set default: ${e.message}"
            }
        }
    }

    private fun createSystemDefault(
        templateType: String,
        isDefault: Boolean
    ): PdfTemplateEntity {
        val now = System.currentTimeMillis()
        return PdfTemplateEntity(
            templateId = generateId(),
            templateName = "${PdfTemplateType.displayName(templateType)} Default",
            templateType = templateType,
            pageWidth = PdfUtils.A4_PORTRAIT.first.toFloat(),
            pageHeight = PdfUtils.A4_PORTRAIT.second.toFloat(),
            orientation = "PORTRAIT",
            marginLeft = 16f,
            marginTop = 16f,
            marginRight = 16f,
            marginBottom = 16f,
            status = PdfTemplateStatus.PUBLISHED,
            isDefault = isDefault,
            isSystemDefault = true,
            baseTemplateId = null,
            createdAt = now,
            modifiedAt = now,
            publishedAt = now,
            description = "System default template"
        )
    }

    private fun createDefaultElements(
        templateId: String,
        templateType: String
    ): List<PdfElementEntity> {
        val required = PdfTemplateValidation.requiredBindings(templateType)
        if (required.isEmpty()) return emptyList()

        val itemBindings = required.filter { it.startsWith("items.") }
        val textBindings = required.filterNot { it.startsWith("items.") }
        val elements = mutableListOf<PdfElementEntity>()
        var y = 20f
        textBindings.forEach { binding ->
            val elementType = if (binding.endsWith("Signature")) "SIGNATURE" else "TEXT"
            elements.add(
                PdfElementEntity(
                    elementId = generateId(),
                    templateId = templateId,
                    elementType = elementType,
                    x = 20f,
                    y = y,
                    width = 260f,
                    height = 14f,
                    rotation = 0f,
                    zIndex = 1,
                    properties = "{}",
                    dataBinding = binding,
                    isVisible = true
                )
            )
            y += 16f
        }

        if (itemBindings.isNotEmpty()) {
            val columns = itemBindings.joinToString(separator = ",") { """{"binding":"$it"}""" }
            val tableProps = """{"columns":[${columns}]}"""
            elements.add(
                PdfElementEntity(
                    elementId = generateId(),
                    templateId = templateId,
                    elementType = "TABLE",
                    x = 20f,
                    y = y + 8f,
                    width = 500f,
                    height = 220f,
                    rotation = 0f,
                    zIndex = 1,
                    properties = tableProps,
                    dataBinding = "items",
                    isVisible = true
                )
            )
        }

        return elements
    }
}
