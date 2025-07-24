package com.velox.jewelvault.ui.screen.draft_invoice

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.velox.jewelvault.ui.components.SignatureBox
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.sharePdf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex

@Composable
fun DraftPreviewScreen(viewModel: DraftInvoiceViewModel) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val pdfFile = viewModel.generatedPdfFile.value

    BackHandler {
        if (pdfFile != null) {
            viewModel.clearData()
            navController.navigate(Screens.Main.route) {
                popUpTo(Screens.Main.route) {
                    inclusive = true
                }
            }
        }
    }

    LaunchedEffect(true) {
        viewModel.completeOrder(context, onSuccess = {
            viewModel.snackBarState.value = "Draft Order Completed"
        }, onFailure = {
            viewModel.snackBarState.value = it
        })
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Main PDF preview area with zoom/pan/high quality
        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .zIndex(1f)
        ) {
            pdfFile?.let {
                // --- Move dialog features here ---
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }
                val minScale = 1f
                val maxScale = 5f

                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(minScale, maxScale)
                                val maxPanX = 1000f * (scale - 1)
                                val maxPanY = 1000f * (scale - 1)
                                offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                            }
                        }
                ) {
                    PdfRendererPreview(
                        uri = it,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .background(MaterialTheme.colorScheme.surface),
                        highQuality = true // Always high quality
                    )
                    // Reset button
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(18.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom/Pan")
                    }
                }
            } ?: Text("Generate the draft invoice to preview PDF")
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Customer Signature")
            SignatureBox(
                modifier = Modifier
                    .height(250.dp)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)
                    ),
                check = viewModel.customerSign.value != null,
                onSignatureCaptured = { bitmap ->
                    viewModel.customerSign.value = bitmap
                })
            Spacer(modifier = Modifier.height(4.dp))
            Text("Owner Signature")
            SignatureBox(
                modifier = Modifier
                    .height(250.dp)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)
                    ),
                check = viewModel.ownerSign.value != null,
                onSignatureCaptured = { bitmap ->
                    viewModel.ownerSign.value = bitmap
                })
            Spacer(modifier = Modifier.height(24.dp))

            if (pdfFile == null) {
                Button(onClick = {
                    // Allow PDF generation even without signatures
                    viewModel.completeOrder(context, onSuccess = {
                        viewModel.snackBarState.value = "Draft Order Completed"
                    }, onFailure = {
                        viewModel.snackBarState.value = it
                    })
                }) {
                    Text("Complete Draft Order")
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = { sharePdf(context, pdfFile) }) {
                        Text("Share PDF")
                    }
                    Spacer(Modifier.width(16.dp))
                    TextButton(onClick = {
                        viewModel.clearData()
                        navController.navigate(Screens.Main.route) {
                            popUpTo(Screens.Main.route) {
                                inclusive = true
                            }
                        }
                    }) {
                        Text("Exit")
                    }
                }
            }
        }
    }

 
}


@Composable
fun PdfRendererPreview(uri: Uri, modifier: Modifier = Modifier, highQuality: Boolean = false) {
    val context = LocalContext.current
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    // Use higher resolution for better quality
    val scaleFactor = if (highQuality) 3 else 1 // 3x resolution for dialog
    val a4Width = (595 * scaleFactor).toInt()  // 210mm in points
    val a4Height = (421 * scaleFactor).toInt() // 148.5mm in points
    
    LaunchedEffect(uri) {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        if (parcelFileDescriptor != null) {
            val renderer = PdfRenderer(parcelFileDescriptor)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bitmap = createBitmap(a4Width, a4Height)
                
                // Use higher quality rendering mode for better results
                val renderMode = if (highQuality) {
                    PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                } else {
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                }
                
                page.render(bitmap, null, null, renderMode)
                page.close()
                bitmapState.value = bitmap
            }
            renderer.close()
            parcelFileDescriptor.close()
        }
    }

    bitmapState.value?.let { bmp ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(595f / 421f) // Keep original aspect ratio
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "PDF Page",
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
    } ?: Text("Rendering PDF...")
} 