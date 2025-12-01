package com.velox.jewelvault.ui.screen.qr_bar_scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.ui.components.QrBarScannerPage
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.screen.inventory.InventoryViewModel
import com.velox.jewelvault.utils.CalculationUtils
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.parseQrItemPayload
import com.velox.jewelvault.utils.to3FString

@Composable
fun QrBarScannerScreen(viewModel: QrBarScannerViewModel, inventoryViewModel: InventoryViewModel) {
    val baseViewModel = LocalBaseViewModel.current
    val navHost = LocalNavController.current
    var menuExpanded by remember { mutableStateOf(false) }
    var deleteModeEnabled by remember { mutableStateOf(false) }



    QrBarScannerPage(valueProcessing = { raw ->
        val parsedId = parseQrItemPayload(raw)?.id ?: raw
        if (baseViewModel.metalRates.isNotEmpty()) {
            viewModel.processScan(parsedId, baseViewModel.metalRates)
            "Processing ID: $parsedId"
        } else {
            "Load Metal Rate"
        }
    }, overlayContent = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetalRatesTicker(
                        Modifier
                            .weight(1f)
                            .height(50.dp), textColor = Color.White
                    )

                    Icon(
                        Icons.TwoTone.Clear,
                        null,
                        modifier = Modifier
                            .bounceClick { navHost.popBackStack() }
                            .size(50.dp),
                        tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Box {
                        Icon(
                            Icons.TwoTone.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier
                                .size(32.dp)
                                .bounceClick { menuExpanded = true },
                            tint = Color.White
                        )
                        DropdownMenu(
                            expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (deleteModeEnabled) "Disable delete mode" else "Enable delete mode") },
                                onClick = {
                                    deleteModeEnabled = !deleteModeEnabled
                                    menuExpanded = false
                                })
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Place the item code under the camera view to get the details.",
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        LazyColumn {
                            items(viewModel.selectedItemList) { item ->
                                val existing = item.second
                                if (existing != null) {
                                    val res =
                                        "Id: ${existing.itemId} ${existing.catName} ${existing.subCatName} ${existing.itemAddName}" + "\nWt: ${existing.gsWt.to3FString()}(${existing.fnWt.to3FString()}) gm, ${existing.purity}, E.P: ₹${
                                            CalculationUtils.totalPrice(
                                                existing.price,
                                                existing.chargeAmount,
                                                existing.othCrg,
                                                existing.tax
                                            ).to3FString()
                                        }"

                                    val showDeleteDialog = remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (deleteModeEnabled) {
                                            Icon(
                                                Icons.TwoTone.Delete,
                                                null,
                                                modifier = Modifier
                                                    .bounceClick {
                                                        showDeleteDialog.value = true
                                                    }
                                                    .size(24.dp),
                                                tint = Color.Red)
                                            Spacer(Modifier.size(8.dp))
                                        }
                                        Text(
                                            text = res,
                                            color = Color.Yellow,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (showDeleteDialog.value) {
                                        AlertDialog(
                                            onDismissRequest = {
                                            showDeleteDialog.value = false
                                        },
                                            title = { Text("Delete Item?") },
                                            text = { Text("Are you sure you want to delete ${existing.itemAddName} (${existing.itemId})?") },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    inventoryViewModel.safeDeleteItem(
                                                        itemId = existing.itemId,
                                                        catId = existing.catId,
                                                        subCatId = existing.subCatId,
                                                        onSuccess = {
                                                            baseViewModel.snackBarState =
                                                                "Item deleted successfully"
                                                            showDeleteDialog.value = false
                                                        },
                                                        onFailure = {
                                                            baseViewModel.snackBarState =
                                                                "Unable to delete item."
                                                            showDeleteDialog.value = false
                                                        })
                                                }) { Text("Delete", color = Color.Red) }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = {
                                                    showDeleteDialog.value = false
                                                }) { Text("Cancel") }
                                            })
                                    }
                                }
                            }
                        }

                    }
                }
            }

        }
    })
}
