package com.velox.jewelvault.ui.screen.label

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.entity.label.LabelElementEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.utils.generateId
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelDesignerScreen(
    templateId: String? = null, viewModel: LabelTemplateViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

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

    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    fun clearInlineEdit() {
        inlineEditTarget = null
        inlineEditText = ""
    }

    LaunchedEffect(templateId) {
        if (!templateId.isNullOrEmpty()) {
            val result = viewModel.getTemplateWithElements(templateId)
            if (result.third == null) {
                template = result.first
                elements = result.second
                templateName = result.first?.templateName.orEmpty()
            }
        } else if (template == null) {
            val newTemplate = LabelTemplateEntity(
                templateId = generateId(),
                templateName = "New Template",
                templateType = "ITEM_LABEL",
                labelWidth = 100f,
                labelHeight = 50f,
                gapWidth = 3f,
                gapHeight = 0f,
                printDensity = 6,
                orientation = "PORTRAIT",
                printLanguage = "TSPL",
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                isDefault = false,
                description = null
            )
            template = newTemplate
            templateName = newTemplate.templateName
            viewModel.createTemplate(newTemplate)
        }
    }

    LaunchedEffect(elements, template?.templateId) {
        val tid = template?.templateId.orEmpty()
        if (tid.isNotEmpty()) {
            kotlinx.coroutines.delay(400)
            viewModel.saveElementsSilent(elements)
        }
    }

    val tpl = template
    if (tpl == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    fun recordSnapshot() {
        undoStack = (undoStack + listOf(elements)).takeLast(50)
        redoStack = emptyList()
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

    fun updateSelection(newSelection: Set<String>) {
        selectedIds = newSelection
        selectedElement = elements.firstOrNull { newSelection.contains(it.elementId) }
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
            viewModel.saveElementsSilent(elements)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f))
            .padding(12.dp),
    ) {
            TemplateHeader(
                templateName = templateName,
                onTemplateNameChange = { templateName = it },
                template = tpl,
                onOpenSettings = { showSettingsDialog = true },
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

            ElementPalette(onAddElement = { type ->
                recordSnapshot()
                val newElement = clampElementToTemplate(
                    createElement(type, elements.size, tpl), tpl
                )
                elements = elements + newElement
                selectedElement = newElement
                updateSelection(setOf(newElement.elementId))
            })

        Row(
            modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            LabelCanvas(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
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
                        val minSize = 2f
                        var newX = it.x
                        var newY = it.y
                        var newW = it.width
                        var newH = it.height
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
                onInlineEditCancel = { clearInlineEdit() },
                onZoomIn = { zoom = (zoom * 1.1f).coerceAtMost(5f) },
                onZoomOut = { zoom = (zoom / 1.1f).coerceAtLeast(0.5f) }
            )


            PropertiesPanel(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight(),
                template = tpl,
                selectedElement = selectedElement,
                onUpdateElement = { updated ->
                    recordSnapshot()
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
                    updateSelection(selectedIds - toDelete.elementId)
                    clearInlineEdit()
                })
        }
    }


    if (showSettingsDialog) {
        TemplateSettingsDialog(
            template = tpl,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated ->
                template = updated
                viewModel.updateTemplate(updated)
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
    onSave: () -> Unit
) {
    val templateNameState = remember(templateName) { InputFieldState(templateName) }
    Column(modifier = Modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            CusOutlinedTextField(
                state = templateNameState,
                onTextChange = onTemplateNameChange,
                placeholderText = "Template name",
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            MetricChip("Size", "${template.labelWidth}mm × ${template.labelHeight}mm")
            MetricChip("Gap", "${template.gapWidth} / ${template.gapHeight} mm")
            MetricChip("Density", "${template.printDensity}")
            MetricChip("Language", template.printLanguage)

            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier
                    .bounceClick { onOpenSettings() }
                    .size(30.dp))

            Button(onClick = onSave) {
                Icon(
                    Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }

        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .border(
                1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
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
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionChipButton(
            icon = if (multiSelectEnabled) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            label = if (multiSelectEnabled) "Multi" else "Single",
            onClick = onToggleMultiSelect
        )
        ActionChipButton(
            icon = Icons.Default.SelectAll,
            label = "Select all",
            onClick = onSelectAll,
            enabled = multiSelectEnabled
        )
        ActionChipButton(
            icon = Icons.Default.ContentCopy,
            label = "Duplicate",
            onClick = onDuplicate,
            enabled = selectedCount > 0
        )
        ActionChipButton(
            icon = Icons.Default.RotateRight,
            label = "Rotate 90°",
            onClick = onRotate,
            enabled = selectedCount > 0
        )
        ActionChipButton(
            icon = if (anyLocked) Icons.Default.LockOpen else Icons.Default.Lock,
            label = if (anyLocked) "Unlock" else "Lock",
            onClick = onLockToggle,
            enabled = selectedCount > 0
        )
        ActionChipButton(
            icon = Icons.Default.Delete,
            label = "Delete",
            onClick = onDelete,
            enabled = selectedCount > 0
        )
        ActionChipButton(
            icon = Icons.Default.Undo,
            label = "Undo",
            onClick = onUndo,
            enabled = canUndo
        )
        ActionChipButton(
            icon = Icons.Default.Redo,
            label = "Redo",
            onClick = onRedo,
            enabled = canRedo
        )
    }
}

@Composable
private fun ActionChipButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
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
    val elementTypes = listOf(
        "TEXT" to Icons.Default.TextFields as ImageVector,
        "IMAGE" to Icons.Default.Image as ImageVector,
        "QR_CODE" to Icons.Default.QrCode as ImageVector,
        "BARCODE" to Icons.Default.QrCode as ImageVector,
        "DATE" to Icons.Default.DateRange as ImageVector,
        "LINE" to Icons.Default.RotateRight as ImageVector
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        elementTypes.forEach { (type, icon) ->
            AssistChip(
                onClick = { onAddElement(type) },
                label = { Text(type) },
                leadingIcon = { Icon(icon, contentDescription = type) })
        }
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
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    val density = LocalDensity.current
    val dpi = density.density * 160f
    val pxPerMm = dpi / 25.4f

    fun mmToDp(mm: Float): androidx.compose.ui.unit.Dp {
        val px = mm * pxPerMm
        return with(density) { (px / density.density).dp }
    }

    val primarySelection = elements.firstOrNull { selectedIds.contains(it.elementId) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = mmToDp(template.labelWidth) + 20.dp,
                    height = mmToDp(template.labelHeight) + 20.dp
                )
                .graphicsLayer(
                    scaleX = zoom, scaleY = zoom, translationX = pan.x, translationY = pan.y
                )
                .pointerInput(Unit) {
                    detectTransformGestures(panZoomLock = true) { _, panChange, zoomChange, _ ->
                        onTransform(zoomChange, panChange)
                    }
                }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 20.dp, y = 20.dp)
                    .size(
                        width = mmToDp(template.labelWidth), height = mmToDp(template.labelHeight)
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    elements.forEach { element ->
                        val displayText = parseTextFromProperties(element.properties)
                        CanvasElement(
                            element = element,
                            isSelected = selectedIds.contains(element.elementId),
                            isLocked = isLockedFromProperties(element.properties),
                            isInlineEditing = inlineEditTarget == element.elementId,
                            inlineEditText = inlineEditText,
                            onClick = { onElementClick(element) },
                            onDragStart = onElementDragStart,
                            onDragEnd = onElementDragEnd,
                            onDragBy = { dxPx, dyPx ->
                                val dxMm = dxPx / (pxPerMm * zoom)
                                val dyMm = dyPx / (pxPerMm * zoom)
                                onElementDrag(element.elementId, dxMm, dyMm)
                            },
                            mmToDp = ::mmToDp,
                            pxToMm = { it / (pxPerMm * zoom) },
                            onResizeBy = { corner, dxMm, dyMm ->
                                onElementResize(element.elementId, corner, dxMm, dyMm)
                            },
                            onResizeStart = onElementResizeStart,
                            onResizeEnd = onElementResizeEnd,
                            onRequestInlineEdit = {
                                if (element.elementType.equals("TEXT", true)) {
                                    onRequestInlineEdit(element, displayText)
                                }
                            },
                            onInlineEditChange = onInlineEditChange,
                            onInlineEditCommit = { text -> onInlineEditCommit(element, text) },
                            onInlineEditCancel = onInlineEditCancel
                        )
                    }

                    primarySelection?.let { sel ->
                        val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        val centerX = sel.x + (sel.width / 2f)
                        val centerY = sel.y + (sel.height / 2f)

                        Box(
                            modifier = Modifier
                                .offset(x = mmToDp(sel.x), y = mmToDp(sel.y))
                                .size(width = mmToDp(sel.width), height = mmToDp(sel.height))
                                .border(1.dp, guideColor, RoundedCornerShape(2.dp))
                        )

                        Box(
                            modifier = Modifier
                                .offset(y = mmToDp(centerY))
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(guideColor)
                        )

                        Box(
                            modifier = Modifier
                                .offset(x = mmToDp(centerX))
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(guideColor)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row {

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
    }
}

@Composable
fun CanvasElement(
    element: LabelElementEntity,
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
            detectDragGestures(
                onDragStart = { pos ->
                    last = pos
                    onDragStart()
                },
                onDragEnd = {
                    last = null
                    onDragEnd()
                },
                onDragCancel = {
                    last = null
                    onDragEnd()
                },
                onDrag = { change, _ ->
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
                detectTapGestures(
                    onDoubleTap = {
                        if (!isLocked && element.elementType.equals("TEXT", true)) {
                            onRequestInlineEdit()
                        }
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isInlineEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(6.dp)
            ) {
                BasicTextField(
                    value = inlineEditText,
                    onValueChange = { onInlineEditChange(it.replace("\n", " ")) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                onInlineEditCommit(inlineEditText)
                            }
                        }
                )
            }
        } else {
            Text(
                text = when {
                    element.elementType.equals(
                        "TEXT", true
                    ) -> parseTextFromProperties(element.properties).ifEmpty { "Text" }

                    else -> element.elementType
                },
                style = MaterialTheme.typography.bodySmall
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
    onUpdate: (LabelElementEntity) -> Unit,
    onDelete: () -> Unit
) {
    var x by remember { mutableStateOf(element.x.toString()) }
    var y by remember { mutableStateOf(element.y.toString()) }
    var width by remember { mutableStateOf(element.width.toString()) }
    var height by remember { mutableStateOf(element.height.toString()) }
    LaunchedEffect(element.elementId, element.x, element.y, element.width, element.height) {
        x = element.x.toString()
        y = element.y.toString()
        width = element.width.toString()
        height = element.height.toString()
    }
    val isText = remember(element.elementType) {
        element.elementType.equals("TEXT", true) || element.elementType.equals("Text", true)
    }
    val supportsBinding = remember(element.elementType) {
        val t = element.elementType.uppercase()
        t == "TEXT" || t == "QR_CODE" || t == "BARCODE"
    }
    var textContent by remember(element.properties) { mutableStateOf(parseTextFromProperties(element.properties)) }
    var dataBinding by remember(element.dataBinding) { mutableStateOf(element.dataBinding ?: "") }
    var bindingMenuExpanded by remember { mutableStateOf(false) }
    val bindingOptions = listOf(
        "",
        "item.itemId",
        "item.itemAddName",
        "item.gsWt",
        "item.fnWt",
        "item.purity",
        "item.quantity",
        "item.huid",
        "item.addDesKey",
        "item.addDesValue",
        "store.name",
        "store.proprietor",
        "store.phone",
        "store.address",
        "store.gstinNo",
        "store.panNo",
        "store.upiId"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Properties",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val updated = element.copy(
                        x = x.toFloatOrNull() ?: element.x,
                        y = y.toFloatOrNull() ?: element.y,
                        width = width.toFloatOrNull() ?: element.width,
                        height = height.toFloatOrNull() ?: element.height,
                        properties = if (isText) setTextInProperties(
                            element.properties, textContent
                        ) else element.properties,
                        dataBinding = if (supportsBinding) dataBinding.ifBlank { null } else element.dataBinding)
                    onUpdate(clampElementToTemplate(updated, template))
                }, modifier = Modifier
            ) {
                Text("Update")
            }
            Spacer(Modifier.width(20.dp))
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Element",
                modifier = Modifier.bounceClick { onDelete() })

        }


        if (supportsBinding) {
            OutlinedTextField(
                value = if (dataBinding.isBlank()) "None" else dataBinding,
                onValueChange = {},
                label = { Text("Binding") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { bindingMenuExpanded = !bindingMenuExpanded }) {
                        Icon(Icons.Default.Settings, contentDescription = "Choose Binding")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = bindingMenuExpanded,
                onDismissRequest = { bindingMenuExpanded = false }) {
                bindingOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(if (opt.isBlank()) "None" else opt) },
                        onClick = {
                            dataBinding = opt
                            bindingMenuExpanded = false
                        })
                }
            }
            if (isText) {
                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    label = { Text("Text (fallback/static)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        OutlinedTextField(
            value = x,
            onValueChange = { x = it },
            label = { Text("X (mm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = y,
            onValueChange = { y = it },
            label = { Text("Y (mm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = width,
            onValueChange = { width = it },
            label = { Text("Width (mm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Height (mm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

    }
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

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Template Settings") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                value = density,
                onValueChange = { density = it },
                label = { Text("Density (1-15)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                onSave(
                    template.copy(
                        labelWidth = width.toFloatOrNull() ?: template.labelWidth,
                        labelHeight = height.toFloatOrNull() ?: template.labelHeight,
                        gapWidth = gapWidth.toFloatOrNull() ?: template.gapWidth,
                        gapHeight = gapHeight.toFloatOrNull() ?: template.gapHeight,
                        printDensity = density.toIntOrNull() ?: template.printDensity,
                        modifiedAt = System.currentTimeMillis()
                    )
                )
            }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun clampElementToTemplate(
    element: LabelElementEntity, template: LabelTemplateEntity, minSize: Float = 2f
): LabelElementEntity {
    val width = element.width.coerceIn(minSize, template.labelWidth)
    val height = element.height.coerceIn(minSize, template.labelHeight)
    val x = element.x.coerceIn(0f, template.labelWidth - width)
    val y = element.y.coerceIn(0f, template.labelHeight - height)
    return element.copy(x = x, y = y, width = width, height = height)
}

fun createElement(
    elementType: String, index: Int, template: LabelTemplateEntity?
): LabelElementEntity {
    val defaultSize = 10f
    val spacing = 15f
    val baseY = spacing + (index * spacing)
    val defaultProps = when (elementType) {
        "TEXT" -> JSONObject().put("text", "Text").put("fontSize", 12).toString()
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
            "QR_CODE" -> 20f
            "BARCODE" -> 60f
            "LINE" -> 40f
            else -> defaultSize
        },
        height = when (elementType) {
            "QR_CODE" -> 20f
            "BARCODE" -> 15f
            "LINE" -> 2f
            else -> defaultSize
        },
        rotation = 0f,
        zIndex = index,
        properties = defaultProps,
        dataBinding = null,
        isVisible = true
    )
}

private fun parseTextFromProperties(properties: String): String {
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        obj.optString("text", "")
    } catch (_: Exception) {
        ""
    }
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
    return try {
        val obj = JSONObject(properties.ifEmpty { "{}" })
        obj.put("text", text)
        obj.toString()
    } catch (_: Exception) {
        JSONObject().put("text", text).toString()
    }
}
