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
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add customer_payment table
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
            
            // Create customer_khata_payment table (legacy - will be migrated in next version)
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
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add planName column to customer_khata_book table
            try {
                db.execSQL("ALTER TABLE customer_khata_book ADD COLUMN planName TEXT NOT NULL DEFAULT 'Standard Plan'")
            } catch (e: Exception) {
                // Column might already exist, ignore error
            }
            
            // Create customer_transaction table (unified transaction entity)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_transaction (
                    transactionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerMobile TEXT NOT NULL,
                    transactionDate INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    transactionType TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT,
                    referenceNumber TEXT,
                    paymentMethod TEXT,
                    khataBookId INTEGER,
                    monthNumber INTEGER,
                    notes TEXT,
                    userId INTEGER NOT NULL,
                    storeId INTEGER NOT NULL,
                    FOREIGN KEY (customerMobile) REFERENCES CustomerEntity (mobileNo) ON DELETE CASCADE,
                    FOREIGN KEY (khataBookId) REFERENCES customer_khata_book (khataBookId) ON DELETE CASCADE
                )
            """)
            
            // Create indexes for customer_transaction
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_transaction_customerMobile ON customer_transaction (customerMobile)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_transaction_khataBookId ON customer_transaction (khataBookId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_transaction_category ON customer_transaction (category)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_transaction_type ON customer_transaction (transactionType)")
            
            // Migrate existing outstanding transactions to the new unified table
            db.execSQL("""
                INSERT INTO customer_transaction (
                    customerMobile, transactionDate, amount, transactionType, category, 
                    description, notes, userId, storeId
                )
                SELECT 
                    customerMobile, transactionDate, 
                    CASE 
                        WHEN transactionType = 'payment' THEN amount
                        ELSE amount
                    END as amount,
                    CASE 
                        WHEN transactionType = 'payment' THEN 'credit'
                        WHEN transactionType = 'debt' THEN 'debit'
                        ELSE 'debit'
                    END as transactionType,
                    'outstanding' as category,
                    description, notes, userId, storeId
                FROM customer_outstanding
            """)
            
            // Migrate existing khata payments to the new unified table
            db.execSQL("""
                INSERT INTO customer_transaction (
                    customerMobile, transactionDate, amount, transactionType, category,
                    khataBookId, monthNumber, notes, userId, storeId
                )
                SELECT 
                    ckp.customerMobile, ckp.paymentDate, ckp.amount, 'khata_payment' as transactionType,
                    'khata_book' as category, ckp.khataBookId, ckp.monthNumber, ckp.notes, 
                    ckp.userId, ckp.storeId
                FROM customer_khata_payment ckp
            """)
            
            // Migrate existing regular payments to the new unified table
            db.execSQL("""
                INSERT INTO customer_transaction (
                    customerMobile, transactionDate, amount, transactionType, category,
                    paymentMethod, referenceNumber, notes, userId, storeId
                )
                SELECT 
                    customerMobile, paymentDate, amount, 'credit' as transactionType,
                    'regular_payment' as category, paymentMethod, referenceNumber, notes, 
                    userId, storeId
                FROM customer_payment
            """)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Drop old tables after successful migration to unified transaction system
            try {
                db.execSQL("DROP TABLE IF EXISTS customer_outstanding")
                db.execSQL("DROP TABLE IF EXISTS customer_payment")
                db.execSQL("DROP TABLE IF EXISTS customer_khata_payment")
            } catch (e: Exception) {
                // Tables might not exist, ignore error
            }
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add upiId column to StoreEntity table
            try {
                db.execSQL("ALTER TABLE StoreEntity ADD COLUMN upiId TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {
                // Column might already exist, ignore error
            }
        }
    }
}