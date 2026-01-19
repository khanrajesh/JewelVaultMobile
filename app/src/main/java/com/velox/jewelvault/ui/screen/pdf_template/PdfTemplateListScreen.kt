package com.velox.jewelvault.ui.screen.pdf_template

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Publish
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material.icons.twotone.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.data.model.pdf.PdfTemplateStatus
import com.velox.jewelvault.data.model.pdf.PdfTemplateType
import com.velox.jewelvault.data.roomdb.entity.pdf.PdfTemplateEntity
import com.velox.jewelvault.ui.components.RowOrColumn

@Composable
fun PdfTemplateListScreen(
    viewModel: PdfTemplateViewModel,
    onNavigateToDesigner: (String?) -> Unit = {}
) {
    val templates by viewModel.templates.collectAsState()
    var selectedType by rememberSaveable { mutableStateOf(PdfTemplateType.INVOICE) }
    var showCreateDialog by remember { mutableStateOf(false) }

    viewModel.currentScreenHeadingState.value = "PDF Templates"

    LaunchedEffect(Unit) {
        viewModel.ensureDefaults()
    }

    val filtered = templates.filter { it.templateType == selectedType }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            TemplateTypeRow(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No templates for ${PdfTemplateType.displayName(selectedType)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered) { template ->
                        PdfTemplateCard(
                            template = template,
                            onEdit = { onNavigateToDesigner(template.templateId) },
                            onDelete = { viewModel.deleteTemplate(template.templateId) },
                            onSetDefault = { viewModel.setDefaultTemplate(template.templateId) },
                            onPublish = { viewModel.publishTemplate(template.templateId) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.TwoTone.Add,
                contentDescription = "Create Template"
            )
        }
    }

    if (showCreateDialog) {
        CreatePdfTemplateDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type, startFromDefault ->
                viewModel.createTemplate(type, name, startFromDefault) { templateId ->
                    onNavigateToDesigner(templateId)
                }
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun TemplateTypeRow(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PdfTemplateType.all.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(PdfTemplateType.displayName(type)) }
            )
        }
    }
}

@Composable
private fun PdfTemplateCard(
    template: PdfTemplateEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onPublish: () -> Unit
) {
    val isLocked = template.isDefault || template.isSystemDefault
    val statusLabel = if (template.status == PdfTemplateStatus.PUBLISHED) "Published" else "Draft"
    val defaultLabel = when {
        template.isSystemDefault -> "System Default"
        template.isDefault -> "Default"
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (template.isDefault)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = template.templateName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (defaultLabel != null) {
                    Text(
                        text = defaultLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            RowOrColumn {
                Row(
                    modifier = if (it) Modifier.weight(2f) else Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        enabled = !isLocked
                    ) {
                        Icon(Icons.TwoTone.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        enabled = !isLocked
                    ) {
                        Icon(Icons.TwoTone.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
                if (!it) Spacer(Modifier.width(8.dp))
                Column(
                    modifier = if (it) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (template.status == PdfTemplateStatus.DRAFT) {
                        OutlinedButton(
                            onClick = onPublish,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLocked
                        ) {
                            Icon(Icons.TwoTone.Publish, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Publish")
                        }
                    }
                    if (!template.isDefault && template.status == PdfTemplateStatus.PUBLISHED) {
                        Button(
                            onClick = onSetDefault,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.TwoTone.StarBorder, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Default")
                        }
                    } else if (template.isDefault) {
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        ) {
                            Icon(Icons.TwoTone.Star, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Default")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePdfTemplateDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Boolean) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PdfTemplateType.INVOICE) }
    var startFromDefault by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create PDF Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Template type",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PdfTemplateType.all.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(PdfTemplateType.displayName(type)) }
                        )
                    }
                }
                Text(
                    text = "Start from",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !startFromDefault,
                        onClick = { startFromDefault = false },
                        label = { Text("Blank") }
                    )
                    FilterChip(
                        selected = startFromDefault,
                        onClick = { startFromDefault = true },
                        label = { Text("Default") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(templateName, selectedType, startFromDefault) }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
