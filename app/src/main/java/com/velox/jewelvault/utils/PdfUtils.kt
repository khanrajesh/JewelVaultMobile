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
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.roomdb.entity.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// PdfUtils.kt

object PdfUtils {

    val A4_PORTRAIT = Pair(545, 842)
    val A4_HALF_PORTRAIT = Pair(545, 421)

}

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
    canvas.drawText(
        "${store.name ?: "Store Name"}, ${store.address ?: "Address"}",
        startX,
        y,
        boldPaint
    )
    y += gapY
    canvas.drawText("GSTIN/UIN: ${store.gstinNo ?: "N/A"}", startX, y, paint)
    y += gapY

    // Invoice No and Date (Fallback to system time)
    canvas.drawText(
        "Invoice No.: ${/*store.invoiceNo ?:*/ "INV-${System.currentTimeMillis()}"}",
        startX + 300f,
        y - 18f,
        boldPaint
    )
    canvas.drawText(
        "Date: ${/*store.invoiceDate ?:*/ SimpleDateFormat(
            "dd-MMM-yy",
            Locale.getDefault()
        ).format(Date())
        }", startX + 300f, y, boldPaint
    )
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
    canvas.drawText(
        "goods described and that all particulars are true and correct.",
        startX,
        y,
        paint
    )
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
        put(
            MediaStore.Downloads.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS + "/JewelVault/Invoice"
        )
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

fun generateDraftInvoicePdf(
    context: Context,
    store: StoreEntity,
    customer: CustomerEntity,
    items: List<ItemSelectedModel>,
    scale: Float = 2f,
//    customerSign: ImageBitmap,
//    ownerSign: ImageBitmap,
    onFileReady: (Uri) -> Unit
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(
        (PdfUtils.A4_HALF_PORTRAIT.first * scale).toInt(),
        (PdfUtils.A4_HALF_PORTRAIT.second * scale).toInt(),
        1
    ).create()

    val s02f = 0.2f * scale
    val s05f = 0.5f * scale
    val s1f = 1f * scale
    val s2f = 2f * scale
    val s3f = 3f * scale
    val s5f = 5f * scale
    val s7f = 7f * scale
    val s9f = 9f * scale
    val s10f = 10f * scale
    val s15f = 15f * scale
    val s20f = 20f * scale
    val s25f = 25f * scale
    val s30f = 30f * scale
    val s40f = 40f * scale
    val s50f = 50f * scale
    val s60f = 60f * scale
    val s80f = 80f * scale
    val s100f = 100f * scale
    val s120f = 120f * scale
    val s150f = 150f * scale
    val s200f = 200f * scale
    val s250f = 250f * scale
    val s300f = 300f * scale
    val s350f = 350f * scale
    val s400f = 400f * scale
    val s450f = 450f * scale
    val s500f = 500f * scale
    val s545f = 545f * scale

    val startX = s15f
    val startY = s15f
    val endX = pageInfo.pageWidth - s15f
    val endY = pageInfo.pageHeight - s15f

    val page = pdfDocument.startPage(pageInfo)

    val canvas = page.canvas

    val paint = Paint().apply {
        color = Color.BLACK
        textSize = 10f
    }

    val tp14Bold = Paint(paint).apply {
        textSize = 14f
        isFakeBoldText = true
        color = Color.BLACK
    }
    val tp14 = Paint(paint).apply {
        textSize = 14f
        color = Color.BLACK
    }
    val tp16Bold = Paint(paint).apply {
        textSize = 16f
        isFakeBoldText = true
        color = Color.BLACK
    }
    val tp16 = Paint(paint).apply {
        textSize = 16f
        color = Color.BLACK
    }
    val headerPaint = Paint(paint).apply {
        textSize = 14f
        isFakeBoldText = true
        color = Color.rgb(139, 0, 0)
    }

    // White background
    canvas.drawColor(Color.WHITE)
    
    /*// Draw a point at (30f, 515f) with thickness 5
    val thickPointPaint = Paint(titlePaint).apply { strokeWidth = 5f }
    // Top-left corner
    canvas.drawPoint(startX, startY, thickPointPaint)
    // Top-right corner
    canvas.drawPoint(endX, startY, thickPointPaint)
    // Bottom-left corner
    canvas.drawPoint(startX, endY, thickPointPaint)
    // Bottom-right corner
    canvas.drawPoint(endX, endY, thickPointPaint)*/

    //--------------------------------------------------------- PDF DESIGN Start -------------------------------------------------------//

    //outer box
    // Draw the outer box with stroke width s3f
    val outerBoxPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    canvas.drawRect(startX, startY+s100f-s15f, endX, endY, outerBoxPaint)

    val innerBoxPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    val nameTextStartX = startX + s5f

    val nameText1Y = startY+s100f-s5f
    canvas.drawText("Name: Rajesh Khan ", nameTextStartX, nameText1Y, tp16Bold )

    val textEndX = endX - s5f
    val dateTextStartX = textEndX-s80f
    canvas.drawText("Date: 12/12/2023", dateTextStartX, nameText1Y, tp16Bold )

    val invoiceValueStartX = dateTextStartX-s50f+s5f
    canvas.drawText("A003568", invoiceValueStartX, nameText1Y, tp16Bold )

    val invoiceKeyStartX = invoiceValueStartX-s30f-s10f
    canvas.drawText("Invoice No:", invoiceKeyStartX, nameText1Y, tp16 )

    val issueTimeKeyStartY = nameText1Y+s7f+s05f
    canvas.drawText("Issue Time:", invoiceKeyStartX, issueTimeKeyStartY, tp16 )
    canvas.drawText("20:30", invoiceValueStartX, issueTimeKeyStartY, tp16Bold )

    val saleManeKeyStartY = issueTimeKeyStartY+s7f+s05f
    canvas.drawText("Sales Man:", invoiceKeyStartX, saleManeKeyStartY, tp16 )
    canvas.drawText("Estimate", invoiceValueStartX, saleManeKeyStartY, tp16Bold )

    canvas.drawRect(startX, startY+s150f-s15f-s20f, endX, endY-s50f-s30f, innerBoxPaint)
    canvas.drawRect(startX, startY+s150f-s10f, endX, endY-s50f-s30f, innerBoxPaint)



    //--------------------------------------------------------- PDF DESIGN END ---------------------------------------------------------//
    pdfDocument.finishPage(page)

    val fileName = "draft_invoice_${System.currentTimeMillis()}.pdf"
    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(
            MediaStore.Downloads.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS + "/JewelVault/DraftInvoice"
        )
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    if (uri != null) {
        var success = false
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
                success = true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("PDF_GENERATION", "Error writing PDF: ${e.message}")
        } finally {
            pdfDocument.close()
        }

        if (success) {
            onFileReady(uri)
        } else {
            resolver.delete(uri, null, null)
            Log.e("PDF_GENERATION", "Failed to write PDF file")
        }
    } else {
        Log.e("PDF_GENERATION", "Failed to create file in MediaStore")
        pdfDocument.close()
    }
}


fun generateDraftInvoicePdf2(

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
        textSize = 10f
    }
    val boldPaint = Paint(paint).apply {
        isFakeBoldText = true
    }
    val titlePaint = Paint(paint).apply {
        textSize = 16f
        isFakeBoldText = true
        color = Color.rgb(139, 0, 0) // Dark red/maroon
    }
    val headerPaint = Paint(paint).apply {
        textSize = 14f
        isFakeBoldText = true
        color = Color.rgb(139, 0, 0)
    }

    // White background
    canvas.drawColor(Color.WHITE)

    var y = 30f
    val startX = 30f
    val gapY = 15f
    // Left side - Logo placeholder
    canvas.drawRect(
        startX,
        y,
        startX + 60f,
        y + 60f,
        Paint().apply { color = Color.rgb(139, 0, 0) })
    boldPaint.textSize = 12f
    canvas.drawText("R.K.J", startX + 10f, y + 35f, boldPaint)

    // Center - Certifications
    val certText = "Govt. Registered ✓ DIC: Registered ✓ ISO: Certified ✓ BIS: Certified"
    paint.textSize = 8f
    val certBoxLeft = startX + 80f
    val certBoxTop = y
    val certBoxRight = certBoxLeft + 270f
    val certBoxBottom = certBoxTop + 20f
    canvas.drawRect(
        certBoxLeft,
        certBoxTop,
        certBoxRight,
        certBoxBottom,
        Paint().apply { color = Color.rgb(255, 255, 224) })
    canvas.drawText(certText, certBoxLeft + 5f, certBoxTop + 12f, paint)

    // Company name (centered above cert box)
    titlePaint.textSize = 18f
    val companyName = "R. K. JEWELLERS"
    val companyNameX =
        certBoxLeft + (certBoxRight - certBoxLeft) / 2 - titlePaint.measureText(companyName) / 2
    canvas.drawText(companyName, companyNameX, y + 40f, titlePaint)

    // Address (below cert box)
    paint.textSize = 10f
    canvas.drawText(
        "D.N.K. Chowk, Main Road, Malkangiri, Odisha - 764048",
        certBoxLeft,
        y + 65f,
        paint
    )
    canvas.drawText("Phone : 6861-796018, 8895311750, 9411111425", certBoxLeft, y + 80f, paint)

    // Right side - Logos (aligned to right margin)
    val rightLogoX = startX + 420f
    val logoY = y + 10f
    canvas.drawCircle(rightLogoX + 20f, logoY + 10f, 10f, Paint().apply { color = Color.GREEN })
    paint.textSize = 6f
    canvas.drawText("HALLMARKED", rightLogoX + 35f, logoY + 15f, paint)
    canvas.drawText("GOLD", rightLogoX + 35f, logoY + 25f, paint)
    canvas.drawCircle(rightLogoX + 20f, logoY + 40f, 10f, Paint().apply { color = Color.GREEN })
    canvas.drawText("ISO 9001", rightLogoX + 35f, logoY + 45f, paint)
    canvas.drawText("CERTIFIED", rightLogoX + 35f, logoY + 55f, paint)

    y += 100f

    // Customer and Invoice Details
    // Left Column - Customer Details
    boldPaint.textSize = 11f
    canvas.drawText("Name : ${customer.name ?: "PADMA DURA"}", startX, y, boldPaint)
    y += gapY
    canvas.drawText("PODAVETA", startX, y, paint)
    y += gapY
    canvas.drawText("Mobile : ${customer.mobileNo ?: "7854900171"}", startX, y, paint)

    // Right Column - Invoice Details
    val rightDetailX = startX + 350f
    y -= 30f
    canvas.drawText("Invoice No. : ${store.invoiceNo ?: "A003439"}", rightDetailX, y, boldPaint)
    y += gapY
    canvas.drawText(
        "Date : ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}",
        rightDetailX,
        y,
        paint
    )
    y += gapY
    canvas.drawText(
        "Issue Time : ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
        rightDetailX,
        y,
        paint
    )
    y += gapY
    canvas.drawText("Sales Man :", rightDetailX, y, paint)

    // ESTIMATE text (centered below details)
    titlePaint.textSize = 20f
    val estimateText = "ESTIMATE"
    val estimateX = startX + 250f - titlePaint.measureText(estimateText) / 2
    canvas.drawText(estimateText, estimateX, y + 40f, titlePaint)

    y += 50f

    // Item Details Table Header
    val tableStartX = startX
    val colWidths = listOf(40f, 120f, 50f, 70f, 70f, 60f, 70f, 50f, 50f, 70f) // 10 columns
    val headers = listOf(
        "Sr Ref No.",
        "Product Description",
        "Qty./Set",
        "Gross.Wt in GMS",
        "Net Weight in GMS",
        "Rate/Gm",
        "Making Amount",
        "Purity %",
        "E.G.",
        "Total Amount"
    )

    // Draw table border
    val tablePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    var currentX = tableStartX
    val tableWidth = colWidths.sum()
    val tableHeight = 20f + (items.size * 25f)

    // Draw outer table border
    canvas.drawRect(tableStartX, y, tableStartX + tableWidth, y + tableHeight, tablePaint)

    // Draw header row background and text
    headers.forEachIndexed { index, header ->
        canvas.drawRect(
            currentX,
            y,
            currentX + colWidths[index],
            y + 20f,
            Paint().apply { color = Color.LTGRAY })
        // Use a consistent left padding for header text
        canvas.drawText(header, currentX + 6f, y + 14f, paint.apply { textSize = 7f })
        currentX += colWidths[index]
    }

    y += 25f

    // Item Data Row
    items.forEachIndexed { index, item ->
        currentX = tableStartX
        val rowData = listOf(
            "${index + 1}",
            "${item.huid ?: "R1047371"}",
            "${item.itemAddName ?: "NECKLACE 750/HUID"}",
            "${item.quantity}",
            "${item.gsWt}",
            "${item.ntWt}",
            "₹${item.fnMetalPrice}",
            "₹${item.chargeAmount}",
            "${item.purity}",
            "₹${item.price}"
        )

        rowData.forEachIndexed { colIndex, data ->
            // Draw cell background
            canvas.drawRect(
                currentX,
                y,
                currentX + colWidths[colIndex],
                y + 20f,
                Paint().apply { color = Color.WHITE })
            // Draw cell border
            canvas.drawRect(currentX, y, currentX + colWidths[colIndex], y + 20f, Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            })
            // Use a consistent left padding for data text
            canvas.drawText(data, currentX + 6f, y + 14f, paint.apply { textSize = 7f })
            currentX += colWidths[colIndex]
        }
        y += 25f
    }

    // Draw vertical lines for table columns
    currentX = tableStartX
    colWidths.forEachIndexed { index, width ->
        if (index < colWidths.size - 1) {
            canvas.drawLine(
                currentX + width, y - (items.size * 25f),
                currentX + width, y,
                tablePaint
            )
        }
        currentX += width
    }

    y += 20f // Summary Section - Three Columns
    val col1X = startX
    val col2X = startX + 200f
    val col3X = startX + 400f
    // Left Column - Gold/Silver Details
    boldPaint.textSize = 11f
    canvas.drawText("GOLD DETAIL", col1X, y, boldPaint)
    y += gapY
    canvas.drawText("Weight: ${items.sumOf { it.gsWt }}", col1X, y, paint)
    y += gapY
    canvas.drawText("Total Val: ${items.sumOf { it.price }}", col1X, y, paint)
    y += gapY
    canvas.drawText("Tot-MC: ${items.sumOf { it.chargeAmount }}", col1X, y, paint)
    y += gapY
    canvas.drawText("E.G.: ${items.sumOf { it.othCrg }}", col1X, y, paint)
    y += gapY
    canvas.drawText(
        "Tot-Amt: ${items.sumOf { it.price + it.chargeAmount + it.othCrg }}",
        col1X,
        y,
        paint
    )
    y += gapY

    canvas.drawText("SILVER DETAIL", col1X, y, boldPaint)
    y += gapY
    canvas.drawText("Weight: 0", col1X, y, paint)
    y += gapY
    canvas.drawText("Total Val: 0", col1X, y, paint)
    y += gapY
    canvas.drawText("Tot-MC: 0", col1X, y, paint)
    y += gapY
    canvas.drawText("E.G.: 0", col1X, y, paint)
    y += gapY
    canvas.drawText("Tot-Amt: 0", col1X, y, paint)
    y += gapY

    // Amount in words
    val totalAmount = items.sumOf { it.price + it.chargeAmount + it.othCrg }
    val amountInWords = "Rs. ${numberToWords(totalAmount.toInt())} only"
    canvas.drawText("Amount in words:", col1X, y, boldPaint)
    y += gapY
    canvas.drawText(amountInWords, col1X, y, paint)
    y += gapY

    // Declaration
    canvas.drawText("Declaration:", col1X, y, boldPaint)
    y += gapY
    val declaration =
        "Goods once sold will not be taken back. Goods once sold will not be taken back or exchanged. All disputes subject to Malkangiri Jurisdiction only. Please refer backside for all Terms & Conditions."
    paint.textSize = 8f
    canvas.drawText(declaration, col1X, y, paint)
    y += gapY
    canvas.drawText("E.& O.E.", col1X, y, paint)

    // Middle Column - Current Rates & Payment
    y = 400f
    canvas.drawText("Current Rates:", col2X, y, boldPaint)
    y += gapY
    canvas.drawText("Gold @ Rs. 92150", col2X, y, paint)
    y += gapY
    canvas.drawText("Silver @ Rs. 1000", col2X, y, paint)
    y += gapY

    canvas.drawText("Payment Detail:", col2X, y, boldPaint)
    y += gapY
    canvas.drawText("CASH ${totalAmount}", col2X, y, paint)

    // Right Column - Final Calculation
    y = 400f
    canvas.drawText("SUB TOTAL: ${totalAmount}", col3X, y, paint)
    y += gapY
    canvas.drawText("GST @3%: 0.00", col3X, y, paint)
    y += gapY
    canvas.drawText("Discount: 39", col3X, y, paint)
    y += gapY
    canvas.drawText("Card Charges: 0.00", col3X, y, paint)
    y += gapY
    canvas.drawText("Total Amt: ${totalAmount - 39}", col3X, y, paint)
    y += gapY
    canvas.drawText("Old Exchange: 0.00", col3X, y, paint)
    y += gapY
    canvas.drawText("R. off: 0.26", col3X, y, paint)
    y += gapY
    canvas.drawText("Net Amount: ${totalAmount}", col3X, y, paint)

    // PAID Stamp (place in summary area, right column)
    val paidCircleX = col3X + 80f
    val paidCircleY = 400f + 4 * gapY + 10f
    canvas.drawCircle(paidCircleX, paidCircleY, 18f, Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    })
    boldPaint.textSize = 12f
    canvas.drawText("PAID", paidCircleX - 16f, paidCircleY + 5f, boldPaint)

    // Footer
    y = 800f
    paint.textSize = 12f
    canvas.drawText("Thank You.. Please Visit Again.", startX + 120f, y, paint)
    canvas.drawText("For R.K. JEWELLERS", startX + 370f, y, paint)

    // Signatures
    y = 780f
    canvas.drawBitmap(customerSign.asAndroidBitmap(), startX, y, paint)
    canvas.drawText("Customer Signature", startX, y + 60f, paint)

    canvas.drawBitmap(ownerSign.asAndroidBitmap(), startX + 30f, y, paint)
    canvas.drawText("Authorised Signatory", startX + 300f, y + 60f, paint)

    pdfDocument.finishPage(page)

    val fileName = "draft_invoice_${System.currentTimeMillis()}.pdf"
    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(
            MediaStore.Downloads.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS + "/JewelVault/DraftInvoice"
        )
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    if (uri != null) {
        var success = false
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
                success = true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("PDF_GENERATION", "Error writing PDF: ${e.message}")
        } finally {
            pdfDocument.close()
        }

        if (success) {
            onFileReady(uri)
        } else {
            resolver.delete(uri, null, null)
            Log.e("PDF_GENERATION", "Failed to write PDF file")
        }
    } else {
        Log.e("PDF_GENERATION", "Failed to create file in MediaStore")
        pdfDocument.close()
    }
}
