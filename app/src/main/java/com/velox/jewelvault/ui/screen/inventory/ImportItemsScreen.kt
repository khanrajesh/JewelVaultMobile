package com.velox.jewelvault.ui.screen.inventory

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.AutoFixHigh
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.Download
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.UploadFile
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.data.roomdb.dto.ImportRowStatus
import com.velox.jewelvault.data.roomdb.dto.ImportSummary
import com.velox.jewelvault.data.roomdb.dto.ImportedItemRow
import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.ui.components.WidthThenHeightSpacer
import com.velox.jewelvault.ui.theme.LightGreen
import com.velox.jewelvault.ui.theme.LightRed
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.isLandscape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportItemsScreen(
    viewModel: ImportItemsViewModel,
) {

    viewModel.currentScreenHeadingState.value = "Import Items"

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.parseExcelFile(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp),
    ) {
        item {
            // Header Actions
            RowOrColumn(
                rowModifier = Modifier.fillMaxWidth(),
                columnModifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) { isLandscape ->
                // Compact Import Summary
                if (viewModel.importSummary.value.totalRows > 0) {
                    CompactImportSummary(
                        modifier = if (isLandscape) Modifier else Modifier.fillMaxWidth(),
                        summary = viewModel.importSummary.value
                    )
                }
                if (viewModel.importSummary.value.totalRows > 0) {
                    WidthThenHeightSpacer(8.dp)
                }
                ImportHeaderActions(
                    modifier = if (isLandscape) Modifier else Modifier.fillMaxWidth(),
                    fileImported = viewModel.importSummary.value.totalRows > 0,
                    onExportExample = { viewModel.exportExampleExcel() },
                    onChooseFile = {
                        filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    },
                    onClearImport = { viewModel.clearImport() })
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        if (viewModel.isLoading.value) {
            item {
                LinearProgressIndicator(
                    progress = { viewModel.importProgress.value },
                    modifier = Modifier.fillMaxWidth(),
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Imported Rows List
        if (viewModel.importedRows.isNotEmpty()) {
            importedRowsList(
                rows = viewModel.importedRows,
                onRowClick = { row -> viewModel.selectedRow.value = row },
                selectedRow = viewModel.selectedRow.value,
                viewModel = viewModel
            )
        } else {
            item {
                // Empty State
                EmptyImportState()
            }
        }
    }

    // Mapping Dialog
    if (viewModel.showMappingDialog.value) {
        CategoryMappingDialog(
            row = viewModel.selectedRow.value!!,
            categories = viewModel.categories,
            subCategories = viewModel.subCategories,
            onDismiss = { viewModel.showMappingDialog.value = false },
            onConfirm = { categoryId, subCategoryId ->
                viewModel.updateItemCategory(
                    viewModel.selectedRow.value!!, categoryId, subCategoryId
                )
                viewModel.showMappingDialog.value = false
            })
    }

    // Confirm Import Dialog
    if (viewModel.showConfirmImportDialog.value) {
        ConfirmImportDialog(
            viewModel = viewModel, onDismiss = { viewModel.showConfirmImportDialog.value = false })
    }

}

@Composable
private fun ImportHeaderActions(
    modifier: Modifier = Modifier,
    fileImported: Boolean,
    onExportExample: () -> Unit,
    onChooseFile: () -> Unit,
    onClearImport: () -> Unit
) {

    val isLandScape = isLandscape()
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onExportExample, modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.TwoTone.Download, contentDescription = null
            )
            if (!fileImported && isLandScape) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Example")
            }
        }

        Button(
            onClick = onChooseFile, modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.TwoTone.UploadFile, contentDescription = null
            )
            if (!fileImported && isLandScape) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("Choose File")
            }
        }

        Button(
            onClick = onClearImport, modifier = Modifier.weight(1f)
        ) {

            Icon(
                imageVector = Icons.TwoTone.Clear, contentDescription = null
            )
            if (!fileImported && isLandScape) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear")
            }
        }
    }


}


@Composable
private fun CompactImportSummary(modifier: Modifier = Modifier, summary: ImportSummary) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            CompactSummaryItem(
                "Total",
                summary.totalRows.toString(),
                MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            CompactSummaryItem("Valid", summary.validRows.toString(), LightGreen)
            Spacer(Modifier.weight(1f))
            CompactSummaryItem(
                "Needs Fix",
                summary.needsMappingRows.toString(),
                Color(0xFFFF9800)
            ) // Orange
            Spacer(Modifier.weight(1f))
            CompactSummaryItem("Errors", summary.errorRows.toString(), LightRed)
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactSummaryItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactBulkMappingActions(viewModel: ImportItemsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            RowOrColumn(
                rowModifier = Modifier.fillMaxWidth(),
                columnModifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) { isLandScape ->
                CusOutlinedTextField(
                    state = InputFieldState().apply {
                    text = viewModel.bulkCategoryMapping.value ?: ""
                },
                    placeholderText = "Select Category",
                    dropdownItems = viewModel.categories.map { it.catName },
                    onDropdownItemSelected = { categoryName ->
                        val category = viewModel.categories.find { it.catName == categoryName }
                        viewModel.bulkCategoryMapping.value = category?.catId
                        viewModel.bulkSubCategoryMapping.value = null
                    },
                    modifier = if (isLandScape) Modifier.weight(1f) else Modifier
                )

                CusOutlinedTextField(
                    state = InputFieldState().apply {
                    text = viewModel.bulkSubCategoryMapping.value ?: ""
                },
                    placeholderText = "Select SubCategory",
                    dropdownItems = viewModel.subCategories.filter { it.catId == viewModel.bulkCategoryMapping.value }
                        .map { it.subCatName },
                    onDropdownItemSelected = { subCategoryName ->
                        val subCategory =
                            viewModel.subCategories.find { it.subCatName == subCategoryName }
                        viewModel.bulkSubCategoryMapping.value = subCategory?.subCatId
                    },
                    modifier = if (isLandScape) Modifier.weight(1f) else Modifier
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.applyBulkMapping() }, modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.CheckCircle, contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply Mapping")
                }

                OutlinedButton(
                    onClick = { viewModel.exportErrors() }, modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Download, contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Errors")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.autoMapSimilarRows() },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.importSummary.value.needsMappingRows > 0
            ) {
                Icon(
                    imageVector = Icons.TwoTone.AutoFixHigh, contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Auto-Map Similar Categories (${viewModel.getAutoMappableRowsCount()})")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val appliedCount = viewModel.applyAllSuggestions()
                    if (appliedCount > 0) {
                        // Success message will be shown via snackbar
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.importSummary.value.needsMappingRows > 0
            ) {
                Icon(
                    imageVector = Icons.TwoTone.CheckCircle, contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply All Suggestions (${viewModel.getAutoMappableRowsCount()})")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.exportImprovementsReport() },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.importedRows.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Description, contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Improvements Report")
            }
        }
    }
}

private fun LazyListScope.importedRowsList(
    rows: List<ImportedItemRow>,
    onRowClick: (ImportedItemRow) -> Unit,
    selectedRow: ImportedItemRow?,
    viewModel: ImportItemsViewModel
) {
    item {
        RowOrColumn(
            rowModifier = Modifier.fillMaxWidth(),
            columnModifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Imported Items (${rows.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Show summary of what needs to be fixed
            if (!viewModel.isImportReady()) {
                WidthThenHeightSpacer()
                Text(
                    text = viewModel.getFixSummary(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                )
            }
            WidthThenHeightSpacer()

            Button(
                onClick = { viewModel.showConfirmImportDialog.value = true },
                enabled = viewModel.isImportReady()
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Add, contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import Valid Items (${viewModel.getImportableItemsCount()})")
            }
        }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }

    if (viewModel.importSummary.value.needsMappingRows > 0) {
        item {
            CompactBulkMappingActions(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    itemsIndexed(rows) { index, row ->
        ImportedRowItem(
            row = row,
            viewModel = viewModel,
            isSelected = selectedRow == row,
            onClick = { onRowClick(row) })
        if (index < rows.lastIndex) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportedRowItem(
    row: ImportedItemRow, viewModel: ImportItemsViewModel, isSelected: Boolean, onClick: () -> Unit
) {
    val statusColor = when (row.status) {
        ImportRowStatus.VALID -> LightGreen
        ImportRowStatus.NEEDS_MAPPING -> Color(0xFFFF9800) // Orange for warning
        ImportRowStatus.ERROR -> LightRed
    }

    val statusIcon = when (row.status) {
        ImportRowStatus.VALID -> Icons.TwoTone.CheckCircle
        ImportRowStatus.NEEDS_MAPPING -> Icons.TwoTone.Warning
        ImportRowStatus.ERROR -> Icons.TwoTone.Error
    }

    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .then(
            if (isSelected) {
                Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                )
            } else {
                Modifier
            }
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Status header row
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Row : ${row.rowNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = row.status.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error message if any
            if (row.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = if (row.status == ImportRowStatus.NEEDS_MAPPING) Color(
                            0xFFFF9800
                        ).copy(alpha = 0.1f) // Orange for warning
                        else LightRed.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = row.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (row.status == ImportRowStatus.NEEDS_MAPPING) Color(
                                0xFFFF9800
                            ) // Orange for warning
                            else LightRed
                        )

                        // Show suggestion for mapping rows
                        if (row.status == ImportRowStatus.NEEDS_MAPPING) {
                            val suggestion = viewModel.getBestMappingSuggestion(row)
                            if (suggestion != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Suggestion: ${suggestion.first.catName} → ${suggestion.second.subCatName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Single row with all fields (horizontal scrolling if needed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Create reactive state variables that update when row changes
                var itemName by remember { mutableStateOf(row.itemName ?: "") }
                var entryType by remember { mutableStateOf(row.entryType ?: "") }
                var quantity by remember { mutableStateOf(row.quantity?.toString() ?: "") }
                var category by remember { mutableStateOf(row.category ?: "") }
                var subCategory by remember { mutableStateOf(row.subCategory ?: "") }
                var unit by remember { mutableStateOf(row.unit ?: "") }
                var gsWt by remember { mutableStateOf(row.gsWt?.toString() ?: "") }
                var ntWt by remember { mutableStateOf(row.ntWt?.toString() ?: "") }
                var fnWt by remember { mutableStateOf(row.fnWt?.toString() ?: "") }
                var purity by remember { mutableStateOf(row.purity ?: "") }
                var mcType by remember { mutableStateOf(row.mcType ?: "") }
                var mcChr by remember { mutableStateOf(row.mcChr?.toString() ?: "") }
                var othChr by remember { mutableStateOf(row.othChr ?: "") }
                var chr by remember { mutableStateOf(row.chr?.toString() ?: "") }
                var cgst by remember { mutableStateOf(row.cgst?.toString() ?: "") }
                var sgst by remember { mutableStateOf(row.sgst?.toString() ?: "") }
                var igst by remember { mutableStateOf(row.igst?.toString() ?: "") }
                var huid by remember { mutableStateOf(row.huid ?: "") }
                var addDesKey by remember { mutableStateOf(row.addDesKey ?: "") }
                var addDesValue by remember { mutableStateOf(row.addDesValue ?: "") }
                var extra by remember { mutableStateOf(row.extra ?: "") }

                // Update state when row data changes (for reactive updates)
                LaunchedEffect(row.itemName) { itemName = row.itemName ?: "" }
                LaunchedEffect(row.entryType) { entryType = row.entryType ?: "" }
                LaunchedEffect(row.quantity) { quantity = row.quantity?.toString() ?: "" }
                LaunchedEffect(row.category) { category = row.category ?: "" }
                LaunchedEffect(row.subCategory) { subCategory = row.subCategory ?: "" }
                LaunchedEffect(row.unit) { unit = row.unit ?: "" }
                LaunchedEffect(row.gsWt) { gsWt = row.gsWt?.toString() ?: "" }
                LaunchedEffect(row.ntWt) { ntWt = row.ntWt?.toString() ?: "" }
                LaunchedEffect(row.fnWt) { fnWt = row.fnWt?.toString() ?: "" }
                LaunchedEffect(row.purity) { purity = row.purity ?: "" }
                LaunchedEffect(row.mcType) { mcType = row.mcType ?: "" }
                LaunchedEffect(row.mcChr) { mcChr = row.mcChr?.toString() ?: "" }
                LaunchedEffect(row.othChr) { othChr = row.othChr ?: "" }
                LaunchedEffect(row.chr) { chr = row.chr?.toString() ?: "" }
                LaunchedEffect(row.cgst) { cgst = row.cgst?.toString() ?: "" }
                LaunchedEffect(row.sgst) { sgst = row.sgst?.toString() ?: "" }
                LaunchedEffect(row.igst) { igst = row.igst?.toString() ?: "" }
                LaunchedEffect(row.huid) { huid = row.huid ?: "" }
                LaunchedEffect(row.addDesKey) { addDesKey = row.addDesKey ?: "" }
                LaunchedEffect(row.addDesValue) { addDesValue = row.addDesValue ?: "" }
                LaunchedEffect(row.extra) { extra = row.extra ?: "" }

                // Basic Information
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { text ->
                        itemName = text
                        row.itemName = text
                    },
                    label = { Text("Item Name") },
                    placeholder = { Text("Enter Item Name") },
                    modifier = Modifier.wrapContentWidth(),
                    singleLine = true
                )

                // Entry Type dropdown
                var entryTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = entryTypeExpanded,
                    onExpandedChange = { entryTypeExpanded = !entryTypeExpanded }) {
                    OutlinedTextField(
                        value = entryType,
                        onValueChange = { /* Read-only for dropdown */ },
                        label = { Text("Entry Type") },
                        placeholder = { Text("Select Entry Type") },
                        modifier = Modifier
                            .wrapContentWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entryTypeExpanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = entryTypeExpanded,
                        onDismissRequest = { entryTypeExpanded = false }) {
                        EntryType.list().forEach { entryTypeValue ->
                            DropdownMenuItem(text = { Text(entryTypeValue) }, onClick = {
                                entryType = entryTypeValue
                                row.entryType = entryTypeValue
                                // Auto-fill quantity for Piece type
                                if (entryTypeValue == EntryType.Piece.type) {
                                    row.quantity = 1
                                    quantity = "1"
                                }
                                entryTypeExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { text ->
                        quantity = text
                        val qtyValue = text.toIntOrNull()
                        if (qtyValue != null && qtyValue > 0) {
                            row.quantity = qtyValue
                        } else if (text.isEmpty()) {
                            row.quantity = 1
                        }
                    },
                    label = { Text("Qty") },
                    placeholder = { Text("Enter Qty") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { text ->
//                        category = text
//                        row.category = text
                    },
                    label = { Text("Category") },
                    placeholder = { Text("Enter Category") },
                    modifier = Modifier.wrapContentWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = subCategory,
                    onValueChange = { text ->
//                        subCategory = text
//                        row.subCategory = text
                    },
                    label = { Text("SubCategory") },
                    placeholder = { Text("Enter SubCategory") },
                    modifier = Modifier.wrapContentWidth(),
                    singleLine = true
                )


                // Weight Information - Using reactive state for better reactivity
                OutlinedTextField(
                    value = gsWt,
                    onValueChange = { text ->
                        gsWt = text
                        val gsWtValue = text.toDoubleOrNull()
                        viewModel.updateGsWt(row, gsWtValue)
                    },
                    label = { Text("GsWt") },
                    placeholder = { Text("Enter GsWt") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ntWt,
                    onValueChange = { text ->
                        ntWt = text
                        val ntWtValue = text.toDoubleOrNull()
                        viewModel.updateNtWt(row, ntWtValue)
                    },
                    label = { Text("NtWt") },
                    placeholder = { Text("Enter NtWt") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )


                // Purity dropdown using ExposedDropdownMenuBox for better reactivity
                var purityExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = purityExpanded,
                    onExpandedChange = { purityExpanded = !purityExpanded }) {
                    OutlinedTextField(
                        value = purity,
                        onValueChange = { /* Read-only for dropdown */ },
                        label = { Text("Purity") },
                        placeholder = { Text("Select Purity") },
                        modifier = Modifier
                            .wrapContentWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = purityExpanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = purityExpanded, onDismissRequest = { purityExpanded = false }) {
                        Purity.list().forEach { purityValue ->
                            DropdownMenuItem(text = { Text(purityValue) }, onClick = {
                                purity = purityValue
                                row.purity = purityValue
                                purityExpanded = false
                                viewModel.updatePurity(row, purityValue)
                            })
                        }
                    }
                }

                OutlinedTextField(
                    value = fnWt,
                    onValueChange = { /* Read-only */ },
                    label = { Text("FnWt (Auto-calculated)") },
                    placeholder = { Text("Auto-calculated") },
                    modifier = Modifier.wrapContentWidth(),
                    enabled = true,
                    singleLine = true
                )

                // Charges and GST
                var mcTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = mcTypeExpanded,
                    onExpandedChange = { mcTypeExpanded = !mcTypeExpanded }) {
                    OutlinedTextField(
                        value = mcType,
                        onValueChange = { },
                        label = { Text("McType") },
                        placeholder = { Text("Select McType") },
                        modifier = Modifier
                            .wrapContentWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mcTypeExpanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = mcTypeExpanded, onDismissRequest = { mcTypeExpanded = false }) {
                        ChargeType.list().forEach { mcTypeValue ->
                            DropdownMenuItem(text = { Text(mcTypeValue) }, onClick = {
                                mcType = mcTypeValue
                                row.mcType = mcTypeValue
                                mcTypeExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(
                    value = mcChr,
                    onValueChange = { text ->
                        mcChr = text
                        val mcChrValue = text.toDoubleOrNull()
                        if (mcChrValue != null && mcChrValue >= 0) {
                            row.mcChr = mcChrValue
                        } else if (text.isEmpty()) {
                            row.mcChr = null
                        }
                    },
                    label = { Text("McChr") },
                    placeholder = { Text("Enter McChr") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = othChr,
                    onValueChange = { text ->
                        othChr = text
                        row.othChr = text
                    },
                    label = { Text("OthChr") },
                    placeholder = { Text("Enter OthChr") },
                    modifier = Modifier.wrapContentWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = chr,
                    onValueChange = { text ->
                        chr = text
                        val chrValue = text.toDoubleOrNull()
                        if (chrValue != null && chrValue >= 0) {
                            row.chr = chrValue
                        } else if (text.isEmpty()) {
                            row.chr = null
                        }
                    },
                    label = { Text("Chr") },
                    placeholder = { Text("Enter Chr") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { text ->
//                        unit = text
//                        row.unit = text
                    },
                    label = { Text("Unit") },
                    placeholder = { Text("Enter Unit") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // GST Percentage Fields
                OutlinedTextField(
                    value = cgst,
                    onValueChange = { text ->
                        cgst = text
                        val cgstValue = text.toDoubleOrNull()
                        if (cgstValue != null && cgstValue >= 0 && cgstValue <= 100) {
                            row.cgst = cgstValue
                        } else if (text.isEmpty()) {
                            row.cgst = null
                        }
                    },
                    label = { Text("CGST %") },
                    placeholder = { Text("Enter CGST %") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sgst,
                    onValueChange = { text ->
                        sgst = text
                        val sgstValue = text.toDoubleOrNull()
                        if (sgstValue != null && sgstValue >= 0 && sgstValue <= 100) {
                            row.sgst = sgstValue
                        } else if (text.isEmpty()) {
                            row.sgst = null
                        }
                    },
                    label = { Text("SGST %") },
                    placeholder = { Text("Enter SGST %") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = igst,
                    onValueChange = { text ->
                        igst = text
                        val igstValue = text.toDoubleOrNull()
                        if (igstValue != null && igstValue >= 0 && igstValue <= 100) {
                            row.igst = igstValue
                        } else if (text.isEmpty()) {
                            row.igst = null
                        }
                    },
                    label = { Text("IGST %") },
                    placeholder = { Text("Enter IGST %") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Additional Information
                OutlinedTextField(
                    value = huid,
                    onValueChange = { text ->
                        // Format HUID to uppercase and limit to 6 characters
                        val formattedHuid = text.trim().uppercase().take(6)
                        huid = formattedHuid
                        row.huid = formattedHuid
                    },
                    label = { Text("HUID") },
                    placeholder = { Text("Enter HUID") },
                    modifier = Modifier.wrapContentWidth(),
                    singleLine = true
                )


                OutlinedTextField(
                    value = addDesKey,
                    onValueChange = { text ->
                        addDesKey = text
                        row.addDesKey = text
                    },
                    label = { Text("DesKey") },
                    placeholder = { Text("Enter DesKey") },
                    modifier = Modifier.wrapContentWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = addDesValue,
                    onValueChange = { text ->
                        addDesValue = text
                        row.addDesValue = text
                    },
                    label = { Text("DesValue") },
                    placeholder = { Text("Enter DesValue") },
                    modifier = Modifier.wrapContentWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = extra,
                    onValueChange = { text ->
                        extra = text
                        row.extra = text
                    },
                    label = { Text("Extra") },
                    placeholder = { Text("Enter Extra") },
                    modifier = Modifier.wrapContentWidth(),
                    singleLine = true
                )
            }

            // Action buttons for all rows
            Spacer(modifier = Modifier.height(8.dp))
            RowOrColumn(
                rowModifier = Modifier.fillMaxWidth(),
                columnModifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) { isLandscape ->
                val showMappingActions =
                    row.status == ImportRowStatus.NEEDS_MAPPING || row.status == ImportRowStatus.ERROR
                if (showMappingActions) {
                    RowOrColumn(
                        rowModifier = if (isLandscape) Modifier else Modifier.fillMaxWidth(),
                        columnModifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.showMappingDialogForRow(row) },
                            modifier = if (it) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Map Categories", style = MaterialTheme.typography.bodySmall)
                        }

                        // Quick Fix button only for items that need mapping (not for errors)
                        if (row.status == ImportRowStatus.NEEDS_MAPPING) {
                            WidthThenHeightSpacer(8.dp)
                            OutlinedButton(
                                onClick = {
                                    if (viewModel.applyBestSuggestion(row)) {
                                        // Success - row is now mapped
                                    } else {
                                        // No suggestion available
                                    }
                                },
                                modifier = if (it) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.TwoTone.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Quick Fix", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    WidthThenHeightSpacer(8.dp)
                }

                // Right side: Remove button for all rows
                OutlinedButton(
                    onClick = { viewModel.removeItem(row) },
                    modifier = if (isLandscape) Modifier.wrapContentWidth() else Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyImportState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.TwoTone.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No items imported yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Upload an Excel file to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CategoryMappingDialog(
    row: ImportedItemRow,
    categories: List<CategoryEntity>,
    subCategories: List<SubCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    // State for selected category in the dialog
    var selectedCategoryId by remember { mutableStateOf(row.mappedCategoryId) }
    var selectedSubCategoryId by remember { mutableStateOf(row.mappedSubCategoryId) }

    // Filter subcategories based on selected category
    val filteredSubCategories = remember(selectedCategoryId) {
        if (selectedCategoryId != null) {
            subCategories.filter { it.catId == selectedCategoryId }
        } else {
            emptyList()
        }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Map Categories") }, text = {
        Column {
            Text("Row #${row.rowNumber}: ${row.itemName}")
            Spacer(modifier = Modifier.height(16.dp))

            CusOutlinedTextField(
                state = InputFieldState(initValue = selectedCategoryId?.let { catId ->
                categories.find { it.catId == catId }?.catName ?: ""
            } ?: ""),
                placeholderText = "Select Category",
                dropdownItems = categories.map { it.catName },
                onDropdownItemSelected = { categoryName ->
                    val category = categories.find { it.catName == categoryName }
                    selectedCategoryId = category?.catId
                    // Clear subcategory selection when category changes
                    selectedSubCategoryId = null
                    row.mappedCategoryId = category?.catId
                    row.mappedSubCategoryId = null
                })

            Spacer(modifier = Modifier.height(8.dp))

            CusOutlinedTextField(
                state = InputFieldState(initValue = selectedSubCategoryId?.let { subCatId ->
                filteredSubCategories.find { it.subCatId == subCatId }?.subCatName ?: ""
            } ?: ""),
                placeholderText = if (selectedCategoryId != null) "Select SubCategory" else "Select Category first",
                dropdownItems = filteredSubCategories.map { it.subCatName },
                onDropdownItemSelected = { subCategoryName ->
                    val subCategory =
                        filteredSubCategories.find { it.subCatName == subCategoryName }
                    selectedSubCategoryId = subCategory?.subCatId
                    row.mappedSubCategoryId = subCategory?.subCatId
                },
                enabled = selectedCategoryId != null
            )
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                if (selectedCategoryId != null && selectedSubCategoryId != null) {
                    row.status = ImportRowStatus.VALID
                    row.errorMessage = null
                    onConfirm(selectedCategoryId!!, selectedSubCategoryId!!)
                }
                onDismiss()
            }) {
            Text("Apply")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
private fun ConfirmImportDialog(
    viewModel: ImportItemsViewModel, onDismiss: () -> Unit
) {
    val summary = viewModel.importSummary.value
    val validRows = summary.validRows

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Confirm Import") }, text = {
        Column {
            Text("You are about to import $validRows items into the database.")
            Spacer(modifier = Modifier.height(8.dp))

            if (summary.needsMappingRows > 0) {
                Text("⚠️ ${summary.needsMappingRows} rows need category mapping.")
            }

            if (summary.errorRows > 0) {
                Text("❌ ${summary.errorRows} rows have errors.")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show preview of what will be imported
            val importPreview = viewModel.getImportPreview()
            if (importPreview.isNotEmpty()) {
                Text(
                    text = "Preview of items to import:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(importPreview.take(10)) { (rowNumber, description) ->
                        Text(
                            text = "Row $rowNumber: $description",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (importPreview.size > 10) {
                        item {
                            Text(
                                text = "... and ${importPreview.size - 10} more items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("This action cannot be undone. Continue?")
        }
    }, confirmButton = {
        Button(
            onClick = {
                viewModel.confirmImport()
                onDismiss()
            }) {
            Text("Import Items")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}
