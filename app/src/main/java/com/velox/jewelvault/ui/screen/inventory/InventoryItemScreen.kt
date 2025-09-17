package com.velox.jewelvault.ui.screen.inventory

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.theme.LightGreen
import com.velox.jewelvault.ui.theme.LightRed
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.generateId
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.to3FString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.apache.poi.hssf.usermodel.HeaderFooter.fontSize
import java.sql.Timestamp

// Helper function to convert ItemEntity to List<String>
private fun ItemEntity.toListString(index: Int): List<String> = listOf(
    "$index",
    "${catName} (${catId})",
    "${subCatName} (${subCatId})",
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
    purchaseOrderId,
)

// Function to generate Excel for individual item and print it
private fun generateItemExcelAndPrint(
    context: android.content.Context,
    item: ItemEntity,
    onComplete: () -> Unit
) {
    ioScope {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Item Details")
            
            // Create header style
            val headerStyle = workbook.createCellStyle().apply {
                val font = workbook.createFont().apply {
                    bold = true
                    fontSize(12)
                }
                setFont(font)
            }
            
            // Item Information as key-value pairs
            val itemData = listOf(
                "Item ID" to item.itemId,
                "Name" to item.itemAddName,
                "Category" to "${item.catName} (${item.catId})",
                "Sub Category" to "${item.subCatName} (${item.subCatId})",
                "Entry Type" to item.entryType,
                "Quantity" to item.quantity.toString(),
                "Gross Weight" to "${item.gsWt.to3FString()} gm",
                "Net Weight" to "${item.ntWt.to3FString()} gm",
                "Fine Weight" to "${item.fnWt.to3FString()} gm",
                "Purity" to item.purity,
                "Charge Type" to item.crgType,
                "Charge" to "₹${item.crg.to3FString()}",
                "Other Charge Description" to item.othCrgDes,
                "Other Charge" to "₹${item.othCrg.to3FString()}",
                "CGST" to "₹${item.cgst.to3FString()}",
                "SGST" to "₹${item.sgst.to3FString()}",
                "IGST" to "₹${item.igst.to3FString()}",
                "Total Tax" to "₹${(item.cgst + item.sgst + item.igst).to3FString()}",
                "HUID" to item.huid,
                "Description Key" to item.addDesKey,
                "Description Value" to item.addDesValue,
                "Purchase Order ID" to item.purchaseOrderId,
                "Added Date" to SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(Date(item.addDate.time))
            )
            
            // Create header row with all property names
            val headerRow = sheet.createRow(0)
            itemData.forEachIndexed { index, (key, _) ->
                val headerCell = headerRow.createCell(index)
                headerCell.setCellValue(key)
                headerCell.cellStyle = headerStyle
            }
            
            // Create data row with all values
            val dataRow = sheet.createRow(1)
            itemData.forEachIndexed { index, (_, value) ->
                val dataCell = dataRow.createCell(index)
                dataCell.setCellValue(value)
            }
            
            // Set column widths manually (avoid autoSizeColumn which uses AWT classes not available on Android)
            itemData.forEachIndexed { index, _ ->
                sheet.setColumnWidth(index, 20 * 256) // 20 characters wide for each column
            }
            
            // Create temporary file
            val fileName = "Item_${item.itemId}_${System.currentTimeMillis()}.xlsx"
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/JewelVault/TempPrint"
                )
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            if (uri != null) {
                var success = false
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        workbook.write(outputStream)
                        outputStream.flush()
                        success = true
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    workbook.close()
                }
                
                if (success) {
                    // Launch print intent
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Print Item Details"))
                    
                    // Schedule file deletion after a delay (5 seconds)
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        delay(5000) // Wait 5 seconds
                        try {
                            resolver.delete(uri, null, null)
                        } catch (e: Exception) {
                            // File might already be deleted or not accessible
                        }
                    }
                    
                    onComplete()
                } else {
                    resolver.delete(uri, null, null)
                }
            } else {
                workbook.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Function to print item details directly using Android's native printing
private fun printItemDirectly(
    context: android.content.Context,
    item: ItemEntity,
    onComplete: () -> Unit
) {
    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
    
    // Item Information as key-value pairs (same as Excel format)
    val itemData = listOf(
        "Item ID" to item.itemId,
        "Name" to item.itemAddName,
        "Category" to "${item.catName} (${item.catId})",
        "Sub Category" to "${item.subCatName} (${item.subCatId})",
        "Entry Type" to item.entryType,
        "Quantity" to item.quantity.toString(),
        "Gross Weight" to "${item.gsWt.to3FString()} gm",
        "Net Weight" to "${item.ntWt.to3FString()} gm",
        "Fine Weight" to "${item.fnWt.to3FString()} gm",
        "Purity" to item.purity,
        "Charge Type" to item.crgType,
        "Charge" to "₹${item.crg.to3FString()}",
        "Other Charge Description" to item.othCrgDes,
        "Other Charge" to "₹${item.othCrg.to3FString()}",
        "CGST" to "₹${item.cgst.to3FString()}",
        "SGST" to "₹${item.sgst.to3FString()}",
        "IGST" to "₹${item.igst.to3FString()}",
        "Total Tax" to "₹${(item.cgst + item.sgst + item.igst).to3FString()}",
        "HUID" to item.huid,
        "Description Key" to item.addDesKey,
        "Description Value" to item.addDesValue,
        "Purchase Order ID" to item.purchaseOrderId,
        "Added Date" to SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(Date(item.addDate.time))
    )
    
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Item Details - ${item.itemId}</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                .header { text-align: center; font-size: 24px; font-weight: bold; margin-bottom: 30px; border-bottom: 2px solid #333; padding-bottom: 10px; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                th { background-color: #f2f2f2; font-weight: bold; }
                .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #888; }
                @media print {
                    body { margin: 0; }
                    table { page-break-inside: avoid; }
                }
            </style>
        </head>
        <body>
            <div class="header">Item Details Report</div>
            
            <table>
                <tr>
                    ${itemData.joinToString("") { (key, _) -> "<th>$key</th>" }}
                </tr>
                <tr>
                    ${itemData.joinToString("") { (_, value) -> "<td>$value</td>" }}
                </tr>
            </table>
            
            <div class="footer">
                Generated by JewelVault Mobile App
            </div>
        </body>
        </html>
    """.trimIndent()
    
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            
            val printAdapter = webView.createPrintDocumentAdapter("Item_${item.itemId}_${System.currentTimeMillis()}")
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("print", "print", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
            
            printManager.print("Item Details Print Job", printAdapter, printAttributes)
            onComplete()
        }
    }
    
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}

@Composable
fun InventoryItemScreen(
    inventoryViewModel: InventoryViewModel,
    catId: String,
    catName: String,
    subCatId: String,
    subCatName: String
) {
    inventoryViewModel.currentScreenHeadingState.value = "Sub Category Details"
    LandscapeInventoryItemScreen(inventoryViewModel, catId, catName, subCatId, subCatName)
}

@Composable
fun LandscapeInventoryItemScreen(
    inventoryViewModel: InventoryViewModel,
    catId: String,
    catName: String,
    subCatId: String,
    subCatName: String
) {
    val context = LocalContext.current
    LaunchedEffect(true) {
        delay(200)
        inventoryViewModel.loadingState.value = true
        inventoryViewModel.categoryFilter.text = catName
        inventoryViewModel.subCategoryFilter.text = subCatName
        inventoryViewModel.startDateFilter.text = ""
        inventoryViewModel.endDateFilter.text = ""
        inventoryViewModel.filterItems()
        inventoryViewModel.loadingState.value = false
    }
    val clipboardManager = LocalClipboardManager.current

    val showOption = remember { mutableStateOf(false) }


    val addItem = remember { mutableStateOf(false) }
    val showDialog = remember { mutableStateOf(false) }
    val selectedItem = remember { mutableStateOf<ItemEntity?>(null) }
    val isUpdateMode = remember { mutableStateOf(false) }
    val itemBeingUpdated = remember { mutableStateOf<ItemEntity?>(null) }

    val baseViewModel = LocalBaseViewModel.current
    val haptic = LocalHapticFeedback.current


    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth()) {
                Text("$catName ($catId) > $subCatName ($subCatId)")
                Spacer(Modifier.weight(1f))
                Text("Add Item", modifier = Modifier.clickable {
                    addItem.value = true
                })
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.MoreVert, null, modifier = Modifier.clickable {
                    showOption.value = !showOption.value
                })
            }
            Spacer(Modifier.height(5.dp))
            Spacer(
                Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            )
            Spacer(Modifier.height(5.dp))
            AddItemSection(
                addItem,
                catId,
                subCatId,
                catName,
                subCatName,
                inventoryViewModel,
                isUpdateMode,
                itemBeingUpdated.value
            )



            TextListView(
                headerList = inventoryViewModel.itemHeaderList,
                items = inventoryViewModel.itemList.mapIndexed { index, item ->
                    item.toListString(index + 1)
                },
                onItemClick = { item ->
                    item[0]
                    clipboardManager.setText(AnnotatedString(item[3]))
//                    inventoryViewModel.snackBarState.value = "Item ID copied: ${item[3]}"

                },
                onItemLongClick = { itemData ->
                    val itemId = itemData[3] // itemId is at index 3
                    val item = inventoryViewModel.itemList.find { it.itemId == itemId }
                    item?.let {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedItem.value = it
                        showDialog.value = true
                    }
                })
        }

        if (showOption.value) Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(y = 40.dp)
                .wrapContentHeight()
                .wrapContentWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Text(
                "Options",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable {

                })
        }

        if (showDialog.value && selectedItem.value != null) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                title = { Text("Item Details") },
                text = {
                    Column {
                        Text("Name: ${selectedItem.value?.itemAddName}")
                        Text("id: ${selectedItem.value?.itemId},cat: ${selectedItem.value?.catId},id: ${selectedItem.value?.subCatId}")
                        Text("Purity: ${selectedItem.value?.purity}")
                        Text("Quantity: ${selectedItem.value?.quantity}")
                        Text("Net Weight: ${selectedItem.value?.gsWt}")
                        // Add more fields as needed...
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            ioScope {
                                if (selectedItem.value != null) {
                                    itemBeingUpdated.value = selectedItem.value
                                    itemBeingUpdated.value?.let {
                                        inventoryViewModel.populateUpdateFields(it, subCatId){
                                            showDialog.value = false
                                            addItem.value = true
                                            isUpdateMode.value = true
                                        }
                                    }
                                }
                            }
                        }) {
                            Text("Update", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            if (selectedItem.value != null) {
                                inventoryViewModel.safeDeleteItem(
                                    itemId = selectedItem.value!!.itemId,
                                    catId = selectedItem.value!!.catId,
                                    subCatId = selectedItem.value!!.subCatId,
                                    onSuccess = {
                                        baseViewModel.snackBarState = "Item deleted successfully"
                                    },
                                    onFailure = {
                                        baseViewModel.snackBarState = "Unable to delete item."
                                    })

                                selectedItem.value = null
                            } else {
                                baseViewModel.snackBarState = "Please select valid item"
                            }

                            showDialog.value = false
                        }) {
                            Text("Delete", color = Color.Red)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            if (selectedItem.value != null) {
                                generateItemExcelAndPrint(context, selectedItem.value!!) {
                                    showDialog.value = false
                                }
                            }
                        }) {
                            Text("Print", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            if (selectedItem.value != null) {
                                printItemDirectly(context, selectedItem.value!!) {
                                    showDialog.value = false
                                }
                            }
                        }) {
                            Text("Direct Print", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog.value = false
                    }) {
                        Text("Cancel")
                    }
                })
        }

    }


}

@SuppressLint("DefaultLocale")
@Composable
private fun AddItemSection(
    addItem: MutableState<Boolean>,
    catId: String,
    subCatId: String,
    catName: String,
    subCatName: String,
    viewModel: InventoryViewModel,
    isUpdateMode: MutableState<Boolean>,
    itemBeingUpdated: ItemEntity?
) {


    if (addItem.value) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(5.dp)
        ) {
            Text(
                if (isUpdateMode.value) "Update Item" else "Add Item",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(5.dp))

            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(5.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                ) { //purchase order section

                    Button(
                        onClick = {
                            viewModel.isSelf.value = !viewModel.isSelf.value
                            // Clear validation errors when switching modes
                            viewModel.purity.error = ""
                            viewModel.fnWt.error = ""
                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isSelf.value) LightGreen else LightRed
                        )
                    ) {
                        Text("SELF", fontWeight = FontWeight.SemiBold)
                    }
                    if (!viewModel.isSelf.value) {
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            viewModel.billDate,
                            placeholderText = "Bill Date",
                            modifier = Modifier.weight(1f),
                            isDatePicker = true,
                            onDateSelected = {
                                viewModel.getBillsFromDate()
                            })

                        if (viewModel.purchaseOrdersByDate.isNotEmpty()) {
                            Spacer(Modifier.width(5.dp))
                            CusOutlinedTextField(
                                modifier = Modifier.weight(1f),
                                state = viewModel.billNo,
                                placeholderText = "Bill No",
                                readOnly = isUpdateMode.value, // Make read-only in update mode
                                dropdownItems = if (isUpdateMode.value) emptyList() else viewModel.purchaseOrdersByDate.map { it.billNo },
                                onDropdownItemSelected = { sel ->
                                    val item =
                                        viewModel.purchaseOrdersByDate.find { it.billNo == sel }
                                    if (item != null) {
                                        viewModel.getPurchaseOrderItemDetails(item, subCatId)
                                    } else {
                                        viewModel.snackBarState.value =
                                            "Unable to find purchase order item"
                                    }
                                })
                        }

                        if (viewModel.billItemDetails.value.isNotBlank()) {
                            Spacer(Modifier.width(5.dp))
                            var showDetailsDialog by remember { mutableStateOf(false) }

                            Text(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDetailsDialog = true },
                                text = "${viewModel.billItemDetails.value} ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                lineHeight = 12.sp
                            )

                            // Detailed Purchase Order Dialog
                            if (showDetailsDialog) {
                                val selectedPurchase = viewModel.purchaseOrdersByDate.find {
                                    viewModel.billItemDetails.value.contains(it.billNo)
                                }

                                if (selectedPurchase != null) {
                                    var detailedReport by remember { mutableStateOf("Loading...") }

                                    LaunchedEffect(selectedPurchase, subCatName) {
                                        detailedReport = viewModel.getDetailedPurchaseOrderReport(
                                            selectedPurchase, subCatName
                                        )
                                    }

                                    AlertDialog(
                                        onDismissRequest = { showDetailsDialog = false },
                                        title = {
                                            Text(
                                                text = "Purchase Order Details",
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                        },
                                        text = {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 400.dp)
                                            ) {
                                                item {
                                                    Text(
                                                        text = detailedReport,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = { showDetailsDialog = false }) {
                                                Text("Close")
                                            }
                                        })
                                }
                            }
                        }
                    }

                    // Show remaining weight guidance for fine weight input
                    if (!viewModel.isSelf.value && viewModel.purchaseItems.isNotEmpty() && viewModel.purity.text.isNotBlank()) {
                        Spacer(Modifier.width(5.dp))

                        var remainingWeight by remember { mutableStateOf(0.0) }

                        LaunchedEffect(viewModel.purity.text, viewModel.purchaseItems.size) {
                            remainingWeight = viewModel.getRemainingFineWeightForPurity(
                                viewModel.purity.text, subCatName
                            )
                        }

                        if (remainingWeight > 0) {
                            val inputWeight = viewModel.fnWt.text.toDoubleOrNull() ?: 0.0
                            val difference = remainingWeight - inputWeight

                            val guidanceText = if (viewModel.fnWt.text.isBlank()) {
                                "Remaining:  ${remainingWeight.to3FString()} g"
                            } else if (difference > 0) {
                                "Still need: ${difference.to3FString()}g (Remaining: ${remainingWeight.to3FString()}g)"
                            } else if (difference < -0.01) {
                                "Exceeds by: ${(-difference).to3FString()}g (Remaining: ${remainingWeight.to3FString()}g)"
                            } else {
                                "✅ Complete (${remainingWeight.to3FString()}g)"
                            }

                            val guidanceColor = when {
                                difference > 0 -> MaterialTheme.colorScheme.primary
                                difference < -0.01 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Text(
                                modifier = Modifier.weight(0.5f),
                                text = guidanceText,
                                fontSize = 11.sp,
                                color = guidanceColor,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }


                Spacer(Modifier.height(5.dp))

                Row(Modifier.fillMaxWidth()) {

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.addToName,
                        placeholderText = "Add to Name",
                        keyboardType = KeyboardType.Text
                    )

                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.entryType,
                        placeholderText = "Entry Type",
                        dropdownItems = EntryType.list(),
                        onDropdownItemSelected = { selected ->
                            when (selected) {
                                EntryType.Piece.type -> {
                                    viewModel.qty.text = "1"
                                }

                                EntryType.Lot.type -> {

                                }

                                else -> {
                                    viewModel.entryType.text = selected
                                }
                            }
                            viewModel.entryType.text = selected
                        })


                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.qty,
                        placeholderText = "Quantity",
                        keyboardType = KeyboardType.Number,
                    )

                }
                Spacer(Modifier.height(5.dp))

                Row {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.grWt,
                        placeholderText = "Gs.Wt/gm",
                        keyboardType = KeyboardType.Number,
                        validation = { text ->
                            if (text.isNotBlank()) {
                                val grWtValue = text.toDoubleOrNull() ?: 0.0
                                val ntWtValue = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0

                                if (ntWtValue > 0 && grWtValue < ntWtValue) {
                                    "Gross weight cannot be less than net weight"
                                } else null
                            } else null
                        },
                        onTextChange = { text ->
                            viewModel.grWt.text = text
                            viewModel.ntWt.text = text

                            // Recalculate fine weight if purity is already selected
                            if (viewModel.purity.text.isNotBlank() && text.isNotBlank()) {
                                val ntWtValue = text.toDoubleOrNull() ?: 0.0
                                val multiplier =
                                    Purity.fromLabel(viewModel.purity.text)?.multiplier ?: 1.0
                                viewModel.fnWt.text = (ntWtValue * multiplier).to3FString()
                            }
                        })
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.ntWt,
                        placeholderText = "Nt.Wt/gm",
                        keyboardType = KeyboardType.Number,
                        validation = { text ->
                            if (text.isNotBlank()) {
                                val ntWtValue = text.toDoubleOrNull() ?: 0.0
                                val grWtValue = viewModel.grWt.text.toDoubleOrNull() ?: 0.0

                                if (grWtValue > 0 && ntWtValue > grWtValue) {
                                    "Net weight cannot be greater than gross weight"
                                } else null
                            } else null
                        },
                        onTextChange = { text ->
                            viewModel.ntWt.text = text

                            // Recalculate fine weight if purity is already selected
                            if (viewModel.purity.text.isNotBlank() && text.isNotBlank()) {
                                val ntWtValue = text.toDoubleOrNull() ?: 0.0
                                val multiplier =
                                    Purity.fromLabel(viewModel.purity.text)?.multiplier ?: 1.0
                                viewModel.fnWt.text = (ntWtValue * multiplier).to3FString()
                            }
                        })

                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.purity,
                        placeholderText = "Purity",
                        dropdownItems = Purity.list(),
                        onDropdownItemSelected = { selected ->
                            if (viewModel.ntWt.text.isNotBlank()) {
                                val ntWtValue = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0
                                val multiplier = Purity.fromLabel(selected)?.multiplier ?: 1.0
                                viewModel.fnWt.text = (ntWtValue * multiplier).to3FString()
                            }
                            viewModel.purity.text = selected

                            // Validate fine weight when purity changes
                            if (!viewModel.isSelf.value && viewModel.purchaseItems.isNotEmpty() && viewModel.fnWt.text.isNotBlank()) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                    .launch {
                                        val error = viewModel.validateFineWeightInput(
                                            viewModel.fnWt.text, selected, subCatName
                                        )
                                        viewModel.fnWt.error = error ?: ""
                                    }
                            } else {
                                viewModel.fnWt.error = ""
                            }
                        })

                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.fnWt,
                        placeholderText = "Fn.Wt/gm",
                        keyboardType = KeyboardType.Number
                    )
                }

                // Validate fine weight when it changes
                LaunchedEffect(
                    viewModel.fnWt.text, viewModel.purity.text, viewModel.isSelf.value
                ) {
                    if (!viewModel.isSelf.value && viewModel.purchaseItems.isNotEmpty() && viewModel.fnWt.text.isNotBlank()) {
                        val error = viewModel.validateFineWeightInput(
                            viewModel.fnWt.text, viewModel.purity.text, subCatName
                        )
                        viewModel.fnWt.error = error ?: ""
                    } else {
                        viewModel.fnWt.error = ""
                    }
                }

                Spacer(Modifier.height(5.dp))

                Row {


                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.chargeType,
                        placeholderText = "Charge Type",
                        dropdownItems = ChargeType.list(),
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.charge,
                        placeholderText = "charge",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.otherChargeDes,
                        placeholderText = "Oth Charge Des",
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.othCharge,
                        placeholderText = "Oth Charge",
                        keyboardType = KeyboardType.Number,
                    )
                }
                Spacer(Modifier.height(5.dp))

                Row {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.cgst,
                        placeholderText = "CGST",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.sgst,
                        placeholderText = "SGST",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.igst,
                        placeholderText = "IGST",
                        keyboardType = KeyboardType.Number,
                    )

                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(2f),
                        state = viewModel.huid,
                        placeholderText = "H-UID",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.height(5.dp))
                }
                Spacer(Modifier.height(5.dp))
                Row {

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.desKey,
                        placeholderText = "Description",
                        keyboardType = KeyboardType.Text,
                    )
                    Spacer(Modifier.width(5.dp))
                    CusOutlinedTextField(
                        modifier = Modifier.weight(2f),
                        state = viewModel.desValue,
                        placeholderText = "Value",
                        keyboardType = KeyboardType.Text,
                    )

                }

                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                    val purItem =
                        viewModel.purchaseItems.filter { it.subCatName.lowercase() == subCatName.lowercase() && it.purity == viewModel.purity.text }

                    val text = purItem.joinToString(", ") {
                        "${it.purity} - Gs.Wt${it.gsWt},  Fn.Wt: ${it.fnWt}/₹${it.fnRate},  wastage: ${it.wastagePercent}%"
                    }

                    if (!viewModel.isSelf.value) {
                        Text(
                            text, modifier = Modifier
                                .weight(1f)
                                .padding(5.dp), fontSize = 16.sp
                        )

                    } else {
                        Spacer(Modifier.weight(1f))
                    }



                    Spacer(Modifier.width(5.dp))
                    Text("Cancel", Modifier
                        .bounceClick {
                            viewModel.clearAddItemFields()
                            isUpdateMode.value = false
                            addItem.value = false
                        }
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(10.dp), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(15.dp))
                    Text(if (isUpdateMode.value) "Update" else "Add", Modifier
                        .bounceClick {

                            return@bounceClick ioScope {
                                if (purItem.isEmpty() && !viewModel.isSelf.value) {
                                    viewModel.snackBarState.value =
                                        "No corresponding item found in purchase order"
                                    return@ioScope
                                }

                                if (!InputValidator.isValidQuantity(viewModel.qty.text)) {
                                    viewModel.snackBarState.value = "Invalid quantity"
                                    return@ioScope
                                }

                                if (!InputValidator.isValidWeight(viewModel.grWt.text)) {
                                    viewModel.snackBarState.value = "Invalid gross weight"
                                    return@ioScope
                                }

                                if (!InputValidator.isValidWeight(viewModel.ntWt.text)) {
                                    viewModel.snackBarState.value = "Invalid net weight"
                                    return@ioScope
                                }

                                if (!InputValidator.isValidWeight(viewModel.fnWt.text)) {
                                    viewModel.snackBarState.value = "Invalid fine weight"
                                    return@ioScope
                                }

                                if (viewModel.purity.text.isBlank()) {
                                    viewModel.snackBarState.value = "Purity is required"
                                    return@ioScope
                                }

                                if (!InputValidator.isValidHUID(viewModel.huid.text)) {
                                    viewModel.snackBarState.value = "Invalid HUID format"
                                    return@ioScope
                                }



                                val userId = viewModel.dataStoreManager.getAdminInfo().first.first()
                                val storeId =
                                    viewModel.dataStoreManager.getSelectedStoreInfo().first.first()

                                if (isUpdateMode.value) {
                                    // Update existing item
                                    val updatedItem = itemBeingUpdated?.copy(
                                        itemAddName = InputValidator.sanitizeText(viewModel.addToName.text),
                                        entryType = InputValidator.sanitizeText(viewModel.entryType.text),
                                        quantity = viewModel.qty.text.toIntOrNull() ?: 1,
                                        gsWt = viewModel.grWt.text.toDoubleOrNull() ?: 0.0,
                                        ntWt = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0,
                                        fnWt = viewModel.fnWt.text.toDoubleOrNull() ?: 0.0,
                                        purity = InputValidator.sanitizeText(viewModel.purity.text),
                                        crgType = InputValidator.sanitizeText(viewModel.chargeType.text),
                                        crg = viewModel.charge.text.toDoubleOrNull() ?: 0.0,
                                        othCrgDes = InputValidator.sanitizeText(viewModel.otherChargeDes.text),
                                        othCrg = viewModel.othCharge.text.toDoubleOrNull() ?: 0.0,
                                        cgst = viewModel.cgst.text.toDoubleOrNull() ?: 0.0,
                                        sgst = viewModel.sgst.text.toDoubleOrNull() ?: 0.0,
                                        igst = viewModel.igst.text.toDoubleOrNull() ?: 0.0,
                                        addDesKey = InputValidator.sanitizeText(viewModel.desKey.text),
                                        addDesValue = InputValidator.sanitizeText(viewModel.desValue.text),
                                        huid = viewModel.huid.text.trim().uppercase(),
                                        modifiedDate = Timestamp(System.currentTimeMillis())
                                    )
                                    updatedItem?.let {
                                        viewModel.safeUpdateItem(it, onFailure = {
                                            viewModel.snackBarState.value = "Update Item Failed"
                                        }, onSuccess = {
                                            // Refresh category data and filter items
                                            viewModel.refreshAndFilterItems()
                                            viewModel.clearAddItemFields()
                                            isUpdateMode.value = false
                                            viewModel.snackBarState.value =
                                                "Item updated successfully"
                                        })
                                    }
                                } else {
                                    // Check validation errors for purchase order items
                                    if (!viewModel.isSelf.value && viewModel.purchaseItems.isNotEmpty()) {
                                        val fnWtError = viewModel.validateFineWeightInput(
                                            viewModel.fnWt.text, viewModel.purity.text, subCatName
                                        )
                                        if (fnWtError != null) {
                                            viewModel.fnWt.error = fnWtError
                                            viewModel.snackBarState.value = fnWtError
                                            return@ioScope
                                        }
                                    }
                                    // Add new item
                                    val newItem = ItemEntity(
                                        itemId = generateId(),
                                        itemAddName = InputValidator.sanitizeText(viewModel.addToName.text),
                                        userId = userId,
                                        storeId = storeId,
                                        catId = catId,
                                        subCatId = subCatId,
                                        catName = catName,
                                        subCatName = subCatName,
                                        entryType = InputValidator.sanitizeText(viewModel.entryType.text),
                                        quantity = viewModel.qty.text.toIntOrNull() ?: 1,
                                        gsWt = viewModel.grWt.text.toDoubleOrNull() ?: 0.0,
                                        ntWt = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0,
                                        fnWt = viewModel.fnWt.text.toDoubleOrNull() ?: 0.0,
                                        purity = InputValidator.sanitizeText(viewModel.purity.text),
                                        crgType = InputValidator.sanitizeText(viewModel.chargeType.text),
                                        crg = viewModel.charge.text.toDoubleOrNull() ?: 0.0,
                                        othCrgDes = InputValidator.sanitizeText(viewModel.otherChargeDes.text),
                                        othCrg = viewModel.othCharge.text.toDoubleOrNull() ?: 0.0,
                                        cgst = viewModel.cgst.text.toDoubleOrNull() ?: 0.0,
                                        sgst = viewModel.sgst.text.toDoubleOrNull() ?: 0.0,
                                        igst = viewModel.igst.text.toDoubleOrNull() ?: 0.0,
                                        addDesKey = InputValidator.sanitizeText(viewModel.desKey.text),
                                        addDesValue = InputValidator.sanitizeText(viewModel.desValue.text),
                                        huid = viewModel.huid.text.trim().uppercase(),
                                        addDate = Timestamp(System.currentTimeMillis()),
                                        modifiedDate = Timestamp(System.currentTimeMillis()),
                                        sellerFirmId = storeId,
                                        purchaseOrderId = if (viewModel.isSelf.value) storeId else if (purItem.isEmpty()) storeId else purItem[0].purchaseOrderId,
                                        purchaseItemId = if (viewModel.isSelf.value) storeId else if (purItem.isEmpty()) storeId else purItem[0].purchaseItemId,

                                        )

                                    viewModel.safeInsertItem(newItem, onFailure = {
                                        viewModel.snackBarState.value = "Add Item Failed"
                                    }, onSuccess = { itemEntity, l ->

                                        // Refresh category data and filter items
                                        viewModel.refreshAndFilterItems()

                                        viewModel.clearAddItemFields()

//                                    inventoryViewModel.loadingState.value = false
                                    })
                                }

                                addItem.value = false
                            }


                        }
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(10.dp), fontWeight = FontWeight.Bold)
                }



        }
    }
}

