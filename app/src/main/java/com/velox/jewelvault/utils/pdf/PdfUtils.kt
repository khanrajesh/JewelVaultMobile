package com.velox.jewelvault.utils.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
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
import com.velox.jewelvault.utils.CalculationUtils
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.numberToWords
import com.velox.jewelvault.utils.to0FString
import com.velox.jewelvault.utils.to2FString
import com.velox.jewelvault.utils.to3FString
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
    invoiceDate: String = "",

): InvoiceData {

    val tag = "createDraftInvoiceData"
    val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val resolvedInvoiceDate = invoiceDate.trim().ifBlank { currentDate }

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
    val computedRoundOff = itemGrandTotalAmount - (subTotalAmount + totalTaxAmount)
    val explicitRoundOff = roundOff.toDoubleOrNull()
    val resolvedRoundOff = if (explicitRoundOff == null || explicitRoundOff == 0.0) {
        computedRoundOff
    } else {
        explicitRoundOff
    }

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
    val resolvedJurisdiction = store.jurisdiction.ifBlank { "" }



    return InvoiceData(
        storeInfo = store,
        customerInfo = customer,
        invoiceMeta = InvoiceData.InvoiceMetadata(
            invoiceNumber = invoiceNo,
            date = resolvedInvoiceDate,
            time = currentTime,
            salesMan = "$currentLoginUserName",
            documentType = "INVOICE"
        ),
        items = draftItems,
        jurisdiction = resolvedJurisdiction,
        goldRate = goldRate,
        silverRate = silverRate,
        paymentSummary = InvoiceData.PaymentSummary(
            subTotal = "₹${subTotalAmount.to3FString()}",
            gstAmount = "₹${totalTaxAmount.to3FString()}",
            gstLabel = gstLabel,
            discount = discount,
            cardCharges = cardCharges,
            totalAmountBeforeOldExchange = itemGrandTotalAmount.to0FString(),
            oldExchange = oldExchange,
            roundOff = resolvedRoundOff.to2FString(),
            netAmountPayable = "₹${netAmountPayable.to3FString()}",
            amountInWords = "Indian Rupee ${numberToWords(netAmountPayable.roundToInt())} Only"
        ),
        paymentReceived = InvoiceData.PaymentReceivedDetails(
            cashLabel1 = paymentMethod.uppercase(),
            cashAmount1 = "${paidAmount.to3FString()}"
        ),
        declarationPoints = listOf(
            "We declare that this invoice shows the actual price of the goods described.",
            "All disputes are subject to the courts at $resolvedJurisdiction.",
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
            "Metal\nPrice", //todo
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
                entity.metalPrice, //todo
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

      /*  drawText("Current Rate", metalBoxRight + s2f, metalBoxTop + s9f, tp16)
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
        ) */

        drawText(
            "Payment Details",
            metalBoxRight + s2f,
            metalBoxTop + s9f ,//+ s9f + s9f + s9f + s2f,
            tp16
        )
        // Underline for 'Payment Details'
        val paymentDetailsText = "Payment Details"
        val paymentDetailsX = metalBoxRight + s2f
        val paymentDetailsY = metalBoxTop + s9f + s9f //+ s9f + s9f
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
                Rect(0, 0, customerBitmap.width, customerBitmap.height)
            val customerDestRect = Rect(
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
            val ownerSrcRect = Rect(0, 0, ownerBitmap.width, ownerBitmap.height)
            val ownerDestRect = Rect(
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


// Represents the overall information needed for the draft invoice
data class InvoiceData(
    val storeInfo: StoreEntity,
    val customerInfo: CustomerEntity,
    val invoiceMeta: InvoiceMetadata,
    val items: List<DraftItemModel>,
    val jurisdiction: String = " ",
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
