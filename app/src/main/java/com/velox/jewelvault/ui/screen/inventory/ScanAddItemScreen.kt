package com.velox.jewelvault.ui.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.ui.components.QrBarScannerPage
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.parseQrItemPayload
import com.velox.jewelvault.utils.to3FString

@Composable
fun ScanAddItemScreen(inventoryViewModel: InventoryViewModel) {
    inventoryViewModel.currentScreenHeadingState.value = "Scan & Add"

    val baseVm = LocalBaseViewModel.current
    val subNav = LocalSubNavController.current
    val lastScan = remember { mutableStateOf<String?>(null) }
    val status = remember { mutableStateOf("Scan the item's QR to pre-fill the add form.") }
    val parsed = remember { mutableStateOf<com.velox.jewelvault.utils.QrItemPayload?>(null) }

    QrBarScannerPage(
        showPage = remember { mutableStateOf(true) },
        scanAndClose = false,
        onCodeScanned = { raw ->
            if (raw == lastScan.value) return@QrBarScannerPage
            lastScan.value = raw
            val payload = parseQrItemPayload(raw)
            if (payload != null) {
                parsed.value = payload
                inventoryViewModel.prefillAddItemFromQr(payload)
                status.value = "Ready: ${payload.id}"

                val catName = payload.catName ?: "Category"
                val subCatName = payload.subCatName ?: "SubCategory"
                baseVm.snackBarState = "Select category/subcategory manually."
            } else {
                parsed.value = null
                status.value = "Unrecognized QR. Ensure it uses JV1 CSV format."
                baseVm.snackBarState = "Cannot parse QR payload"
            }
        },
        valueProcessing = { it },
        overlayContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(text = status.value, style = MaterialTheme.typography.bodyMedium)
                    parsed.value?.let { p ->
                        Spacer(Modifier.height(8.dp))
                        Text("ID: ${p.id}")
                        Row {
                            Text("Cat: ${p.catName ?: "-"}")
                            Spacer(Modifier.width(12.dp))
                            Text("Sub: ${p.subCatName ?: "-"}")
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Wt: gs=${p.gs?.to3FString() ?: "-"}, nt=${p.nt?.to3FString() ?: "-"}, fn=${p.fn?.to3FString() ?: "-"}")
                        Text("Purity: ${p.purity ?: "-"}  MC: ${p.mcType ?: "-"} ${p.mc?.to3FString() ?: ""}")
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                baseVm.snackBarState = "Select category/subcategory manually before saving."
                            }
                        ) {
                            Text("Open Add Form")
                        }
                    }
                }
            }
        }
    )
}
