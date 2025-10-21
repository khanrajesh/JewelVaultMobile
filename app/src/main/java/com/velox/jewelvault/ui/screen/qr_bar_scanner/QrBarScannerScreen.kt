package com.velox.jewelvault.ui.screen.qr_bar_scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.ui.components.QrBarScannerPage
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.screen.inventory.InventoryViewModel
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.to3FString
import com.velox.jewelvault.utils.CalculationUtils

@Composable
fun QrBarScannerScreen(viewModel: QrBarScannerViewModel, inventoryViewModel: InventoryViewModel) {
    val baseViewModel = LocalBaseViewModel.current
    val navHost = LocalNavController.current

    QrBarScannerPage(valueProcessing = {
        if (baseViewModel.metalRates.isNotEmpty()){
                viewModel.processScan(it, baseViewModel.metalRates)
                "Processing ID: $it…"
        }else{
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
                Row {
                    MetalRatesTicker(
                        Modifier
                            .weight(1f)
                            .height(50.dp), textColor = Color.White
                    )

                    Icon(Icons.Default.Clear,
                        null,
                        modifier = Modifier
                            .bounceClick { navHost.popBackStack() }
                            .size(50.dp),
                        tint = Color.White)

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
                                        "Id: ${existing.itemId} ${existing.catName} ${existing.subCatName} ${existing.itemAddName}" +
                                                "\nWt: ${existing.gsWt.to3FString()}(${existing.fnWt.to3FString()}) gm, ${existing.purity}, E.P: ₹${CalculationUtils.totalPrice(existing.price, existing.chargeAmount, existing.othCrg,existing.tax).to3FString()}"

                                    val showDeleteDialog = remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            null,
                                            modifier = Modifier
                                                .bounceClick { showDeleteDialog.value = true }
                                                .size(24.dp),
                                            tint = Color.Red
                                        )
                                        Spacer(Modifier.size(8.dp))
                                        Text(text = res, color = Color.Yellow, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    }

                                    if (showDeleteDialog.value) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteDialog.value = false },
                                            title = { Text("Delete Item?") },
                                            text = { Text("Are you sure you want to delete ${existing.itemAddName} (${existing.itemId})?") },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    inventoryViewModel.safeDeleteItem(
                                                        itemId = existing.itemId,
                                                        catId = existing.catId,
                                                        subCatId = existing.subCatId,
                                                        onSuccess = {
                                                            baseViewModel.snackBarState = "Item deleted successfully"
                                                            showDeleteDialog.value = false
                                                        },
                                                        onFailure = {
                                                            baseViewModel.snackBarState = "Unable to delete item."
                                                            showDeleteDialog.value = false
                                                        }
                                                    )
                                                }) { Text("Delete", color = Color.Red) }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDeleteDialog.value = false }) { Text("Cancel") }
                                            }
                                        )
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
