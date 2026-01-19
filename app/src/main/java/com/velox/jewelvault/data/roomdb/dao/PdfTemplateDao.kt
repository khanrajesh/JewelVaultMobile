package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.velox.jewelvault.data.roomdb.entity.pdf.PdfTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfTemplateDao {

    @Query("SELECT * FROM pdf_template ORDER BY modifiedAt DESC")
    fun getAllTemplates(): Flow<List<PdfTemplateEntity>>

    @Query("SELECT * FROM pdf_template WHERE templateType = :templateType ORDER BY modifiedAt DESC")
    fun getTemplatesByType(templateType: String): Flow<List<PdfTemplateEntity>>

    @Query("SELECT * FROM pdf_template WHERE templateType = :templateType AND status = :status ORDER BY modifiedAt DESC")
    suspend fun getTemplatesByTypeAndStatusSync(
        templateType: String,
        status: String
    ): List<PdfTemplateEntity>

    @Query("SELECT * FROM pdf_template WHERE templateId = :templateId")
    suspend fun getTemplateById(templateId: String): PdfTemplateEntity?

    @Query("SELECT * FROM pdf_template WHERE templateType = :templateType AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultTemplateByTypeSync(templateType: String): PdfTemplateEntity?

    @Query("SELECT * FROM pdf_template WHERE templateType = :templateType AND isSystemDefault = 1 LIMIT 1")
    suspend fun getSystemDefaultByTypeSync(templateType: String): PdfTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: PdfTemplateEntity)

    @Update
    suspend fun updateTemplate(template: PdfTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: PdfTemplateEntity)

    @Query("DELETE FROM pdf_template WHERE templateId = :templateId")
    suspend fun deleteTemplateById(templateId: String)

    @Query("UPDATE pdf_template SET isDefault = 0 WHERE templateType = :templateType")
    suspend fun clearDefaultForType(templateType: String)

    @Query("UPDATE pdf_template SET isDefault = 1 WHERE templateId = :templateId")
    suspend fun setDefaultTemplate(templateId: String)
}
