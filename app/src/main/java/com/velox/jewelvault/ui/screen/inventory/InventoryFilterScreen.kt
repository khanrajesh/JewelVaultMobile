package com.velox.jewelvault.ui.screen.inventory

import android.annotation.SuppressLint


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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.export.enqueueExportWorker
import com.velox.jewelvault.utils.to2FString

import java.text.SimpleDateFormat
import java.util.Locale

// Helper function to convert ItemEntity to List<String>
private fun ItemEntity.toListString(index: Int): List<String> = listOf(
    "$index",
    "${catName} (${catId})",
    "${subCatName} (${subCatId})",
    itemId.toString(),
    itemAddName,
    entryType,
    quantity.toString(),
    gsWt.to2FString(),
    ntWt.to2FString(),
    unit,
    purity,
    fnWt.to2FString(),
    crgType,
    crg.to2FString(),
    othCrgDes,
    othCrg.to2FString(),
    (cgst + sgst + igst).to2FString(),
    huid,
    addDate.toString(),
    addDesKey,
    addDesValue,
    "Extra value"
)

@Composable
fun InventoryFilterScreen(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    viewModel.currentScreenHeadingState.value = "Inventory Filter"

    // Keep track of subcategories for selected category
    val subCategories = remember { mutableStateListOf<SubCategoryEntity>() }
    var showSortMenu by remember { mutableStateOf(false) }
    var isFilterPanelExpanded by remember { mutableStateOf(true) }


    LaunchedEffect(true) {
        viewModel.getCategoryAndSubCategoryDetails()
        viewModel.loadFirmAndOrderLists()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(topStart = 18.dp)
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
                        item.itemId.toString(),
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
                enqueueExportWorker(context, lifecycleOwner, fileName, viewModel.itemHeaderList, rows)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter panel header with expand/collapse
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isFilterPanelExpanded = !isFilterPanelExpanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${viewModel.itemList.size} items found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (isFilterPanelExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isFilterPanelExpanded) "Collapse filters" else "Expand filters",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Animated filter panel
        AnimatedVisibility(
            visible = isFilterPanelExpanded,
            enter = slideInVertically(
                animationSpec = tween(300),
                initialOffsetY = { -it }
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                animationSpec = tween(300),
                targetOffsetY = { -it }
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Row 1: Category, Subcategory, Entry Type, Purity
                    Row {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.categoryFilter,
                            placeholderText = "Category",
                            dropdownItems = viewModel.catSubCatDto.map { it.catName },
                            onDropdownItemSelected = { selected ->
                                viewModel.categoryFilter.text = selected
                                val selectedCat = viewModel.catSubCatDto.find { it.catName == selected }
                                subCategories.clear()
                                selectedCat?.subCategoryList?.let { subCategories.addAll(it) }
                                viewModel.subCategoryFilter.text = ""
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.subCategoryFilter,
                            placeholderText = "Sub Category",
                            dropdownItems = subCategories.map { it.subCatName },
                            onDropdownItemSelected = { selected ->
                                viewModel.subCategoryFilter.text = selected
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.entryTypeFilter,
                            placeholderText = "Entry Type",
                            dropdownItems = EntryType.list(),
                            onDropdownItemSelected = { selected ->
                                viewModel.entryTypeFilter.text = selected
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.purityFilter,
                            placeholderText = "Purity",
                            dropdownItems = Purity.list(),
                            onDropdownItemSelected = { selected ->
                                viewModel.purityFilter.text = selected
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Row 2: Charge Type, Firm, Purchase Order, Date Range
                    Row {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.chargeTypeFilter,
                            placeholderText = "Charge Type",
                            dropdownItems = ChargeType.list(),
                            onDropdownItemSelected = { selected ->
                                viewModel.chargeTypeFilter.text = selected
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.firmIdFilter,
                            placeholderText = "Firm",
                            dropdownItems = viewModel.firmList.map { it.second },
                            onDropdownItemSelected = { selected ->
                                val firm = viewModel.firmList.find { it.second == selected }
                                viewModel.firmIdFilter.text = firm?.first?.toString() ?: ""
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.purchaseOrderIdFilter,
                            placeholderText = "Purchase Order",
                            dropdownItems = viewModel.purchaseOrderList.map { it.second },
                            onDropdownItemSelected = { selected ->
                                val order = viewModel.purchaseOrderList.find { it.second == selected }
                                viewModel.purchaseOrderIdFilter.text = order?.first?.toString() ?: ""
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.startDateFilter,
                            placeholderText = "Start Date",
                            isDatePicker = true,
                            onDateSelected = { date ->
                                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                viewModel.startDateFilter.text = dateFormat.format(java.util.Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Row 3: End Date, Weight Ranges, Quantity Ranges, Apply Button
                    Row {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.endDateFilter,
                            placeholderText = "End Date",
                            isDatePicker = true,
                            onDateSelected = { date ->
                                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                viewModel.endDateFilter.text = dateFormat.format(java.util.Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()))
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.minGsWtFilter,
                            placeholderText = "Min Gross Wt",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.maxGsWtFilter,
                            placeholderText = "Max Gross Wt",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { viewModel.filterItems() }) {
                            Text("Apply")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Row 4: Quantity Ranges, Net Weight Ranges
                    Row {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.minQuantityFilter,
                            placeholderText = "Min Qty",
                            keyboardType = KeyboardType.Number,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.maxQuantityFilter,
                            placeholderText = "Max Qty",
                            keyboardType = KeyboardType.Number,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.minNtWtFilter,
                            placeholderText = "Min Net Wt",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.maxNtWtFilter,
                            placeholderText = "Max Net Wt",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Item list
        TextListView(
            headerList = viewModel.itemHeaderList,
            items = viewModel.itemList.mapIndexed { index, item -> item.toListString(index + 1) },
            onItemClick = { _ -> },
            onItemLongClick = { _ ->
                viewModel.snackBarState.value = "Long click"
            }
        )
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
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort button
        Box {


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                IconButton(onClick = onSortMenuToggle) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sort direction indicator
                if (viewModel.sortBy.value != "addDate" || viewModel.sortOrder.value != "DESC") {
                    Text(
                        text = "${viewModel.sortOptions.find { it.first == viewModel.sortBy.value }?.second ?: "Date"} ${if (viewModel.sortOrder.value == "ASC") "↑" else "↓"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

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

                if (activeFilters.isNotEmpty()) {
                    Text(
                        text = "Filters: ${activeFilters.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }else{
                    Spacer(Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onClearFilters) {
                    Text("Clear")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = onExport) {
                    Text("Export")
                }
            }

                
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = onSortMenuToggle
                ) {
                    viewModel.sortOptions.forEach { (sortKey, sortLabel) ->
                        DropdownMenuItem(
                            text = { Text(sortLabel) },
                            onClick = {
                                val newOrder = if (viewModel.sortBy.value == sortKey && viewModel.sortOrder.value == "ASC") "DESC" else "ASC"
                                onSortOptionSelected(sortKey, newOrder)
                            },
                            trailingIcon = {
                                if (viewModel.sortBy.value == sortKey) {
                                    Text(
                                        text = if (viewModel.sortOrder.value == "ASC") "↑" else "↓",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }


    }



