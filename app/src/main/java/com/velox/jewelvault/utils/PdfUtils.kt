package com.velox.jewelvault.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.roomdb.entity.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// PdfUtils.kt
fun generateInvoicePdf(
    context: Context,
    store: StoreEntity,
    customer: CustomerEntity,
    items: List<ItemSelectedModel>,
    customerSign: ImageBitmap,
    ownerSign: ImageBitmap,
    onFileReady: (Uri) -> Unit
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint().apply {
        color = Color.BLACK
        textSize = 12f
    }
    val boldPaint = Paint(paint).apply {
        isFakeBoldText = true
    }

    // White background
    canvas.drawColor(Color.WHITE)

    var y = 40f
    val startX = 30f
    val gapY = 18f

    // Header
    boldPaint.textSize = 16f
    canvas.drawText("Tax Invoice", startX + 200f, y, boldPaint)
    y += gapY

    // IRN and Ack Info (Fallback static values)
    paint.textSize = 10f
    canvas.drawText("IRN: ${/*store.irn ?:*/ "fe1df90406b...9375e6"}", startX, y, paint)
    y += 12f
    canvas.drawText("Ack No.: ${/*store.ackNo ?:*/ "112010036563310"}", startX, y, paint)
    canvas.drawText("Ack Date: ${/*store.ackDate ?:*/ "21-Dec-20"}", startX + 300f, y, paint)
    y += gapY + 10f

    // Seller info
    boldPaint.textSize = 12f
    canvas.drawText("${store.name ?: "Store Name"}, ${store.address ?: "Address"}", startX, y, boldPaint)
    y += gapY
    canvas.drawText("GSTIN/UIN: ${store.gstinNo ?: "N/A"}", startX, y, paint)
    y += gapY

    // Invoice No and Date (Fallback to system time)
    canvas.drawText("Invoice No.: ${/*store.invoiceNo ?:*/ "INV-${System.currentTimeMillis()}"}", startX + 300f, y - 18f, boldPaint)
    canvas.drawText("Date: ${/*store.invoiceDate ?:*/ SimpleDateFormat("dd-MMM-yy", Locale.getDefault()).format(Date())}", startX + 300f, y, boldPaint)
    y += gapY + 10f

    // Buyer Info
    canvas.drawText("Buyer (Bill to): ${customer.name ?: "Customer Name"}", startX, y, boldPaint)
    y += gapY
    canvas.drawText("Mobile: ${customer.mobileNo ?: "N/A"}", startX, y, paint)
    y += gapY
    canvas.drawText("GSTIN/UIN: ${customer.gstin_pan ?: "N/A"}", startX, y, paint)
    y += gapY
    canvas.drawText("Address: ${customer.address ?: "Customer Address"}", startX, y, paint)
    y += gapY + 10f

    // Table Header
    boldPaint.textSize = 11f
    canvas.drawText("Sl", startX, y, boldPaint)
    canvas.drawText("Description", startX + 20f, y, boldPaint)
    canvas.drawText("HSN", startX + 150f, y, boldPaint)
    canvas.drawText("Qty", startX + 200f, y, boldPaint)
    canvas.drawText("Rate", startX + 250f, y, boldPaint)
    canvas.drawText("Amount", startX + 320f, y, boldPaint)
    y += gapY

    var totalAmount = 0.0
    var totalCgst = 0.0
    var totalSgst = 0.0

    items.forEachIndexed { index, item ->
        val lineY = y + index * gapY
        canvas.drawText("${index + 1}", startX, lineY, paint)
        canvas.drawText(item.itemAddName ?: "Item", startX + 20f, lineY, paint)
        canvas.drawText("${item.huid ?: "1005"}", startX + 150f, lineY, paint)
        canvas.drawText("${item.quantity}", startX + 200f, lineY, paint)
        canvas.drawText("₹${item.price}", startX + 250f, lineY, paint)
        val lineTotal = item.price
        canvas.drawText("₹$lineTotal", startX + 320f, lineY, paint)

        totalAmount += item.price
        totalCgst += item.cgst
        totalSgst += item.sgst
    }

    y += items.size * gapY + 10f
    val totalTax = totalCgst + totalSgst
    val grandTotal = totalAmount + totalTax

    // Totals
    boldPaint.textSize = 12f
    canvas.drawText("Total:", startX + 250f, y, boldPaint)
    canvas.drawText("₹$totalAmount", startX + 320f, y, boldPaint)
    y += gapY
    canvas.drawText("CGST: ₹$totalCgst", startX + 250f, y, paint)
    y += gapY
    canvas.drawText("SGST: ₹$totalSgst", startX + 250f, y, paint)
    y += gapY
    canvas.drawText("Grand Total: ₹$grandTotal", startX + 250f, y, boldPaint)
    y += gapY

    // Amount in words
    val amountInWords = "Indian Rupee ${numberToWords(grandTotal.toInt())} Only"
    canvas.drawText("Amount in words:", startX, y, boldPaint)
    y += gapY
    canvas.drawText(amountInWords, startX, y, paint)
    y += gapY + 10f

    // Declaration
    canvas.drawText("Declaration:", startX, y, boldPaint)
    y += gapY
    canvas.drawText("We declare that this invoice shows the actual price of the", startX, y, paint)
    y += gapY
    canvas.drawText("goods described and that all particulars are true and correct.", startX, y, paint)
    y += gapY + 10f

    // Signatures
    canvas.drawText("for ${store.name ?: "Store"}", startX + 300f, y, boldPaint)
    y += 10f
    canvas.drawBitmap(ownerSign.asAndroidBitmap(), startX + 300f, y, paint)
    canvas.drawText("Authorised Signatory", startX + 300f, y + 60f, paint)

    canvas.drawBitmap(customerSign.asAndroidBitmap(), startX, y, paint)
    canvas.drawText("Customer Signature", startX, y + 60f, paint)

    pdfDocument.finishPage(page)

    val fileName = "invoice_${System.currentTimeMillis()}.pdf"
    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/JewelVault/Invoice")
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    if (uri != null) {
        var success = false
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                outputStream.flush() // ensure all bytes are written
                success = true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("PDF_GENERATION", "Error writing PDF: ${e.message}")
        } finally {
            pdfDocument.close()
        }

        if (success) {
            onFileReady(uri)  // notify caller only if writing succeeded
        } else {
            // Optionally delete incomplete file:
            resolver.delete(uri, null, null)
            Log.e("PDF_GENERATION", "Failed to write PDF file")
        }
    } else {
        Log.e("PDF_GENERATION", "Failed to create file in MediaStore")
        pdfDocument.close()
    }



}


fun sharePdf(context: Context, pdfUri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Invoice PDF"))
}
