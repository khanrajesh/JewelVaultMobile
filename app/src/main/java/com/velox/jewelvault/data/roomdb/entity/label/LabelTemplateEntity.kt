package com.velox.jewelvault.data.roomdb.entity.label

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(tableName = TableNames.LABEL_TEMPLATE)
data class LabelTemplateEntity(
    @PrimaryKey val templateId: String,
    val templateName: String,
    val templateType: String, // "ITEM_LABEL" or "FREE_LABEL"
    val labelWidth: Float, // in mm
    val labelHeight: Float, // in mm
    val gapWidth: Float, // in mm
    val gapHeight: Float, // in mm
    val printDensity: Int, // 1-15
    val printSpeed: Int, // 1-6 (printer dependent)
    val printDirection: Int, // 0 = normal, 1 = reverse
    val referenceX: Float, // in mm, left margin
    val referenceY: Float, // in mm, top margin
    val orientation: String, // "PORTRAIT" or "LANDSCAPE"
    val labelPadding: Float = 1.5f, // in mm, applied on all sides
    val printLanguage: String, // "TSPL", "CPCL", "ESC", etc.
    val createdAt: Long,
    val modifiedAt: Long,
    val isDefault: Boolean = false,
    val description: String? = null
)
