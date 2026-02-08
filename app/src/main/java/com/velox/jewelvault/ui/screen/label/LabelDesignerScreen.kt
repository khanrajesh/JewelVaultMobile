package com.velox.jewelvault.ui.screen.label

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Redo
import androidx.compose.material.icons.automirrored.twotone.RotateRight
import androidx.compose.material.icons.automirrored.twotone.Undo
import androidx.compose.material.icons.twotone.CheckBox
import androidx.compose.material.icons.twotone.CheckBoxOutlineBlank
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.DateRange
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.ExpandLess
import androidx.compose.material.icons.twotone.ExpandMore
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.LockOpen
import androidx.compose.material.icons.twotone.Print
import androidx.compose.material.icons.twotone.QrCode
import androidx.compose.material.icons.twotone.Remove
import androidx.compose.material.icons.twotone.Save
import androidx.compose.material.icons.twotone.SelectAll
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.entity.label.LabelElementEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.ui.components.WidthThenHeightSpacer
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.components.baseBackground10
import com.velox.jewelvault.ui.components.baseBackground9
import com.velox.jewelvault.ui.screen.bluetooth.ManagePrintersViewModel
import com.velox.jewelvault.utils.FileManager
import com.velox.jewelvault.utils.PrintUtils
import com.velox.jewelvault.utils.generateId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelDesignerScreen(
    templateId: String? = null,
    viewModel: LabelTemplateViewModel = hiltViewModel(),
    printerViewModel: ManagePrintersViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var template by remember { mutableStateOf<LabelTemplateEntity?>(null) }
    var elements by remember { mutableStateOf<List<LabelElementEntity>>(emptyList()) }
    var selectedElement by remember { mutableStateOf<LabelElementEntity?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var multiSelectEnabled by remember { mutableStateOf(false) }

    var undoStack by remember { mutableStateOf<List<List<LabelElementEntity>>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<List<LabelElementEntity>>>(emptyList()) }

    var inlineEditTarget by remember { mutableStateOf<String?>(null) }
    var inlineEditText by remember { mutableStateOf("") }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var showSetupDialog by remember { mutableStateOf(templateId.isNullOrEmpty()) }
    var setupTemplateName by remember { mutableStateOf("New Template") }
    var setupWidth by remember { mutableStateOf("50") }
    var setupHeight by remember { mutableStateOf("30") }
    var setupGapWidth by remember { mutableStateOf("2") }
    var setupGapHeight by remember { mutableStateOf("2") }
    var setupDensity by remember { mutableStateOf("6") }
    var setupSpeed by remember { mutableStateOf("2") }
    var setupDirection by remember { mutableStateOf("1") }
    var setupReferenceX by remember { mutableStateOf("0") }
    var setupReferenceY by remember { mutableStateOf("0") }
    var setupLanguage by remember { mutableStateOf("TSPL") }
    var setupPadding by remember { mutableStateOf("1.5") }

    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    var pendingElementType by remember { mutableStateOf<String?>(null) }
    var pendingElementBinding by remember { mutableStateOf("") }
    var pendingElementText by remember { mutableStateOf("") }

    fun clearInlineEdit() {
        inlineEditTarget = null
        inlineEditText = ""
    }

    LaunchedEffect(templateId) {
        if (!templateId.isNullOrEmpty()) {
            val result = viewModel.getTemplateWithElements(templateId)
            if (result.third == null) {
                template = result.first
                result.first?.let { tplEntity ->
                    elements = result.second.map { clampElementToTemplate(it, tplEntity) }
                } ?: run { elements = result.second }
                templateName = result.first?.templateName.orEmpty()
                showSetupDialog = false
            }
        }
    }

    fun createTemplateFromSetup() {
        val width = setupWidth.toFloatOrNull() ?: 50f
        val height = setupHeight.toFloatOrNull() ?: 30f
        val gapW = setupGapWidth.toFloatOrNull() ?: 0f
        val gapH = setupGapHeight.toFloatOrNull() ?: 2f
        val density = setupDensity.toIntOrNull()?.coerceIn(1, 15) ?: 6
        val speed = setupSpeed.toIntOrNull()?.coerceIn(1, 10) ?: 2
        val direction = setupDirection.toIntOrNull()?.coerceIn(0, 1) ?: 1
        val refX = setupReferenceX.toFloatOrNull() ?: 0f
        val refY = setupReferenceY.toFloatOrNull() ?: 0f
        val paddingMax = (minOf(width, height) / 2f).coerceAtLeast(0f)
        val padding = setupPadding.toFloatOrNull()?.coerceIn(0f, paddingMax) ?: 1.5f
        val language = setupLanguage.uppercase()
        val now = System.currentTimeMillis()
        val newTemplate = LabelTemplateEntity(
            templateId = generateId(),
            templateName = setupTemplateName.ifBlank { "New Template" },
            templateType = "ITEM_LABEL",
            labelWidth = width,
            labelHeight = height,
            gapWidth = gapW,
            gapHeight = gapH,
            printDensity = density,
            printSpeed = speed,
            printDirection = direction,
            referenceX = refX,
            referenceY = refY,
            labelPadding = padding,
            orientation = "PORTRAIT",
            printLanguage = language,
            createdAt = now,
            modifiedAt = now,
            isDefault = false,
            description = null
        )
        template = newTemplate
        templateName = newTemplate.templateName
        viewModel.createTemplate(newTemplate)
        showSetupDialog = false
    }

    LaunchedEffect(elements, template?.templateId) {
        val tid = template?.templateId.orEmpty()
        if (tid.isNotEmpty()) {
            viewModel.saveElementsSilent(tid, elements)
        }
    }

    if (showSetupDialog) {
        TemplateSetupDialog(
            templateName = setupTemplateName,
            width = setupWidth,
            height = setupHeight,
            gapWidth = setupGapWidth,
            gapHeight = setupGapHeight,
            density = setupDensity,
            speed = setupSpeed,
            direction = setupDirection,
            referenceX = setupReferenceX,
            referenceY = setupReferenceY,
            padding = setupPadding,
            language = setupLanguage,
            onTemplateNameChange = { setupTemplateName = it },
            onWidthChange = { setupWidth = it },
            onHeightChange = { setupHeight = it },
            onGapWidthChange = { setupGapWidth = it },
            onGapHeightChange = { setupGapHeight = it },
            onDensityChange = { setupDensity = it },
            onSpeedChange = { setupSpeed = it },
            onDirectionChange = { setupDirection = it },
            onReferenceXChange = { setupReferenceX = it },
            onReferenceYChange = { setupReferenceY = it },
            onPaddingChange = { setupPadding = it },
            onLanguageChange = { setupLanguage = it },
            onDismiss = {
                setupTemplateName = "New Template"
                setupWidth = "50"
                setupHeight = "30"
                setupGapWidth = "2"
                setupGapHeight = "2"
                setupDensity = "6"
                setupSpeed = "2"
                setupDirection = "1"
                setupReferenceX = "0"
                setupReferenceY = "0"
                setupLanguage = "TSPL"
                setupPadding = "1.5"
                createTemplateFromSetup()
            },
            onConfirm = { createTemplateFromSetup() })
    }

    val tpl = template
    if (tpl == null) {
        if (!showSetupDialog) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    fun recordSnapshot() {
        undoStack = (undoStack + listOf(elements)).takeLast(50)
        redoStack = emptyList()
    }

    fun updateSelection(newSelection: Set<String>) {
        selectedIds = newSelection
        selectedElement = elements.firstOrNull { newSelection.contains(it.elementId) }
    }

    fun addElementWithBinding(
        elementType: String, binding: String?, fallbackText: String?
    ) {
        recordSnapshot()
        val base = clampElementToTemplate(
            createElement(elementType, elements.size, tpl), tpl
        )
        val props = if (elementType.equals("TEXT", true) && fallbackText != null) {
            setTextInProperties(base.properties, fallbackText)
        } else {
            base.properties
        }
        val final = base.copy(
            properties = props, dataBinding = binding?.takeIf { it.isNotBlank() })
        elements = elements + final
        selectedElement = final
        updateSelection(setOf(final.elementId))
    }

    fun startAddElement(type: String) {
        val normalized = type.uppercase()
        if (shouldRequestBindingOnAdd(normalized)) {
            pendingElementType = normalized
            pendingElementBinding = defaultBindingForElement(normalized)
            pendingElementText = when (normalized) {
                "TEXT" -> parseTextFromProperties(
                    createElement(
                        normalized, elements.size, tpl
                    ).properties
                )

                else -> ""
            }
        } else {
            addElementWithBinding(normalized, null, null)
        }
    }

    fun applyUndo() {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + listOf(elements)
            val prev = undoStack.last()
            undoStack = undoStack.dropLast(1)
            elements = prev
            selectedIds = selectedIds.filter { id -> prev.any { it.elementId == id } }.toSet()
            selectedElement = prev.firstOrNull { it.elementId in selectedIds }
            clearInlineEdit()
        }
    }

    fun applyRedo() {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + listOf(elements)
            val next = redoStack.last()
            redoStack = redoStack.dropLast(1)
            elements = next
            selectedIds = selectedIds.filter { id -> next.any { it.elementId == id } }.toSet()
            selectedElement = next.firstOrNull { it.elementId in selectedIds }
            clearInlineEdit()
        }
    }

    val selectedElements = elements.filter { selectedIds.contains(it.elementId) }
    val anySelectedLocked = selectedElements.any { isLockedFromProperties(it.properties) }
    val allSelectedLocked =
        selectedElements.isNotEmpty() && selectedElements.all { isLockedFromProperties(it.properties) }

    fun deleteSelected() {
        if (selectedIds.isEmpty()) return
        recordSnapshot()
        elements = elements.filterNot { selectedIds.contains(it.elementId) }
        updateSelection(emptySet())
        clearInlineEdit()
    }

    fun duplicateSelected() {
        if (selectedElements.isEmpty()) return
        recordSnapshot()
        val newOnes = selectedElements.mapIndexed { index, el ->
            clampElementToTemplate(
                el.copy(
                    elementId = generateId(),
                    x = el.x + 3f + (index * 1f),
                    y = el.y + 3f + (index * 1f)
                ), tpl
            )
        }
        elements = elements + newOnes
        updateSelection(newOnes.map { it.elementId }.toSet())
        clearInlineEdit()
    }

    fun rotateSelected() {
        if (selectedElements.isEmpty()) return
        recordSnapshot()
        elements = elements.map { el ->
            if (selectedIds.contains(el.elementId)) el.copy(rotation = (el.rotation + 90f) % 360f) else el
        }
    }

    fun toggleLockSelected() {
        if (selectedElements.isEmpty()) return
        recordSnapshot()
        val lockTarget = !allSelectedLocked
        elements = elements.map { el ->
            if (!selectedIds.contains(el.elementId)) return@map el
            el.copy(properties = setLockedInProperties(el.properties, lockTarget))
        }
    }

    val saveTemplate: () -> Unit = {
        scope.launch {
            val updated = tpl.copy(
                templateName = templateName.ifBlank { tpl.templateName },
                modifiedAt = System.currentTimeMillis()
            )
            template = updated
            viewModel.updateTemplate(updated)
            viewModel.saveElementsSilent(updated.templateId, elements)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .baseBackground9()
            .padding(4.dp),
    ) {
        TemplateHeader(
            templateName = templateName,
            onTemplateNameChange = { templateName = it },
            template = tpl,
            onOpenSettings = { showSettingsDialog = true },
            onTestPrint = {
                template?.let {
                    printerViewModel.printLabelTemplate(
                        context = context, template = it, elements = elements, testOverlay = true
                    )
                }
            },
            onSave = saveTemplate
        )

        QuickActionsBar(
            multiSelectEnabled = multiSelectEnabled,
            selectedCount = selectedIds.size,
            anyLocked = anySelectedLocked,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            onToggleMultiSelect = {
                multiSelectEnabled = !multiSelectEnabled
                if (!multiSelectEnabled && selectedIds.size > 1) {
                    updateSelection(selectedIds.take(1).toSet())
                }
            },
            onSelectAll = {
                multiSelectEnabled = true
                updateSelection(elements.map { it.elementId }.toSet())
            },
            onDuplicate = ::duplicateSelected,
            onRotate = ::rotateSelected,
            onLockToggle = ::toggleLockSelected,
            onDelete = ::deleteSelected,
            onUndo = ::applyUndo,
            onRedo = ::applyRedo
        )

        RowOrColumn(
            rowModifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElementPalette(
                modifier = Modifier, onAddElement = { type ->
                    clearInlineEdit()
                    startAddElement(type)
                })
            if (it) Spacer(Modifier.weight(1f)) else WidthThenHeightSpacer()

            ZoomControls(
                zoom = zoom,
                onZoomIn = { zoom = (zoom * 1.1f).coerceAtMost(5f) },
                onZoomOut = { zoom = (zoom / 1.1f).coerceAtLeast(0.5f) })
        }

        RowOrColumn(
            rowModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .baseBackground10(),
            columnModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .baseBackground10(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {

            val modifier = if (it) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()

            LabelCanvas(
                modifier = modifier
                    .weight(3f)
                    .padding(12.dp),
                template = tpl,
                elements = elements,
                selectedIds = selectedIds,
                inlineEditTarget = inlineEditTarget,
                inlineEditText = inlineEditText,
                zoom = zoom,
                pan = pan,
                onTransform = { scaleChange, panChange ->
                    zoom = (zoom * scaleChange).coerceIn(0.5f, 5f)
                    pan = pan + panChange
                },
                onElementClick = { element ->
                    clearInlineEdit()
                    if (multiSelectEnabled) {
                        val newSelection = if (selectedIds.contains(element.elementId)) {
                            selectedIds - element.elementId
                        } else {
                            selectedIds + element.elementId
                        }
                        updateSelection(newSelection)
                    } else {
                        updateSelection(setOf(element.elementId))
                    }
                },
                onElementDragStart = { recordSnapshot() },
                onElementDragEnd = { clearInlineEdit() },
                onElementDrag = { elementId, dxMm, dyMm ->
                    val dragged = elements.find { it.elementId == elementId } ?: return@LabelCanvas
                    if (isLockedFromProperties(dragged.properties)) return@LabelCanvas
                    val updated = elements.map {
                        if (it.elementId == elementId) {
                            clampElementToTemplate(
                                it.copy(
                                    x = it.x + dxMm, y = it.y + dyMm
                                ), tpl
                            )
                        } else it
                    }
                    elements = updated
                    selectedElement = updated.find { it.elementId == elementId }
                },
                onElementResizeStart = { recordSnapshot() },
                onElementResizeEnd = { clearInlineEdit() },
                onElementResize = { elementId, corner, dxMm, dyMm ->
                    val resizing = elements.find { it.elementId == elementId } ?: return@LabelCanvas
                    if (isLockedFromProperties(resizing.properties)) return@LabelCanvas
                    val updated = elements.map {
                        if (it.elementId != elementId) return@map it
                        val minSize = if (it.elementType.equals("LINE", true)) 0.2f else 2f
                        var newX = it.x
                        var newY = it.y
                        var newW = it.width
                        var newH = it.height
                        val keepSquare =
                            it.elementType.equals("QR_CODE", true) || it.elementType.equals(
                                "IMAGE", true
                            )
                        when (corner) {
                            "TopStart" -> {
                                newX += dxMm; newY += dyMm; newW -= dxMm; newH -= dyMm
                            }

                            "TopEnd" -> {
                                newY += dyMm; newW += dxMm; newH -= dyMm
                            }

                            "BottomStart" -> {
                                newX += dxMm; newW -= dxMm; newH += dyMm
                            }

                            else -> {
                                newW += dxMm; newH += dyMm
                            }
                        }
                        if (newW < minSize) newW = minSize
                        if (newH < minSize) newH = minSize
                        if (keepSquare) {
                            val anchorX: Float
                            val anchorY: Float
                            val size = maxOf(minOf(newW, newH), minSize)
                            when (corner) {
                                "TopStart" -> {
                                    anchorX = newX + newW
                                    anchorY = newY + newH
                                    newX = anchorX - size
                                    newY = anchorY - size
                                    newW = size
                                    newH = size
                                }

                                "TopEnd" -> {
                                    anchorX = newX
                                    anchorY = newY + newH
                                    newX = anchorX
                                    newY = anchorY - size
                                    newW = size
                                    newH = size
                                }

                                "BottomStart" -> {
                                    anchorX = newX + newW
                                    anchorY = newY
                                    newX = anchorX - size
                                    newY = anchorY
                                    newW = size
                                    newH = size
                                }

                                else -> { // BottomEnd
                                    newW = size
                                    newH = size
                                }
                            }
                        }
                        clampElementToTemplate(
                            it.copy(x = newX, y = newY, width = newW, height = newH), tpl
                        )
                    }
                    elements = updated
                    selectedElement = updated.find { it.elementId == elementId }
                },
                onRequestInlineEdit = { element, text ->
                    inlineEditTarget = element.elementId
                    inlineEditText = text
                    updateSelection(setOf(element.elementId))
                },
                onInlineEditChange = { inlineEditText = it },
                onInlineEditCommit = { element, newText ->
                    recordSnapshot()
                    val updatedProps = setTextInProperties(element.properties, newText)
                    elements = elements.map {
                        if (it.elementId == element.elementId) it.copy(properties = updatedProps) else it
                    }
                    clearInlineEdit()
                },
                onInlineEditCancel = { clearInlineEdit() })

            PropertiesPanel(
                modifier = modifier.weight(1f),
                template = tpl,
                selectedElement = selectedElement,
                onBeginEdit = { recordSnapshot() },
                onUpdateElement = { updated ->
                    val clamped = clampElementToTemplate(updated, tpl)
                    elements =
                        elements.map { if (it.elementId == clamped.elementId) clamped else it }
                    selectedElement = clamped
                    updateSelection(setOf(clamped.elementId))
                },
                onDeleteElement = { toDelete ->
                    recordSnapshot()
                    elements = elements.filter { it.elementId != toDelete.elementId }
                    if (selectedElement?.elementId == toDelete.elementId) selectedElement = null
                    updateSelection(emptySet())
                    clearInlineEdit()
                })
        }
    }


    pendingElementType?.let { pendingType ->
        BindingPickerDialog(
            elementType = pendingType,
            binding = pendingElementBinding,
            fallbackText = pendingElementText,
            showTextField = pendingType.equals("TEXT", true),
            bindingOptions = bindingOptionsForElement(pendingType),
            onBindingChange = { pendingElementBinding = it },
            onTextChange = { pendingElementText = it },
            onDismiss = { pendingElementType = null },
            onConfirm = {
                addElementWithBinding(
                    pendingType,
                    pendingElementBinding,
                    pendingElementText.takeIf { pendingType.equals("TEXT", true) })
                pendingElementType = null
            })
    }

    if (showSettingsDialog) {
        TemplateSettingsDialog(
            template = tpl,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated ->
                template = updated
                val clamped = elements.map { clampElementToTemplate(it, updated) }
                elements = clamped
                selectedIds =
                    selectedIds.filter { id -> clamped.any { it.elementId == id } }.toSet()
                selectedElement = clamped.firstOrNull { it.elementId == selectedElement?.elementId }
                viewModel.updateTemplate(updated)
                viewModel.saveElementsSilent(updated.templateId, clamped)
                showSettingsDialog = false
            })
    }
}

@Composable
private fun TemplateHeader(
    templateName: String,
    onTemplateNameChange: (String) -> Unit,
    template: LabelTemplateEntity,
    onOpenSettings: () -> Unit,
    onTestPrint: () -> Unit,
    onSave: () -> Unit
) {
    val templateNameState = remember(templateName) { InputFieldState(templateName) }
    RowOrColumn {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onSave, modifier = Modifier) {
                Icon(
                    Icons.TwoTone.Save, contentDescription = null, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }

            Button(onClick = onTestPrint) {
                Icon(
                    Icons.TwoTone.Print, contentDescription = null, modifier = Modifier.size(16.dp)
                )
            }

            Icon(
                Icons.TwoTone.Settings,
                contentDescription = null,
                modifier = Modifier
                    .bounceClick { onOpenSettings() }
                    .size(30.dp))

            CusOutlinedTextField(
                state = templateNameState,
                onTextChange = onTemplateNameChange,
                placeholderText = "Template name",
                modifier = Modifier,
                singleLine = true
            )

            MetricChip("Size", "${template.labelWidth} / ${template.labelHeight} mm")
            MetricChip("Gap", "${template.gapWidth} / ${template.gapHeight} mm")
            MetricChip("Padding", "${template.labelPadding} mm")
            MetricChip("Density", "${template.printDensity}")
            MetricChip("Speed", "${template.printSpeed}")
            MetricChip("Direction", if (template.printDirection == 0) "Normal" else "Reverse")
            MetricChip("Offset", "${template.referenceX} / ${template.referenceY} mm")
            MetricChip("Language", template.printLanguage)

        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .border(
                1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuickActionsBar(
    modifier: Modifier = Modifier,
    multiSelectEnabled: Boolean,
    selectedCount: Int,
    anyLocked: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onToggleMultiSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onDuplicate: () -> Unit,
    onRotate: () -> Unit,
    onLockToggle: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionChipButton(
            icon = if (multiSelectEnabled) Icons.TwoTone.CheckBox else Icons.TwoTone.CheckBoxOutlineBlank,
            label = if (multiSelectEnabled) "Multi" else "Single",
            onClick = onToggleMultiSelect
        )
        ActionChipButton(
            icon = Icons.TwoTone.SelectAll,
            label = "Select all",
            onClick = onSelectAll,
            enabled = multiSelectEnabled
        )
        ActionChipButton(
            icon = Icons.TwoTone.ContentCopy,
            label = "Duplicate",
            onClick = onDuplicate,
            enabled = selectedCount > 0
        )
        ActionChipButton(
            icon = Icons.AutoMirrored.TwoTone.RotateRight,
            label = "Rotate 90�",
            onClick = onRotate,
            enabled = selectedCount > 0
        )
        ActionChipButton(
            icon = if (anyLocked) Icons.TwoTone.LockOpen else Icons.TwoTone.Lock,
            label = if (anyLocked) "Unlock" else "Lock",
            onClick = onLockToggle,
            enabled = selectedCount > 0
        )
        ActionChipButton(
            icon = Icons.TwoTone.Delete,
            label = "Delete",
            onClick = onDelete,
            enabled = selectedCount > 0
        )
        ActionChipButton(
            icon = Icons.AutoMirrored.TwoTone.Undo,
            label = "Undo",
            onClick = onUndo,
            enabled = canUndo
        )
        ActionChipButton(
            icon = Icons.AutoMirrored.TwoTone.Redo,
            label = "Redo",
            onClick = onRedo,
            enabled = canRedo
        )
    }
}

@Composable
private fun ActionChipButton(
    icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = label) })
}


@Composable
fun ElementPalette(
    modifier: Modifier = Modifier, onAddElement: (String) -> Unit
) {
    val context = LocalContext.current
    val elementTypes = listOf(
        Triple("TEXT", "Text", Icons.TwoTone.TextFields),
        Triple("IMAGE", "Logo", Icons.TwoTone.Image),
        Triple("QR_CODE", "QR", Icons.TwoTone.QrCode),
        Triple("BARCODE", "Barcode", Icons.TwoTone.QrCode),
        Triple("DATE", "Date", Icons.TwoTone.DateRange),
        Triple("LINE", "Line", Icons.TwoTone.Remove)
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        elementTypes.forEach { (type, label, icon) ->
            AssistChip(onClick = {
                if (type != "IMAGE") {
                    onAddElement(type)
                } else {
                    Toast.makeText(context, "Feature not available yet!", Toast.LENGTH_SHORT).show()
                }
            }, label = { Text(label) }, leadingIcon = { Icon(icon, contentDescription = label) })
        }
    }
}

@Composable
private fun ZoomControls(
    zoom: Float, onZoomIn: () -> Unit, onZoomOut: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "-",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onZoomOut() })
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Zoom: ${(zoom * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "+",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onZoomIn() })
    }
}

@Composable
fun LabelCanvas(
    modifier: Modifier = Modifier,
    template: LabelTemplateEntity,
    elements: List<LabelElementEntity>,
    selectedIds: Set<String>,
    inlineEditTarget: String?,
    inlineEditText: String,
    zoom: Float,
    pan: Offset,
    onTransform: (Float, Offset) -> Unit,
    onElementClick: (LabelElementEntity) -> Unit,
    onElementDragStart: () -> Unit,
    onElementDragEnd: () -> Unit,
    onElementDrag: (String, Float, Float) -> Unit,
    onElementResizeStart: () -> Unit,
    onElementResizeEnd: () -> Unit,
    onElementResize: (String, String, Float, Float) -> Unit,
    onRequestInlineEdit: (LabelElementEntity, String) -> Unit,
    onInlineEditChange: (String) -> Unit,
    onInlineEditCommit: (LabelElementEntity, String) -> Unit,
    onInlineEditCancel: () -> Unit,
) {
    val density = LocalDensity.current
    val dpi = density.density * 160f
    val pxPerMm = dpi / 25.4f
    val mmToPx: (Float) -> Float = { it * pxPerMm }

    fun mmToDp(mm: Float): androidx.compose.ui.unit.Dp {
        val px = mm * pxPerMm
        return with(density) { (px / density.density).dp }
    }

    val contentPadding = mmToDp(template.labelPadding.coerceAtLeast(0f))

    val cardWidthDp = mmToDp(template.labelWidth)
    val cardHeightDp = mmToDp(template.labelHeight)
    val containerWidth = cardWidthDp
    val containerHeight = cardHeightDp
    val scaleThickness = 30.dp

    val rulerColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    val guideColor = MaterialTheme.colorScheme.primary
    val topRulerPaint = remember(density) { rulerPaint(density, rulerColor.toArgb()) }
    val leftRulerPaint = remember(density) { rulerPaint(density, rulerColor.toArgb()) }
    val primarySelection = elements.firstOrNull { selectedIds.contains(it.elementId) }

    Box(modifier = modifier) {
        Column(modifier = Modifier, horizontalAlignment = Alignment.Start) {
            // Top ruler aligned to label start with left spacer matching the left scale
            Row {
                Spacer(modifier = Modifier.width(scaleThickness))
                Box(
                    modifier = Modifier
                        .width(cardWidthDp)
                        .height(scaleThickness)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxMm = template.labelWidth
                        val tickColor = rulerColor
                        val paddingMm = template.labelPadding.coerceAtLeast(0f)
                        topRulerPaint.color = rulerColor.toArgb()
                        var idx = 0
                        while (idx.toFloat() <= maxMm + 0.001f) {
                            val mm = idx.toFloat()
                            // Move tick positions with pan/zoom but keep the ruler anchored
                            val x = (mmToPx(mm) * zoom) + pan.x
                            val isMajor = idx % 5 == 0
                            val tickHeight = size.height * 0.6f
                            val stroke = 1.5f
                            drawLine(
                                color = tickColor,
                                start = Offset(x, size.height),
                                end = Offset(x, tickHeight),
                                strokeWidth = stroke
                            )
                            if (isMajor) {
                                drawIntoCanvas {
                                    val label = mm.toInt().toString()
                                    it.nativeCanvas.drawText(
                                        label, x + 2f, size.height * 0.4f, topRulerPaint
                                    )
                                }
                            }
                            idx++
                        }

                        primarySelection?.let { sel ->
                            val startX = (mmToPx(sel.x + paddingMm) * zoom) + pan.x
                            val endX = (mmToPx(sel.x + sel.width + paddingMm) * zoom) + pan.x
                            drawLine(
                                color = guideColor,
                                start = Offset(startX, size.height),
                                end = Offset(startX, 0f),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = guideColor,
                                start = Offset(endX, size.height),
                                end = Offset(endX, 0f),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Top, modifier = Modifier.height(cardHeightDp)
            ) {
                // Left ruler
                Box(
                    modifier = Modifier
                        .height(cardHeightDp)
                        .width(scaleThickness)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxMm = template.labelHeight
                        val tickColor = rulerColor
                        val paddingMm = template.labelPadding.coerceAtLeast(0f)
                        leftRulerPaint.color = rulerColor.toArgb()
                        var idx = 0
                        while (idx.toFloat() <= maxMm + 0.001f) {
                            val mm = idx.toFloat()
                            // Move tick positions with pan/zoom but keep the ruler anchored
                            val y = (mmToPx(mm) * zoom) + pan.y
                            val isMajor = idx % 5 == 0
                            val tickWidthPx = size.width * 0.6f
                            val stroke = 1.5f
                            drawLine(
                                color = tickColor,
                                start = Offset(size.width, y),
                                end = Offset(tickWidthPx, y),
                                strokeWidth = stroke
                            )
                            if (isMajor) {
                                drawIntoCanvas {
                                    val label = mm.toInt().toString()
                                    it.nativeCanvas.drawText(
                                        label, 2f, y - 2f, leftRulerPaint
                                    )
                                }
                            }
                            idx++
                        }

                        primarySelection?.let { sel ->
                            val startY = (mmToPx(sel.y + paddingMm) * zoom) + pan.y
                            val endY = (mmToPx(sel.y + sel.height + paddingMm) * zoom) + pan.y
                            drawLine(
                                color = guideColor,
                                start = Offset(size.width, startY),
                                end = Offset(0f, startY),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = guideColor,
                                start = Offset(size.width, endY),
                                end = Offset(0f, endY),
                                strokeWidth = 2f
                            )
                        }
                    }
                }

                Box(modifier = Modifier.size(width = cardWidthDp, height = cardHeightDp)) {

                    Box(
                        modifier = Modifier
                            .size(width = containerWidth, height = containerHeight)
                            .graphicsLayer(
                                scaleX = zoom,
                                scaleY = zoom,
                                translationX = pan.x,
                                translationY = pan.y,
                                transformOrigin = TransformOrigin(0f, 0f)
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures(panZoomLock = true) { _, panChange, zoomChange, _ ->
                                    onTransform(zoomChange, panChange)
                                }
                            }) {


                        // Label card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.size(width = cardWidthDp, height = cardHeightDp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding)
                            ) {
                                // Guide dots for calibration (match test print overlay)
                                val guideInsetMm = 0.2f
                                val guideDotMm = 1.2f
                                val dotColor =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val insetPx = mmToPx(guideInsetMm)
                                    val dotRadius = (mmToPx(guideDotMm) / 2f).coerceAtLeast(1f)
                                    val leftX = insetPx + dotRadius
                                    val rightX = size.width - insetPx - dotRadius
                                    val topY = insetPx + dotRadius
                                    val bottomY = size.height - insetPx - dotRadius
                                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(leftX, topY))
                                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(rightX, topY))
                                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(leftX, bottomY))
                                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(rightX, bottomY))
                                }

                                elements.forEach { element ->
                                    val textProps = parseTextProps(element.properties)
                                    val resolvedContent =
                                        resolvePreviewContent(element, textProps.text)
                                    val displayText = when (element.elementType.uppercase()) {
                                        "TEXT" -> resolvedContent
                                        "QR_CODE" -> resolvedContent.ifBlank { "QR data" }
                                        "BARCODE" -> "BAR: ${resolvedContent}"
                                        "DATE" -> resolvedContent.ifBlank { "Date" }
                                        "LINE" -> "Line"
                                        else -> element.elementType
                                    }
                                    CanvasElement(
                                        element = element,
                                        resolvedContent = resolvedContent,
                                        displayText = displayText,
                                        textFontSize = textProps.fontSize,
                                        textBold = textProps.isBold,
                                        isSelected = selectedIds.contains(element.elementId),
                                        isLocked = isLockedFromProperties(element.properties),
                                        isInlineEditing = inlineEditTarget == element.elementId,
                                        inlineEditText = inlineEditText,
                                        onClick = { onElementClick(element) },
                                        onDragStart = onElementDragStart,
                                        onDragEnd = onElementDragEnd,
                                        onDragBy = { dxPx, dyPx ->
                                            // Pointer deltas are already in the element's local coords, so no need to scale by zoom
                                            val dxMm = dxPx / pxPerMm
                                            val dyMm = dyPx / pxPerMm
                                            onElementDrag(element.elementId, dxMm, dyMm)
                                        },
                                        mmToDp = ::mmToDp,
                                        pxToMm = { it / pxPerMm },
                                        onResizeBy = { corner, dxMm, dyMm ->
                                            onElementResize(
                                                element.elementId, corner, dxMm, dyMm
                                            )
                                        },
                                        onResizeStart = onElementResizeStart,
                                        onResizeEnd = onElementResizeEnd,
                                        onRequestInlineEdit = {
                                            if (element.elementType.equals("TEXT", true)) {
                                                onRequestInlineEdit(element, displayText)
                                            }
                                        },
                                        onInlineEditChange = onInlineEditChange,
                                        onInlineEditCommit = { text ->
                                            onInlineEditCommit(
                                                element, text
                                            )
                                        },
                                        onInlineEditCancel = onInlineEditCancel
                                    )
                                }


                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
fun CanvasElement(
    element: LabelElementEntity,
    resolvedContent: String,
    displayText: String,
    textFontSize: Float,
    textBold: Boolean,
    isSelected: Boolean,
    isLocked: Boolean,
    isInlineEditing: Boolean,
    inlineEditText: String,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragBy: (Float, Float) -> Unit,
    mmToDp: (Float) -> androidx.compose.ui.unit.Dp,
    pxToMm: (Float) -> Float,
    onResizeBy: (corner: String, dxMm: Float, dyMm: Float) -> Unit,
    onResizeStart: () -> Unit,
    onResizeEnd: () -> Unit,
    onRequestInlineEdit: () -> Unit,
    onInlineEditChange: (String) -> Unit,
    onInlineEditCommit: (String) -> Unit,
    onInlineEditCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var previewImage by remember(
        element.properties, element.dataBinding
    ) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(element.properties, element.dataBinding, resolvedContent) {
        previewImage = when {
            element.elementType.equals("IMAGE", true) -> {
                withContext(Dispatchers.IO) { loadPreviewImage(context, element) }?.asImageBitmap()
            }

            element.elementType.equals("QR_CODE", true) -> {
                val minSideMm = maxOf(min(element.width, element.height), 2f)
                val targetSizePx =
                    with(density) { mmToDp(minSideMm).toPx().toInt().coerceAtLeast(32) }
                withContext(Dispatchers.Default) {
                    if (resolvedContent.isNotBlank()) {
                        PrintUtils.generateQRCode(
                            resolvedContent, targetSizePx, margin = 0, trimWhitespace = true
                        )?.asImageBitmap()
                    } else null
                }
            }

            else -> null
        }
    }
    var last by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(isInlineEditing) {
        if (!isInlineEditing) {
            onInlineEditCancel()
        }
    }
    val dragModifier = if (isLocked) {
        Modifier
    } else {
        Modifier.pointerInput(element.elementId) {
            detectDragGestures(onDragStart = { pos ->
                last = pos
                onDragStart()
            }, onDragEnd = {
                last = null
                onDragEnd()
            }, onDragCancel = {
                last = null
                onDragEnd()
            }, onDrag = { change, _ ->
                val prev = last ?: change.previousPosition
                val dx = change.position.x - prev.x
                val dy = change.position.y - prev.y
                last = change.position
                onDragBy(dx, dy)
            })
        }
    }
    Box(
        modifier = Modifier
            .offset(x = mmToDp(element.x), y = mmToDp(element.y))
            .size(width = mmToDp(element.width), height = mmToDp(element.height))
            .background(
                color = if (isSelected) Color.Blue.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            )
            .then(dragModifier)
            .pointerInput("tap_${element.elementId}") {
                detectTapGestures(onDoubleTap = {
                    if (!isLocked && element.elementType.equals("TEXT", true)) {
                        onRequestInlineEdit()
                    }
                }, onTap = { onClick() })
            }, contentAlignment = Alignment.Center
    ) {
        val textStyle = MaterialTheme.typography.bodySmall.copy(
            fontSize = textFontSize.sp,
            fontWeight = if (textBold) FontWeight.Bold else FontWeight.Normal
        )
        if ((element.elementType.equals("IMAGE", true) || element.elementType.equals(
                "QR_CODE", true
            )) && previewImage != null && !isInlineEditing
        ) {
            Image(
                bitmap = previewImage!!,
                contentDescription = "Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else if (isInlineEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(6.dp)
            ) {
                BasicTextField(
                    value = inlineEditText,
                    onValueChange = { onInlineEditChange(it.replace("\n", " ")) },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                onInlineEditCommit(inlineEditText)
                            }
                        })
            }
        } else {
            Text(
                text = displayText.ifBlank { element.elementType }, style = textStyle
            )
        }

        if (isSelected && !isLocked) {
            val handleSize = 12.dp
            val handleColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(handleSize)
                    .align(Alignment.TopStart)
                    .background(handleColor, CircleShape)
                    .pointerInput("TL_${element.elementId}") {
                        detectDragGestures(
                            onDragStart = { onResizeStart() },
                            onDragEnd = { onResizeEnd() },
                            onDragCancel = { onResizeEnd() },
                            onDrag = { _, drag ->
                                onResizeBy(
                                    "TopStart", pxToMm(drag.x), pxToMm(drag.y)
                                )
                            })
                    })
            Box(
                modifier = Modifier
                    .size(handleSize)
                    .align(Alignment.TopEnd)
                    .background(handleColor, CircleShape)
                    .pointerInput("TR_${element.elementId}") {
                        detectDragGestures(
                            onDragStart = { onResizeStart() },
                            onDragEnd = { onResizeEnd() },
                            onDragCancel = { onResizeEnd() },
                            onDrag = { _, drag ->
                                onResizeBy(
                                    "TopEnd", pxToMm(drag.x), pxToMm(drag.y)
                                )
                            })
                    })
            Box(
                modifier = Modifier
                    .size(handleSize)
                    .align(Alignment.BottomStart)
                    .background(handleColor, CircleShape)
                    .pointerInput("BL_${element.elementId}") {
                        detectDragGestures(
                            onDragStart = { onResizeStart() },
                            onDragEnd = { onResizeEnd() },
                            onDragCancel = { onResizeEnd() },
                            onDrag = { _, drag ->
                                onResizeBy(
                                    "BottomStart", pxToMm(drag.x), pxToMm(drag.y)
                                )
                            })
                    })
            Box(
                modifier = Modifier
                    .size(handleSize)
                    .align(Alignment.BottomEnd)
                    .background(handleColor, CircleShape)
                    .pointerInput("BR_${element.elementId}") {
                        detectDragGestures(
                            onDragStart = { onResizeStart() },
                            onDragEnd = { onResizeEnd() },
                            onDragCancel = { onResizeEnd() },
                            onDrag = { _, drag ->
                                onResizeBy(
                                    "BottomEnd", pxToMm(drag.x), pxToMm(drag.y)
                                )
                            })
                    })
        }
    }
}

@Composable
fun PropertiesPanel(
    modifier: Modifier = Modifier,
    template: LabelTemplateEntity,
    selectedElement: LabelElementEntity?,
    onBeginEdit: () -> Unit,
    onUpdateElement: (LabelElementEntity) -> Unit,
    onDeleteElement: (LabelElementEntity) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedElement == null) {
                Text(
                    "Properties",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "No element selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ElementPropertiesForm(
                    element = selectedElement,
                    template = template,
                    onBeginEdit = onBeginEdit,
                    onUpdate = onUpdateElement,
                    onDelete = { onDeleteElement(selectedElement) })
            }
        }
    }
}

@Composable
fun ElementPropertiesForm(
    element: LabelElementEntity,
    template: LabelTemplateEntity,
    onBeginEdit: () -> Unit,
    onUpdate: (LabelElementEntity) -> Unit,
    onDelete: () -> Unit
) {
    var x by remember { mutableStateOf(element.x.toString()) }
    var y by remember { mutableStateOf(element.y.toString()) }
    var width by remember { mutableStateOf(element.width.toString()) }
    var height by remember { mutableStateOf(element.height.toString()) }
    val isTextElement = remember(element.elementType) { element.elementType.equals("TEXT", true) }
    val bindingSupported = remember(element.elementType) { supportsBinding(element.elementType) }
    val isImageElement = remember(element.elementType) { element.elementType.equals("IMAGE", true) }
    val bindingOptions =
        remember(element.elementType) { bindingOptionsForElement(element.elementType) }
    var textContent by remember(element.properties) { mutableStateOf(parseTextFromProperties(element.properties)) }
    var fontSize by remember(element.properties) { mutableStateOf(parseTextProps(element.properties).fontSize.toString()) }
    var isBold by remember(element.properties) { mutableStateOf(parseTextProps(element.properties).isBold) }
    var invertImage by remember(element.properties) {
        mutableStateOf(
            parseInvertFromProperties(
                element.properties
            )
        )
    }
    var imageThreshold by remember(element.properties) {
        mutableStateOf(
            parseImageThreshold(
                element.properties, element.dataBinding
            )
        )
    }
    var imageDitherMode by remember(element.properties) {
        mutableStateOf(
            parseImageDitherMode(
                element.properties
            )
        )
    }
    var imageMode by remember(element.properties) { mutableStateOf(parseImageMode(element.properties)) }
    var imageDpi by remember(element.properties) { mutableStateOf(parseImageDpi(element.properties)) }
    var dataBinding by remember(element.dataBinding) { mutableStateOf(element.dataBinding ?: "") }
    var bindingMenuExpanded by remember { mutableStateOf(false) }
    var snapshotTaken by remember(element.elementId) { mutableStateOf(false) }
    val fieldModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minWidth = 1.dp)
    val bindingLabelText = if (isImageElement) "Logo binding" else "Binding"
    LaunchedEffect(element.elementId, element.x, element.y, element.width, element.height) {
        x = element.x.toString()
        y = element.y.toString()
        width = element.width.toString()
        height = element.height.toString()
        val props = parseTextProps(element.properties)
        fontSize = props.fontSize.toString()
        isBold = props.isBold
        textContent = props.text
        invertImage = parseInvertFromProperties(element.properties)
        imageThreshold = parseImageThreshold(element.properties, element.dataBinding)
        imageDitherMode = parseImageDitherMode(element.properties)
        imageMode = parseImageMode(element.properties)
        imageDpi = parseImageDpi(element.properties)
        if (dataBinding.isBlank() && isImageElement) {
            dataBinding = defaultBindingForElement(element.elementType)
        }
        snapshotTaken = false
    }

    fun pushUpdate() {
        if (!snapshotTaken) {
            onBeginEdit()
            snapshotTaken = true
        }
        applyUpdate(
            element = element,
            template = template,
            x = x,
            y = y,
            width = width,
            height = height,
            textContent = textContent,
            fontSize = fontSize,
            isBold = isBold,
            imageThreshold = imageThreshold.takeIf { isImageElement },
            imageDitherMode = imageDitherMode.takeIf { isImageElement },
            imageMode = imageMode.takeIf { isImageElement },
            imageDpi = imageDpi.takeIf { isImageElement },
            invertImage = invertImage.takeIf { isImageElement },
            dataBinding = dataBinding.takeIf { bindingSupported },
            onUpdate = onUpdate
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Properties",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.TwoTone.Delete,
                contentDescription = "Delete Element",
                modifier = Modifier.bounceClick { onDelete() })

        }

        if (bindingSupported) {
            OutlinedTextField(
                value = bindingLabel(dataBinding),
                onValueChange = {},
                label = { Text(bindingLabelText) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { bindingMenuExpanded = !bindingMenuExpanded }) {
                        Icon(Icons.TwoTone.Settings, contentDescription = "Choose Binding")
                    }
                },
                modifier = fieldModifier
            )
            DropdownMenu(
                expanded = bindingMenuExpanded,
                onDismissRequest = { bindingMenuExpanded = false }) {
                bindingOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(bindingLabel(opt)) }, onClick = {
                        dataBinding = opt
                        pushUpdate()
                        bindingMenuExpanded = false
                    })
                }
            }
            if (isTextElement) {
                OutlinedTextField(
                    value = textContent, onValueChange = {
                    textContent = it
                    pushUpdate()
                }, label = { Text("Text (fallback/static)") }, modifier = fieldModifier
                )
                OutlinedTextField(
                    value = fontSize, onValueChange = {
                    fontSize = it
                    pushUpdate()
                }, label = { Text("Font size") }, modifier = fieldModifier, singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isBold, onCheckedChange = {
                            isBold = it
                            pushUpdate()
                        })
                        Text("Bold")
                    }
                }
            }
        }

        if (isImageElement) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = invertImage, onCheckedChange = {
                            invertImage = it
                            pushUpdate()
                        })
                    Text("Invert colors")
                }
                Text("Print mode", style = MaterialTheme.typography.bodySmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        "bitmap" to "Bitmap (BITMAP)", "download" to "Download (RAM + PUTBMP)"
                    ).forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RadioButton(
                                selected = imageMode == mode, onClick = {
                                    imageMode = mode
                                    pushUpdate()
                                })
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text("DPI", style = MaterialTheme.typography.bodySmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(203, 300).forEach { dpi ->
                        AssistChip(onClick = {
                            imageDpi = dpi
                            pushUpdate()
                        }, label = { Text("$dpi dpi") }, leadingIcon = {
                            Icon(
                                if (imageDpi == dpi) Icons.TwoTone.CheckBox else Icons.TwoTone.CheckBoxOutlineBlank,
                                contentDescription = null
                            )
                        })
                    }
                }
                Text(
                    "Threshold: $imageThreshold (higher = darker)",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = imageThreshold.toFloat(),
                    onValueChange = { imageThreshold = it.toInt().coerceIn(60, 220) },
                    onValueChangeFinished = {
                        pushUpdate()
                    },
                    valueRange = 60f..220f,
                    colors = SliderDefaults.colors()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Dither", style = MaterialTheme.typography.bodySmall)
                    listOf(
                        "floyd" to "Floyd–Steinberg", "ordered" to "Ordered 4x4", "none" to "None"
                    ).forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RadioButton(
                                selected = imageDitherMode == value, onClick = {
                                    imageDitherMode = value
                                    pushUpdate()
                                })
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = x, onValueChange = {
            x = it
            pushUpdate()
        }, label = { Text("X (mm)") }, modifier = fieldModifier, singleLine = true
        )
        OutlinedTextField(
            value = y, onValueChange = {
            y = it
            pushUpdate()
        }, label = { Text("Y (mm)") }, modifier = fieldModifier, singleLine = true
        )
        OutlinedTextField(
            value = width, onValueChange = {
            width = it
            pushUpdate()
        }, label = { Text("Width (mm)") }, modifier = fieldModifier, singleLine = true
        )
        OutlinedTextField(
            value = height, onValueChange = {
            height = it
            pushUpdate()
        }, label = { Text("Height (mm)") }, modifier = fieldModifier, singleLine = true
        )

    }
}


@Composable
private fun BindingPickerDialog(
    elementType: String,
    binding: String,
    fallbackText: String,
    showTextField: Boolean,
    bindingOptions: List<String>,
    onBindingChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = if (elementType.equals("IMAGE", true)) "Choose logo" else "Choose binding"
    val noneOptions = bindingOptions.filter { it.isBlank() }
    val itemOptions = bindingOptions.filter { it.startsWith("item.") }
    val storeOptions = bindingOptions.filter { it.startsWith("store.") }
    var itemExpanded by remember { mutableStateOf(false) }
    var storeExpanded by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            @Composable
            fun optionRow(option: String) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBindingChange(option) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = binding == option, onClick = { onBindingChange(option) })
                    Spacer(Modifier.width(8.dp))
                    Text(bindingLabel(option))
                }
            }

            @Composable
            fun sectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.TwoTone.ExpandLess else Icons.TwoTone.ExpandMore,
                        contentDescription = null
                    )
                    Text(title, style = MaterialTheme.typography.labelMedium)
                }
            }
            noneOptions.forEach { optionRow(it) }
            if (itemOptions.isNotEmpty()) {
                sectionHeader("Item", itemExpanded) { itemExpanded = !itemExpanded }
                if (itemExpanded) {
                    itemOptions.forEach { optionRow(it) }
                }
            }
            if (storeOptions.isNotEmpty()) {
                sectionHeader("Store", storeExpanded) { storeExpanded = !storeExpanded }
                if (storeExpanded) {
                    storeOptions.forEach { optionRow(it) }
                }
            }
            if (showTextField) {
                OutlinedTextField(
                    value = fallbackText,
                    onValueChange = onTextChange,
                    label = { Text("Static / fallback text") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }, confirmButton = {
        Button(onClick = onConfirm) { Text("Add") }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancel") }
    })
}

@Composable
private fun TemplateSetupDialog(
    templateName: String,
    width: String,
    height: String,
    gapWidth: String,
    gapHeight: String,
    density: String,
    speed: String,
    direction: String,
    referenceX: String,
    referenceY: String,
    padding: String,
    language: String,
    onTemplateNameChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onGapWidthChange: (String) -> Unit,
    onGapHeightChange: (String) -> Unit,
    onDensityChange: (String) -> Unit,
    onSpeedChange: (String) -> Unit,
    onDirectionChange: (String) -> Unit,
    onReferenceXChange: (String) -> Unit,
    onReferenceYChange: (String) -> Unit,
    onPaddingChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Label setup") }, text = {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            LabelSetupDiagram(
                padding = padding,
                gapWidth = gapWidth,
                gapHeight = gapHeight
            )
            OutlinedTextField(
                value = templateName,
                onValueChange = onTemplateNameChange,
                label = { Text("Template name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = width,
                    onValueChange = onWidthChange,
                    label = { Text("Width (mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = onHeightChange,
                    label = { Text("Height (mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = gapWidth,
                    onValueChange = onGapWidthChange,
                    label = { Text("Gap width (mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = gapHeight,
                    onValueChange = onGapHeightChange,
                    label = { Text("Gap height (mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = padding,
                onValueChange = onPaddingChange,
                label = { Text("Padding (mm)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "Applied to all sides. Default 1.5mm.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = density,
                    onValueChange = onDensityChange,
                    label = { Text("Density (1-15)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = speed,
                    onValueChange = onSpeedChange,
                    label = { Text("Speed (1-10)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Language", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TSPL", "CPCL", "PPLB", "ESC").forEach { lang ->
                        val selected = language.equals(lang, ignoreCase = true)
                        AssistChip(
                            onClick = { onLanguageChange(lang) },
                            label = { Text(lang) },
                            leadingIcon = {
                                Icon(
                                    if (selected) Icons.TwoTone.CheckBox else Icons.TwoTone.CheckBoxOutlineBlank,
                                    contentDescription = null
                                )
                            })
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Direction", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isNormalSelected = direction != "1"
                    AssistChip(
                        onClick = { onDirectionChange("0") },
                        label = { Text("0 � Normal") },
                        leadingIcon = {
                            Icon(
                                if (isNormalSelected) Icons.TwoTone.CheckBox else Icons.TwoTone.CheckBoxOutlineBlank,
                                contentDescription = null
                            )
                        })
                    val isReverseSelected = direction == "1"
                    AssistChip(
                        onClick = { onDirectionChange("1") },
                        label = { Text("1 � Reverse") },
                        leadingIcon = {
                            Icon(
                                if (isReverseSelected) Icons.TwoTone.CheckBox else Icons.TwoTone.CheckBoxOutlineBlank,
                                contentDescription = null
                            )
                        })
                }
                Text(
                    "Adjust direction only if the printer feeds backwards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = referenceX,
                    onValueChange = onReferenceXChange,
                    label = { Text("Start X offset (mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = referenceY,
                    onValueChange = onReferenceYChange,
                    label = { Text("Start Y offset (mm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Text(
                "Offsets move the printable origin to account for top/side spacing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }, confirmButton = {
        Button(onClick = onConfirm) { Text("Start designing") }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Use defaults") }
    })
}

@Composable
fun TemplateSettingsDialog(
    template: LabelTemplateEntity, onDismiss: () -> Unit, onSave: (LabelTemplateEntity) -> Unit
) {
    var width by remember { mutableStateOf(template.labelWidth.toString()) }
    var height by remember { mutableStateOf(template.labelHeight.toString()) }
    var gapWidth by remember { mutableStateOf(template.gapWidth.toString()) }
    var gapHeight by remember { mutableStateOf(template.gapHeight.toString()) }
    var density by remember { mutableStateOf(template.printDensity.toString()) }
    var speed by remember { mutableStateOf(template.printSpeed.toString()) }
    var direction by remember { mutableStateOf(template.printDirection.toString()) }
    var referenceX by remember { mutableStateOf(template.referenceX.toString()) }
    var referenceY by remember { mutableStateOf(template.referenceY.toString()) }
    var padding by remember { mutableStateOf(template.labelPadding.toString()) }
    var language by remember { mutableStateOf(template.printLanguage.uppercase()) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Template Settings") }, text = {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = width,
                onValueChange = { width = it },
                label = { Text("Width (mm)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Height (mm)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = gapWidth,
                onValueChange = { gapWidth = it },
                label = { Text("Gap Width (mm)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = gapHeight,
                onValueChange = { gapHeight = it },
                label = { Text("Gap Height (mm)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = padding,
                onValueChange = { padding = it },
                label = { Text("Padding (mm)") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Padding applies on all sides of the label.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = density,
                onValueChange = { density = it },
                label = { Text("Density (1-15)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = speed,
                onValueChange = { speed = it },
                label = { Text("Speed (1-10)") },
                modifier = Modifier.fillMaxWidth()
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Language", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TSPL", "CPCL", "PPLB", "ESC").forEach { lang ->
                        val selected = language.equals(lang, ignoreCase = true)
                        AssistChip(
                            onClick = { language = lang },
                            label = { Text(lang) },
                            leadingIcon = {
                                Icon(
                                    if (selected) Icons.TwoTone.CheckBox else Icons.TwoTone.CheckBoxOutlineBlank,
                                    contentDescription = null
                                )
                            })
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Direction", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isNormalSelected = direction != "1"
                    AssistChip(
                        onClick = { direction = "0" },
                        label = { Text("0 � Normal") },
                        leadingIcon = {
                            Icon(
                                if (isNormalSelected) Icons.TwoTone.CheckBox else Icons.TwoTone.CheckBoxOutlineBlank,
                                contentDescription = null
                            )
                        })
                    val isReverseSelected = direction == "1"
                    AssistChip(
                        onClick = { direction = "1" },
                        label = { Text("1 � Reverse") },
                        leadingIcon = {
                            Icon(
                                if (isReverseSelected) Icons.TwoTone.CheckBox else Icons.TwoTone.CheckBoxOutlineBlank,
                                contentDescription = null
                            )
                        })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = referenceX,
                    onValueChange = { referenceX = it },
                    label = { Text("Start X offset (mm)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = referenceY,
                    onValueChange = { referenceY = it },
                    label = { Text("Start Y offset (mm)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                val resolvedWidth = width.toFloatOrNull() ?: template.labelWidth
                val resolvedHeight = height.toFloatOrNull() ?: template.labelHeight
                val resolvedDirection =
                    direction.toIntOrNull()?.coerceIn(0, 1) ?: template.printDirection
                val paddingMax = (minOf(resolvedWidth, resolvedHeight) / 2f).coerceAtLeast(0f)
                val resolvedPadding =
                    padding.toFloatOrNull()?.coerceIn(0f, paddingMax) ?: template.labelPadding
                onSave(
                    template.copy(
                        labelWidth = resolvedWidth,
                        labelHeight = resolvedHeight,
                        gapWidth = gapWidth.toFloatOrNull() ?: template.gapWidth,
                        gapHeight = gapHeight.toFloatOrNull() ?: template.gapHeight,
                        printDensity = density.toIntOrNull() ?: template.printDensity,
                        printSpeed = speed.toIntOrNull()?.coerceIn(1, 10) ?: template.printSpeed,
                        printDirection = resolvedDirection,
                        referenceX = referenceX.toFloatOrNull() ?: template.referenceX,
                        referenceY = referenceY.toFloatOrNull() ?: template.referenceY,
                        labelPadding = resolvedPadding,
                        printLanguage = language.uppercase(),
                        modifiedAt = System.currentTimeMillis()
                    )
                )
            }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun LabelSetupDiagram(
    padding: String,
    gapWidth: String,
    gapHeight: String
) {
    val pad = (padding.toFloatOrNull() ?: 1.5f).coerceAtLeast(0f)
    val gapW = (gapWidth.toFloatOrNull() ?: 2f).coerceAtLeast(0f)
    val gapH = (gapHeight.toFloatOrNull() ?: 2f).coerceAtLeast(0f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val borderColor = MaterialTheme.colorScheme.primary
            val paddingColor = MaterialTheme.colorScheme.tertiary
            val gapColor = MaterialTheme.colorScheme.outline
            Text(
                text = "Label setup preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.Transparent)
            ) {
                val labelW = 60f
                val labelH = 35f
                val padClamped = pad.coerceAtMost(minOf(labelW, labelH) / 2f - 0.5f)

                val totalW = labelW + gapW + labelW * 0.5f
                val totalH = labelH + gapH + labelH * 0.5f
                val scale = minOf(size.width / totalW, size.height / totalH) * 0.9f
                val originX = (size.width - totalW * scale) / 2f
                val originY = (size.height - totalH * scale) / 2f

                val mainX = originX
                val mainY = originY
                val mainW = labelW * scale
                val mainH = labelH * scale

                val nextX = originX + (labelW + gapW) * scale
                val nextY = originY + (labelH + gapH) * scale

                // Main label
                drawRect(
                    color = borderColor,
                    topLeft = Offset(mainX, mainY),
                    size = Size(mainW, mainH),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Padding inset
                val padPx = padClamped * scale
                drawRect(
                    color = paddingColor,
                    topLeft = Offset(mainX + padPx, mainY + padPx),
                    size = Size(mainW - padPx * 2f, mainH - padPx * 2f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Next label outlines (right and bottom)
                drawRect(
                    color = gapColor,
                    topLeft = Offset(nextX, mainY),
                    size = Size(mainW, mainH),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawRect(
                    color = gapColor,
                    topLeft = Offset(mainX, nextY),
                    size = Size(mainW, mainH),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Gap indicators
                drawLine(
                    color = gapColor,
                    start = Offset(mainX + mainW, mainY + mainH * 0.5f),
                    end = Offset(nextX, mainY + mainH * 0.5f),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = gapColor,
                    start = Offset(mainX + mainW * 0.5f, mainY + mainH),
                    end = Offset(mainX + mainW * 0.5f, nextY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

private val textBindingOptions = listOf(
    "",
    "item.itemId",
    "item.itemAddName",
    "item.catName",
    "item.catId",
    "item.subCatName",
    "item.subCatId",
    "item.entryType",
    "item.gsWt",
    "item.ntWt",
    "item.fnWt",
    "item.purity",
    "item.quantity",
    "item.huid",
    "item.unit",
    "item.crgType",
    "item.crg",
    "item.compDes",
    "item.compCrg",
    "item.cgst",
    "item.sgst",
    "item.igst",
    "item.addDesKey",
    "item.addDesValue",
    "item.purchaseOrderId",
    "store.name",
    "store.proprietor",
    "store.email",
    "store.phone",
    "store.address",
    "store.gstinNo",
    "store.panNo",
    "store.registrationNo",
    "store.upiId"
)

private val logoBindingOptions = listOf(
    "", "store.logo", "store.bsiMark"
)

private val previewItemData = mapOf(
    "itemId" to "ITEM-123",
    "itemAddName" to "Gold Ring",
    "catName" to "Gold",
    "catId" to "CAT-01",
    "subCatName" to "Ring",
    "subCatId" to "SUB-01",
    "entryType" to "Purchase",
    "gsWt" to "12.50",
    "ntWt" to "12.10",
    "fnWt" to "11.85",
    "purity" to "22K",
    "quantity" to "1",
    "huid" to "HUID123456",
    "unit" to "gm",
    "crgType" to "Making",
    "crg" to "1500",
    "compDes" to "Polish",
    "compCrg" to "200",
    "cgst" to "3%",
    "sgst" to "3%",
    "igst" to "0%",
    "addDesKey" to "Stone",
    "addDesValue" to "Ruby",
    "purchaseOrderId" to "PO-789"
)

private val previewStoreData = mapOf(
    "name" to "Jewel Vault",
    "proprietor" to "Rajesh K.",
    "email" to "contact@jewelvault.com",
    "phone" to "+91 98765 43210",
    "address" to "123 MG Road, Bengaluru",
    "gstinNo" to "29ABCDE1234F1Z5",
    "panNo" to "ABCDE1234F",
    "registrationNo" to "REG-2025-01",
    "upiId" to "jewelvault@upi",
    "logo" to "Store Logo",
    "bsiMark" to "BSI Mark"
)

private fun bindingOptionsForElement(elementType: String): List<String> {
    return when (elementType.uppercase()) {
        "IMAGE" -> logoBindingOptions
        else -> textBindingOptions
    }
}

private fun defaultBindingForElement(elementType: String): String {
    return bindingOptionsForElement(elementType).firstOrNull { it.isNotBlank() } ?: ""
}

private fun bindingLabel(binding: String): String {
    return when (binding) {
        "" -> "None"
        "store.logo" -> "Store logo"
        "store.bsiMark" -> "BSI mark"
        else -> binding
    }
}

private fun supportsBinding(elementType: String): Boolean {
    val type = elementType.uppercase()
    return type == "TEXT" || type == "QR_CODE" || type == "BARCODE" || type == "IMAGE"
}

private fun applyUpdate(
    element: LabelElementEntity,
    template: LabelTemplateEntity,
    x: String,
    y: String,
    width: String,
    height: String,
    textContent: String,
    fontSize: String,
    isBold: Boolean,
    imageThreshold: Int?,
    imageDitherMode: String?,
    imageMode: String?,
    imageDpi: Int?,
    invertImage: Boolean?,
    dataBinding: String?,
    onUpdate: (LabelElementEntity) -> Unit
) {
    val bindingSupported = supportsBinding(element.elementType)
    val isTextElement = element.elementType.equals("TEXT", true)
    val isImageElement = element.elementType.equals("IMAGE", true)
    val resolvedFontSize = fontSize.toFloatOrNull() ?: parseTextProps(element.properties).fontSize
    val updatedProps = when {
        isTextElement -> updateTextProps(
            element.properties, text = textContent, fontSize = resolvedFontSize, isBold = isBold
        )

        isImageElement -> setImageProps(
            element.properties,
            threshold = imageThreshold,
            ditherMode = imageDitherMode,
            mode = imageMode,
            dpi = imageDpi,
            invert = invertImage
        )

        else -> element.properties
    }
    val updated = element.copy(
        x = x.toFloatOrNull() ?: element.x,
        y = y.toFloatOrNull() ?: element.y,
        width = width.toFloatOrNull() ?: element.width,
        height = height.toFloatOrNull() ?: element.height,
        properties = updatedProps,
        dataBinding = if (bindingSupported) dataBinding?.ifBlank { null } else element.dataBinding)
    onUpdate(clampElementToTemplate(updated, template))
}

private fun resolvePreviewBinding(binding: String, fallback: String): String {
    if (binding.isBlank()) return fallback
    return try {
        when {
            binding.startsWith("item.") -> {
                val key = binding.removePrefix("item.")
                previewItemData[key] ?: fallback
            }

            binding.startsWith("store.") -> {
                val key = binding.removePrefix("store.")
                previewStoreData[key] ?: fallback
            }

            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

private fun shouldRequestBindingOnAdd(elementType: String): Boolean {
    val type = elementType.uppercase()
    return type == "TEXT" || type == "IMAGE"
}

private fun clampElementToTemplate(
    element: LabelElementEntity, template: LabelTemplateEntity, minSize: Float = 2f
): LabelElementEntity {
    val padding = template.labelPadding.coerceAtLeast(0f)
    val isLine = element.elementType.equals("LINE", true)
    val isQr = element.elementType.equals("QR_CODE", true)
    val elementMinSize = if (isLine) 0.2f else minSize
    val availableWidth = (template.labelWidth - (padding * 2f)).coerceAtLeast(elementMinSize)
    val availableHeight = (template.labelHeight - (padding * 2f)).coerceAtLeast(elementMinSize)
    var width = element.width.coerceIn(elementMinSize, availableWidth)
    var height = element.height.coerceIn(elementMinSize, availableHeight)
    if (isQr) {
        val squareMax = minOf(availableWidth, availableHeight)
        val size = maxOf(minOf(width, height), 2f).coerceAtMost(squareMax)
        width = size
        height = size
    }
    val maxX = (availableWidth - width).coerceAtLeast(0f)
    val maxY = (availableHeight - height).coerceAtLeast(0f)
    val x = element.x.coerceIn(0f, maxX)
    val y = element.y.coerceIn(0f, maxY)
    return element.copy(x = x, y = y, width = width, height = height)
}

fun createElement(
    elementType: String, index: Int, template: LabelTemplateEntity?
): LabelElementEntity {
    val defaultSize = 10f
    val spacing = 15f
    val baseY = spacing + (index * spacing)
    val defaultProps = when (elementType) {
        "TEXT" -> JSONObject().put("text", "Text").put("fontSize", 6).put("isBold", false)
            .put("isUnderline", false).toString()

        "QR_CODE" -> JSONObject().put("qrData", "QR data").toString()
        "BARCODE" -> JSONObject().put("barcodeData", "123456789012").put("barcodeType", "CODE128")
            .toString()

        "DATE" -> JSONObject().put("text", "01/01/2025").toString()
        "LINE" -> JSONObject().put("thickness", 1).toString()
        else -> "{}"
    }
    return LabelElementEntity(
        elementId = generateId(),
        templateId = template?.templateId ?: "",
        elementType = elementType,
        x = spacing,
        y = baseY,
        width = when (elementType) {
            "QR_CODE" -> 2f
            "BARCODE" -> 60f
            "LINE" -> 40f
            else -> defaultSize
        },
        height = when (elementType) {
            "QR_CODE" -> 2f
            "BARCODE" -> 15f
            "LINE" -> 0.2f
            else -> defaultSize
        },
        rotation = 0f,
        zIndex = index,
        properties = defaultProps,
        dataBinding = null,
        isVisible = true
    )
}

private fun defaultImageThreshold(binding: String?): Int {
    return if (binding == "store.logo" || binding == "store.bsiMark") 170 else 150
}

private fun parseImageThreshold(properties: String, binding: String?): Int {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        obj.optInt("threshold", defaultImageThreshold(binding))
    } catch (_: Exception) {
        defaultImageThreshold(binding)
    }
}

private fun parseImageDitherMode(properties: String): String {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        obj.optString("ditherMode", "floyd")
    } catch (_: Exception) {
        "floyd"
    }
}

private fun parseImageMode(properties: String): String {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        obj.optString("mode", "bitmap")
    } catch (_: Exception) {
        "bitmap"
    }
}

private fun parseImageDpi(properties: String): Int {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        obj.optInt("dpi", 203)
    } catch (_: Exception) {
        203
    }
}

private fun parseInvertFromProperties(properties: String): Boolean {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        obj.optBoolean("invert", false)
    } catch (_: Exception) {
        false
    }
}

private data class TextProps(
    val text: String, val fontSize: Float, val isBold: Boolean, val isUnderline: Boolean
)

private fun parseTextProps(properties: String): TextProps {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        TextProps(
            text = obj.optString("text", ""),
            fontSize = obj.optDouble("fontSize", 6.0).toFloat(),
            isBold = obj.optBoolean("isBold", false),
            isUnderline = obj.optBoolean("isUnderline", false)
        )
    } catch (_: Exception) {
        TextProps(text = "", fontSize = 6f, isBold = false, isUnderline = false)
    }
}

private fun parseTextFromProperties(properties: String): String {
    return parseTextProps(properties).text
}

private fun isLockedFromProperties(properties: String): Boolean {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        obj.optBoolean("locked", false)
    } catch (_: Exception) {
        false
    }
}

private fun setLockedInProperties(properties: String, locked: Boolean): String {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        obj.put("locked", locked)
        obj.toString()
    } catch (_: Exception) {
        JSONObject().put("locked", locked).toString()
    }
}

private fun setTextInProperties(properties: String, text: String): String {
    return updateTextProps(properties, text = text)
}

private fun updateTextProps(
    properties: String,
    text: String? = null,
    fontSize: Float? = null,
    isBold: Boolean? = null,
    isUnderline: Boolean? = null
): String {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        text?.let { obj.put("text", it) }
        fontSize?.let { obj.put("fontSize", it.toDouble()) }
        isBold?.let { obj.put("isBold", it) }
        isUnderline?.let { obj.put("isUnderline", it) }
        obj.toString()
    } catch (_: Exception) {
        JSONObject().apply {
            text?.let { put("text", it) }
            fontSize?.let { put("fontSize", it.toDouble()) }
            isBold?.let { put("isBold", it) }
            isUnderline?.let { put("isUnderline", it) }
        }.toString()
    }
}

private fun setImageProps(
    properties: String,
    threshold: Int? = null,
    ditherMode: String? = null,
    mode: String? = null,
    dpi: Int? = null,
    invert: Boolean? = null
): String {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        threshold?.let { obj.put("threshold", it) }
        ditherMode?.let { obj.put("ditherMode", it) }
        mode?.let { obj.put("mode", it) }
        dpi?.let { obj.put("dpi", it) }
        invert?.let { obj.put("invert", it) }
        obj.toString()
    } catch (_: Exception) {
        JSONObject().apply {
            threshold?.let { put("threshold", it) }
            ditherMode?.let { put("ditherMode", it) }
            mode?.let { put("mode", it) }
            dpi?.let { put("dpi", it) }
            invert?.let { put("invert", it) }
        }.toString()
    }
}

private fun rulerPaint(
    density: androidx.compose.ui.unit.Density, colorInt: Int
): android.graphics.Paint {
    return android.graphics.Paint().apply {
        color = colorInt
        textSize = 10 * density.density
        isAntiAlias = true
    }
}

private fun resolvePreviewContent(element: LabelElementEntity, fallback: String): String {
    val bindingValue = resolvePreviewBinding(element.dataBinding ?: "", fallback)
    return when (element.elementType.uppercase()) {
        "TEXT" -> bindingValue
        "QR_CODE" -> bindingValue.ifBlank { "QR data" }
        "BARCODE" -> bindingValue.ifBlank { "Barcode data" }
        "DATE" -> bindingValue.ifBlank { "Date" }
        "IMAGE" -> bindingValue.ifBlank { "Logo" }
        "LINE" -> fallback.ifBlank { "Line" }
        else -> fallback
    }
}

private fun loadPreviewImage(context: Context, element: LabelElementEntity): Bitmap? {
    return try {
        val props = JSONObject(element.properties.ifEmpty { "{}" })
        val imagePath = props.optString("imagePath", "").takeIf { it.isNotBlank() }
        val imageUri = props.optString("imageUri", "").takeIf { it.isNotBlank() }
        val threshold = parseImageThreshold(element.properties, element.dataBinding)
        val ditherMode = parseImageDitherMode(element.properties)
        val invert = parseInvertFromProperties(element.properties)

        val rawBitmap = when {
            element.dataBinding == "store.logo" -> {
                FileManager.getLogoFileUri(context)?.let { uri ->
                    context.contentResolver.openInputStream(uri)
                        ?.use { BitmapFactory.decodeStream(it) }
                }
            }

            element.dataBinding == "store.bsiMark" -> loadBsiMarkPreview(context)
            imagePath != null -> BitmapFactory.decodeFile(imagePath)
            imageUri != null -> {
                val uri = Uri.parse(imageUri)
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }

            else -> null
        }
        rawBitmap?.let {
            toMonochromeDither(
                it, threshold = threshold.toFloat(), invert = invert, ditherMode = ditherMode
            )
        }
    } catch (_: Exception) {
        null
    }
}

private fun loadBsiMarkPreview(context: Context): Bitmap? {
    return try {
        val hallmarkDrawable =
            context.resources.getIdentifier("hallmark", "drawable", context.packageName)
        if (hallmarkDrawable != 0) {
            BitmapFactory.decodeResource(context.resources, hallmarkDrawable)
        } else null
    } catch (_: Exception) {
        null
    }
}

private fun toMonochromeDither(
    source: Bitmap, threshold: Float = 128f, invert: Boolean = false, ditherMode: String = "floyd"
): Bitmap {
    val width = source.width
    val height = source.height
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val lum = FloatArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val p = source.getPixel(x, y)
            lum[y * width + x] =
                (AndroidColor.red(p) + AndroidColor.green(p) + AndroidColor.blue(p)) / 3f
        }
    }
    if (ditherMode == "ordered") {
        val matrix = arrayOf(
            intArrayOf(0, 8, 2, 10),
            intArrayOf(12, 4, 14, 6),
            intArrayOf(3, 11, 1, 9),
            intArrayOf(15, 7, 13, 5)
        )
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val thresholdBias = (matrix[y % 4][x % 4] / 15f) * 255f - 128f
                val adjusted = lum[idx] + thresholdBias
                val isBlack = adjusted < threshold
                val color = if (isBlack.xor(invert)) AndroidColor.BLACK else AndroidColor.WHITE
                result.setPixel(x, y, color)
            }
        }
    } else {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val old = lum[idx]
                val newVal = if (old < threshold) 0f else 255f
                val isBlack = newVal == 0f
                val color = if (isBlack.xor(invert)) AndroidColor.BLACK else AndroidColor.WHITE
                result.setPixel(x, y, color)
                if (ditherMode == "floyd") {
                    val err = old - newVal
                    if (x + 1 < width) lum[idx + 1] += err * 7f / 16f
                    if (y + 1 < height) {
                        if (x > 0) lum[idx + width - 1] += err * 3f / 16f
                        lum[idx + width] += err * 5f / 16f
                        if (x + 1 < width) lum[idx + width + 1] += err * 1f / 16f
                    }
                }
            }
        }
    }
    return result
}








