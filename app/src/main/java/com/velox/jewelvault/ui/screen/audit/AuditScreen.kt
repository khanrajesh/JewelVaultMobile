@file:Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")

package com.velox.jewelvault.ui.screen.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.baseBackground1
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.to3FString
import com.velox.jewelvault.utils.parseQrItemPayload
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.ui.theme.LightGreen
import com.velox.jewelvault.ui.theme.LightRed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun AuditScreen(
    viewModel: AuditViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    viewModel.currentScreenHeadingState.value = "Audit"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .baseBackground1()
    ) {
        // Category/Subcategory Selection
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row {
                    Text(
                        text = "Select Category & Subcategory",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (viewModel.getScannedCount() > 0) {
                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.clearScannedItems() }
                            ) {
                                Text("Clear Scanned (${viewModel.getScannedCount()})")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.selectedCategory,
                        placeholderText = "Select Category",
                        dropdownItems = viewModel.catSubCatDto.map { it.catName },
                        onDropdownItemSelected = { selected ->
                            viewModel.onCategorySelected(selected)
                        }
                    )

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = viewModel.selectedSubCategory,
                        placeholderText = "Select Sub Category",
                        dropdownItems = viewModel.availableSubCategories.map { it.subCatName },
                        onDropdownItemSelected = { selected ->
                            viewModel.onSubCategorySelected(selected)
                        }
                    )
                }

                // Progress indicator
                if (viewModel.getTotalCount() > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progress: ${viewModel.getScannedCount()}/${viewModel.getTotalCount()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        val progress = if (viewModel.getTotalCount() > 0) {
                            viewModel.getScannedCount().toFloat() / viewModel.getTotalCount()
                                .toFloat()
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            color = ProgressIndicatorDefaults.linearColor,
                            trackColor = ProgressIndicatorDefaults.linearTrackColor,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        )
                    }
                }
            }
        }

        // Main Content - Split Screen
        if (viewModel.selectedCategory.text.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                // Left Panel - Items List (Fixed width)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Items (${viewModel.getTotalCount()})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(viewModel.allItems) { item ->
                            ItemCard(
                                item = item,
                                isScanned = viewModel.isItemScanned(item.itemId)
                            )
                        }
                    }
                }

                // Right Panel - Camera and Scanned Items (Remaining space)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Camera View
                    Text(
                        text = "Scanner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Embedded Camera Preview
                    EmbeddedCameraView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp) ,
                        onCodeScanned = { scannedId ->
                            viewModel.processScannedItem(scannedId)
                        },
                        selectedCategory = viewModel.selectedCategory.text,
                        selectedSubCategory = viewModel.selectedSubCategory.text
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scanned Items List
                    Text(
                        text = "Scanned Items (${viewModel.getScannedCount()})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(viewModel.scannedItems) { item ->
                            ScannedItemCard(item = item)
                        }
                    }
                }
            }
        } else {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Please select a category to start auditing",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }

}

@Composable
private fun ItemCard(
    item: ItemEntity,
    isScanned: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isScanned) LightGreen.copy(alpha = 0.1f) else LightRed.copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            1.dp,
            if (isScanned) LightGreen else LightRed
        )
    ) {

        Text(
            modifier = Modifier.padding(5.dp),
            text = "${item.itemAddName} (${item.itemId}), Wt: ${item.gsWt.to3FString()}g (${item.fnWt.to3FString()}g), Purity: ${item.purity}",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )


    }
}

@Composable
private fun ScannedItemCard(
    item: ItemSelectedModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LightGreen.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, LightGreen)
    ) {
        Text(
            modifier = Modifier.padding(5.dp),
            text = "${item.itemAddName} (${item.itemId}), Wt: ${item.gsWt.to3FString()}g (${item.fnWt.to3FString()}g), Purity: ${item.purity}",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
@Composable
private fun EmbeddedCameraView(
    modifier: Modifier = Modifier,
    onCodeScanned: (String) -> Unit,
    selectedCategory: String,
    selectedSubCategory: String
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var barcodeResults by remember { mutableStateOf(listOf<Pair<RectF, String>>()) }
    val previewSize = remember { mutableStateOf(Size.Zero) }

    fun scaleRectToPreview(
        rect: Rect,
        imageWidth: Int,
        imageHeight: Int,
        previewWidth: Int,
        previewHeight: Int
    ): RectF {
        val scaleX = previewWidth.toFloat() / imageWidth
        val scaleY = previewHeight.toFloat() / imageHeight

        return RectF(
            rect.left * scaleX,
            rect.top * scaleY,
            rect.right * scaleX,
            rect.bottom * scaleY
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val width = coordinates.size.width.toFloat()
                    val height = coordinates.size.height.toFloat()
                    previewSize.value = Size(width, height)
                }
        ) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val barcodeScanner = BarcodeScanning.getClient()

                val imageAnalyzer = ImageAnalysis.Builder().build().also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val imageWidth = imageProxy.width
                                    val imageHeight = imageProxy.height
                                    val previewWidth = previewView.width
                                    val previewHeight = previewView.height

                                    val scaled = barcodes.mapNotNull { barcode ->
                                        val rect = barcode.boundingBox ?: return@mapNotNull null
                                        val value = barcode.rawValue ?: return@mapNotNull null

                                        val scaledRect = scaleRectToPreview(
                                            rect,
                                            imageWidth,
                                            imageHeight,
                                            previewWidth,
                                            previewHeight
                                        )

                                        scaledRect to value
                                    }
                                    barcodeResults = scaled

                                    // Process the first detected barcode
                                    barcodeResults.firstOrNull()?.let { result ->
                                        val rawValue = result.second.trim()
                                        val normalizedId = parseQrItemPayload(rawValue)?.id ?: rawValue
                                        onCodeScanned(normalizedId)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("Camera", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // Overlay with scanning frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw semi-transparent overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.3f),
                size = size
            )

            // Draw scanning frame (square in center)
            val frameSize = minOf(canvasWidth, canvasHeight) * 0.6f
            val frameLeft = (canvasWidth - frameSize) / 2
            val frameTop = (canvasHeight - frameSize) / 2

            // Clear the scanning area
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize)
            )

            // Draw frame border
            drawRect(
                color = Color.White,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize),
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw corner indicators
            val cornerLength = 20.dp.toPx()
            val cornerWidth = 3.dp.toPx()

            // Top-left corner
            drawLine(
                color = Color.White,
                start = Offset(frameLeft, frameTop),
                end = Offset(frameLeft + cornerLength, frameTop),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = Color.White,
                start = Offset(frameLeft, frameTop),
                end = Offset(frameLeft, frameTop + cornerLength),
                strokeWidth = cornerWidth
            )

            // Top-right corner
            drawLine(
                color = Color.White,
                start = Offset(frameLeft + frameSize, frameTop),
                end = Offset(frameLeft + frameSize - cornerLength, frameTop),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = Color.White,
                start = Offset(frameLeft + frameSize, frameTop),
                end = Offset(frameLeft + frameSize, frameTop + cornerLength),
                strokeWidth = cornerWidth
            )

            // Bottom-left corner
            drawLine(
                color = Color.White,
                start = Offset(frameLeft, frameTop + frameSize),
                end = Offset(frameLeft + cornerLength, frameTop + frameSize),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = Color.White,
                start = Offset(frameLeft, frameTop + frameSize),
                end = Offset(frameLeft, frameTop + frameSize - cornerLength),
                strokeWidth = cornerWidth
            )

            // Bottom-right corner
            drawLine(
                color = Color.White,
                start = Offset(frameLeft + frameSize, frameTop + frameSize),
                end = Offset(frameLeft + frameSize - cornerLength, frameTop + frameSize),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = Color.White,
                start = Offset(frameLeft + frameSize, frameTop + frameSize),
                end = Offset(frameLeft + frameSize, frameTop + frameSize - cornerLength),
                strokeWidth = cornerWidth
            )
        }

        // Category/Subcategory info overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedCategory.isNotEmpty()) {
                    Text(
                        text = "$selectedCategory, $selectedSubCategory",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

//                if (selectedSubCategory.isNotEmpty()) {
//                    Text(
//                        text = "Subcategory: $selectedSubCategory",
//                        color = Color.White,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier
//                            .background(
//                                Color.Black.copy(alpha = 0.7f),
//                                RoundedCornerShape(4.dp)
//                            )
//                            .padding(horizontal = 8.dp, vertical = 4.dp)
//                    )
//                }

            }
        }
    }
}
