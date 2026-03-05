package com.velox.jewelvault.data.roomdb

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate8To9_preservesCustomerTransactionData() {
        val dbName = "migration-test-8-9"

        helper.createDatabase(dbName, 8).apply {
            execSQL(
                """
                INSERT INTO customer (
                    mobileNo, name, address, gstin_pan, addDate, lastModifiedDate,
                    totalItemBought, totalAmount, notes, userId, storeId
                ) VALUES (
                    '9999999999', 'Test Customer', '', '', 1700000000000, 1700000000000,
                    0, 0.0, '', 'user-1', 'store-1'
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO customer_transaction (
                    transactionId, customerMobile, transactionDate, amount, transactionType, category,
                    description, referenceNumber, paymentMethod, khataBookId, monthNumber, notes, userId, storeId
                ) VALUES (
                    'tx-1', '9999999999', 1700000000000, 100.0, 'credit', 'regular_payment',
                    'desc', NULL, NULL, NULL, NULL, 'notes', 'user-1', 'store-1'
                )
                """.trimIndent()
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            9,
            true,
            RoomMigration.MIGRATION_8_9
        )

        val cursor = migratedDb.query(
            "SELECT COUNT(*) FROM customer_transaction WHERE transactionId = 'tx-1' AND customerMobile = '9999999999'"
        )
        assertTrue(cursor.moveToFirst())
        assertEquals(1, cursor.getInt(0))
        cursor.close()
        migratedDb.close()
    }

    @Test
    fun migrate16To17_addsJurisdictionWithDefaultValue() {
        val dbName = "migration-test-16-17"

        helper.createDatabase(dbName, 16).apply {
            execSQL(
                """
                INSERT INTO store (
                    storeId, userId, proprietor, name, email, phone, address,
                    registrationNo, gstinNo, panNo, image, invoiceNo, upiId, lastUpdated
                ) VALUES (
                    'store-1', 'user-1', 'Owner', 'Test Store', 'test@example.com', '9999999999', 'Address',
                    'REG123', 'GST123', 'PAN123', '', 1, 'upi@test', 1700000000000
                )
                """.trimIndent()
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            17,
            true,
            RoomMigration.MIGRATION_16_17
        )

        val cursor = migratedDb.query("SELECT jurisdiction FROM store WHERE storeId = 'store-1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("", cursor.getString(0))
        cursor.close()
        migratedDb.close()
    }

    @Test
    fun migrate6To17_fullChainCompletes() {
        val dbName = "migration-test-6-17"

        helper.createDatabase(dbName, 6).close()

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            17,
            true,
            *allMigrations()
        )
        migratedDb.close()
    }

    private fun allMigrations() = arrayOf(
        RoomMigration.MIGRATION_1_2,
        RoomMigration.MIGRATION_2_3,
        RoomMigration.MIGRATION_3_4,
        RoomMigration.MIGRATION_4_5,
        RoomMigration.MIGRATION_5_6,
        RoomMigration.MIGRATION_6_7,
        RoomMigration.MIGRATION_7_8,
        RoomMigration.MIGRATION_8_9,
        RoomMigration.MIGRATION_9_10,
        RoomMigration.MIGRATION_10_11,
        RoomMigration.MIGRATION_11_12,
        RoomMigration.MIGRATION_12_13,
        RoomMigration.MIGRATION_13_14,
        RoomMigration.MIGRATION_14_15,
        RoomMigration.MIGRATION_15_16,
        RoomMigration.MIGRATION_16_17
    )
}
