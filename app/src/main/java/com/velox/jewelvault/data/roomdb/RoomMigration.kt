package com.velox.jewelvault.data.roomdb

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RoomMigration {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add new columns with default values
            // todo change with actual sql queries
            db.execSQL("ALTER TABLE Product ADD COLUMN price REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `Order` ADD COLUMN discount REAL NOT NULL DEFAULT 0.0")
        }
    }
}