package com.velox.jewelvault.data.roomdb.entity.label

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(tableName = TableNames.LABEL_ELEMENT)
data class LabelElementEntity(
    @PrimaryKey val elementId: String,
    val templateId: String, // Foreign key to LabelTemplateEntity
    val elementType: String, // "TEXT", "IMAGE", "QR_CODE", "BARCODE"
    val x: Float, // Absolute position in mm
    val y: Float, // Absolute position in mm
    val width: Float, // in mm
    val height: Float, // in mm
    val rotation: Float, // degrees (0-360)
    val zIndex: Int, // Layer order (higher = on top)
    val properties: String, // JSON string for element-specific properties
    val dataBinding: String? = null, // For dynamic data (e.g., "item.itemId")
    val isVisible: Boolean = true
)
