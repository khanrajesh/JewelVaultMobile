package com.velox.jewelvault.ui.components

import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QrBarScannerPage(
    showPage: MutableState<Boolean> = mutableStateOf(true),
    scanAndClose:Boolean = false,
    onCodeScanned: (String) -> Unit = {},
    valueProcessing: (String) -> String = { it },
    overlayContent: @Composable ( List<Pair<RectF, String>>) -> Unit = {}
) {
    if (showPage.value){
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val previewView = remember { PreviewView(context) }

        var barcodeResults by remember { mutableStateOf(listOf<Pair<RectF, String>>()) }
        val previewSize = remember { mutableStateOf(Size.Zero) }
        fun scaleRectToPreview(rect: Rect, imageWidth: Int, imageHeight: Int, previewWidth: Int, previewHeight: Int): RectF {
            val scaleX = previewWidth.toFloat() / imageWidth
            val scaleY = previewHeight.toFloat() / imageHeight

            return RectF(
                rect.left * scaleX,
                rect.top * scaleY,
                rect.right * scaleX,
                rect.bottom * scaleY
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            // Preview
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()   .onGloballyPositioned { coordinates ->
                val width = coordinates.size.width.toFloat()
                val height = coordinates.size.height.toFloat()
                previewSize.value = Size(width, height)
            }) {
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
                                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

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

                                        if (scanAndClose){
                                            barcodeResults.firstOrNull()?.let {res->
                                                onCodeScanned(res.second)
                                                showPage.value = ! showPage.value
                                            }
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

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraX", "Binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(context))
            }

            // Dark overlay
            if (!scanAndClose){
                Canvas(modifier = Modifier
                    .size(
                        with(LocalDensity.current) { previewSize.value.width.toDp() },
                        with(LocalDensity.current) { previewSize.value.height.toDp() }
                    )) {
                    drawRect(Color(0x88000000), size = size) // dark overlay

                    barcodeResults.forEach { (rect, _) ->
                        val left = rect.left
                        val top = rect.top
                        val width = rect.width()
                        val height = rect.height()

                        // Transparent barcode area
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            blendMode = BlendMode.Clear
                        )

                        // Draw corner caps for the barcode box
                        val cornerLength = 20.dp.toPx()
                        val strokeWidth = 4f
                        val brush = Brush.linearGradient(
                            colors = listOf(Color.White, Color.White) // Solid white color
                        )

                        // Top-left corner
                        drawLine(
                            brush = brush,
                            start = Offset(left, top),
                            end = Offset(left + cornerLength, top),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            brush = brush,
                            start = Offset(left, top),
                            end = Offset(left, top + cornerLength),
                            strokeWidth = strokeWidth
                        )

                        // Top-right corner
                        drawLine(
                            brush = brush,
                            start = Offset(left + width, top),
                            end = Offset(left + width - cornerLength, top),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            brush = brush,
                            start = Offset(left + width, top),
                            end = Offset(left + width, top + cornerLength),
                            strokeWidth = strokeWidth
                        )

                        // Bottom-left corner
                        drawLine(
                            brush = brush,
                            start = Offset(left, top + height),
                            end = Offset(left + cornerLength, top + height),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            brush = brush,
                            start = Offset(left, top + height),
                            end = Offset(left, top + height - cornerLength),
                            strokeWidth = strokeWidth
                        )

                        // Bottom-right corner
                        drawLine(
                            brush = brush,
                            start = Offset(left + width, top + height),
                            end = Offset(left + width - cornerLength, top + height),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            brush = brush,
                            start = Offset(left + width, top + height),
                            end = Offset(left + width, top + height - cornerLength),
                            strokeWidth = strokeWidth
                        )
                    }
                }


                barcodeResults.forEach { (rect, value) ->
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(rect.left.toInt(), (rect.bottom - -50).toInt().coerceAtLeast(0)) }
                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        val res= valueProcessing(value)
                        Text(text = res, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Box( modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                overlayContent(barcodeResults)
            }
        }


    }
}
