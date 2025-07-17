package com.velox.jewelvault.ui.screen.sell_invoice

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.ui.components.SignatureBox
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.sharePdf
import androidx.core.graphics.createBitmap


@Composable
fun SellPreviewScreen(sellInvoiceViewModel: SellInvoiceViewModel) {

    val context = LocalContext.current

    val pdfFile = sellInvoiceViewModel.generatedPdfFile
    val navController = LocalNavController.current

    BackHandler {
        sellInvoiceViewModel.clearData()
        navController.navigate(Screens.Main.route) {
            popUpTo(Screens.Main.route) {
                inclusive = true
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {

        Box(modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surface)) {
            pdfFile?.let { PdfRendererPreview(it) } ?: Text("Generate the invoice to preview PDF")
        }


        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Customer Signature")
            SignatureBox(
                modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)
                ),
                check = sellInvoiceViewModel.customerSign.value!=null,
                onSignatureCaptured = { bitmap ->
                sellInvoiceViewModel.customerSign.value = bitmap
            })

            Spacer(modifier = Modifier.height(16.dp))

            Text("Owner Signature")
            SignatureBox(
                modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)
                ),
                check = sellInvoiceViewModel.ownerSign.value!=null,
                onSignatureCaptured = { bitmap -> sellInvoiceViewModel.ownerSign.value = bitmap })

            Spacer(modifier = Modifier.height(24.dp))

            if (pdfFile == null) {
                Button(onClick = {
                    if (sellInvoiceViewModel.customerSign.value != null && sellInvoiceViewModel.ownerSign.value != null) {
                        sellInvoiceViewModel.completeOrder(onSuccess = {
                            sellInvoiceViewModel.snackBarState.value = "Order Completed"
                        }, onFailure = {
                            sellInvoiceViewModel.snackBarState.value = it
                        })
                    } else {
                        sellInvoiceViewModel.snackBarState.value = "Please Sign"
                    }
                }) {
                    Text("Complete Order")
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = { sharePdf(context, pdfFile) }) {
                        Text("Share PDF")
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(onClick = {
                        sellInvoiceViewModel.clearData()
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
fun PdfRendererPreview(uri: Uri) {
    val context = LocalContext.current
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    val a4Width = 595
    val a4Height = 842

    LaunchedEffect(uri) {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        if (parcelFileDescriptor != null) {
            val renderer = PdfRenderer(parcelFileDescriptor)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bitmap = createBitmap(a4Width, a4Height)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmapState.value = bitmap
            }
            renderer.close()
            parcelFileDescriptor.close()
        }
    }

    bitmapState.value?.let { bmp ->
        Box(
            modifier = Modifier
                .width(500.dp)
                .aspectRatio(595f / 842f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "PDF Page",
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.White)
            )
        }
    } ?: Text("Rendering PDF...")
}




