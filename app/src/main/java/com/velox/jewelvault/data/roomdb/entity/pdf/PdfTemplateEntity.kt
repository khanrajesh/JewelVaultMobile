package com.velox.jewelvault.data.roomdb.entity.pdf

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(tableName = TableNames.PDF_TEMPLATE)
data class PdfTemplateEntity(
    @PrimaryKey val templateId: String,
    val templateName: String,
    val templateType: String,
    val pageWidth: Float,
    val pageHeight: Float,
    val orientation: String,
    val marginLeft: Float,
    val marginTop: Float,
    val marginRight: Float,
    val marginBottom: Float,
    val status: String,
    val isDefault: Boolean = false,
    val isSystemDefault: Boolean = false,
    val baseTemplateId: String? = null,
    val createdAt: Long,
    val modifiedAt: Long,
    val publishedAt: Long? = null,
    val description: String? = null
)
