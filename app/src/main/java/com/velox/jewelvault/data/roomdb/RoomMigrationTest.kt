package com.velox.jewelvault.data.roomdb


import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException


@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Create DB with version 1
        val db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO Product (productId, name) VALUES (1, 'Old Product')")
            execSQL("INSERT INTO `Order` (orderId, productId, quantity) VALUES (1, 1, 5)")
            close()
        }

        // Migrate to version 2
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(RoomMigration.MIGRATION_1_2)
            .build()
            .also {
                it.openHelper.writableDatabase // trigger the migration
            }
    }
}
