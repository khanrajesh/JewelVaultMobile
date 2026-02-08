package com.velox.jewelvault.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.ExchangeItemDto
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.ui.components.PaymentInfo
import com.velox.jewelvault.ui.components.baseBackground8
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// PdfUtils.kt

object PdfUtils {

    val A4_PORTRAIT = Pair(545, 842)
    val A4_HALF_PORTRAIT = Pair(545, 421)

}

@Composable
fun PdfRendererPreview(uri: Uri, modifier: Modifier = Modifier, highQuality: Boolean = false) {
    val context = LocalContext.current
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    // Use higher resolution for better quality
    val scaleFactor = if (highQuality) 3 else 1 // 3x resolution for dialog
    val a4Width = (PdfUtils.A4_PORTRAIT.first * scaleFactor).toInt()  // 210mm in points
    val a4Height = (PdfUtils.A4_PORTRAIT.second * scaleFactor).toInt() // 148.5mm in points

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
                .baseBackground8()
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

@Suppress("RemoveSingleExpressionStringTemplate")
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


@Suppress("RemoveSingleExpressionStringTemplate")
fun createDraftInvoiceData(
    appDatabase: AppDatabase,
    dataStoreManager: DataStoreManager,
    store: StoreEntity,
    customer: CustomerEntity,
    items: List<ItemSelectedModel>,
    metalRates: List<MetalRate>,
    paymentInfo: MutableState<PaymentInfo?>,
    customerSign: MutableState<ImageBitmap?>,
    ownerSign: MutableState<ImageBitmap?>,
    gstLabel: String,
    discount: String,
    cardCharges: String,
    oldExchange: String,
    roundOff: String = "0.00",
    invoiceNo: String,

): InvoiceData {

    val tag = "createDraftInvoiceData"
    val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    // Convert ItemSelectedModel to DraftItemModel
    val draftItems = items.mapIndexed { index, item ->
        val price = item.price+item.chargeAmount
        InvoiceData.DraftItemModel(
            serialNumber = "${index + 1}",
            productDescription = "${item.catName} ${item.subCatName} ${item.itemAddName}",
            quantitySet = "${item.quantity}",
            grossWeightGms = item.gsWt.to3FString(),
            netWeightGms = item.ntWt.to3FString(),
            ratePerGm = item.fnMetalPrice.to3FString(),
            makingAmount = item.chargeAmount.to3FString(),
            purityPercent = item.purity,
            totalAmount = price.to3FString(),
            metalType = item.catName,
            metalPrice = (item.fnWt*item.fnMetalPrice).to3FString(),
        )
    }

    // Calculate totals using CalculationUtils
    val summary = CalculationUtils.summaryTotals(items, exchangeItemList = SnapshotStateList<ExchangeItemDto>(), 0.0)
    val subTotalAmount = summary.totalPriceBeforeTax
    val totalTaxAmount = summary.totalTax
    val itemGrandTotalAmount = summary.grandTotal

    // Get actual metal rates from passed parameter
    // Log available metal rates for debugging
    if (metalRates.isNotEmpty()) {
        log(
            "Available metal rates: ${metalRates.joinToString { "${it.metal} (${it.caratOrPurity}): ${it.price}" }}",
            tag
        )
    }

    // Use CalculationUtils for metal rate validation
    val goldRate =
        CalculationUtils.metalUnitPrice("Gold", metalRates)?.let { "₹${it.to3FString()}/gm" }
            ?: "NA"
    val silverRate =
        CalculationUtils.metalUnitPrice("Silver", metalRates)?.let { "₹${it.to3FString()}/gm" }
            ?: "NA"

    log("Selected gold rate: $goldRate, silver rate: $silverRate", tag)

    // Get payment type info from paymentInfo
    val paymentMethod = paymentInfo.value?.paymentMethod ?: "Cash"
    val isPaidInFull = paymentInfo.value?.isPaidInFull ?: true
    val paidAmount = paymentInfo.value?.paidAmount ?: itemGrandTotalAmount
    val outstandingAmount = paymentInfo.value?.outstandingAmount ?: 0.0

    //todo
    val netAmountPayable = itemGrandTotalAmount- (discount.toDoubleOrNull() ?:0.0)+(cardCharges.toDoubleOrNull() ?:0.0)-(oldExchange.toDoubleOrNull() ?:0.0)

    val currentLoginUserName = dataStoreManager.getCurrentLoginUser().let {
        if (it.role.lowercase()=="admin"){
            store.proprietor
        }else{
            it.name
        }
    }



    return InvoiceData(
        storeInfo = store,
        customerInfo = customer,
        invoiceMeta = InvoiceData.InvoiceMetadata(
            invoiceNumber = invoiceNo,
            date = currentDate,
            time = currentTime,
            salesMan = "$currentLoginUserName",
            documentType = "INVOICE"
        ),
        items = draftItems,
        jurisdiction = "Malkangiri",
        goldRate = goldRate,
        silverRate = silverRate,
        paymentSummary = InvoiceData.PaymentSummary(
            subTotal = "₹${subTotalAmount.to3FString()}",
            gstAmount = "₹${totalTaxAmount.to3FString()}",
            gstLabel = gstLabel,
            discount = discount,
            cardCharges = cardCharges,
            totalAmountBeforeOldExchange = "${itemGrandTotalAmount.to3FString()}",
            oldExchange = oldExchange,
            roundOff = roundOff,
            netAmountPayable = "₹${netAmountPayable.to3FString()}",
            amountInWords = "Indian Rupee ${numberToWords(netAmountPayable.roundToInt())} Only"
        ),
        paymentReceived = InvoiceData.PaymentReceivedDetails(
            cashLabel1 = paymentMethod.uppercase(),
            cashAmount1 = "${paidAmount.to3FString()}"
        ),
        declarationPoints = listOf(
            "We declare that this invoice shows the actual price of the goods described.",
            "All disputes are subject to the courts at the store's location.",
            "This is a draft invoice for estimation purposes.",
            if (!isPaidInFull) "Outstanding Amount: ₹${outstandingAmount.to3FString()}" else "",
            "Payment Method: ${paymentMethod.uppercase()}",
            if (paymentInfo.value?.notes?.isNotEmpty() == true) "Notes: ${paymentInfo.value?.notes}" else "",

        ).filter { it.isNotEmpty() },
        termsAndConditions = "Terms and conditions apply as per company policy.",
        customerSignature = customerSign.value,
        ownerSignature = ownerSign.value,
        thankYouMessage = "Thank You! Please Visit Again"
    )
}

@Suppress("RemoveSingleExpressionStringTemplate")
fun generateInvoicePdf(
    context: Context,
    data: InvoiceData,
    scale: Float = 1f,
    onFileReady: (Uri) -> Unit
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(
        (PdfUtils.A4_PORTRAIT.first * scale).toInt(),
        (PdfUtils.A4_PORTRAIT.second * scale).toInt(),
        1
    ).create()

    val s2f = 2f * scale
    val s3f = 3f * scale
    val s5f = 5f * scale
    val s7f = 7f * scale
    val s8f = 8f * scale
    val s9f = 9f * scale
    val s10f = 10f * scale
    val s15f = 15f * scale
    val s20f = 20f * scale
    val s30f = 30f * scale
    val s50f = 50f * scale
    val s80f = 80f * scale
    val s100f = 100f * scale
    val s200f = 200f * scale

    val startX = s15f
    val startY = s15f
    val endX = pageInfo.pageWidth - s15f
    val endY = (pageInfo.pageHeight/2) - s15f

    val page = pdfDocument.startPage(pageInfo)

    val canvas = page.canvas

    val paint = Paint().apply {
        color = Color.BLACK
        textSize = 10f
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
    canvas.apply {

        val headingStartX = startX
        val headingStartY = startY
        val headingEndX = endX
        val headingEndY = startY + s100f - s15f
        
        // Calculate heading area dimensions
        val headingWidth = headingEndX - headingStartX
        val headingHeight = 80f // Fixed height for header section
        val headingCenterX = headingStartX + (headingWidth / 2)

        // Create paint objects for different text styles
        val storeNamePaint = Paint().apply {
            color = Color.BLACK
            textSize = 54f // Increased by 6 more from 38f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        
        val storeAddressPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f // Increased by 4 more from 16f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        
        val contactInfoPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f // Increased by 4 from 10f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }
        
        val rightAlignPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f // Increased by 4 from 10f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.RIGHT
        }
        
        // Draw store name centered
        val storeNameY = headingStartY + 35f // Increased spacing for larger text
        canvas.drawText(
            data.storeInfo.name ?: "Store Name",
            headingCenterX,
            storeNameY,
            storeNamePaint
        )
        
        // Draw store address centered below name
        val addressY = storeNameY + 30f // Increased spacing for larger text
        val addressLines = (data.storeInfo.address ?: "Store Address").split('\n')
        addressLines.forEachIndexed { index, line ->
            canvas.drawText(
                line.trim(),
                headingCenterX,
                addressY + (index * 22f), // Increased line spacing for larger text
                storeAddressPaint
            )
        }

        
        // Draw contact information on the left side
        val leftInfoX = headingStartX + 15f
        val leftInfoY = addressY + (addressLines.size * 22f) + 25f // Increased spacing for larger address text
        
        canvas.drawText(
            "Proprietor: ${data.storeInfo.proprietor ?: "N/A"}",
            leftInfoX,
            leftInfoY,
            contactInfoPaint
        )
        
        canvas.drawText(
            "Phone: ${data.storeInfo.phone ?: "N/A"}",
            leftInfoX,
            leftInfoY + 18f, // Increased line spacing
            contactInfoPaint
        )
        
        canvas.drawText(
            "Email: ${data.storeInfo.email ?: "N/A"}",
            leftInfoX,
            leftInfoY + 36f, // Increased line spacing
            contactInfoPaint
        )
        
        // Draw registration details on the right side
        val rightInfoX = headingEndX - 15f
        val rightInfoY = leftInfoY
        
        canvas.drawText(
            "Reg. No: ${data.storeInfo.registrationNo ?: "N/A"}",
            rightInfoX,
            rightInfoY,
            rightAlignPaint
        )
        
        canvas.drawText(
            "GSTIN: ${data.storeInfo.gstinNo ?: "N/A"}",
            rightInfoX,
            rightInfoY + 18f, // Increased line spacing
            rightAlignPaint
        )
        
        canvas.drawText(
            "PAN: ${data.storeInfo.panNo ?: "N/A"}",
            rightInfoX,
            rightInfoY + 36f, // Increased line spacing
            rightAlignPaint
        )
        
        // Draw a horizontal line below the header
        val lineY = leftInfoY + 50f // Increased spacing for larger text
        canvas.drawLine(
            headingStartX + 5f,
            lineY,
            headingEndX - 5f,
            lineY,
            Paint().apply {
                color = Color.BLACK
                strokeWidth = 1f
            }
        )






        val outerBoxPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        drawRect(startX, startY + s100f - s15f, endX, endY, outerBoxPaint)

        val innerBoxPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val nameTextStartX = startX + s5f

        val nameText1Y = startY + s100f - s5f
        drawText(
            "Name: ${data.customerInfo.name} (${data.customerInfo.mobileNo})",
            nameTextStartX,
            nameText1Y,
            tp16Bold
        )

        val textEndX = endX - s5f
        val dateTextStartX = textEndX - s80f
        drawText("Date: ${data.invoiceMeta.date}", dateTextStartX, nameText1Y, tp16Bold)

        val invoiceValueStartX = dateTextStartX - s50f + s5f
        drawText("${data.invoiceMeta.invoiceNumber}", invoiceValueStartX, nameText1Y, tp16Bold)

        val invoiceKeyStartX = invoiceValueStartX - s30f - s10f
        drawText("Invoice No:", invoiceKeyStartX, nameText1Y, tp16)

        val issueTimeKeyStartY = nameText1Y + s10f
        drawText("Issue Time:", invoiceKeyStartX, issueTimeKeyStartY, tp16)
        drawText("${data.invoiceMeta.time}", invoiceValueStartX, issueTimeKeyStartY, tp16Bold)

        drawText("Address: ${data.customerInfo.address}", nameTextStartX, issueTimeKeyStartY, tp16)

        val saleManeKeyStartY = issueTimeKeyStartY + s10f
        drawText("Sales Man:", invoiceKeyStartX, saleManeKeyStartY, tp16)
        drawText("${data.invoiceMeta.salesMan}", invoiceValueStartX, saleManeKeyStartY, tp16Bold)

        // item box section with column lines and headings

        val itemBoxTop = saleManeKeyStartY + s5f
        val itemBoxBottom = endY - s100f - s30f
        val itemBoxLeft = startX
        val itemBoxRight = endX
        // Draw the outer item box
        drawRect(itemBoxLeft, itemBoxTop, itemBoxRight, itemBoxBottom, innerBoxPaint)
        // Calculate total available width for columns
        val totalWidth = itemBoxRight - itemBoxLeft
        // Define relative proportions for each column (should sum to 1.0)
        val columnProportions = listOf(
            0.03f,  // Sr
            0.18f,  // Product Description
            0.07f,  // Qty/Set
            0.08f,  // Gross.Wt in GMS
            0.08f,  // Net.Wt in GMS
            0.10f,  // Rate/Gm
            0.12f,  // Metal Price
            0.12f,  // Making Amount
            0.07f,  // Purity %
            0.14f   // Total Amount (reduced, rest is right margin)
        )
        // Calculate actual widths
        val columns = listOf(
            "Sr",
            "Product Description",
            "Qty/Set",
            "Gross.Wt\nin GMS",
            "Net.Wt\nin GMS",
            "Rate/Gm",
            "Metal\nPrice",
            "Making\nAmount",
            "Purity %",
            "Total Amount"
        ).zip(columnProportions.map { it * totalWidth })
        // Calculate x positions for columns
        val columnX = mutableListOf(itemBoxLeft)
        for ((_, width) in columns) {
            columnX.add(columnX.last() + width)
        }
        // Draw vertical lines for columns
        for (i in 1 until columnX.size - 1) {
            drawLine(columnX[i], itemBoxTop, columnX[i], itemBoxBottom, innerBoxPaint)
        }
        // Draw headings (centered in each column, handle 2-line headings)
        val headingY = itemBoxTop + s8f
        val headingLineSpacing = s10f // increased for header only
        for (i in columns.indices) {
            val (heading, width) = columns[i]
            val lines = heading.split("\n")
            val xCenter = columnX[i] + width / 2
            if (lines.size == 2) {
                val text1Width = tp16Bold.measureText(lines[0])
                val text2Width = tp16Bold.measureText(lines[1])
                drawText(lines[0], xCenter - text1Width / 2, headingY, tp16Bold)
                drawText(
                    lines[1],
                    xCenter - text2Width / 2,
                    headingY + headingLineSpacing,
                    tp16Bold
                )
            } else {
                val textWidth = tp16Bold.measureText(heading)
                drawText(
                    heading,
                    xCenter - textWidth / 2,
                    headingY + headingLineSpacing / 2,
                    tp16Bold
                )
            }
        }
        // Draw horizontal line under headings (increased by s5f)
        val headingBottomY = itemBoxTop + s15f + s5f
        drawLine(itemBoxLeft, headingBottomY, itemBoxRight, headingBottomY, innerBoxPaint)
        // Add 2 dummy item rows
        val rowHeight = s15f

        val itemRows = data.items.mapIndexed { index, entity ->
            listOf(
                "${index + 1}",
                entity.productDescription,
                entity.quantitySet,
                entity.grossWeightGms,
                entity.netWeightGms,
                entity.ratePerGm,
                entity.metalPrice,
                entity.makingAmount,
                entity.purityPercent,
                entity.totalAmount
            )
        }


        for (rowIndex in itemRows.indices) {
            val y = headingBottomY + rowHeight * (rowIndex + 1)
            val row = itemRows[rowIndex]
            for (colIndex in row.indices) {
                val (heading, width) = columns[colIndex]
                val value = row[colIndex]
                val textWidth = tp16.measureText(value)
                val xCenter = columnX[colIndex] + width / 2
                // For the last column, add a right margin (s8f)
                if (colIndex == columns.size - 1) {
                    drawText(
                        value,
                        minOf(xCenter - textWidth / 2, itemBoxRight - s8f - textWidth),
                        y - s3f,
                        tp16
                    )
                } else {
                    drawText(value, xCenter - textWidth / 2, y - s3f, tp16)
                }
            }
            // Draw horizontal line under each row
            drawLine(itemBoxLeft, y, itemBoxRight, y, innerBoxPaint)
        }


        // Top small box: Headings in bold
        val metalBoxTop = itemBoxBottom
        val metalBoxBottom = itemBoxBottom + s15f
        val metalBoxRight = startX + s200f + s15f
        drawRect(startX, metalBoxTop, metalBoxRight, metalBoxBottom, innerBoxPaint)

        // Headings for the top small box
        val headings = listOf("Pcs", "Weight", "Total Val", "Tot-Mc", "Total Amount")
        val headingColumnWidth = (metalBoxRight - startX) / headings.size
        for (i in headings.indices) {
            val heading = headings[i]
            val textWidth = tp16Bold.measureText(heading)
            val x = s5f + i * headingColumnWidth + (headingColumnWidth - textWidth) / 2
            val y = metalBoxTop + s8f // vertical centering in the box
            drawText(heading, x, y, tp16Bold)
        }

        // Gold box
        val goldBoxTop = metalBoxBottom
        val goldBoxBottom = goldBoxTop + s20f + s2f
        drawRect(startX, goldBoxTop, metalBoxRight, goldBoxBottom, innerBoxPaint)


        val goldLabelX = startX + s5f
        val goldLabelY = goldBoxTop + s7f
        val goldLabel = "Gold Details"
        val goldLabelWidth = tp16Bold.measureText(goldLabel)

        drawText(goldLabel, goldLabelX, goldLabelY, tp16)
        drawLine(
            goldLabelX,
            goldLabelY + s2f,
            goldLabelX + goldLabelWidth,
            goldLabelY + s2f,
            innerBoxPaint
        )

        // Calculate gold items totals
        val goldItems = data.items.filter { it.metalType.trim().lowercase() == "gold" }

        val goldPcs = goldItems.size
        val goldWeight = goldItems.sumOf { it.grossWeightGms.toDoubleOrNull() ?: 0.0}
        val goldTotalVal = goldItems.sumOf { it.totalAmount.toDoubleOrNull() ?: 0.0} //todo mistake
        val goldTotMc = goldItems.sumOf { it.makingAmount.toDoubleOrNull() ?: 0.0 }
        val goldTotalAmount = goldTotalVal+ goldTotMc

        // Display gold data
        val goldData = listOf(
            goldPcs.toString(),
            goldWeight.to3FString(),
            goldTotalVal.to3FString(),
            goldTotMc.to3FString(),
            goldTotalAmount.to3FString()
        )

        for (i in headings.indices) {
            val value = goldData[i]
            val textWidth = tp16.measureText(value)
            val x = s5f + i * headingColumnWidth + (headingColumnWidth - textWidth) / 2
            val y = goldLabelY + s10f + s2f
            drawText(value, x, y, tp16)
        }


        // Silver box
        val silverBoxTop = goldBoxBottom
        val silverBoxBottom = silverBoxTop + s20f + s2f
        drawRect(startX, silverBoxTop, metalBoxRight, silverBoxBottom, innerBoxPaint)


        val silverLabelX = startX + s5f
        val silverLabelY = goldBoxBottom + s7f
        val silverLabel = "Silver Details"
        val silverLabelWidth = tp16Bold.measureText(silverLabel)

        drawText(silverLabel, silverLabelX, silverLabelY, tp16)
        drawLine(
            silverLabelX,
            silverLabelY + s2f,
            silverLabelX + silverLabelWidth,
            silverLabelY + s2f,
            innerBoxPaint
        )

        // Calculate silver items totals
        val silverItems = data.items.filter { it.metalType.trim().lowercase() == "silver" }

        val silverPcs = silverItems.size
        val silverWeight = silverItems.sumOf { it.grossWeightGms.toDoubleOrNull() ?: 0.0}
        val silverTotalVal = silverItems.sumOf { it.totalAmount.toDoubleOrNull() ?: 0.0} //todo mistake
        val silverTotMc = silverItems.sumOf { it.makingAmount.toDoubleOrNull() ?: 0.0 }
        val silverTotalAmount =silverTotalVal+ silverTotMc

        // Display silver data
        val silverData = listOf(
            silverPcs.toString(),
            silverWeight.to3FString(),
            silverTotalVal.to3FString(),
            silverTotMc.to3FString(),
            silverTotalAmount.to3FString()
        )

        for (i in headings.indices) {
            val value = silverData[i]
            val textWidth = tp16.measureText(value)
            val x = s5f + i * headingColumnWidth + (headingColumnWidth - textWidth) / 2
            val y = silverLabelY + s10f + s2f
            drawText(value, x, y, tp16)
        }

        // After silver box and before metal rate section
        // 1. Amount in words (dummy)
        val amountInWordsY = silverBoxBottom + s9f
        drawText(
            "Amount in words: ${data.paymentSummary.amountInWords}",
            startX + s2f,
            amountInWordsY,
            tp16
        )

        // 2. Declaration section
        val declarationTitle = "Declaration"
        val declarationY = amountInWordsY + s10f + s2f
        drawText(declarationTitle, startX + s2f, declarationY, tp16Bold)
        // Underline for Declaration
        val declarationTitleWidth = tp16Bold.measureText(declarationTitle)
        drawLine(
            startX,
            declarationY + s2f,
            startX + declarationTitleWidth,
            declarationY + s2f,
            innerBoxPaint
        )
        // Declaration points
        var currentDeclarationY = declarationY + s10f + s2f
        data.declarationPoints.forEach { declarationPoint ->
            drawText(declarationPoint, startX + s2f, currentDeclarationY, tp16)
            currentDeclarationY += s10f + s2f
        }

        // metal rate

        drawText("Current Rate", metalBoxRight + s2f, metalBoxTop + s9f, tp16)
        // Underline for 'Current Rate'
        val currentRateText = "Current Rate"
        val currentRateX = metalBoxRight + s2f
        val currentRateY = metalBoxTop + s9f
        val currentRateWidth = tp16.measureText(currentRateText)
        drawLine(
            currentRateX,
            currentRateY + s2f,
            currentRateX + currentRateWidth,
            currentRateY + s2f,
            innerBoxPaint
        )

        drawText("Gold @ Rs", metalBoxRight + s2f, metalBoxTop + s9f + s9f + s2f, tp14)
        drawText(data.goldRate, metalBoxRight + s2f + s50f, metalBoxTop + s9f + s9f + s2f, tp14)

        drawText("Silver @ Rs", metalBoxRight + s2f, metalBoxTop + s9f + s9f + s9f + s2f, tp14)
        drawText(
            data.silverRate,
            metalBoxRight + s2f + s50f,
            metalBoxTop + s9f + s9f + s9f + s2f,
            tp14
        )

        drawText(
            "Payment Details",
            metalBoxRight + s2f,
            metalBoxTop + s9f + s9f + s9f + s9f + s2f,
            tp16
        )
        // Underline for 'Payment Details'
        val paymentDetailsText = "Payment Details"
        val paymentDetailsX = metalBoxRight + s2f
        val paymentDetailsY = metalBoxTop + s9f + s9f + s9f + s9f
        val paymentDetailsWidth = tp16.measureText(paymentDetailsText)
        drawLine(
            paymentDetailsX,
            paymentDetailsY + s2f + s2f,
            paymentDetailsX + paymentDetailsWidth,
            paymentDetailsY + s2f + s2f,
            innerBoxPaint
        )


        val cashText1 = data.paymentReceived.cashLabel1
        val cashText1X = metalBoxRight + s2f
        val cashText1Y = metalBoxTop + s9f + s9f + s9f + s9f + s9f + s5f
        drawText(cashText1, cashText1X, cashText1Y, tp14)
        drawText(
            "${data.paymentReceived.cashAmount1}",
            metalBoxRight + s2f + s50f,
            cashText1Y,
            tp14
        )


        // Draw "Thank You Please Visit Again" in cursive, centered above the outer box bottom line
        val thankYouText = data.thankYouMessage
        val thankYouPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        // Center X between startX and endX
        val centerX = (startX + endX) / 2f
        // Place just above the outer box bottom line (endY)
        val thankYouY = endY - s5f
//        canvas.drawText(thankYouText, centerX, thankYouY, thankYouPaint)


        val amountBoxStartX = endX - s200f
        val amountBoxEndX = endX
        val amountBoxTop = itemBoxBottom
        val amountBoxBottomY = silverBoxBottom + s30f
        drawRect(amountBoxStartX, amountBoxTop, amountBoxEndX, amountBoxBottomY, innerBoxPaint)

        val amountPairs = listOf(
            "SUB TOTAL" to "₹ ${data.paymentSummary.subTotal}",
            "Old Exchange" to "₹ ${data.paymentSummary.oldExchange}",
            "Discount" to "₹ ${data.paymentSummary.discount}",
            "${data.paymentSummary.gstLabel}" to "₹ ${data.paymentSummary.gstAmount}",
            "Total Amt." to "₹ ${data.paymentSummary.totalAmountBeforeOldExchange}",
            "Card Charges" to "₹ ${data.paymentSummary.cardCharges}",
            "R. Off" to "₹ ${data.paymentSummary.roundOff}",
            "Net Amount" to "₹ ${data.paymentSummary.netAmountPayable}"
        )

        val rowCount = amountPairs.size
        val rowHeight1 = (amountBoxBottomY - amountBoxTop) / rowCount

        for (i in amountPairs.indices) {
            val (key, value) = amountPairs[i]
            val y = amountBoxTop + (i + 0.7f) * rowHeight1
            drawText(key, amountBoxStartX + s8f, y, tp16)
            val valueTextWidth = tp16Bold.measureText(value)
            val valueX = amountBoxEndX - s8f - valueTextWidth
            drawText(value, valueX, y, tp16Bold)
        }

        // Signature section
        val signatureWidth = s80f
        val signatureHeight = s50f
        val signatureY = endY - s20f - s5f

        // Customer signature
        val customerSignX = endX - s200f - s50f
        data.customerSignature?.let { customerSign ->
            val customerBitmap = customerSign.asAndroidBitmap()
            val customerSrcRect =
                android.graphics.Rect(0, 0, customerBitmap.width, customerBitmap.height)
            val customerDestRect = android.graphics.Rect(
                customerSignX.toInt(),
                signatureY.toInt(),
                (customerSignX + signatureWidth).toInt(),
                (signatureY + signatureHeight).toInt()
            )
            drawBitmap(customerBitmap, customerSrcRect, customerDestRect, null)
        }
        drawText("For Customer", customerSignX, endY - s5f, tp16)

        // Owner signature  
        val ownerSignX = endX - s100f - s10f
        data.ownerSignature?.let { ownerSign ->
            val ownerBitmap = ownerSign.asAndroidBitmap()
            val ownerSrcRect = android.graphics.Rect(0, 0, ownerBitmap.width, ownerBitmap.height)
            val ownerDestRect = android.graphics.Rect(
                ownerSignX.toInt(),
                signatureY.toInt(),
                (ownerSignX + signatureWidth).toInt(),
                (signatureY + signatureHeight).toInt()
            )
            drawBitmap(ownerBitmap, ownerSrcRect, ownerDestRect, null)
        }
        drawText("For ${data.storeInfo.name ?: "STORE"}", ownerSignX, endY - s5f, tp16)
    }


    //--------------------------------------------------------- PDF DESIGN END ---------------------------------------------------------//
    pdfDocument.finishPage(page)

    val fileName = "${data.customerInfo.name}_${data.customerInfo.mobileNo}_${System.currentTimeMillis()}.pdf"
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


/*@Suppress("RemoveSingleExpressionStringTemplate")
fun generateTaxInvoicePdf(
    context: Context,
    store: StoreEntity,
    customer: CustomerEntity,
    items: List<ItemSelectedModel>,
    onFileReady: (Uri) -> Unit
) {
    val pdfDocument = PdfDocument()
    // A4 size: 595 x 842 points (8.27 x 11.69 inches)
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    // White background
    canvas.drawColor(Color.WHITE)

    // Define paint for borders and lines
    val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    val thickBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Text paints with proper sizing
    val paint = Paint().apply {
        color = Color.BLACK
        textSize = 8f
    }
    val boldPaint = Paint(paint).apply {
        isFakeBoldText = true
        textSize = 9f
    }
    val titlePaint = Paint(paint).apply {
        textSize = 14f
        isFakeBoldText = true
        color = Color.BLACK
    }
    val headerPaint = Paint(paint).apply {
        textSize = 10f
        isFakeBoldText = true
        color = Color.BLACK
    }

    // Page dimensions and layout
    val pageWidth = 595f
    val pageHeight = 842f
    val margin = 25f
    val contentWidth = pageWidth - (2 * margin)

    // Main outer border
    canvas.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, thickBorderPaint)

    // Define section positions and sizes
    val headerStartY = margin + 20f
    val headerHeight = 120f
    val customerStartY = headerStartY + headerHeight + 15f
    val customerHeight = 90f
    val tableStartY = customerStartY + customerHeight + 15f
    val tableHeight = 30f + (items.size * 20f) + 20f // header + data rows + total row
    val summaryStartY = tableStartY + tableHeight + 15f
    val summaryHeight = 120f
    val termsStartY = summaryStartY + summaryHeight + 15f
    val termsHeight = 200f
    val certStartY = termsStartY + termsHeight + 15f
    val certHeight = 60f

    // Company section width (left side of header)
    val companySectionWidth = 350f
    val invoiceSectionWidth = contentWidth - companySectionWidth

    // Summary section width (right side)
    val summaryWidth = 200f
    val termsWidth = contentWidth - summaryWidth - 10f

    // Table column widths
    val colWidths = listOf(35f, 120f, 60f, 60f, 50f, 70f, 50f, 60f, 50f, 60f) // 10 columns total
    val tableWidth = colWidths.sum()

    // Draw all borders first
    // Header section
    canvas.drawRect(
        margin,
        headerStartY,
        pageWidth - margin,
        headerStartY + headerHeight,
        borderPaint
    )
    canvas.drawRect(
        margin,
        headerStartY,
        margin + companySectionWidth,
        headerStartY + headerHeight,
        borderPaint
    )
    canvas.drawRect(
        margin + companySectionWidth,
        headerStartY,
        pageWidth - margin,
        headerStartY + headerHeight,
        borderPaint
    )

    // Customer section
    canvas.drawRect(
        margin,
        customerStartY,
        pageWidth - margin,
        customerStartY + customerHeight,
        borderPaint
    )

    // Table section
    canvas.drawRect(margin, tableStartY, pageWidth - margin, tableStartY + tableHeight, borderPaint)

    // Summary section
    val summaryX = pageWidth - margin - summaryWidth
    canvas.drawRect(
        summaryX,
        summaryStartY,
        pageWidth - margin,
        summaryStartY + summaryHeight,
        borderPaint
    )

    // Terms section
    canvas.drawRect(
        margin,
        termsStartY,
        margin + termsWidth,
        termsStartY + termsHeight,
        borderPaint
    )

    // Certification section
    canvas.drawRect(margin, certStartY, pageWidth - margin, certStartY + certHeight, borderPaint)
    canvas.drawRect(summaryX, certStartY, pageWidth - margin, certStartY + certHeight, borderPaint)

    // Table column borders
    var currentX = margin
    colWidths.forEach { width ->
        currentX += width
        canvas.drawLine(currentX, tableStartY, currentX, tableStartY + tableHeight, borderPaint)
    }

    // Table row borders
    val headerRowY = tableStartY + 30f
    canvas.drawLine(margin, headerRowY, pageWidth - margin, headerRowY, borderPaint)

    items.forEachIndexed { index, _ ->
        val rowY = headerRowY + (index + 1) * 20f
        canvas.drawLine(margin, rowY, pageWidth - margin, rowY, borderPaint)
    }

    val totalRowY = headerRowY + (items.size + 1) * 20f
    canvas.drawLine(margin, totalRowY, pageWidth - margin, totalRowY, borderPaint)

    // Table header background
    val headerBgPaint = Paint().apply {
        color = Color.rgb(245, 245, 245)
        style = Paint.Style.FILL
    }
    canvas.drawRect(margin, tableStartY, pageWidth - margin, headerRowY, headerBgPaint)

    // Now draw all text content with proper positioning
    var y = headerStartY + 15f
    val startX = margin + 10f
    val gapY = 12f

    // Header - DUPLICATE COPY (centered at top)
    titlePaint.textSize = 14f
    val duplicateText = "DUPLICATE COPY"
    val duplicateTextWidth = titlePaint.measureText(duplicateText)
    canvas.drawText(duplicateText, (pageWidth - duplicateTextWidth) / 2, y, titlePaint)
    y += gapY + 5f

    // Company Name and Address (left side of header)
    headerPaint.textSize = 12f
    canvas.drawText(store.name ?: "Raj jewellers", startX, y, headerPaint)
    y += gapY
    paint.textSize = 9f
    canvas.drawText(
        store.address ?: "Old medical road, Malkangiri, Odisha - 764048",
        startX,
        y,
        paint
    )
    y += gapY
    canvas.drawText("GSTIN: ${store.gstinNo ?: "21APEPK7976C1ZZ"}", startX, y, paint)
    y += gapY

    // Contact Person Details
    canvas.drawText(store.proprietor ?: "Ranjan khan", startX, y, paint)
    y += gapY
    canvas.drawText("Phone: ${store.phone ?: "9437206994"}", startX, y, paint)
    y += gapY
    canvas.drawText("Email: ${store.email ?: "Khanranjan45@gmail.com"}", startX, y, paint)
    y += gapY + 5f

    // Invoice Details (Right side of header)
    val rightX = margin + companySectionWidth + 10f
    y -= 60f
    headerPaint.textSize = 11f
    canvas.drawText("Invoice No.: ${store.invoiceNo ?: "292"}", rightX, y, headerPaint)
    y += gapY
    canvas.drawText(
        "Invoice Date: ${
            SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(
                Date()
            )
        }", rightX, y, paint
    )
    y += gapY + 10f

    // Customer Details Section
    y = customerStartY + 15f
    headerPaint.textSize = 11f
    canvas.drawText("Customer Detail", startX, y, headerPaint)
    y += gapY
    paint.textSize = 9f
    canvas.drawText("Name: ${customer.name ?: "Customer Name"}", startX, y, paint)
    y += gapY
    canvas.drawText("Address: ${customer.address ?: "Customer Address"}", startX, y, paint)
    y += gapY
    canvas.drawText("Phone: ${customer.mobileNo ?: "N/A"}", startX, y, paint)
    y += gapY
    canvas.drawText("GSTIN: ${customer.gstin_pan ?: ""}", startX, y, paint)
    y += gapY
    canvas.drawText("Place of Supply: Odisha (21)", startX, y, paint)
    y += gapY + 10f

    // Table Header with proper column positioning
    y = tableStartY + 15f
    headerPaint.textSize = 9f

    // Calculate column positions
    var colX = margin + 5f
    val headerY = tableStartY + 15f
    val subHeaderY = tableStartY + 30f

    // Main headers
    canvas.drawText("Sr. No.", colX, headerY, headerPaint)
    colX += colWidths[0]
    canvas.drawText("Name of Product/Service", colX, headerY, headerPaint)
    colX += colWidths[1]
    canvas.drawText("HSN/SAC", colX, headerY, headerPaint)
    colX += colWidths[2]
    canvas.drawText("Qty", colX, headerY, headerPaint)
    colX += colWidths[3]
    canvas.drawText("Rate", colX, headerY, headerPaint)
    colX += colWidths[4]
    canvas.drawText("Taxable Value", colX, headerY, headerPaint)
    colX += colWidths[5]
    canvas.drawText("CGST", colX, headerY, headerPaint)
    colX += colWidths[6]
    canvas.drawText("SGST", colX, headerY, headerPaint)

    // CGST and SGST sub-headers
    paint.textSize = 7f
    colX =
        margin + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] + colWidths[4] + colWidths[5] + 5f
    canvas.drawText("%", colX, subHeaderY, paint)
    colX += colWidths[6]
    canvas.drawText("Amount", colX, subHeaderY, paint)
    colX += colWidths[7]
    canvas.drawText("%", colX, subHeaderY, paint)
    colX += colWidths[8]
    canvas.drawText("Amount", colX, subHeaderY, paint)

    // Calculate totals
    var totalTaxableValue = 0.0
    var totalCgst = 0.0
    var totalSgst = 0.0
    var totalQuantity = 0.0

    // Table Data - Dynamic items with proper column positioning
    paint.textSize = 8f
    items.forEachIndexed { index, item ->
        val itemName =
            "${item.catName ?: ""} ${item.subCatName ?: ""} ${item.itemAddName ?: "Item"}".trim()
        val hsnCode = item.huid ?: "7113"
        val quantity = item.quantity
        val rate = item.fnMetalPrice
        val taxableValue = item.price
        val cgstPercent = 1.5 // Default CGST rate
        val sgstPercent = 1.5 // Default SGST rate
        val cgstAmount = (taxableValue * cgstPercent) / 100
        val sgstAmount = (taxableValue * sgstPercent) / 100

        val rowY = headerRowY + (index + 1) * 20f - 5f

        // Calculate column positions for this row
        colX = margin + 5f
        canvas.drawText("${index + 1}", colX, rowY, paint)
        colX += colWidths[0]
        canvas.drawText(itemName, colX, rowY, paint)
        colX += colWidths[1]
        canvas.drawText(hsnCode, colX, rowY, paint)
        colX += colWidths[2]
        canvas.drawText("${quantity} Gms", colX, rowY, paint)
        colX += colWidths[3]
        canvas.drawText("${rate}", colX, rowY, paint)
        colX += colWidths[4]
        canvas.drawText("${taxableValue.to3FString()}", colX, rowY, paint)
        colX += colWidths[5]
        canvas.drawText("${cgstPercent}", colX, rowY, paint)
        colX += colWidths[6]
        canvas.drawText("${cgstAmount.to3FString()}", colX, rowY, paint)
        colX += colWidths[7]
        canvas.drawText("${sgstPercent}", colX, rowY, paint)
        colX += colWidths[8]
        canvas.drawText("${sgstAmount.to3FString()}", colX, rowY, paint)

        totalTaxableValue += taxableValue
        totalCgst += cgstAmount
        totalSgst += sgstAmount
        totalQuantity += quantity
    }

    // Total row with proper positioning
    val totalRowY2 = headerRowY + (items.size + 1) * 20f - 5f
    headerPaint.textSize = 9f
    colX = margin + colWidths[0] + 5f
    canvas.drawText("Total", colX, totalRowY2, headerPaint)
    colX += colWidths[1]
    colX += colWidths[2]
    canvas.drawText("${totalQuantity.to3FString()} Gms", colX, totalRowY2, paint)
    colX += colWidths[3]
    colX += colWidths[4]
    canvas.drawText("${totalTaxableValue.to3FString()}", colX, totalRowY2, paint)
    colX += colWidths[5]
    colX += colWidths[6]
    canvas.drawText("${totalCgst.to3FString()}", colX, totalRowY2, paint)
    colX += colWidths[7]
    colX += colWidths[8]
    canvas.drawText("${totalSgst.to3FString()}", colX, totalRowY2, paint)

    // Calculate grand total
    val totalTax = totalCgst + totalSgst
    val grandTotal = totalTaxableValue + totalTax

    // Amount in Words
    boldPaint.textSize = 12f
    canvas.drawText("Amount in Words:", startX, y, boldPaint)
    y += gapY
    paint.textSize = 10f
    val amountInWords = "INDIAN RUPEES ${numberToWords(grandTotal.toInt())} ONLY"
    canvas.drawText(amountInWords, startX, y, paint)
    y += gapY + 10f

    // Summary of Charges (Right side)
    y = summaryStartY + 15f
    headerPaint.textSize = 11f
    canvas.drawText("Summary of Charges", summaryX + 5f, y, headerPaint)
    y += gapY
    paint.textSize = 9f
    canvas.drawText(
        "Taxable Amount: ${totalTaxableValue.to3FString()}",
        summaryX + 5f,
        y,
        paint
    )
    y += gapY
    canvas.drawText("Add: CGST: ${totalCgst.to3FString()}", summaryX + 5f, y, paint)
    y += gapY
    canvas.drawText("Add: SGST: ${totalSgst.to3FString()}", summaryX + 5f, y, paint)
    y += gapY
    canvas.drawText("Total Tax: ${totalTax.to3FString()}", summaryX + 5f, y, paint)
    y += gapY
    canvas.drawText("Discount: 0.00", summaryX + 5f, y, paint)
    y += gapY
    headerPaint.textSize = 11f
    canvas.drawText(
        "Total Amount After Tax: ₹${grandTotal.to3FString()} (E & O.E.)",
        summaryX + 5f,
        y,
        headerPaint
    )
    y += gapY + 20f

    // Terms and Conditions (Left side)
    y = termsStartY + 15f
    headerPaint.textSize = 11f
    canvas.drawText("Terms and Conditions:", startX, y, headerPaint)
    y += gapY
    paint.textSize = 7f
    val terms = listOf(
        "1. Only 1 Gram will be deducted each 10 gram for Replacement or Old exchanged & 1.5 Gram for Refund of cash other than 916 BIS (Hallmark) Ornaments.",
        "2. Used ornament will be taken back as Old exchanged Policies.",
        "3. Making Charge, Sale Taxes, Stone cost & other accessories are not refundable.",
        "4. 99% Guarantee on the quality, Carats & Weigh & subject to Standard Weight Tolerance 0.050 Gms.",
        "5. Customer are requested to Check Carefully the actual Design / Patterns / Shapes of Pairs and Similarity weight before delivery or made payments.",
        "6. Replacement or Exchange will be done within 24 hours for Local Areas & 48 hours for abroad.",
        "7. We are not responsible, the Trade Mark / Sea RAJ is broken or not found on the particular Ornaments 7 after all any documents has been produced.",
        "8. Complaints arguments will not be granted accepted without presentation of the bill or proper document, hence it may be solved by our mutual understanding.",
        "9. Problem / disputes arises during the using period of a particulars ornaments please contact us at our shown or by Dailing our Cell No: 9437206994/9437847948.",
        "10. Repair & Other Service are done in every Working Day.",
        "11. Thursday the Show Room remains closed."
    )

    terms.forEach { term ->
        canvas.drawText(term, startX, y, paint)
        y += gapY - 3f
    }
    y += gapY

    // Certification (Bottom section)
    y = certStartY + 15f
    paint.textSize = 9f
    canvas.drawText(
        "Certified that the particulars given above are true and correct.",
        startX,
        y,
        paint
    )
    y += gapY + 10f

    // Signature section (Right side of certification)
    canvas.drawText("For: ${store.name ?: "Raj jewellers"}", summaryX + 5f, y, paint)
    y += gapY
    canvas.drawText("Authorised Signatory", summaryX + 5f, y, paint)



    pdfDocument.finishPage(page)

    val fileName = "tax_invoice_${System.currentTimeMillis()}.pdf"
    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(
            MediaStore.Downloads.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS + "/JewelVault/TaxInvoice"
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
}*/

/*@Suppress("RemoveSingleExpressionStringTemplate")
fun generateEstimatePdf(
    context: Context,
    store: StoreEntity,
    customer: CustomerEntity,
    items: List<ItemSelectedModel>,
    estimateNumber: String, // Specific to Estimate
    estimateDate: String,   // Specific to Estimate (e.g., "dd-MMM-yyyy")
    onFileReady: (Uri) -> Unit
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    // White background
    canvas.drawColor(Color.WHITE)

    // --- PAINTS ---
    val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 0.5f // Thinner border for some elements if needed
    }
    val thickBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f // Standard border thickness
    }

    val regularPaint = Paint().apply {
        color = Color.BLACK
        textSize = 8f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val boldPaint = Paint(regularPaint).apply {
        isFakeBoldText = true // More compatible than setting typeface bold for some systems
        // textSize will be set as needed, inherits regularPaint's size initially
    }
    val smallBoldPaint = Paint(regularPaint).apply {
        isFakeBoldText = true
        textSize = 7f
    }
    val smallRegularPaint = Paint(regularPaint).apply {
        textSize = 7f
    }
    val titleEstimatePaint = Paint().apply {
        color = Color.rgb(0, 100, 0) // Dark Green for "ESTIMATE"
        textSize = 18f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val headerCompanyPaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val tableHeaderPaint = Paint(regularPaint).apply {
        isFakeBoldText = true
        textSize = 7.5f // Slightly smaller for table headers
        textAlign = Paint.Align.CENTER
    }
    val tableCellPaint = Paint(regularPaint).apply {
        textSize = 7.5f
    }
    val tableCellRightAlignPaint = Paint(tableCellPaint).apply {
        textAlign = Paint.Align.RIGHT
    }
    val tableHeaderBgPaint = Paint().apply {
        color = Color.rgb(230, 230, 230) // Light gray for header background
        style = Paint.Style.FILL
    }

    // --- LAYOUT DIMENSIONS & POSITIONS ---
    val pageWidth = 595f
    val pageHeight = 842f
    val margin = 30f // Adjusted margin
    val contentWidth = pageWidth - (2 * margin)

    var currentY = margin + 10f // Start Y position

    // --- TOP SECTION: Store Info (Left) & Contact (Right) ---
    // Store Name
    canvas.drawText(store.name ?: "Raj jewellers", margin, currentY + 5f, headerCompanyPaint)
    currentY += 18f
    // Store Address
    (store.address ?: "Old medical road\nMalkangiri, Odisha - 764048").split('\n').forEach { line ->
        canvas.drawText(line, margin, currentY, regularPaint)
        currentY += 10f
    }
    currentY += 5f // Extra space

    // Contact Info (Top Right)
    val topRightX = pageWidth - margin
    var contactY = margin + 15f
    regularPaint.textAlign = Paint.Align.RIGHT
    boldPaint.textAlign = Paint.Align.RIGHT // For labels like "Name:"
    canvas.drawText(
        "Name : ${store.proprietor ?: "Ranjan khan"}",
        topRightX,
        contactY,
        regularPaint
    )
    contactY += 12f
    canvas.drawText("Phone : ${store.phone ?: "9437206994"}", topRightX, contactY, regularPaint)
    contactY += 12f
    canvas.drawText(
        "Email : ${store.email ?: "Khanranjan45@gmail.com"}",
        topRightX,
        contactY,
        regularPaint
    )
    regularPaint.textAlign = Paint.Align.LEFT // Reset alignment
    boldPaint.textAlign = Paint.Align.LEFT   // Reset alignment

    currentY =
        maxOf(currentY, contactY) + 15f // Ensure currentY is below both left and right top sections

    // --- "ESTIMATE" TITLE ---
    canvas.drawText("ESTIMATE", pageWidth / 2, currentY, titleEstimatePaint)
    currentY += 25f

    // --- GSTIN BOX (Below Company Name, Left) & Estimate Details (Right) ---
    val gstinBoxStartY = currentY
    val gstinBoxWidth = contentWidth * 0.55f // Approx 55% width for GSTIN box
    val estimateDetailsX = margin + gstinBoxWidth + 10f // Start X for estimate details
    val sectionHeight = 25f // Height for GSTIN box and Estimate No/Date line

    // GSTIN Box
    canvas.drawRect(
        margin,
        gstinBoxStartY,
        margin + gstinBoxWidth,
        gstinBoxStartY + sectionHeight,
        thickBorderPaint
    )
    boldPaint.textSize = 9f
    regularPaint.textSize = 9f
    canvas.drawText("GSTIN : ", margin + 5f, gstinBoxStartY + 17f, boldPaint) // Label bold
    canvas.drawText(
        store.gstinNo ?: "21APEPK7976C1ZZ",
        margin + 5f + boldPaint.measureText("GSTIN : "),
        gstinBoxStartY + 17f,
        regularPaint
    )

    // Estimate Details (Right of GSTIN box)
    // "CUSTOMER COPY" or similar (instead of DUPLICATE COPY) - Optional
    val customerCopyText = "CUSTOMER COPY"
    regularPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(
        customerCopyText,
        pageWidth - margin,
        gstinBoxStartY - 8f,
        regularPaint
    ) // Position above the estimate details
    regularPaint.textAlign = Paint.Align.LEFT // Reset

    val estimateDetailLabelX = estimateDetailsX
    val estimateDetailValueX = estimateDetailsX + 65f // X for the value part
    boldPaint.textSize = 8f
    regularPaint.textSize = 8f

    canvas.drawText("Estimate No.", estimateDetailLabelX, gstinBoxStartY + 17f, boldPaint)
    canvas.drawText(
        ":",
        estimateDetailLabelX + boldPaint.measureText("Estimate No.") + 2f,
        gstinBoxStartY + 17f,
        boldPaint
    )
    canvas.drawText(estimateNumber, estimateDetailValueX, gstinBoxStartY + 17f, regularPaint)

    val estimateDateLabelX =
        estimateDetailsX + (contentWidth * 0.45f / 2) + 10f // Position Date more to the right
    val estimateDateValueX = estimateDateLabelX + 55f
    canvas.drawText("Est. Date", estimateDateLabelX, gstinBoxStartY + 17f, boldPaint)
    canvas.drawText(
        ":",
        estimateDateLabelX + boldPaint.measureText("Est. Date") + 2f,
        gstinBoxStartY + 17f,
        boldPaint
    )
    canvas.drawText(estimateDate, estimateDateValueX, gstinBoxStartY + 17f, regularPaint)

    currentY = gstinBoxStartY + sectionHeight + 3f // Padding after this section

    // --- CUSTOMER DETAIL BOX ---
    val customerBoxStartY = currentY
    val customerBoxHeight = 55f // Adjust as needed
    canvas.drawRect(
        margin,
        customerBoxStartY,
        pageWidth - margin,
        customerBoxStartY + customerBoxHeight,
        thickBorderPaint
    )
    currentY += 5f // Padding inside box

    boldPaint.textSize = 8f
    regularPaint.textSize = 8f
    val detailPadding = 3f
    var customerDetailY = customerBoxStartY + 12f
    val customerCol1X = margin + 5f
    val customerCol1LabelWidth = 50f
    val customerCol1ValueX = customerCol1X + customerCol1LabelWidth + detailPadding

    canvas.drawText("Name", customerCol1X, customerDetailY, boldPaint)
    canvas.drawText(":", customerCol1X + customerCol1LabelWidth, customerDetailY, boldPaint)
    canvas.drawText(customer.name ?: "N/A", customerCol1ValueX, customerDetailY, regularPaint)
    customerDetailY += 11f

    canvas.drawText("Address", customerCol1X, customerDetailY, boldPaint)
    canvas.drawText(":", customerCol1X + customerCol1LabelWidth, customerDetailY, boldPaint)
    canvas.drawText(customer.address ?: "N/A", customerCol1ValueX, customerDetailY, regularPaint)
    customerDetailY += 11f

    // Split Phone and GSTIN on the same line if space allows, or stack
    canvas.drawText("Phone", customerCol1X, customerDetailY, boldPaint)
    canvas.drawText(":", customerCol1X + customerCol1LabelWidth, customerDetailY, boldPaint)
    canvas.drawText(customer.mobileNo ?: "N/A", customerCol1ValueX, customerDetailY, regularPaint)

    val customerCol2X = margin + contentWidth / 2 + 10f
    val customerCol2LabelWidth = 60f
    val customerCol2ValueX = customerCol2X + customerCol2LabelWidth + detailPadding
    customerDetailY =
        customerBoxStartY + 12f // Reset Y for second column of customer details (if any)
    // Example for a second column if needed
    // canvas.drawText("GSTIN", customerCol2X, customerDetailY + 22f, boldPaint)
    // canvas.drawText(":", customerCol2X + customerCol2LabelWidth, customerDetailY + 22f, boldPaint)
    // canvas.drawText(customer.gstin ?: "N/A", customerCol2ValueX, customerDetailY + 22f, regularPaint)

    canvas.drawText("Place of Supply", customerCol1X, customerDetailY + 11f, boldPaint)
    canvas.drawText(":", customerCol1X + customerCol1LabelWidth, customerDetailY + 11f, boldPaint)
    canvas.drawText(
        customer.address ?: "N/A",
        customerCol1ValueX,
        customerDetailY + 11f,
        regularPaint
    )


    currentY = customerBoxStartY + customerBoxHeight + 5f // Space after customer box

    // --- ITEMS TABLE ---
    val tableStartY = currentY
    val tableHeaderHeight = 28f // Increased for sub-headers for CGST/SGST
    val tableRowHeight = 18f // Height for each item row
    val tableBottomPadding = 5f

    // Define column widths - CRITICAL - these must sum up to contentWidth or less
    // Sr, Product, HSN, Qty, Rate, Taxable, CGST%, CGST Amt, SGST%, SGST Amt, Total
    val colWidths = floatArrayOf(
        contentWidth * 0.04f, // Sr.No.
        contentWidth * 0.25f, // Name of Product
        contentWidth * 0.08f, // HSN/SAC
        contentWidth * 0.09f, // Qty
        contentWidth * 0.09f, // Rate
        contentWidth * 0.10f, // Taxable Value
        contentWidth * 0.05f, // CGST %
        contentWidth * 0.08f, // CGST Amt
        contentWidth * 0.05f, // SGST %
        contentWidth * 0.08f, // SGST Amt
        contentWidth * 0.09f  // Total
    )
    // Draw table background for header
    canvas.drawRect(
        margin,
        tableStartY,
        pageWidth - margin,
        tableStartY + tableHeaderHeight,
        tableHeaderBgPaint
    )

    // Table Headers
    var currentX = margin
    val headerTextY = tableStartY + 10f // Y for main header text
    val subHeaderTextY = tableStartY + 19f // Y for "%" and "Amount" sub-headers

    // Sr.No.
    canvas.drawText("Sr.", currentX + colWidths[0] / 2, headerTextY + 5f, tableHeaderPaint)
    currentX += colWidths[0]
    // Name of Product / Service
    canvas.drawText(
        "Name of Product / Service",
        currentX + colWidths[1] / 2,
        headerTextY + 5f,
        tableHeaderPaint
    )
    currentX += colWidths[1]
    // HSN / SAC
    canvas.drawText("HSN/SAC", currentX + colWidths[2] / 2, headerTextY + 5f, tableHeaderPaint)
    currentX += colWidths[2]
    // Qty
    canvas.drawText("Qty", currentX + colWidths[3] / 2, headerTextY + 5f, tableHeaderPaint)
    currentX += colWidths[3]
    // Rate
    canvas.drawText("Rate", currentX + colWidths[4] / 2, headerTextY + 5f, tableHeaderPaint)
    currentX += colWidths[4]
    // Taxable Value
    canvas.drawText("Taxable", currentX + colWidths[5] / 2, headerTextY, tableHeaderPaint)
    canvas.drawText("Value", currentX + colWidths[5] / 2, headerTextY + 8f, tableHeaderPaint)
    currentX += colWidths[5]

    // CGST (with sub-headers)
    val cgstBaseX = currentX
    canvas.drawText(
        "CGST",
        cgstBaseX + (colWidths[6] + colWidths[7]) / 2,
        headerTextY,
        tableHeaderPaint
    )
    canvas.drawText("%", cgstBaseX + colWidths[6] / 2, subHeaderTextY, tableHeaderPaint)
    canvas.drawText(
        "Amount",
        cgstBaseX + colWidths[6] + colWidths[7] / 2,
        subHeaderTextY,
        tableHeaderPaint
    )
    currentX += colWidths[6] + colWidths[7]

    // SGST (with sub-headers)
    val sgstBaseX = currentX
    canvas.drawText(
        "SGST",
        sgstBaseX + (colWidths[8] + colWidths[9]) / 2,
        headerTextY,
        tableHeaderPaint
    )
    canvas.drawText("%", sgstBaseX + colWidths[8] / 2, subHeaderTextY, tableHeaderPaint)
    canvas.drawText(
        "Amount",
        sgstBaseX + colWidths[8] + colWidths[9] / 2,
        subHeaderTextY,
        tableHeaderPaint
    )
    currentX += colWidths[8] + colWidths[9]

    // Total
    canvas.drawText("Total", currentX + colWidths[10] / 2, headerTextY + 5f, tableHeaderPaint)

    // Table Rows
    var tableRowY = tableStartY + tableHeaderHeight
    items.forEachIndexed { index, item ->
        val textY = tableRowY + tableRowHeight - 5f // Adjust for text baseline
        currentX = margin
        // Sr.No.
        tableCellPaint.textAlign = Paint.Align.CENTER
        canvas.drawText((index + 1).toString(), currentX + colWidths[0] / 2, textY, tableCellPaint)
        currentX += colWidths[0]
        // Name of Product
        tableCellPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            item.itemAddName.take(25),
            currentX + 3f,
            textY,
            tableCellPaint
        ) // Truncate if too long
        currentX += colWidths[1]
        // HSN/SAC
        tableCellPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(item.huid.toString(), currentX + colWidths[2] / 2, textY, tableCellPaint)
        currentX += colWidths[2]
        // Qty
        canvas.drawText(
            "${item.gsWt.to3FString()} Gms",
            currentX + colWidths[3] / 2,
            textY,
            tableCellRightAlignPaint
        ) // Assuming gWt is quantity
        currentX += colWidths[3]
        // Rate
        canvas.drawText(
            item.fnMetalPrice.to3FString(),
            currentX + colWidths[4] - 3f,
            textY,
            tableCellRightAlignPaint
        )
        currentX += colWidths[4]
        // Taxable Value
        val taxableValue = item.price // Assuming item.price is taxable value before tax
        canvas.drawText(
            taxableValue.to3FString(),
            currentX + colWidths[5] - 3f,
            textY,
            tableCellRightAlignPaint
        )
        currentX += colWidths[5]
        // CGST % & Amount (Assuming you have these in ItemSelectedModel or calculate them)
        // For Estimate, these might be 0 or calculated based on settings
        val cgstRate = item.cgst // Example: item.cgstPct = 1.5
        val cgstAmount = item.cgst // Example: item.cgst = taxableValue * (cgstRate / 100.0)
        canvas.drawText(
            cgstRate.to3FString(),
            currentX + colWidths[6] - 3f,
            textY,
            tableCellRightAlignPaint
        )
        currentX += colWidths[6]
        canvas.drawText(
            cgstAmount.to3FString(),
            currentX + colWidths[7] - 3f,
            textY,
            tableCellRightAlignPaint
        )
        currentX += colWidths[7]
        // SGST % & Amount
        val sgstRate = item.sgst // Example: item.sgstPct = 1.5
        val sgstAmount = item.sgst // Example: item.sgst = taxableValue * (sgstRate / 100.0)
        canvas.drawText(
            sgstRate.to3FString(),
            currentX + colWidths[8] - 3f,
            textY,
            tableCellRightAlignPaint
        )
        currentX += colWidths[8]
        canvas.drawText(
            sgstAmount.to3FString(),
            currentX + colWidths[9] - 3f,
            textY,
            tableCellRightAlignPaint
        )
        currentX += colWidths[9]
        // Total
        val itemTotal =
            taxableValue + cgstAmount + sgstAmount // item.price already includes this usually
        canvas.drawText(
            item.price.to3FString(),
            currentX + colWidths[10] - 3f,
            textY,
            tableCellRightAlignPaint
        )

        tableRowY += tableRowHeight
        if (index < items.size - 1) { // Draw row line
            canvas.drawLine(margin, tableRowY, pageWidth - margin, tableRowY, borderPaint)
        }
    }

    // Table Border
    canvas.drawRect(
        margin,
        tableStartY,
        pageWidth - margin,
        tableRowY + tableBottomPadding,
        thickBorderPaint
    ) // Outer table border
    canvas.drawLine(
        margin,
        tableStartY + tableHeaderHeight,
        pageWidth - margin,
        tableStartY + tableHeaderHeight,
        thickBorderPaint
    ) // Header bottom line
    // Vertical Column Lines
    currentX = margin
    colWidths.forEach { width ->
        canvas.drawLine(
            currentX,
            tableStartY,
            currentX,
            tableRowY + tableBottomPadding,
            thickBorderPaint
        )
        currentX += width
    }
    canvas.drawLine(
        pageWidth - margin,
        tableStartY,
        pageWidth - margin,
        tableRowY + tableBottomPadding,
        thickBorderPaint
    ) // Ensure last vertical line


    currentY = tableRowY + tableBottomPadding + 5f

    // --- TOTALS ROW (BELOW TABLE) ---
    val totalRowY = currentY
    val totalRowHeight = 20f
    canvas.drawRect(
        margin,
        totalRowY,
        pageWidth - margin,
        totalRowY + totalRowHeight,
        tableHeaderBgPaint
    ) // Background for total row
    canvas.drawLine(
        margin,
        totalRowY + totalRowHeight,
        pageWidth - margin,
        totalRowY + totalRowHeight,
        thickBorderPaint
    )


    val totalTextY = totalRowY + totalRowHeight - 6f
    boldPaint.textSize = 8f
    tableCellRightAlignPaint.isFakeBoldText = true // Make totals bold

    // Calculate total Qty (if applicable), Taxable, CGST, SGST, Grand Total
    val totalQty = items.sumOf { it.gsWt }
    val summaryTotals = CalculationUtils.summaryTotals(items)
    val totalTaxableValue = summaryTotals.totalPriceBeforeTax
    val totalCGST = items.sumOf { it.cgst }
    val totalSGST = items.sumOf { it.sgst }
    val grandTotal = summaryTotals.grandTotal


    currentX = margin + colWidths[0] + colWidths[1] + colWidths[2] // Start after HSN
    canvas.drawText("Total", currentX - 25f, totalTextY, boldPaint) // "Total" label

    currentX += colWidths[3] // Skip Qty for text label, draw value under Qty col
    canvas.drawText(
        "${totalQty.to3FString()} Gms",
        currentX - colWidths[3] / 2,
        totalTextY,
        tableCellRightAlignPaint
    ) // Total Qty

    currentX += colWidths[4] // Skip Rate column
    currentX += colWidths[5]
    canvas.drawText(
        totalTaxableValue.to3FString(),
        currentX - 3f,
        totalTextY,
        tableCellRightAlignPaint
    )

    currentX += colWidths[6] // Skip CGST %
    currentX += colWidths[7]
    canvas.drawText(
        totalCGST.to3FString(),
        currentX - 3f,
        totalTextY,
        tableCellRightAlignPaint
    )

    currentX += colWidths[8] // Skip SGST %
    currentX += colWidths[9]
    canvas.drawText(
        totalSGST.to3FString(),
        currentX - 3f,
        totalTextY,
        tableCellRightAlignPaint
    )

    currentX += colWidths[10]
    canvas.drawText(
        grandTotal.to3FString(),
        currentX - 3f,
        totalTextY,
        tableCellRightAlignPaint
    )
    tableCellRightAlignPaint.isFakeBoldText = false // Reset bold

    currentY = totalRowY + totalRowHeight + 10f

    // --- TOTAL IN WORDS & SUMMARY (SIDE-BY-SIDE) ---
    val amountInWordsY = currentY
    val summarySectionX = pageWidth - margin - (contentWidth * 0.35f) // Summary takes right 35%
    val amountInWordsWidth = contentWidth * 0.60f // Amount in words takes left 60%

    // Total In Words (Left)
    boldPaint.textSize = 8f
    regularPaint.textSize = 8f
    canvas.drawText("Total in words", margin, amountInWordsY, boldPaint)
    currentY += 12f
    // You need a numberToWords function
    val words = try {
        numberToWords(grandTotal.toInt()) + " ONLY"
    } catch (e: Exception) {
        "Error in words"
    }
    // Draw potentially multi-line words
    var lineY = currentY
    val wordsPaint = regularPaint
    wordsPaint.textSize = 9f
    words.split(" ").fold("") { currentLine, word ->
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (wordsPaint.measureText(testLine) > amountInWordsWidth - 5f) {
            canvas.drawText(currentLine, margin, lineY, wordsPaint)
            lineY += 10f
            word
        } else {
            testLine
        }
    }.also { canvas.drawText(it, margin, lineY, wordsPaint) }
    val endOfWordsY = lineY + 10f


    // Summary (Right Side)
    var summaryY = amountInWordsY
    val summaryLabelX = summarySectionX + 5f
    val summaryValueX = pageWidth - margin - 5f
    val summaryLineHeight = 11f

    regularPaint.textAlign = Paint.Align.LEFT
    boldPaint.textAlign = Paint.Align.LEFT
    regularPaint.textSize = 7.5f
    boldPaint.textSize = 7.5f


    canvas.drawText("Taxable Amount", summaryLabelX, summaryY, regularPaint)
    regularPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(totalTaxableValue.to3FString(), summaryValueX, summaryY, regularPaint)
    summaryY += summaryLineHeight
    regularPaint.textAlign = Paint.Align.LEFT

    canvas.drawText("Add: CGST", summaryLabelX, summaryY, regularPaint)
    regularPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(totalCGST.to3FString(), summaryValueX, summaryY, regularPaint)
    summaryY += summaryLineHeight
    regularPaint.textAlign = Paint.Align.LEFT

    canvas.drawText("Add: SGST", summaryLabelX, summaryY, regularPaint)
    regularPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(totalSGST.to3FString(), summaryValueX, summaryY, regularPaint)
    summaryY += summaryLineHeight
    regularPaint.textAlign = Paint.Align.LEFT

    val totalTax = totalCGST + totalSGST
    canvas.drawText("Total Tax", summaryLabelX, summaryY, regularPaint) // Label bold if needed
    regularPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(totalTax.to3FString(), summaryValueX, summaryY, regularPaint)
    summaryY += summaryLineHeight
    regularPaint.textAlign = Paint.Align.LEFT

    // Discount (Example: assume 0 for estimate or pass as param)
    val discount = 0.00
    canvas.drawText("Discount", summaryLabelX, summaryY, regularPaint)
    regularPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(discount.to3FString(), summaryValueX, summaryY, regularPaint)
    summaryY += summaryLineHeight + 2f // Extra space before total
    regularPaint.textAlign = Paint.Align.LEFT

    // Line before Total Amount
    canvas.drawLine(summarySectionX, summaryY - 2f, pageWidth - margin, summaryY - 2f, borderPaint)

    // Total Amount After Tax / ESTIMATED TOTAL
    boldPaint.textSize = 9f
    canvas.drawText("ESTIMATED TOTAL", summaryLabelX, summaryY, boldPaint)
    boldPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(
        "₹${(grandTotal - discount).to3FString()}",
        summaryValueX,
        summaryY,
        boldPaint
    )
    summaryY += summaryLineHeight
    boldPaint.textAlign = Paint.Align.LEFT

    val eAndOE_Y = summaryY + 3f
    smallRegularPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText("(E. & O.E.)", summaryValueX, eAndOE_Y, smallRegularPaint)
    smallRegularPaint.textAlign = Paint.Align.LEFT // Reset

    currentY = maxOf(endOfWordsY, summaryY) + 10f // Position below both sections


    // --- TERMS AND CONDITIONS ---
    val termsStartY = currentY
    canvas.drawText("Terms and Conditions", margin, termsStartY, smallBoldPaint)
    currentY += 10f
    smallRegularPaint.textSize = 6.5f // Smaller font for terms
    val estimateTerms = listOf(
        "1. This estimate is valid for 15 days from the date of issue.",
        "2. Prices are subject to change based on market fluctuations of precious metals.",
        "3. All items are subject to availability at the time of order confirmation.",
        "4. Any alterations to design may affect the final price.",
        "5. Payment: 50% advance to confirm order, balance on delivery.",
        "6. Delivery timeline will be communicated upon order confirmation.",
        "7. This is an estimate and not a tax invoice. Tax invoice will be provided upon sale.",
        "8. All disputes shall be subject to the jurisdiction of the courts at the place where the store is located."
    )
    estimateTerms.forEach { term ->
        // Basic line wrapping for terms
        var currentLineText = ""
        term.split(" ").forEach { word ->
            val testLine = if (currentLineText.isEmpty()) word else "$currentLineText $word"
            if (smallRegularPaint.measureText(testLine) < (pageWidth - (2 * margin) - (contentWidth * 0.35f) - 15f)) { // available width for terms
                currentLineText = testLine
            } else {
                canvas.drawText(currentLineText, margin, currentY, smallRegularPaint)
                currentY += 8f
                currentLineText = word
            }
        }
        if (currentLineText.isNotEmpty()) {
            canvas.drawText(currentLineText, margin, currentY, smallRegularPaint)
        }
        currentY += 8f // Line spacing for terms
    }
    currentY += 5f


    // --- SIGNATURE AREA ---
    val signatureY = pageHeight - margin - 50f // Position from bottom
    val forStoreText = "For ${store.name}"
    val authorisedSignatoryText = "Authorised Signatory"

    boldPaint.textSize = 9f
    boldPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(forStoreText, pageWidth - margin, signatureY, boldPaint)
    canvas.drawText(authorisedSignatoryText, pageWidth - margin, signatureY + 30f, boldPaint)
    boldPaint.textAlign = Paint.Align.LEFT // Reset

    // --- FINISH PAGE AND SAVE ---
    pdfDocument.finishPage(page)

    val fileName = "estimate_${estimateNumber.replace("/", "_")}_${System.currentTimeMillis()}.pdf"
    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/JewelVault/Estimates"
            )
        } else {
            // For older versions, you might need to handle saving differently or use legacy external storage.
            // This example simplifies for Q+
            val estimatesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "/JewelVault/Estimates"
            )
            if (!estimatesDir.exists()) {
                estimatesDir.mkdirs()
            }
            // Note: Direct file path access is restricted in newer Android versions without specific permissions or SAF.
            // MediaStore is preferred. This is a fallback concept.
        }
    }

    val resolver = context.contentResolver
    var uri: Uri? = null
    try {
        uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                outputStream.flush() // Ensure all data is written
                Log.d("PDF_GENERATION", "Estimate PDF generated successfully at $uri")
                onFileReady(uri)
            } ?: run {
                Log.e("PDF_GENERATION", "Failed to open output stream for $uri")
                uri = null // To indicate failure
            }
        } else {
            Log.e("PDF_GENERATION", "Failed to create new MediaStore entry for estimate.")
        }
    } catch (e: IOException) {
        Log.e("PDF_GENERATION", "Error writing Estimate PDF: ${e.message}", e)
        uri?.let {
            resolver.delete(
                it,
                null,
                null
            )
        } // Clean up if URI was created but writing failed
        uri = null
    } finally {
        pdfDocument.close()
        if (uri == null) {
            // Potentially call a failure callback if you have one, instead of just logging
            Log.e("PDF_GENERATION", "Estimate PDF generation failed.")
            // onFileReady(Uri.EMPTY) // Or some indicator of failure if your callback expects it
        }
    }
}


private fun extractCategoryFromDescription(description: String): String {
    return description.trim().split(" ").firstOrNull() ?: ""
}*/

// Represents the overall information needed for the draft invoice
data class InvoiceData(
    val storeInfo: StoreEntity,
    val customerInfo: CustomerEntity,
    val invoiceMeta: InvoiceMetadata,
    val items: List<DraftItemModel>,
    val jurisdiction: String = "Malkangiri",
    val goldRate: String,
    val silverRate: String,
    val paymentSummary: PaymentSummary,
    val paymentReceived: PaymentReceivedDetails, // For CASH/Card details received
    val declarationPoints: List<String>,
    val termsAndConditions: String? = null, // Optional, if you have more extensive T&Cs
    val customerSignature: ImageBitmap? = null, // Or android.graphics.Bitmap
    val ownerSignature: ImageBitmap? = null,    // Or android.graphics.Bitmap
    val thankYouMessage: String = "Thank You Please Visit Again" // Default
) {

    data class InvoiceMetadata(
        val invoiceNumber: String, // e.g., "A003568"
        val date: String,          // e.g., "12/12/2023"
        val time: String,          // e.g., "20:30"
        val salesMan: String,      // e.g., "Ramesh" or "Counter 1"
        val documentType: String   // e.g., "ESTIMATE" or "DRAFT INVOICE"
    )

    data class DraftItemModel(
        val serialNumber: String,
        val productDescription: String, // from catName, subCatName, itemAddName
        val quantitySet: String,        // from quantity (formatted as string)
        val grossWeightGms: String,     // from gsWt (formatted)
        val netWeightGms: String,       // from ntWt (formatted)
        val metalPrice: String,          // metal type
        val metalType: String,          // metal type
        val ratePerGm: String,          // from fnMetalPrice (formatted)
        val makingAmount: String,       // from chargeAmount (formatted)
        val purityPercent: String,      // from purity (formatted)
        val totalAmount: String         // from price (formatted)
    )


    data class PaymentSummary(
        val subTotal: String,       // e.g., "₹ 10,000"
        val gstAmount: String,      // e.g., "₹ 500" (or break down to CGST, SGST if needed)
        val gstLabel: String = "GST @", // Customizable label
        val discount: String,       // e.g., "₹ 950"
        val cardCharges: String,    // e.g., "₹ 0"
        val totalAmountBeforeOldExchange: String, // "Total Amt." e.g., "₹ 10,450"
        val oldExchange: String,    // e.g., "₹ 10,450"
        val roundOff: String,       // e.g., "₹ 0.50" or "-₹0.50"
        val netAmountPayable: String, // "Net Amount" e.g., "₹ 0"
        val amountInWords: String   // Calculated based on Net Amount Payable
    )

    data class PaymentReceivedDetails(
        val cashLabel1: String = "CASH",
        val cashAmount1: String,
        // Add more fields if multiple payment modes are common
    )


}




