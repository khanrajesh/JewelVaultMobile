package com.velox.jewelvault.ui.screen.inventory

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Sort
import androidx.compose.material.icons.twotone.KeyboardArrowDown
import androidx.compose.material.icons.twotone.KeyboardArrowUp
import androidx.compose.material.icons.twotone.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.WidthThenHeightSpacer
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.PrintUtils
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.export.enqueueExportWorker
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.to3FString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper function to convert ItemEntity to List<String>
private fun ItemEntity.toListString(index: Int): List<String> = listOf(
    "$index",
    "${catName}", //"${catName} (${catId})",
    "${subCatName}",// "${subCatName} (${subCatId})",
    itemId,
    itemAddName,
    entryType,
    quantity.toString(),
    gsWt.to3FString(),
    ntWt.to3FString(),
    unit,
    purity,
    fnWt.to3FString(),
    crgType,
    crg.to3FString(),
    othCrgDes,
    othCrg.to3FString(),
    (cgst + sgst + igst).to3FString(),
    huid,
    addDate.toString(),
    addDesKey,
    addDesValue,
    purchaseOrderId
)


@Composable
fun InventoryFilterScreen(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    viewModel.currentScreenHeadingState.value = "Inventory Filter"

    // Keep track of subcategories for selected category
    val subCategories = remember { mutableStateListOf<SubCategoryEntity>() }
    var showSortMenu by remember { mutableStateOf(false) }
    val isFilterPanelExpanded = remember { mutableStateOf(true) }

    // State to track exported file URI for print functionality
    var exportedFileUri by remember { mutableStateOf<String?>(null) }

    // State for print dialog
    val showPrintDialog = remember { mutableStateOf(false) }
    val selectedItem = remember { mutableStateOf<ItemEntity?>(null) }

    LaunchedEffect(true) {
        viewModel.clearCategoryOverrides()
        viewModel.categoryFilter.clear()
        viewModel.subCategoryFilter.clear()
        viewModel.getCategoryAndSubCategoryDetails()
        viewModel.loadFirmAndOrderLists()
        viewModel.filterItems()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp)
            )
            .padding(8.dp)

    ) {
        // Header with sort and action buttons
        HeaderSection(
            viewModel = viewModel,
            showSortMenu = showSortMenu,
            onSortMenuToggle = { showSortMenu = !showSortMenu },
            onSortOptionSelected = { sortBy, sortOrder ->
                viewModel.sortBy.value = sortBy
                viewModel.sortOrder.value = sortOrder
                showSortMenu = false
                viewModel.filterItems()
            },
            onClearFilters = {
                viewModel.clearAllFilters()
            },
            onExport = {
                val rows = viewModel.itemList.mapIndexed { index, item ->
                    listOf(
                        (index + 1).toString(),
                        item.catName,
                        item.subCatName,
                        item.itemId,
                        item.itemAddName,
                        item.entryType,
                        item.quantity.toString(),
                        item.gsWt.toString(),
                        item.ntWt.toString(),
                        item.unit,
                        item.purity,
                        item.fnWt.toString(),
                        item.crgType,
                        item.crg.toString(),
                        item.othCrgDes,
                        item.othCrg.toString(),
                        (item.cgst + item.sgst + item.igst).toString(),
                        item.huid,
                        item.addDate.toString(),
                        item.addDesKey,
                        item.addDesValue,
                        "Extra"
                    )
                }
                val fileName = "ItemExport_${System.currentTimeMillis()}.xlsx"
                enqueueExportWorker(
                    context,
                    lifecycleOwner,
                    fileName,
                    viewModel.itemHeaderList,
                    rows,
                    onExportComplete = { fileUri ->
                        fileUri?.let { uri ->
                            // Store the file URI for print functionality
                            exportedFileUri = uri
                            log("Export completed. File URI: $uri")
                        }
                    })
            },
            isFilterPanelExpanded,
            exportedFileUri = exportedFileUri,
            onPrint = { fileUri ->
                // Print the exported file
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse(fileUri),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Print with"))
            })

        Spacer(modifier = Modifier.height(8.dp))

        // Animated filter panel
        AnimatedVisibility(
            visible = isFilterPanelExpanded.value,
            enter = slideInVertically(
                animationSpec = tween(300),
                initialOffsetY = { -it }) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                animationSpec = tween(300),
                targetOffsetY = { -it }) + fadeOut(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {


                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Row 1: Category, Subcategory, Entry Type, Purity
                    RowOrColumn {

                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.categoryFilter,
                            placeholderText = "Category",
                            dropdownItems = viewModel.catSubCatDto.map { it.catName },
                            onDropdownItemSelected = { selected ->
                                val selectedCat = viewModel.catSubCatDto.find { it.catName == selected }
                                subCategories.clear()
                                selectedCat?.subCategoryList?.let { subCategories.addAll(it) }
                                selectedCat?.let { viewModel.setCategoryOverride(it.catId, it.catName) }
                                viewModel.subCategoryFilter.text = ""
                            })
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.subCategoryFilter,
                            placeholderText = "Sub Category",
                            dropdownItems = subCategories.map { it.subCatName },
                            onDropdownItemSelected = { selected ->
                                val selectedSubCat =
                                    subCategories.find { it.subCatName == selected }
                                if (selectedSubCat != null) {
                                    viewModel.setSubCategoryOverride(
                                        selectedSubCat.subCatId,
                                        selectedSubCat.subCatName
                                    )
                                } else {
                                    viewModel.subCategoryFilter.text = selected
                                }
                            })
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.entryTypeFilter,
                            placeholderText = "Entry Type",
                            dropdownItems = EntryType.list(),
                            onDropdownItemSelected = { selected ->
                                viewModel.entryTypeFilter.text = selected
                            })
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.purityFilter,
                            placeholderText = "Purity",
                            dropdownItems = Purity.list(),
                            onDropdownItemSelected = { selected ->
                                viewModel.purityFilter.text = selected
                            })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Row 2: Charge Type, Firm, Purchase Order, Date Range
                    RowOrColumn {
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.chargeTypeFilter,
                            placeholderText = "Charge Type",
                            dropdownItems = ChargeType.list(),
                            onDropdownItemSelected = { selected ->
                                viewModel.chargeTypeFilter.text = selected
                            })
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.firmIdFilter,
                            placeholderText = "Firm",
                            dropdownItems = viewModel.firmList.map { it.second },
                            onDropdownItemSelected = { selected ->
                                val firm = viewModel.firmList.find { it.second == selected }
                                viewModel.firmIdFilter.text = firm?.first?.toString() ?: ""
                            })
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.purchaseOrderIdFilter,
                            placeholderText = "Purchase Order",
                            dropdownItems = viewModel.purchaseOrderList.map { it.second },
                            onDropdownItemSelected = { selected ->
                                val order =
                                    viewModel.purchaseOrderList.find { it.second == selected }
                                viewModel.purchaseOrderIdFilter.text =
                                    order?.first?.toString() ?: ""
                            })
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.startDateFilter,
                            placeholderText = "Start Date",
                            isDatePicker = true,
                            onDateSelected = { date ->
                                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                viewModel.startDateFilter.text = dateFormat.format(
                                    Date.from(
                                        date.atStartOfDay(java.time.ZoneId.systemDefault())
                                            .toInstant()
                                    )
                                )
                            })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Row 3: End Date, Weight Ranges, Quantity Ranges, Apply Button
                    RowOrColumn {
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.endDateFilter,
                            placeholderText = "End Date",
                            isDatePicker = true,
                            onDateSelected = { date ->
                                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                viewModel.endDateFilter.text = dateFormat.format(
                                    Date.from(
                                        date.atStartOfDay(java.time.ZoneId.systemDefault())
                                            .toInstant()
                                    )
                                )
                            })
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.minGsWtFilter,
                            placeholderText = "Min Gross Wt",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.maxGsWtFilter,
                            placeholderText = "Max Gross Wt",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Row 4: Quantity Ranges, Net Weight Ranges
                    RowOrColumn {
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.minQuantityFilter,
                            placeholderText = "Min Qty",
                            keyboardType = KeyboardType.Number,
                            singleLine = true
                        )
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.maxQuantityFilter,
                            placeholderText = "Max Qty",
                            keyboardType = KeyboardType.Number,
                            singleLine = true
                        )
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.minNtWtFilter,
                            placeholderText = "Min Net Wt",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                        WidthThenHeightSpacer()
                        CusOutlinedTextField(
                            modifier = if (it) Modifier.weight(1f) else Modifier,
                            state = viewModel.maxNtWtFilter,
                            placeholderText = "Max Net Wt",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                        WidthThenHeightSpacer()
                        Button(onClick = {
                            viewModel.filterItems()
                            isFilterPanelExpanded.value = !isFilterPanelExpanded.value
                        }) {
                            Text("Apply")
                        }

                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Show loader while loading, otherwise show the list
        val isLoading by viewModel.loadingState
        if (isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Loading items...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            TextListView(
                headerList = viewModel.itemHeaderList,
                items = viewModel.itemList.mapIndexed { index, item -> item.toListString(index + 1) },
                onItemClick = { item ->
                    // Copy item ID to clipboard
                    clipboardManager.setText(AnnotatedString(item[3]))
                },
                onItemLongClick = { itemData ->
                    val itemId = itemData[3] // itemId is at index 3
                    val item = viewModel.itemList.find { it.itemId == itemId }
                    item?.let {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedItem.value = it
                        showPrintDialog.value = true
                    }
                })
        }
    }

    // Print Dialog
    if (showPrintDialog.value && selectedItem.value != null) {
        AlertDialog(
            onDismissRequest = { showPrintDialog.value = false },
            title = { Text("Item Details") },
            text = {
                Column {
                    Text("Name: ${selectedItem.value?.itemAddName}")
                    Text("ID: ${selectedItem.value?.itemId}")
                    Text("Category: ${selectedItem.value?.catName}")
                    Text("Sub Category: ${selectedItem.value?.subCatName}")
                    Text("Purity: ${selectedItem.value?.purity}")
                    Text("Quantity: ${selectedItem.value?.quantity}")
                    Text("Net Weight: ${selectedItem.value?.ntWt}")
                    Text("Fine Weight: ${selectedItem.value?.fnWt}")
                    // Add more fields as needed...
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        if (selectedItem.value != null) {
                            PrintUtils.generateItemExcelAndPrint(context, selectedItem.value!!) {
                                showPrintDialog.value = false
                            }
                        }
                    }) {
                        Text("Print", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        if (selectedItem.value != null) {
                            PrintUtils.printThermalLabel(context, selectedItem.value!!) {
                                showPrintDialog.value = false
                            }
                        }
                    }) {
                        Text("Direct Print", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPrintDialog.value = false
                }) {
                    Text("Cancel")
                }
            })
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
private fun HeaderSection(
    viewModel: InventoryViewModel,
    showSortMenu: Boolean,
    onSortMenuToggle: () -> Unit,
    onSortOptionSelected: (String, String) -> Unit,
    onClearFilters: () -> Unit,
    onExport: () -> Unit,
    isFilterPanelExpanded: MutableState<Boolean>,
    exportedFileUri: String?,
    onPrint: (String) -> Unit
) {
    // Active filters count
    val activeFilters = listOfNotNull(
        if (viewModel.categoryFilter.text.isNotEmpty()) "Cat" else null,
        if (viewModel.subCategoryFilter.text.isNotEmpty()) "SubCat" else null,
        if (viewModel.entryTypeFilter.text.isNotEmpty()) "Type" else null,
        if (viewModel.purityFilter.text.isNotEmpty()) "Purity" else null,
        if (viewModel.chargeTypeFilter.text.isNotEmpty()) "Charge" else null,
        if (viewModel.firmIdFilter.text.isNotEmpty()) "Firm" else null,
        if (viewModel.purchaseOrderIdFilter.text.isNotEmpty()) "Order" else null,
        if (viewModel.startDateFilter.text.isNotEmpty()) "StartDate" else null,
        if (viewModel.endDateFilter.text.isNotEmpty()) "EndDate" else null,
        if (viewModel.minGsWtFilter.text.isNotEmpty() || viewModel.maxGsWtFilter.text.isNotEmpty()) "GrossWt" else null,
        if (viewModel.minNtWtFilter.text.isNotEmpty() || viewModel.maxNtWtFilter.text.isNotEmpty()) "NetWt" else null,
        if (viewModel.minQuantityFilter.text.isNotEmpty() || viewModel.maxQuantityFilter.text.isNotEmpty()) "Qty" else null
    )


    // Sort button
    Column (modifier = Modifier.fillMaxWidth()) {
        RowOrColumn {

            Row(
                modifier = Modifier, verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSortMenuToggle) {
                    Icon(
                        imageVector = Icons.AutoMirrored.TwoTone.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Sort direction indicator
                if (viewModel.sortBy.value != "addDate" || viewModel.sortOrder.value != "DESC") {
                    Text(
                        modifier = Modifier,
                        text = "${viewModel.sortOptions.find { it.first == viewModel.sortBy.value }?.second ?: "Date"} ${if (viewModel.sortOrder.value == "ASC") "↑" else "↓"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }


            if (activeFilters.isNotEmpty()) {
                Text(
                    text = "Filters: ${activeFilters.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            if (it) Spacer(modifier = Modifier.weight(1f))
            else WidthThenHeightSpacer()

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable {
                        isFilterPanelExpanded.value = !isFilterPanelExpanded.value
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${viewModel.itemList.size} items found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(20.dp))
                    Icon(
                        imageVector = if (isFilterPanelExpanded.value) Icons.TwoTone.KeyboardArrowUp else Icons.TwoTone.KeyboardArrowDown,
                        contentDescription = if (isFilterPanelExpanded.value) "Collapse filters" else "Expand filters",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onClearFilters) {
                    Text("Clear")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = onExport) {
                    Text("Export")
                }

                // Show print button only when file is exported
                if (exportedFileUri != null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onPrint(exportedFileUri!!) }) {
                        Icon(
                            imageVector = Icons.TwoTone.Print,
                            contentDescription = "Print",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Print")
                    }
                }
            }

        }


        DropdownMenu(
            expanded = showSortMenu, onDismissRequest = onSortMenuToggle
        ) {
            viewModel.sortOptions.forEach { (sortKey, sortLabel) ->
                DropdownMenuItem(text = { Text(sortLabel) }, onClick = {
                    val newOrder =
                        if (viewModel.sortBy.value == sortKey && viewModel.sortOrder.value == "ASC") "DESC" else "ASC"
                    onSortOptionSelected(sortKey, newOrder)
                }, trailingIcon = {
                    if (viewModel.sortBy.value == sortKey) {
                        Text(
                            text = if (viewModel.sortOrder.value == "ASC") "↑" else "↓",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                })
            }
        }
    }


}



