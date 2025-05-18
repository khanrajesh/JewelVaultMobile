package com.velox.jewelvault.ui.screen.sell_invoice

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import com.velox.jewelvault.ui.components.SignatureBox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.utils.sharePdf


@Composable
fun SellPreviewScreen(sellInvoiceViewModel: SellInvoiceViewModel) {

    val context = LocalContext.current

    val pdfFile = sellInvoiceViewModel.generatedPdfFile

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        Box(modifier = Modifier.weight(1f)) {
            pdfFile?.let { PdfRendererPreview(it) } ?: Text("Generate the invoice to preview PDF")
        }


        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Customer Signature")
            SignatureBox(
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    ),
                onSignatureCaptured = { bitmap -> sellInvoiceViewModel.customerSign.value = bitmap }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Owner Signature")
            SignatureBox(
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    ),
                onSignatureCaptured = { bitmap -> sellInvoiceViewModel.ownerSign.value = bitmap }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
                if (sellInvoiceViewModel.customerSign.value != null && sellInvoiceViewModel.ownerSign.value != null) {
                    sellInvoiceViewModel.generateInvoice()
                }else{
                    Toast.makeText(context, "Please Sign ", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Generate Invoice")
            }

            pdfFile?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { sharePdf(context, it) }) {
                    Text("Share PDF")
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
                val bitmap = Bitmap.createBitmap(a4Width, a4Height, Bitmap.Config.ARGB_8888)
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
                .width(300.dp)
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




