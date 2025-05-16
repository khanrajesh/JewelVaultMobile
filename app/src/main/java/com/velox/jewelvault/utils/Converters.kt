package com.velox.jewelvault.utils

import androidx.room.TypeConverter
import java.sql.Timestamp


/*
* Don't add anything any more here it is used for SQL Migration
* */
class Converters {

    // Convert Timestamp to Long (milliseconds)
    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp?): Long? {
        return timestamp?.time
    }

    // Convert Long (milliseconds) back to Timestamp
    @TypeConverter
    fun toTimestamp(time: Long?): Timestamp? {
        return time?.let { Timestamp(it) }
    }
}