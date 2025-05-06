package com.velox.jewelvault.data.roomdb

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.velox.jewelvault.data.roomdb.dao.ItemDao
import com.velox.jewelvault.data.roomdb.entity.ItemEntity


@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = true,
    autoMigrations = [

    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

}