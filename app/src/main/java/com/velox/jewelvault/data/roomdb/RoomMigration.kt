package com.velox.jewelvault.data.roomdb

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RoomMigration {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Check if CustomerEntity table exists before adding columns
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='CustomerEntity'")
            val tableExists = cursor.count > 0
            cursor.close()
            
            if (tableExists) {
                // Add userId and storeId columns to CustomerEntity if they don't exist
                try {
                    db.execSQL("ALTER TABLE CustomerEntity ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist, ignore error
                }
                
                try {
                    db.execSQL("ALTER TABLE CustomerEntity ADD COLUMN storeId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist, ignore error
                }
            }
            
            // Create customer_outstanding table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_outstanding (
                    outstandingId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerMobile TEXT NOT NULL,
                    amount REAL NOT NULL,
                    transactionType TEXT NOT NULL,
                    transactionDate INTEGER NOT NULL,
                    description TEXT,
                    notes TEXT,
                    userId INTEGER NOT NULL,
                    storeId INTEGER NOT NULL,
                    FOREIGN KEY (customerMobile) REFERENCES CustomerEntity (mobileNo) ON DELETE CASCADE
                )
            """)
            
            // Create index for customer_outstanding foreign key
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_outstanding_customerMobile ON customer_outstanding (customerMobile)")
            
            // Create customer_khata_book table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_khata_book (
                    khataBookId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerMobile TEXT NOT NULL,
                    startDate INTEGER NOT NULL,
                    endDate INTEGER NOT NULL,
                    monthlyAmount REAL NOT NULL,
                    totalMonths INTEGER NOT NULL,
                    totalAmount REAL NOT NULL,
                    status TEXT NOT NULL,
                    notes TEXT,
                    userId INTEGER NOT NULL,
                    storeId INTEGER NOT NULL,
                    FOREIGN KEY (customerMobile) REFERENCES CustomerEntity (mobileNo) ON DELETE CASCADE
                )
            """)
            
            // Create index for customer_khata_book foreign key
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_khata_book_customerMobile ON customer_khata_book (customerMobile)")
            
            // Create customer_khata_payment table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_khata_payment (
                    paymentId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    khataBookId INTEGER NOT NULL,
                    customerMobile TEXT NOT NULL,
                    monthNumber INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    paymentDate INTEGER NOT NULL,
                    paymentType TEXT NOT NULL,
                    notes TEXT,
                    userId INTEGER NOT NULL,
                    storeId INTEGER NOT NULL,
                    FOREIGN KEY (khataBookId) REFERENCES customer_khata_book (khataBookId) ON DELETE CASCADE,
                    FOREIGN KEY (customerMobile) REFERENCES CustomerEntity (mobileNo) ON DELETE CASCADE
                )
            """)
            
            // Create indexes for customer_khata_payment foreign keys
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_khata_payment_khataBookId ON customer_khata_payment (khataBookId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_khata_payment_customerMobile ON customer_khata_payment (customerMobile)")
            
            // Create customer_payment table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_payment (
                    paymentId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerMobile TEXT NOT NULL,
                    paymentDate INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    paymentType TEXT NOT NULL,
                    paymentMethod TEXT,
                    referenceNumber TEXT,
                    notes TEXT,
                    userId INTEGER NOT NULL,
                    storeId INTEGER NOT NULL,
                    FOREIGN KEY (customerMobile) REFERENCES CustomerEntity (mobileNo) ON DELETE CASCADE
                )
            """)
            
            // Create index for customer_payment foreign key
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payment_customerMobile ON customer_payment (customerMobile)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add missing columns to CustomerEntity table
            try {
                db.execSQL("ALTER TABLE CustomerEntity ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            } catch (e: Exception) {
                // Column might already exist, ignore error
            }
            
            try {
                db.execSQL("ALTER TABLE CustomerEntity ADD COLUMN notes TEXT")
            } catch (e: Exception) {
                // Column might already exist, ignore error
            }

            // Fix customer_payment table foreign key issue using temporary table approach
            // Check if customer_payment table exists
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='customer_payment'")
            val tableExists = cursor.count > 0
            cursor.close()
            
            if (tableExists) {
                // Create temporary table with proper structure
                db.execSQL("""
                    CREATE TABLE customer_payment_temp (
                        paymentId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        customerMobile TEXT NOT NULL,
                        paymentDate INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        paymentType TEXT NOT NULL,
                        paymentMethod TEXT,
                        referenceNumber TEXT,
                        notes TEXT,
                        userId INTEGER NOT NULL,
                        storeId INTEGER NOT NULL
                    )
                """)
                
                // Copy data from old table to temporary table
                db.execSQL("""
                    INSERT INTO customer_payment_temp 
                    SELECT paymentId, customerMobile, paymentDate, amount, paymentType, 
                           paymentMethod, referenceNumber, notes, userId, storeId 
                    FROM customer_payment
                """)
                
                // Drop old table
                db.execSQL("DROP TABLE customer_payment")
                
                // Rename temporary table to final table
                db.execSQL("ALTER TABLE customer_payment_temp RENAME TO customer_payment")
                
                // Add foreign key constraint
                db.execSQL("""
                    CREATE TABLE customer_payment_new (
                        paymentId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        customerMobile TEXT NOT NULL,
                        paymentDate INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        paymentType TEXT NOT NULL,
                        paymentMethod TEXT,
                        referenceNumber TEXT,
                        notes TEXT,
                        userId INTEGER NOT NULL,
                        storeId INTEGER NOT NULL,
                        FOREIGN KEY (customerMobile) REFERENCES CustomerEntity (mobileNo) ON DELETE CASCADE
                    )
                """)
                
                // Copy data to new table with foreign key
                db.execSQL("""
                    INSERT INTO customer_payment_new 
                    SELECT paymentId, customerMobile, paymentDate, amount, paymentType, 
                           paymentMethod, referenceNumber, notes, userId, storeId 
                    FROM customer_payment
                """)
                
                // Drop old table and rename new one
                db.execSQL("DROP TABLE customer_payment")
                db.execSQL("ALTER TABLE customer_payment_new RENAME TO customer_payment")
                
                // Create index for customer_payment foreign key
                db.execSQL("CREATE INDEX index_customer_payment_customerMobile ON customer_payment (customerMobile)")
            }
        }
    }
}