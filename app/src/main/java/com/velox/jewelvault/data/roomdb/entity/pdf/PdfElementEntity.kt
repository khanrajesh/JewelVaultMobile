package com.velox.jewelvault.data.roomdb.entity.pdf

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(tableName = TableNames.PDF_ELEMENT)
data class PdfElementEntity(
    @PrimaryKey val elementId: String,
    val templateId: String,
    val elementType: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val zIndex: Int,
    val properties: String,
    val dataBinding: String? = null,
    val isVisible: Boolean = true
)
