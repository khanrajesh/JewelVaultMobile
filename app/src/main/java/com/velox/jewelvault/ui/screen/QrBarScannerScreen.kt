package com.velox.jewelvault.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.velox.jewelvault.utils.LocalNavController

@Composable
fun QrBarScannerScreen() {
    val navHost = LocalNavController.current
    QrBarScannerPage(
        valueProcessing = {
            "Id: $it \n" +
                    "wt: 2gm 18k\n" +
                    "$50000"

        },
        overlayContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(Modifier.fillMaxWidth()) {
                    MetalRatesTicker(Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                        textColor = Color.White
                    )
                    Row(
                        Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Place the item code under the camera view to get the details of item. \n" +
                                    "Count: ${it.size}",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Clear, null,
                            modifier = Modifier.bounceClick {
                                navHost.popBackStack()
                            }.size(50.dp),
                            tint = Color.White
                        )
                    }

                }
            }
        }
    )
}