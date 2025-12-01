package com.velox.jewelvault.data.roomdb.dao.label

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.label.LabelElementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelElementDao {
    
    @Query("SELECT * FROM label_element WHERE templateId = :templateId ORDER BY zIndex ASC")
    fun getElementsByTemplateId(templateId: String): Flow<List<LabelElementEntity>>
    
    @Query("SELECT * FROM label_element WHERE templateId = :templateId ORDER BY zIndex ASC")
    suspend fun getElementsByTemplateIdSync(templateId: String): List<LabelElementEntity>
    
    @Query("SELECT * FROM label_element WHERE elementId = :elementId")
    suspend fun getElementById(elementId: String): LabelElementEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElement(element: LabelElementEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElements(elements: List<LabelElementEntity>)
    
    @Update
    suspend fun updateElement(element: LabelElementEntity)
    
    @Delete
    suspend fun deleteElement(element: LabelElementEntity)
    
    @Query("DELETE FROM label_element WHERE elementId = :elementId")
    suspend fun deleteElementById(elementId: String)
    
    @Query("DELETE FROM label_element WHERE templateId = :templateId")
    suspend fun deleteElementsByTemplateId(templateId: String)
    
    @Query("UPDATE label_element SET zIndex = :zIndex WHERE elementId = :elementId")
    suspend fun updateElementZIndex(elementId: String, zIndex: Int)
    
    @Query("UPDATE label_element SET x = :x, y = :y WHERE elementId = :elementId")
    suspend fun updateElementPosition(elementId: String, x: Float, y: Float)
    
    @Query("UPDATE label_element SET width = :width, height = :height WHERE elementId = :elementId")
    suspend fun updateElementSize(elementId: String, width: Float, height: Float)
}
