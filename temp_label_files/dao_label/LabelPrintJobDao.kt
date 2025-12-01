package com.velox.jewelvault.data.roomdb.dao.label

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.label.LabelPrintJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelPrintJobDao {
    
    @Query("SELECT * FROM label_print_job ORDER BY createdAt DESC")
    fun getAllPrintJobs(): Flow<List<LabelPrintJobEntity>>
    
    @Query("SELECT * FROM label_print_job WHERE jobId = :jobId")
    suspend fun getPrintJobById(jobId: String): LabelPrintJobEntity?
    
    @Query("SELECT * FROM label_print_job WHERE status = :status ORDER BY createdAt DESC")
    fun getPrintJobsByStatus(status: String): Flow<List<LabelPrintJobEntity>>
    
    @Query("SELECT * FROM label_print_job WHERE printerAddress = :printerAddress ORDER BY createdAt DESC")
    fun getPrintJobsByPrinter(printerAddress: String): Flow<List<LabelPrintJobEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrintJob(job: LabelPrintJobEntity)
    
    @Update
    suspend fun updatePrintJob(job: LabelPrintJobEntity)
    
    @Delete
    suspend fun deletePrintJob(job: LabelPrintJobEntity)
    
    @Query("DELETE FROM label_print_job WHERE jobId = :jobId")
    suspend fun deletePrintJobById(jobId: String)
    
    @Query("UPDATE label_print_job SET status = :status WHERE jobId = :jobId")
    suspend fun updatePrintJobStatus(jobId: String, status: String)
    
    @Query("UPDATE label_print_job SET status = :status, completedAt = :completedAt, errorMessage = :errorMessage WHERE jobId = :jobId")
    suspend fun completePrintJob(jobId: String, status: String, completedAt: Long, errorMessage: String? = null)
    
    @Query("DELETE FROM label_print_job WHERE createdAt < :cutoffTime")
    suspend fun deleteOldPrintJobs(cutoffTime: Long)
}
