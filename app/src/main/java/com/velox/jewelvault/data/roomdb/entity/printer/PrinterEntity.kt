package com.velox.jewelvault.data.roomdb.entity.printer

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(tableName = TableNames.PRINTER)
data class PrinterEntity(
    @PrimaryKey val address: String,
    val name: String?,
    val method: String, // "RFCOMM" | "GATT" | "A2DP" | "HEADSET" | "HID" | "BONDED"
    val isDefault: Boolean = false,
    val lastConnectedAt: Long? = null,
    val supportedLanguages: String? = null, // JSON string of List<String>
    val currentLanguage: String? = null
)
