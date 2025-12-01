package com.velox.jewelvault.ui.screen.label

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import com.velox.jewelvault.data.model.label.LabelElementType
import com.velox.jewelvault.utils.generateId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelDesignerScreen(
    templateId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: LabelTemplateViewModel = hiltViewModel()
) {
    var templateName by remember { mutableStateOf("") }
    var labelWidth by remember { mutableStateOf(100f) }
    var labelHeight by remember { mutableStateOf(50f) }
    var showCreateDialog by remember { mutableStateOf(templateId == null) }
    var showElementDialog by remember { mutableStateOf(false) }
    var selectedElementType by remember { mutableStateOf<LabelElementType?>(null) }
    
    val templates by viewModel.savedTemplates.collectAsState()
    
    // Load existing template if templateId is provided
    LaunchedEffect(templateId) {
        templateId?.let { id ->
            viewModel.getTemplateWithElements(id)?.let { template ->
                templateName = template.template.templateName
                labelWidth = template.template.labelWidth
                labelHeight = template.template.labelHeight
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Label Designer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showElementDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Element")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Template Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Template Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Template Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = labelWidth.toString(),
                            onValueChange = { labelWidth = it.toFloatOrNull() ?: 100f },
                            label = { Text("Width (mm)") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = labelHeight.toString(),
                            onValueChange = { labelHeight = it.toFloatOrNull() ?: 50f },
                            label = { Text("Height (mm)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (templateId != null) {
                                    // Update existing template
                                    val template = templates.find { it.templateId == templateId }
                                    template?.let {
                                        viewModel.updateTemplate(
                                            it.copy(
                                                templateName = templateName,
                                                labelWidth = labelWidth,
                                                labelHeight = labelHeight
                                            )
                                        )
                                    }
                                } else {
                                    // Create new template
                                    viewModel.createTemplate(
                                        templateName = templateName,
                                        templateType = "FREE_LABEL",
                                        labelWidth = labelWidth,
                                        labelHeight = labelHeight
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (templateId != null) "Update Template" else "Create Template")
                        }
                        
                        if (templateId != null) {
                            Button(
                                onClick = {
                                    templateId.let { id ->
                                        viewModel.deleteTemplate(id)
                                        onNavigateBack()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Label Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Label Preview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Label canvas placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Label Canvas\n${labelWidth.toInt()}mm × ${labelHeight.toInt()}mm",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Element Types
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Add Elements",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(getElementTypes()) { elementType ->
                            ElementTypeCard(
                                elementType = elementType,
                                onClick = {
                                    selectedElementType = elementType
                                    showElementDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Element Type Selection Dialog
    if (showElementDialog && selectedElementType != null) {
        ElementPropertiesDialog(
            elementType = selectedElementType!!,
            onDismiss = {
                showElementDialog = false
                selectedElementType = null
            },
            onAdd = { elementType ->
                // Add element to template
                templateId?.let { id ->
                    viewModel.addElement(
                        templateId = id,
                        elementType = elementType,
                        x = 10f,
                        y = 10f,
                        width = 30f,
                        height = 10f,
                        zIndex = 0
                    )
                }
                showElementDialog = false
                selectedElementType = null
            }
        )
    }
}

@Composable
private fun ElementTypeCard(
    elementType: LabelElementType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (elementType) {
                    is LabelElementType.TextElement -> Icons.Default.Edit
                    is LabelElementType.ImageElement -> Icons.Default.Add
                    is LabelElementType.QRElement -> Icons.Default.Add
                    is LabelElementType.BarcodeElement -> Icons.Default.Add
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = when (elementType) {
                    is LabelElementType.TextElement -> "Text Element"
                    is LabelElementType.ImageElement -> "Image Element"
                    is LabelElementType.QRElement -> "QR Code Element"
                    is LabelElementType.BarcodeElement -> "Barcode Element"
                },
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ElementPropertiesDialog(
    elementType: LabelElementType,
    onDismiss: () -> Unit,
    onAdd: (LabelElementType) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var fontSize by remember { mutableStateOf(12f) }
    var isBold by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Element") },
        text = {
            Column {
                when (elementType) {
                    is LabelElementType.TextElement -> {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = fontSize.toString(),
                            onValueChange = { fontSize = it.toFloatOrNull() ?: 12f },
                            label = { Text("Font Size") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isBold,
                                onCheckedChange = { isBold = it }
                            )
                            Text("Bold")
                        }
                    }
                    else -> {
                        Text("Element configuration coming soon...")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (elementType) {
                        is LabelElementType.TextElement -> {
                            onAdd(
                                LabelElementType.TextElement(
                                    text = text,
                                    fontSize = fontSize,
                                    isBold = isBold
                                )
                            )
                        }
                        else -> onAdd(elementType)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getElementTypes(): List<LabelElementType> {
    return listOf(
        LabelElementType.TextElement(
            text = "Sample Text",
            fontSize = 12f
        ),
        LabelElementType.ImageElement(
            imageUri = null,
            imagePath = null
        ),
        LabelElementType.QRElement(
            qrData = "Sample QR Data",
            size = 20f
        ),
        LabelElementType.BarcodeElement(
            barcodeData = "123456789",
            width = 50f,
            height = 20f
        )
    )
}
