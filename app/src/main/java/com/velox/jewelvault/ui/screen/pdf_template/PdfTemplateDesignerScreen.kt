package com.velox.jewelvault.ui.screen.pdf_template

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.model.pdf.PdfTemplateType
import com.velox.jewelvault.data.model.pdf.PdfTemplateValidation
import com.velox.jewelvault.data.roomdb.entity.pdf.PdfElementEntity
import com.velox.jewelvault.data.roomdb.entity.pdf.PdfTemplateEntity
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.utils.generateId
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

@Composable
fun PdfTemplateDesignerScreen(
    templateId: String? = null,
    viewModel: PdfTemplateViewModel = hiltViewModel()
) {
    var template by remember { mutableStateOf<PdfTemplateEntity?>(null) }
    var elements by remember { mutableStateOf<List<PdfElementEntity>>(emptyList()) }
    var templateName by remember { mutableStateOf("") }
    var showElementDialog by remember { mutableStateOf(false) }
    var editingElement by remember { mutableStateOf<PdfElementEntity?>(null) }
    var selectedElementId by remember { mutableStateOf<String?>(null) }

    viewModel.currentScreenHeadingState.value = "PDF Template Designer"

    LaunchedEffect(templateId) {
        if (!templateId.isNullOrBlank()) {
            val result = viewModel.getTemplateWithElements(templateId)
            template = result.first
            elements = result.second
            templateName = result.first?.templateName.orEmpty()
            selectedElementId = result.second.firstOrNull()?.elementId
        }
    }

    val resolvedTemplate = template
    val isLocked = resolvedTemplate?.isDefault == true || resolvedTemplate?.isSystemDefault == true
    val missingBindings by remember(resolvedTemplate, elements) {
        derivedStateOf {
            if (resolvedTemplate == null) emptyList()
            else PdfTemplateValidation.missingBindings(resolvedTemplate.templateType, elements)
        }
    }

    if (resolvedTemplate == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Template not found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("Template name") },
                enabled = !isLocked,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${PdfTemplateType.displayName(resolvedTemplate.templateType)} • ${resolvedTemplate.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isLocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Default templates are locked for editing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val updated = resolvedTemplate.copy(
                            templateName = templateName
                        )
                        viewModel.saveDraft(updated, elements)
                    },
                    enabled = !isLocked
                ) {
                    Text("Save Draft")
                }
                Button(
                    onClick = {
                        val updated = resolvedTemplate.copy(
                            templateName = templateName
                        )
                        viewModel.saveAndPublish(updated, elements)
                    },
                    enabled = !isLocked
                ) {
                    Icon(Icons.TwoTone.Publish, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Publish")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            RowOrColumn(
                rowModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                columnModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) { isLandscape ->
                val previewModifier = if (isLandscape) {
                    Modifier.fillMaxHeight().weight(2f)
                } else {
                    Modifier.fillMaxWidth().heightIn(min = 260.dp)
                }
                val listModifier = if (isLandscape) {
                    Modifier.fillMaxHeight().weight(1f)
                } else {
                    Modifier.fillMaxWidth().weight(1f)
                }

                Column(modifier = previewModifier) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PdfTemplateCanvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        template = resolvedTemplate,
                        elements = elements,
                        selectedElementId = selectedElementId,
                        enabled = !isLocked,
                        onSelectElement = { element -> selectedElementId = element.elementId },
                        onElementDrag = { elementId, dx, dy ->
                            elements = elements.map {
                                if (it.elementId == elementId) {
                                    clampElementToPage(
                                        it.copy(x = it.x + dx, y = it.y + dy),
                                        resolvedTemplate
                                    )
                                } else {
                                    it
                                }
                            }
                            selectedElementId = elementId
                        },
                        onElementResize = { elementId, handle, dx, dy ->
                            val target = elements.firstOrNull { it.elementId == elementId }
                                ?: return@PdfTemplateCanvas
                            val resized = resizeElement(target, handle, dx, dy, resolvedTemplate)
                            elements = elements.map {
                                if (it.elementId == elementId) resized else it
                            }
                            selectedElementId = elementId
                        }
                    )
                }

                Column(modifier = listModifier) {
                    Text(
                        text = "Elements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            editingElement = null
                            showElementDialog = true
                        },
                        enabled = !isLocked
                    ) {
                        Icon(Icons.TwoTone.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Element")
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(elements.sortedBy { it.zIndex }) { element ->
                            ElementCard(
                                element = element,
                                selected = element.elementId == selectedElementId,
                                onSelect = { selectedElementId = element.elementId },
                                onEdit = {
                                    selectedElementId = element.elementId
                                    editingElement = element
                                    showElementDialog = true
                                },
                                onDelete = {
                                    elements =
                                        elements.filterNot { it.elementId == element.elementId }
                                    if (selectedElementId == element.elementId) {
                                        selectedElementId = null
                                    }
                                },
                                enabled = !isLocked
                            )
                        }
                    }

                    if (missingBindings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Missing required fields",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            missingBindings.forEach { binding ->
                                FilterChip(
                                    selected = false,
                                    onClick = {},
                                    label = { Text(binding) },
                                    enabled = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showElementDialog) {
        PdfElementDialog(
            templateId = resolvedTemplate.templateId,
            templateType = resolvedTemplate.templateType,
            initialElement = editingElement,
            onDismiss = { showElementDialog = false },
            onSave = { updated ->
                elements = if (editingElement == null) {
                    elements + updated
                } else {
                    elements.map { if (it.elementId == updated.elementId) updated else it }
                }
                selectedElementId = updated.elementId
                showElementDialog = false
            }
        )
    }
}

@Composable
private fun ElementCard(
    element: PdfElementEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean
) {
    val bindingSummary = remember(element) {
        if (element.elementType == "TABLE") {
            val bindings = parseTableBindings(element.properties)
            if (bindings.isEmpty()) "Table"
            else "Table: ${bindings.joinToString()}"
        } else {
            element.dataBinding ?: "No binding"
        }
    }

    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = element.elementType,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bindingSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, enabled = enabled) {
                    Icon(Icons.TwoTone.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                OutlinedButton(onClick = onDelete, enabled = enabled) {
                    Icon(Icons.TwoTone.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    }
}

private enum class ResizeHandle {
    TopStart,
    TopEnd,
    BottomStart,
    BottomEnd
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun PdfTemplateCanvas(
    modifier: Modifier = Modifier,
    template: PdfTemplateEntity,
    elements: List<PdfElementEntity>,
    selectedElementId: String?,
    enabled: Boolean,
    onSelectElement: (PdfElementEntity) -> Unit,
    onElementDrag: (String, Float, Float) -> Unit,
    onElementResize: (String, ResizeHandle, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val outlineColor = MaterialTheme.colorScheme.outline
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val scale = if (template.pageWidth <= 0f || template.pageHeight <= 0f) {
            1f
        } else {
            minOf(maxWidthPx / template.pageWidth, maxHeightPx / template.pageHeight)
        }
        val pageWidthPx = template.pageWidth * scale
        val pageHeightPx = template.pageHeight * scale
        val pageWidthDp = with(density) { pageWidthPx.toDp() }
        val pageHeightDp = with(density) { pageHeightPx.toDp() }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(pageWidthDp, pageHeightDp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, outlineColor, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val left = template.marginLeft * scale
                    val top = template.marginTop * scale
                    val right = size.width - template.marginRight * scale
                    val bottom = size.height - template.marginBottom * scale
                    if (left > 0f || top > 0f || right < size.width || bottom < size.height) {
                        drawRect(
                            color = primaryColor.copy(alpha = 0.1f),
                            topLeft = Offset(left, top),
                            size = Size(
                                (right - left).coerceAtLeast(0f),
                                (bottom - top).coerceAtLeast(0f)
                            ),
                            style = Stroke(width = 1f)
                        )
                    }
                }

                elements.sortedBy { it.zIndex }.forEach { element ->
                    if (!element.isVisible) return@forEach
                    val isSelected = element.elementId == selectedElementId
                    val offsetX = (element.x * scale).roundToInt()
                    val offsetY = (element.y * scale).roundToInt()
                    val widthDp = with(density) { (element.width * scale).toDp() }
                    val heightDp = with(density) { (element.height * scale).toDp() }

                    val borderColor = if (isSelected) {
                        primaryColor
                    } else {
                        outlineColor
                    }
                    val backgroundColor = if (isSelected) {
                        primaryColor.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }

                    val dragModifier = if (enabled) {
                        Modifier.pointerInput(element.elementId) {
                            detectDragGestures(
                                onDragStart = { onSelectElement(element) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onElementDrag(
                                        element.elementId,
                                        dragAmount.x / scale,
                                        dragAmount.y / scale
                                    )
                                }
                            )
                        }
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .size(widthDp, heightDp)
                            .background(backgroundColor, RoundedCornerShape(4.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .then(dragModifier)
                            .clickable { onSelectElement(element) }
                    ) {
                        Text(
                            text = previewLabelForElement(element),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(4.dp)
                        )

                        if (isSelected && enabled) {
                            ResizeHandleBox(
                                alignment = Alignment.TopStart,
                                onResize = { dx, dy ->
                                    onElementResize(
                                        element.elementId,
                                        ResizeHandle.TopStart,
                                        dx / scale,
                                        dy / scale
                                    )
                                }
                            )
                            ResizeHandleBox(
                                alignment = Alignment.TopEnd,
                                onResize = { dx, dy ->
                                    onElementResize(
                                        element.elementId,
                                        ResizeHandle.TopEnd,
                                        dx / scale,
                                        dy / scale
                                    )
                                }
                            )
                            ResizeHandleBox(
                                alignment = Alignment.BottomStart,
                                onResize = { dx, dy ->
                                    onElementResize(
                                        element.elementId,
                                        ResizeHandle.BottomStart,
                                        dx / scale,
                                        dy / scale
                                    )
                                }
                            )
                            ResizeHandleBox(
                                alignment = Alignment.BottomEnd,
                                onResize = { dx, dy ->
                                    onElementResize(
                                        element.elementId,
                                        ResizeHandle.BottomEnd,
                                        dx / scale,
                                        dy / scale
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ResizeHandleBox(
    alignment: Alignment,
    onResize: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .align(alignment)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onResize(dragAmount.x, dragAmount.y)
                }
            }
    )
}

private fun previewLabelForElement(element: PdfElementEntity): String {
    return when {
        element.elementType.equals("TABLE", true) -> {
            val bindings = parseTableBindings(element.properties)
            if (bindings.isEmpty()) "Table"
            else "Table (${bindings.size} cols)"
        }

        element.elementType.equals("SIGNATURE", true) -> {
            val label = parseLabel(element.properties)
            if (label.isNotBlank()) label else "Signature"
        }

        else -> {
            val label = parseLabel(element.properties)
            val binding = element.dataBinding.orEmpty()
            when {
                label.isNotBlank() && binding.isNotBlank() -> "$label: $binding"
                label.isNotBlank() -> label
                binding.isNotBlank() -> binding
                else -> element.elementType
            }
        }
    }
}

private fun resizeElement(
    element: PdfElementEntity,
    handle: ResizeHandle,
    dx: Float,
    dy: Float,
    template: PdfTemplateEntity
): PdfElementEntity {
    var left = element.x
    var top = element.y
    var right = element.x + element.width
    var bottom = element.y + element.height

    when (handle) {
        ResizeHandle.TopStart -> {
            left += dx
            top += dy
        }
        ResizeHandle.TopEnd -> {
            right += dx
            top += dy
        }
        ResizeHandle.BottomStart -> {
            left += dx
            bottom += dy
        }
        ResizeHandle.BottomEnd -> {
            right += dx
            bottom += dy
        }
    }

    val minWidth = elementMinWidth(element)
    val minHeight = elementMinHeight(element)

    if (right - left < minWidth) {
        if (handle == ResizeHandle.TopStart || handle == ResizeHandle.BottomStart) {
            left = right - minWidth
        } else {
            right = left + minWidth
        }
    }

    if (bottom - top < minHeight) {
        if (handle == ResizeHandle.TopStart || handle == ResizeHandle.TopEnd) {
            top = bottom - minHeight
        } else {
            bottom = top + minHeight
        }
    }

    val updated = element.copy(
        x = left,
        y = top,
        width = right - left,
        height = bottom - top
    )
    return clampElementToPage(updated, template)
}

private fun clampElementToPage(
    element: PdfElementEntity,
    template: PdfTemplateEntity
): PdfElementEntity {
    val minWidth = elementMinWidth(element)
    val minHeight = elementMinHeight(element)
    val width = element.width.coerceAtLeast(minWidth)
    val height = element.height.coerceAtLeast(minHeight)
    var x = element.x
    var y = element.y

    if (x < 0f) x = 0f
    if (y < 0f) y = 0f
    if (x + width > template.pageWidth) x = (template.pageWidth - width).coerceAtLeast(0f)
    if (y + height > template.pageHeight) y = (template.pageHeight - height).coerceAtLeast(0f)

    return element.copy(x = x, y = y, width = width, height = height)
}

private fun elementMinWidth(element: PdfElementEntity): Float {
    return when {
        element.elementType.equals("TABLE", true) -> 140f
        element.elementType.equals("SIGNATURE", true) -> 80f
        else -> 40f
    }
}

private fun elementMinHeight(element: PdfElementEntity): Float {
    return when {
        element.elementType.equals("TABLE", true) -> 60f
        element.elementType.equals("SIGNATURE", true) -> 40f
        else -> 18f
    }
}

@Composable
private fun PdfElementDialog(
    templateId: String,
    templateType: String,
    initialElement: PdfElementEntity?,
    onDismiss: () -> Unit,
    onSave: (PdfElementEntity) -> Unit
) {
    val availableBindings = PdfTemplateValidation.requiredBindings(templateType)
    val textBindings = availableBindings.filterNot { it.startsWith("items.") }
    val itemBindings = availableBindings.filter { it.startsWith("items.") }

    var elementType by remember { mutableStateOf(initialElement?.elementType ?: "TEXT") }
    var binding by remember { mutableStateOf(initialElement?.dataBinding.orEmpty()) }
    var label by remember { mutableStateOf(parseLabel(initialElement?.properties.orEmpty())) }
    var selectedColumns by remember {
        mutableStateOf(
            if (initialElement?.elementType == "TABLE") parseTableBindings(initialElement.properties)
            else emptyList()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialElement == null) "Add element" else "Edit element") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("TEXT", "TABLE", "SIGNATURE").forEach { type ->
                        FilterChip(
                            selected = elementType == type,
                            onClick = { elementType = type },
                            label = { Text(type) }
                        )
                    }
                }

                if (elementType == "TABLE") {
                    Text(
                        text = "Table columns",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemBindings.forEach { column ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = selectedColumns.contains(column),
                                    onCheckedChange = { checked ->
                                        selectedColumns = if (checked) {
                                            selectedColumns + column
                                        } else {
                                            selectedColumns.filterNot { it == column }
                                        }
                                    }
                                )
                                Text(text = column, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = binding,
                        onValueChange = { binding = it },
                        label = { Text("Binding") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (textBindings.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            textBindings.forEach { option ->
                                FilterChip(
                                    selected = binding == option,
                                    onClick = { binding = option },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Display label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val elementId = initialElement?.elementId ?: generateId()
                    val props = when (elementType) {
                        "TABLE" -> buildTableProperties(selectedColumns)
                        else -> buildLabelProperties(label)
                    }
                    val resolvedBinding = when (elementType) {
                        "TABLE" -> "items"
                        else -> binding.trim()
                    }
                    val resolvedElement = PdfElementEntity(
                        elementId = elementId,
                        templateId = templateId,
                        elementType = elementType,
                        x = initialElement?.x ?: 20f,
                        y = initialElement?.y ?: 20f,
                        width = initialElement?.width ?: 240f,
                        height = initialElement?.height ?: 20f,
                        rotation = initialElement?.rotation ?: 0f,
                        zIndex = initialElement?.zIndex ?: 1,
                        properties = props,
                        dataBinding = resolvedBinding.ifBlank { null },
                        isVisible = initialElement?.isVisible ?: true
                    )
                    onSave(resolvedElement)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun parseTableBindings(properties: String): List<String> {
    if (properties.isBlank()) return emptyList()
    return runCatching {
        val obj = JSONObject(properties)
        val columns = obj.optJSONArray("columns") ?: JSONArray()
        val bindings = mutableListOf<String>()
        for (i in 0 until columns.length()) {
            val col = columns.optJSONObject(i) ?: continue
            val binding = col.optString("binding")
            if (binding.isNotBlank()) bindings.add(binding)
        }
        bindings
    }.getOrDefault(emptyList())
}

private fun parseLabel(properties: String): String {
    if (properties.isBlank()) return ""
    return runCatching {
        val obj = JSONObject(properties)
        obj.optString("label")
    }.getOrDefault("")
}

private fun buildTableProperties(bindings: List<String>): String {
    val columns = bindings.joinToString(separator = ",") { """{"binding":"$it"}""" }
    return """{"columns":[${columns}]}"""
}

private fun buildLabelProperties(label: String): String {
    if (label.isBlank()) return "{}"
    return """{"label":"${label.replace("\"", "'")}"}"""
}
