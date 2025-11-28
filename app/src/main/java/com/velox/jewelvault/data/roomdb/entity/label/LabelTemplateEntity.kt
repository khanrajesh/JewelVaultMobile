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
    val orientation: String, // "PORTRAIT" or "LANDSCAPE"
    val printLanguage: String, // "TSPL", "CPCL", "ESC", etc.
    val createdAt: Long,
    val modifiedAt: Long,
    val isDefault: Boolean = false,
    val description: String? = null
)

