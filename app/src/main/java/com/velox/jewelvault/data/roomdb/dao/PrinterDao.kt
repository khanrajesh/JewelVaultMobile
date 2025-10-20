package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.printer.PrinterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterDao {
    
    @Query("SELECT * FROM printer ORDER BY isDefault DESC, lastConnectedAt DESC")
    fun getAllPrinters(): Flow<List<PrinterEntity>>
    
    @Query("SELECT * FROM printer WHERE address = :address")
    suspend fun getPrinterByAddress(address: String): PrinterEntity?
    
    @Query("SELECT * FROM printer WHERE isDefault = 1 LIMIT 1")
    fun getDefaultPrinter(): Flow<PrinterEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrinter(printer: PrinterEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrinters(printers: List<PrinterEntity>)
    
    @Update
    suspend fun updatePrinter(printer: PrinterEntity)
    
    @Delete
    suspend fun deletePrinter(printer: PrinterEntity)
    
    @Query("DELETE FROM printer WHERE address = :address")
    suspend fun deletePrinterByAddress(address: String)
    
    @Query("UPDATE printer SET isDefault = 0")
    suspend fun clearAllDefaults()
    
    @Query("UPDATE printer SET isDefault = 1 WHERE address = :address")
    suspend fun setDefaultPrinter(address: String)
    
    @Query("UPDATE printer SET lastConnectedAt = :timestamp WHERE address = :address")
    suspend fun updateLastConnectedAt(address: String, timestamp: Long)
    
    @Query("UPDATE printer SET supportedLanguages = :languages WHERE address = :address")
    suspend fun updateSupportedLanguages(address: String, languages: String?)
    
    @Query("UPDATE printer SET currentLanguage = :language WHERE address = :address")
    suspend fun updateCurrentLanguage(address: String, language: String?)
}
