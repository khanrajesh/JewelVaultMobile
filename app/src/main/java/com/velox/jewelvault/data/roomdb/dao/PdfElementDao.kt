package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.velox.jewelvault.data.roomdb.entity.pdf.PdfElementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfElementDao {

    @Query("SELECT * FROM pdf_element WHERE templateId = :templateId ORDER BY zIndex ASC")
    fun getElementsByTemplateId(templateId: String): Flow<List<PdfElementEntity>>

    @Query("SELECT * FROM pdf_element WHERE templateId = :templateId ORDER BY zIndex ASC")
    suspend fun getElementsByTemplateIdSync(templateId: String): List<PdfElementEntity>

    @Query("SELECT * FROM pdf_element WHERE elementId = :elementId")
    suspend fun getElementById(elementId: String): PdfElementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElement(element: PdfElementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElements(elements: List<PdfElementEntity>)

    @Update
    suspend fun updateElement(element: PdfElementEntity)

    @Delete
    suspend fun deleteElement(element: PdfElementEntity)

    @Query("DELETE FROM pdf_element WHERE elementId = :elementId")
    suspend fun deleteElementById(elementId: String)

    @Query("DELETE FROM pdf_element WHERE templateId = :templateId")
    suspend fun deleteElementsByTemplateId(templateId: String)

    @Query("UPDATE pdf_element SET zIndex = :zIndex WHERE elementId = :elementId")
    suspend fun updateElementZIndex(elementId: String, zIndex: Int)

    @Query("UPDATE pdf_element SET x = :x, y = :y WHERE elementId = :elementId")
    suspend fun updateElementPosition(elementId: String, x: Float, y: Float)

    @Query("UPDATE pdf_element SET width = :width, height = :height WHERE elementId = :elementId")
    suspend fun updateElementSize(elementId: String, width: Float, height: Float)
}
