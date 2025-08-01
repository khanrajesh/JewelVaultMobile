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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.ui.components.QrBarScannerPage
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.canBeInt
import com.velox.jewelvault.utils.to2FString

@Composable
fun QrBarScannerScreen(viewModel: QrBarScannerViewModel) {
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
                                                "\nWt: ${existing.gsWt.to2FString()}(${existing.fnWt.to2FString()}) gm, ${existing.purity}, E.P: ₹${(existing.price + existing.chargeAmount + existing.tax).to2FString()}"
                                    Text(text = res, color = Color.Yellow, fontSize = 14.sp)

                                }
                            }
                        }
                    }


                }
            }
        }
    })
}
