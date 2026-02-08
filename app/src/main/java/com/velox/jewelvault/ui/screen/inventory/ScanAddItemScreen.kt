package com.velox.jewelvault.ui.screen.inventory

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.ui.components.WidthThenHeightSpacer
import com.velox.jewelvault.ui.components.baseBackground0
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.parseQrItemPayload

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ScanAddItemScreen(inventoryViewModel: InventoryViewModel) {
    inventoryViewModel.currentScreenHeadingState.value = "Scan & Add"

    val baseVm = LocalBaseViewModel.current
    val lastScan = remember { mutableStateOf<String?>(null) }
    val status = remember { mutableStateOf("Scan the item's QR to extract details.") }
    val scannedItems = inventoryViewModel.scannedItems
    val listState = rememberLazyListState()
    val lastCount = remember { mutableStateOf(0) }

    LaunchedEffect(true) {
        inventoryViewModel.getCategoryAndSubCategoryDetails()
    }
    LaunchedEffect(scannedItems.size) {
        if (scannedItems.size > lastCount.value) {
            listState.animateScrollToItem(0)
        }
        lastCount.value = scannedItems.size
    }

    val config = LocalConfiguration.current
    config.screenHeightDp / 4
    val contentWidth = (config.screenWidthDp.dp - 16.dp).coerceAtLeast(0.dp)
    val gap = 12.dp
    val scannerWidthLandscape = (contentWidth * 0.32f).coerceAtLeast(190.dp)
    val detailsWidthLandscape = (contentWidth - scannerWidthLandscape - gap).coerceAtLeast(0.dp)

    RowOrColumn(
        rowModifier = Modifier
            .fillMaxSize()
            .baseBackground0()
            .padding(8.dp),
        columnModifier = Modifier
            .fillMaxSize()
            .baseBackground0()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) { isLandscape ->
        val scannerModifier = if (isLandscape) {
            Modifier.width(scannerWidthLandscape)
        } else {
            Modifier.fillMaxWidth()
        }
        val detailsModifier = if (isLandscape) {
            Modifier.width(detailsWidthLandscape)
        } else {
            Modifier.fillMaxWidth()
        }

        Column(
            scannerModifier, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(185.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val squareSize = if (maxWidth < maxHeight) maxWidth else maxHeight
                    Box(
                        modifier = Modifier
                            .size(squareSize)
                            .background(
                                MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)
                            )
                            .border(
                                1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)
                            )
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        EmbeddedScanCamera(
                            onCodeScanned = { raw ->
                                if (raw == lastScan.value) return@EmbeddedScanCamera
                                lastScan.value = raw
                                val payload = parseQrItemPayload(raw)
                                if (payload != null) {
                                    inventoryViewModel.addScannedItemFromQr(payload)
                                    status.value = "Scanned: ${payload.id}"
                                } else {
                                    status.value = "Unrecognized QR. Ensure it uses JV1 CSV format."
                                    baseVm.snackBarState = "Cannot parse QR payload"
                                }
                            }
                        )
                    }
                }
            }
        }

        WidthThenHeightSpacer(12.dp)

        Column(detailsModifier) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Scanned: ${scannedItems.size}",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(scannedItems, key = { _, draft -> System.identityHashCode(draft) }) { index, draft ->
                    ScannedItemCard(
                        index = index,
                        draft = draft,
                        inventoryViewModel = inventoryViewModel
                    )
                }
            }

            if (scannedItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { inventoryViewModel.addValidScannedItems() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Valid Items")
                    }
                    Button(
                        onClick = { inventoryViewModel.clearScannedItems() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedItemCard(
    index: Int,
    draft: ScannedQrItemDraft,
    inventoryViewModel: InventoryViewModel
) {
    val statusColor = when (draft.status.value) {
        ScanRowStatus.READY -> Color(0xFF2E7D32)
        ScanRowStatus.WARNING -> Color(0xFFF9A825)
        ScanRowStatus.ERROR -> Color(0xFFC62828)
    }

    val categoryNames = inventoryViewModel.catSubCatDto.map { it.catName }
    val selectedCat = inventoryViewModel.catSubCatDto.firstOrNull {
        it.catName.equals(draft.catName.text.trim(), true)
    }
    val subCategoryNames = selectedCat?.subCategoryList?.map { it.subCatName } ?: emptyList()

    val containerColor = when (draft.status.value) {
        ScanRowStatus.READY -> MaterialTheme.colorScheme.tertiaryContainer
        ScanRowStatus.WARNING -> MaterialTheme.colorScheme.secondaryContainer
        ScanRowStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
    }.copy(alpha = if (index % 2 == 0) 0.55f else 0.7f)

    val transitionState = remember(System.identityHashCode(draft)) {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "No. ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "ID: ${draft.id.text.ifBlank { "-" }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = draft.status.value.name,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.id,
                        placeholderText = "Item ID",
                        onTextChange = {
                            inventoryViewModel.onScannedItemChanged(draft, checkDb = true)
                        })
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.name,
                        placeholderText = "Item Name",
                        onTextChange = {
                            inventoryViewModel.onScannedItemChanged(draft)
                        })
                }

                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.catName,
                        placeholderText = "Category",
                        dropdownItems = categoryNames,
                        allowEditOnDropdown = true,
                        onDropdownItemSelected = {
                            inventoryViewModel.onScannedItemChanged(draft)
                        },
                        onTextChange = {
                            inventoryViewModel.onScannedItemChanged(draft)
                        })
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.subCatName,
                        placeholderText = "SubCategory",
                        dropdownItems = subCategoryNames,
                        allowEditOnDropdown = true,
                        onDropdownItemSelected = {
                            inventoryViewModel.onScannedItemChanged(draft)
                        },
                        onTextChange = {
                            inventoryViewModel.onScannedItemChanged(draft)
                        })
                }

                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.entryType,
                        placeholderText = "Entry Type",
                        dropdownItems = EntryType.list(),
                        allowEditOnDropdown = true,
                        onDropdownItemSelected = { inventoryViewModel.onScannedItemChanged(draft) },
                        onTextChange = { inventoryViewModel.onScannedItemChanged(draft) })
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.quantity,
                        placeholderText = "Qty",
                        onTextChange = { inventoryViewModel.onScannedItemChanged(draft) })
                }

                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.gs,
                        placeholderText = "Gs Wt",
                        onTextChange = { inventoryViewModel.onScannedItemChanged(draft) })
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.nt,
                        placeholderText = "Nt Wt",
                        onTextChange = { inventoryViewModel.onScannedItemChanged(draft) })
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.fn,
                        placeholderText = "Fn Wt",
                        onTextChange = { inventoryViewModel.onScannedItemChanged(draft) })
                }

                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.purity,
                        placeholderText = "Purity",
                        dropdownItems = Purity.list(),
                        allowEditOnDropdown = true,
                        onDropdownItemSelected = { inventoryViewModel.onScannedItemChanged(draft) },
                        onTextChange = { inventoryViewModel.onScannedItemChanged(draft) })
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.mcType,
                        placeholderText = "MC Type",
                        dropdownItems = ChargeType.list(),
                        allowEditOnDropdown = true,
                        onDropdownItemSelected = { inventoryViewModel.onScannedItemChanged(draft) },
                        onTextChange = { inventoryViewModel.onScannedItemChanged(draft) })
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = draft.mc,
                        placeholderText = "MC",
                        onTextChange = { inventoryViewModel.onScannedItemChanged(draft) })
                }

                if (draft.errors.value.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Errors: ${draft.errors.value.joinToString("; ")}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (draft.warnings.value.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Warnings: ${draft.warnings.value.joinToString("; ")}",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Mapped: ${draft.catName.text.ifBlank { "-" }} -> ${draft.subCatName.text.ifBlank { "-" }}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { inventoryViewModel.removeScannedItem(draft) },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
@Composable
private fun EmbeddedScanCamera(
    modifier: Modifier = Modifier,
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val lastEmit = remember { mutableStateOf<Pair<String, Long>?>(null) }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val raw = barcodes.firstOrNull()?.rawValue?.trim().orEmpty()
                                    if (raw.isNotEmpty()) {
                                        val now = System.currentTimeMillis()
                                        val last = lastEmit.value
                                        if (last == null || last.first != raw || now - last.second > 1200L) {
                                            lastEmit.value = raw to now
                                            onCodeScanned(raw)
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ScanAddItemScreen", "Barcode scan failed", e)
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
                Log.e("ScanAddItemScreen", "Use case binding failed", exc)
            }
        }

        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (exc: Exception) {
                Log.e("ScanAddItemScreen", "Failed to unbind camera", exc)
            }
            barcodeScanner.close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameSize = minOf(size.width, size.height) * 0.7f
            val frameLeft = (size.width - frameSize) / 2f
            val frameTop = (size.height - frameSize) / 2f

            drawRect(
                color = Color.Black.copy(alpha = 0.28f),
                size = size
            )
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
