package com.velox.jewelvault.ui.screen.inventory

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.roomdb.dto.ImportRowStatus
import com.velox.jewelvault.data.roomdb.dto.ImportedItemRow
import com.velox.jewelvault.data.roomdb.dto.ImportSummary
import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.theme.LightGreen
import com.velox.jewelvault.ui.theme.LightRed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.LinearProgressIndicator

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Header Actions
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Import Summary
            if (viewModel.importSummary.value.totalRows > 0) {
                CompactImportSummary(
                    modifier = Modifier.weight(1f),
                    summary = viewModel.importSummary.value
                )
            }
            Spacer(Modifier.width(8.dp))
            ImportHeaderActions(
                modifier = Modifier,
                fileImported = viewModel.importSummary.value.totalRows > 0,
                onExportExample = { viewModel.exportExampleExcel() },
                onChooseFile = {
                    filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                },
                onClearImport = { viewModel.clearImport() }
            )
        }
        if (viewModel.isLoading.value)
            LinearProgressIndicator(
                progress = { viewModel.importProgress.value },
                modifier = Modifier.fillMaxWidth(),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )

        Spacer(modifier = Modifier.height(8.dp))


        // Imported Rows List
        if (viewModel.importedRows.isNotEmpty()) {
            ImportedRowsList(
                rows = viewModel.importedRows,
                onRowClick = { row -> viewModel.selectedRow.value = row },
                selectedRow = viewModel.selectedRow.value,
                viewModel = viewModel
            )
        } else {
            // Empty State
            EmptyImportState()
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
                    viewModel.selectedRow.value!!,
                    categoryId,
                    subCategoryId
                )
                viewModel.showMappingDialog.value = false
            }
        )
    }

    // Confirm Import Dialog
    if (viewModel.showConfirmImportDialog.value) {
        ConfirmImportDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showConfirmImportDialog.value = false }
        )
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onExportExample,
            modifier = Modifier
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null
            )
            if (!fileImported) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Example")
            }
        }

        Button(
            onClick = onChooseFile,
            modifier = Modifier
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null
            )
            if (!fileImported) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("Choose File")
            }
        }

        Button(
            onClick = onClearImport,
            modifier = Modifier
        ) {

            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = null
            )
            if (!fileImported) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            CompactSummaryItem("Total", summary.totalRows.toString(), Color.Black)
            Spacer(Modifier.weight(1f))
            CompactSummaryItem("Valid", summary.validRows.toString(), LightGreen)
            Spacer(Modifier.weight(1f))
            CompactSummaryItem("Needs Fix", summary.needsMappingRows.toString(), Color.Yellow)
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
            color = Color.Gray
        )
    }
}

@Composable
private fun CompactBulkMappingActions(viewModel: ImportItemsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    modifier = Modifier.weight(1f)
                )

                CusOutlinedTextField(
                    state = InputFieldState().apply {
                        text = viewModel.bulkSubCategoryMapping.value ?: ""
                    },
                    placeholderText = "Select SubCategory",
                    dropdownItems = viewModel.subCategories
                        .filter { it.catId == viewModel.bulkCategoryMapping.value }
                        .map { it.subCatName },
                    onDropdownItemSelected = { subCategoryName ->
                        val subCategory =
                            viewModel.subCategories.find { it.subCatName == subCategoryName }
                        viewModel.bulkSubCategoryMapping.value = subCategory?.subCatId
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.applyBulkMapping() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply Mapping")
                }

                OutlinedButton(
                    onClick = { viewModel.exportErrors() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null
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
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null
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
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null
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
                    imageVector = Icons.Default.Description,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Improvements Report")
            }
        }
    }
}

@Composable
private fun ImportedRowsList(
    rows: List<ImportedItemRow>,
    onRowClick: (ImportedItemRow) -> Unit,
    selectedRow: ImportedItemRow?,
    viewModel: ImportItemsViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.getFixSummary(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.showConfirmImportDialog.value = true },
                enabled = viewModel.isImportReady()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import Valid Items (${viewModel.getImportableItemsCount()})")
            }


        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bulk Actions
            if (viewModel.importSummary.value.needsMappingRows > 0) {
                item {
                    CompactBulkMappingActions(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(rows) { row ->
                ImportedRowItem(
                    row = row,
                    viewModel = viewModel,
                    isSelected = selectedRow == row,
                    onClick = { onRowClick(row) }
                )
            }
        }
    }
}

@Composable
private fun ImportedRowItem(
    row: ImportedItemRow,
    viewModel: ImportItemsViewModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (row.status) {
        ImportRowStatus.VALID -> Color(0xFF4CAF50) // Green
        ImportRowStatus.NEEDS_MAPPING -> Color(0xFFFF9800) // Orange
        ImportRowStatus.ERROR -> Color(0xFFF44336) // Red
    }

    val statusIcon = when (row.status) {
        ImportRowStatus.VALID -> Icons.Default.CheckCircle
        ImportRowStatus.NEEDS_MAPPING -> Icons.Default.Warning
        ImportRowStatus.ERROR -> Icons.Default.Error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Status header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Row #${row.rowNumber}",
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (row.status == ImportRowStatus.NEEDS_MAPPING)
                            Color.Yellow.copy(alpha = 0.1f) else LightRed.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = row.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (row.status == ImportRowStatus.NEEDS_MAPPING) Color.Red else LightRed
                        )

                        // Show suggestion for mapping rows
                        if (row.status == ImportRowStatus.NEEDS_MAPPING) {
                            val suggestion = viewModel.getBestMappingSuggestion(row)
                            if (suggestion != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Suggestion: ${suggestion.first.catName} → ${suggestion.second.subCatName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Green,
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
                // Basic Information
                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.itemName),
                    placeholderText = "Item Name",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.entryType),
                    placeholderText = "Entry Type",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.quantity.toString()),
                    placeholderText = "Qty",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.category),
                    placeholderText = "Category",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.subCategory),
                    placeholderText = "SubCategory",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.unit),
                    placeholderText = "Unit",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                // Weight Information
                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.gsWt?.toString() ?: ""),
                    placeholderText = "GsWt",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.ntWt?.toString() ?: ""),
                    placeholderText = "NtWt",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.fnWt?.toString() ?: ""),
                    placeholderText = "FnWt",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.purity ?: ""),
                    placeholderText = "Purity",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                // Charges and GST
                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.mcType ?: ""),
                    placeholderText = "McType",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.mcChr?.toString() ?: ""),
                    placeholderText = "McChr",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.othChr ?: ""),
                    placeholderText = "OthChr",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.chr?.toString() ?: ""),
                    placeholderText = "Chr",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                // GST Percentage Fields
                CusOutlinedTextField(
                    state = InputFieldState(
                        initValue = row.cgst?.toString() ?: ""
                    ), // CGST percentage
                    placeholderText = "CGST %",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(
                        initValue = row.sgst?.toString() ?: ""
                    ), // SGST percentage
                    placeholderText = "SGST %",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(
                        initValue = row.igst?.toString() ?: ""
                    ), // IGST percentage
                    placeholderText = "IGST %",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                // Additional Information
                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.huid ?: ""),
                    placeholderText = "HUID",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.addDate ?: ""),
                    placeholderText = "Date",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.addDesKey ?: ""),
                    placeholderText = "DesKey",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.addDesValue ?: ""),
                    placeholderText = "DesValue",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.extra ?: ""),
                    placeholderText = "Extra",
                    modifier = Modifier.wrapContentWidth(),
                    onTextChange = { /* Direct editing not supported in new structure */ }
                )
            }

            // Action buttons for rows that need special handling
            if (row.status == ImportRowStatus.NEEDS_MAPPING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.showMappingDialogForRow(row) },
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Map Categories", style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = {
                            if (viewModel.applyBestSuggestion(row)) {
                                // Success - row is now mapped
                            } else {
                                // No suggestion available
                            }
                        },
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quick Fix", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyImportState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No items imported yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )

            Text(
                text = "Upload an Excel file to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Map Categories") },
        text = {
            Column {
                Text("Row #${row.rowNumber}: ${row.itemName}")
                Spacer(modifier = Modifier.height(16.dp))

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.mappedCategoryId ?: ""),
                    placeholderText = "Select Category",
                    dropdownItems = categories.map { it.catName },
                    onDropdownItemSelected = { categoryName ->
                        val category = categories.find { it.catName == categoryName }
                        row.mappedCategoryId = category?.catId
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CusOutlinedTextField(
                    state = InputFieldState(initValue = row.mappedSubCategoryId ?: ""),
                    placeholderText = "Select SubCategory",
                    dropdownItems = subCategories.map { it.subCatName },
                    onDropdownItemSelected = { subCategoryName ->
                        val subCategory = subCategories.find { it.subCatName == subCategoryName }
                        row.mappedSubCategoryId = subCategory?.subCatId
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (row.mappedCategoryId != null && row.mappedSubCategoryId != null) {
                        row.status = ImportRowStatus.VALID
                        row.errorMessage = null
                        onConfirm(row.mappedCategoryId!!, row.mappedSubCategoryId!!)
                    }
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfirmImportDialog(
    viewModel: ImportItemsViewModel,
    onDismiss: () -> Unit
) {
    val summary = viewModel.importSummary.value
    val validRows = summary.validRows

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Import") },
        text = {
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
                                color = Color.Gray
                            )
                        }

                        if (importPreview.size > 10) {
                            item {
                                Text(
                                    text = "... and ${importPreview.size - 10} more items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text("This action cannot be undone. Continue?")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.confirmImport()
                    onDismiss()
                }
            ) {
                Text("Import Items")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
