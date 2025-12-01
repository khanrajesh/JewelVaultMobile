package com.velox.jewelvault.data.roomdb.entity.label

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(tableName = TableNames.LABEL_PRINT_JOB)
data class LabelPrintJobEntity(
    @PrimaryKey val jobId: String,
    val templateId: String, // Foreign key to LabelTemplateEntity
    val printerAddress: String,
    val printLanguage: String, // "TSPL", "CPCL", "ESC", etc.
    val itemIds: String?, // JSON array for mass printing
    val customData: String?, // JSON for free labels
    val copies: Int,
    val status: String, // "PENDING", "PRINTING", "COMPLETED", "FAILED"
    val createdAt: Long,
    val completedAt: Long?,
    val errorMessage: String? = null
)
