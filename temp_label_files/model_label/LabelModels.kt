package com.velox.jewelvault.data.model.label

import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import com.velox.jewelvault.data.roomdb.entity.label.LabelElementEntity
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Sealed class representing different types of label elements
 */
sealed class LabelElementType {
    data class TextElement(
        val text: String,
        val fontSize: Float,
        val fontFamily: String = "Arial",
        val alignment: TextAlign = TextAlign.Start,
        val color: Int = Color.BLACK,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val dataBinding: String? = null
    ) : LabelElementType()
    
    data class ImageElement(
        val imageUri: Uri?,
        val imagePath: String? = null,
        val scaleType: ImageScaleType = ImageScaleType.FIT_CENTER,
        val dataBinding: String? = null
    ) : LabelElementType()
    
    data class QRElement(
        val qrData: String,
        val size: Float,
        val errorCorrectionLevel: QRErrorCorrectionLevel = QRErrorCorrectionLevel.MEDIUM,
        val dataBinding: String? = null
    ) : LabelElementType()
    
    data class BarcodeElement(
        val barcodeData: String,
        val barcodeType: BarcodeType = BarcodeType.CODE128,
        val width: Float,
        val height: Float,
        val dataBinding: String? = null
    ) : LabelElementType()
}

enum class ImageScaleType {
    FIT_CENTER,
    CENTER_CROP,
    CENTER_INSIDE,
    FIT_XY
}

enum class QRErrorCorrectionLevel {
    LOW,
    MEDIUM,
    QUARTILE,
    HIGH
}

enum class BarcodeType {
    CODE128,
    CODE39,
    EAN13,
    EAN8,
    UPC_A,
    UPC_E,
    ITF
}

/**
 * Data class representing a complete label template with its elements
 */
data class LabelTemplate(
    val template: LabelTemplateEntity,
    val elements: List<LabelElement>
)

/**
 * Data class representing a label element with its type
 */
data class LabelElement(
    val element: LabelElementEntity,
    val elementType: LabelElementType
)

/**
 * Data class for print job data
 */
data class PrintJobData(
    val templateId: String,
    val itemIds: List<String>? = null,
    val customData: Map<String, Any>? = null,
    val copies: Int = 1,
    val printerAddress: String,
    val printLanguage: String = "TSPL"
)
