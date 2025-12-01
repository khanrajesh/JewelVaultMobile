package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.label.LabelTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelTemplateDao {
    
    @Query("SELECT * FROM label_template ORDER BY modifiedAt DESC")
    fun getAllTemplates(): Flow<List<LabelTemplateEntity>>
    
    @Query("SELECT * FROM label_template WHERE templateId = :templateId")
    suspend fun getTemplateById(templateId: String): LabelTemplateEntity?
    
    @Query("SELECT * FROM label_template WHERE templateType = :templateType ORDER BY modifiedAt DESC")
    fun getTemplatesByType(templateType: String): Flow<List<LabelTemplateEntity>>
    
    @Query("SELECT * FROM label_template WHERE isDefault = 1 LIMIT 1")
    fun getDefaultTemplate(): Flow<LabelTemplateEntity?>
    
    @Query("SELECT * FROM label_template WHERE templateName LIKE '%' || :searchQuery || '%' ORDER BY modifiedAt DESC")
    fun searchTemplates(searchQuery: String): Flow<List<LabelTemplateEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: LabelTemplateEntity)
    
    @Update
    suspend fun updateTemplate(template: LabelTemplateEntity)
    
    @Delete
    suspend fun deleteTemplate(template: LabelTemplateEntity)
    
    @Query("DELETE FROM label_template WHERE templateId = :templateId")
    suspend fun deleteTemplateById(templateId: String)
    
    @Query("UPDATE label_template SET isDefault = 0")
    suspend fun clearAllDefaults()
    
    @Query("UPDATE label_template SET isDefault = 1 WHERE templateId = :templateId")
    suspend fun setDefaultTemplate(templateId: String)
    
    @Query("UPDATE label_template SET modifiedAt = :timestamp WHERE templateId = :templateId")
    suspend fun updateModifiedAt(templateId: String, timestamp: Long)
}

