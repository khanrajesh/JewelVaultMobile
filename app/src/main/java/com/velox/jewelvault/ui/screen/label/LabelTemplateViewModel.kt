package com.velox.jewelvault.ui.screen.label

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.label.LabelElementEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import com.velox.jewelvault.utils.generateId
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class LabelTemplateViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    @Named("snackMessage") private val _snackBarState: MutableState<String>
) : ViewModel() {

    // All templates
    val templates: StateFlow<List<LabelTemplateEntity>> =
        appDatabase.labelTemplateDao().getAllTemplates().stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    /**
     * Create a new label template
     */
    fun createTemplate(template: LabelTemplateEntity) {
        viewModelScope.launch {
            try {
                appDatabase.labelTemplateDao().insertTemplate(template)
                _snackBarState.value = "Template created successfully"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to create template: ${e.message}"
            }
        }
    }

    /**
     * Update an existing label template
     */
    fun updateTemplate(template: LabelTemplateEntity) {
        viewModelScope.launch {
            try {
                val updatedTemplate = template.copy(modifiedAt = System.currentTimeMillis())
                appDatabase.labelTemplateDao().updateTemplate(updatedTemplate)
                _snackBarState.value = "Template updated successfully"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to update template: ${e.message}"
            }
        }
    }

    /**
     * Delete a label template and its elements
     */
    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                appDatabase.labelTemplateDao().deleteTemplateById(templateId)
                _snackBarState.value = "Template deleted successfully"
            } catch (e: Exception) {
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
            } catch (e: Exception) {
                _snackBarState.value = "Failed to set default template: ${e.message}"
            }
        }
    }

    /**
     * Save elements for a template
     */
    fun saveElements(templateId: String, elements: List<LabelElementEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appDatabase.withTransaction {
                    appDatabase.labelElementDao().deleteElementsByTemplateId(templateId)
                    if (elements.isNotEmpty()) {
                        appDatabase.labelElementDao().insertElements(elements)
                    }
                }
                _snackBarState.value = "Elements saved successfully"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to save elements: ${e.message}"
            }
        }
    }

    /**
     * Save elements without showing snack messages (for auto-save flows)
     */
    fun saveElementsSilent(templateId: String, elements: List<LabelElementEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appDatabase.withTransaction {
                    appDatabase.labelElementDao().deleteElementsByTemplateId(templateId)
                    if (elements.isNotEmpty()) {
                        appDatabase.labelElementDao().insertElements(elements)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Save a single element (silent)
     */
    fun upsertElement(element: LabelElementEntity) {
        viewModelScope.launch {
            try {
                appDatabase.labelElementDao().insertElement(element)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Delete elements for a template
     */
    fun deleteElementsByTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                appDatabase.labelElementDao().deleteElementsByTemplateId(templateId)
            } catch (e: Exception) {
                _snackBarState.value = "Failed to delete elements: ${e.message}"
            }
        }
    }

    /**
     * Get template with elements
     */
    suspend fun getTemplateWithElements(templateId: String): Triple<LabelTemplateEntity?, List<LabelElementEntity>, Exception?> {
        return try {
            val template = appDatabase.labelTemplateDao().getTemplateById(templateId)
            val elements = appDatabase.labelElementDao().getElementsByTemplateIdSync(templateId)
            Triple(template, elements, null)
        } catch (e: Exception) {
            Triple(null, emptyList(), e)
        }
    }
}


