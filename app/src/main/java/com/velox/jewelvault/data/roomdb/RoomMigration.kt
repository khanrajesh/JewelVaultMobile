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
                } catch (e: Exception) { }
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
            }
            
            // Create customer_transaction table (unified transaction entity)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_transaction (
                    transactionId TEXT PRIMARY KEY NOT NULL,
                    customerMobile TEXT NOT NULL,
                    transactionDate INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    transactionType TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT,
                    referenceNumber TEXT,
                    paymentMethod TEXT,
                    khataBookId TEXT,
                    monthNumber INTEGER,
                    notes TEXT,
                    userId TEXT NOT NULL,
                    storeId TEXT NOT NULL,
                    FOREIGN KEY (customerMobile) REFERENCES customer (mobileNo) ON DELETE CASCADE,
                    FOREIGN KEY (khataBookId) REFERENCES customer_khata_book (khataBookId) ON DELETE CASCADE
                )
            """)
            
            // Let Room handle index creation automatically
            
            // Migrate existing outstanding transactions to the new unified table
            db.execSQL("""
                INSERT INTO customer_transaction (
                    transactionId, customerMobile, transactionDate, amount, transactionType, category, 
                    description, notes, userId, storeId
                )
                SELECT 
                    'outstanding_' || outstandingId as transactionId,
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
                    transactionId, customerMobile, transactionDate, amount, transactionType, category,
                    khataBookId, monthNumber, notes, userId, storeId
                )
                SELECT 
                    'khata_payment_' || paymentId as transactionId,
                    ckp.customerMobile, ckp.paymentDate, ckp.amount, 'khata_payment' as transactionType,
                    'khata_book' as category, ckp.khataBookId, ckp.monthNumber, ckp.notes, 
                    ckp.userId, ckp.storeId
                FROM customer_khata_payment ckp
            """)
            
            // Migrate existing regular payments to the new unified table
            db.execSQL("""
                INSERT INTO customer_transaction (
                    transactionId, customerMobile, transactionDate, amount, transactionType, category,
                    paymentMethod, referenceNumber, notes, userId, storeId
                )
                SELECT 
                    'regular_payment_' || paymentId as transactionId,
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

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create exchange_item table for handling jewelry exchanges in orders
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS exchange_item (
                    exchangeItemId TEXT PRIMARY KEY NOT NULL,
                    orderId TEXT NOT NULL,
                    orderDate INTEGER NOT NULL,
                    customerMobile TEXT NOT NULL,
                    metalType TEXT NOT NULL,
                    purity TEXT NOT NULL,
                    grossWeight REAL NOT NULL,
                    fineWeight REAL NOT NULL,
                    price REAL NOT NULL,
                    isExchangedByMetal INTEGER NOT NULL,
                    exchangeValue REAL NOT NULL,
                    addDate INTEGER NOT NULL,
                    FOREIGN KEY (orderId) REFERENCES `order` (orderId) ON DELETE CASCADE
                )
            """)
            
            // Create index for exchange_item foreign key
            db.execSQL("CREATE INDEX IF NOT EXISTS index_exchange_item_orderId ON exchange_item (orderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_exchange_item_customerMobile ON exchange_item (customerMobile)")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Remove isActive column from customer table
            // Since SQLite doesn't support DROP COLUMN directly, we need to:
            // 1. Create a new table without the isActive column
            // 2. Copy data from old table to new table
            // 3. Drop the old table
            // 4. Rename the new table
            
            // Check if isActive column exists before attempting migration
            val cursor = db.query("PRAGMA table_info(customer)")
            var hasIsActiveColumn = false
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (columnName == "isActive") {
                    hasIsActiveColumn = true
                    break
                }
            }
            cursor.close()
            
            if (hasIsActiveColumn) {
                // Create new customer table without isActive column
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS customer_new (
                        mobileNo TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        address TEXT,
                        gstin_pan TEXT,
                        addDate INTEGER NOT NULL,
                        lastModifiedDate INTEGER NOT NULL,
                        totalItemBought INTEGER NOT NULL DEFAULT 0,
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        notes TEXT,
                        userId TEXT NOT NULL DEFAULT '',
                        storeId TEXT NOT NULL DEFAULT ''
                    )
                """)
                
                // Copy data from old table to new table (excluding isActive column)
                db.execSQL("""
                    INSERT INTO customer_new (
                        mobileNo, name, address, gstin_pan, addDate, lastModifiedDate,
                        totalItemBought, totalAmount, notes, userId, storeId
                    )
                    SELECT 
                        mobileNo, name, address, gstin_pan, addDate, lastModifiedDate,
                        totalItemBought, totalAmount, notes, userId, storeId
                    FROM customer
                """)
                
                // Drop the old table
                db.execSQL("DROP TABLE customer")
                
                // Rename the new table to the original name
                db.execSQL("ALTER TABLE customer_new RENAME TO customer")
            }
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Fix transactionId type mismatch - convert INTEGER to TEXT
            // Simple approach: drop and recreate the table
            
            // Check if customer_transaction table exists
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='customer_transaction'")
            val tableExists = cursor.count > 0
            cursor.close()
            
            if (tableExists) {
                // Drop the old table completely
                db.execSQL("DROP TABLE customer_transaction")
            }
            
            // Create new customer_transaction table with TEXT transactionId
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_transaction (
                    transactionId TEXT PRIMARY KEY NOT NULL,
                    customerMobile TEXT NOT NULL,
                    transactionDate INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    transactionType TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT,
                    referenceNumber TEXT,
                    paymentMethod TEXT,
                    khataBookId TEXT,
                    monthNumber INTEGER,
                    notes TEXT,
                    userId TEXT NOT NULL,
                    storeId TEXT NOT NULL,
                    FOREIGN KEY (customerMobile) REFERENCES customer (mobileNo) ON DELETE CASCADE,
                    FOREIGN KEY (khataBookId) REFERENCES customer_khata_book (khataBookId) ON DELETE CASCADE
                )
            """)
            
            // Let Room handle index creation automatically
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create printer table for storing printer information
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS printer (
                    address TEXT PRIMARY KEY NOT NULL,
                    name TEXT,
                    method TEXT NOT NULL,
                    isDefault INTEGER NOT NULL,
                    lastConnectedAt INTEGER,
                    supportedLanguages TEXT,
                    currentLanguage TEXT
                )
            """)
        }
    }
    
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create label_template table for storing label templates
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS label_template (
                    templateId TEXT PRIMARY KEY NOT NULL,
                    templateName TEXT NOT NULL,
                    templateType TEXT NOT NULL,
                    labelWidth REAL NOT NULL,
                    labelHeight REAL NOT NULL,
                    gapWidth REAL NOT NULL,
                    gapHeight REAL NOT NULL,
                    printDensity INTEGER NOT NULL,
                    orientation TEXT NOT NULL,
                    printLanguage TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    modifiedAt INTEGER NOT NULL,
                    isDefault INTEGER NOT NULL,
                    description TEXT
                )
            """)
            
            // Create label_element table for storing label elements (no FK per entity definition)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS label_element (
                    elementId TEXT PRIMARY KEY NOT NULL,
                    templateId TEXT NOT NULL,
                    elementType TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    width REAL NOT NULL,
                    height REAL NOT NULL,
                    rotation REAL NOT NULL,
                    zIndex INTEGER NOT NULL,
                    properties TEXT NOT NULL,
                    dataBinding TEXT,
                    isVisible INTEGER NOT NULL
                )
            """)
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE label_template ADD COLUMN printSpeed INTEGER NOT NULL DEFAULT 2")
            db.execSQL("ALTER TABLE label_template ADD COLUMN printDirection INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE label_template ADD COLUMN referenceX REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE label_template ADD COLUMN referenceY REAL NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE label_template ADD COLUMN labelPadding REAL NOT NULL DEFAULT 1.5")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Pre-order tables
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS pre_order (
                    preOrderId TEXT PRIMARY KEY NOT NULL,
                    customerMobile TEXT NOT NULL,
                    storeId TEXT NOT NULL,
                    userId TEXT NOT NULL,
                    orderDate INTEGER NOT NULL,
                    deliveryDate INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    note TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (customerMobile) REFERENCES customer (mobileNo) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_pre_order_customerMobile ON pre_order (customerMobile)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS pre_order_item (
                    preOrderItemId TEXT PRIMARY KEY NOT NULL,
                    preOrderId TEXT NOT NULL,
                    catId TEXT NOT NULL,
                    catName TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    estimatedGrossWt REAL NOT NULL,
                    estimatedPrice REAL NOT NULL,
                    addDesKey TEXT NOT NULL,
                    addDesValue TEXT NOT NULL,
                    note TEXT,
                    FOREIGN KEY (preOrderId) REFERENCES pre_order (preOrderId) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_pre_order_item_preOrderId ON pre_order_item (preOrderId)")

            // Add linkedPreOrderId to customer_transaction by recreating the table (SQLite limitation).
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='customer_transaction'")
            val tableExists = cursor.count > 0
            cursor.close()

            if (tableExists) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS customer_transaction_new (
                        transactionId TEXT PRIMARY KEY NOT NULL,
                        customerMobile TEXT NOT NULL,
                        transactionDate INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        transactionType TEXT NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT,
                        referenceNumber TEXT,
                        paymentMethod TEXT,
                        khataBookId TEXT,
                        monthNumber INTEGER,
                        notes TEXT,
                        userId TEXT NOT NULL,
                        storeId TEXT NOT NULL,
                        linkedPreOrderId TEXT,
                        FOREIGN KEY (customerMobile) REFERENCES customer (mobileNo) ON DELETE CASCADE,
                        FOREIGN KEY (khataBookId) REFERENCES customer_khata_book (khataBookId) ON DELETE CASCADE,
                        FOREIGN KEY (linkedPreOrderId) REFERENCES pre_order (preOrderId) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO customer_transaction_new (
                        transactionId,
                        customerMobile,
                        transactionDate,
                        amount,
                        transactionType,
                        category,
                        description,
                        referenceNumber,
                        paymentMethod,
                        khataBookId,
                        monthNumber,
                        notes,
                        userId,
                        storeId,
                        linkedPreOrderId
                    )
                    SELECT
                        transactionId,
                        customerMobile,
                        transactionDate,
                        amount,
                        transactionType,
                        category,
                        description,
                        referenceNumber,
                        paymentMethod,
                        khataBookId,
                        monthNumber,
                        notes,
                        userId,
                        storeId,
                        NULL AS linkedPreOrderId
                    FROM customer_transaction
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE customer_transaction")
                db.execSQL("ALTER TABLE customer_transaction_new RENAME TO customer_transaction")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_transaction_linkedPreOrderId ON customer_transaction (linkedPreOrderId)")
            } else {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS customer_transaction (
                        transactionId TEXT PRIMARY KEY NOT NULL,
                        customerMobile TEXT NOT NULL,
                        transactionDate INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        transactionType TEXT NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT,
                        referenceNumber TEXT,
                        paymentMethod TEXT,
                        khataBookId TEXT,
                        monthNumber INTEGER,
                        notes TEXT,
                        userId TEXT NOT NULL,
                        storeId TEXT NOT NULL,
                        linkedPreOrderId TEXT,
                        FOREIGN KEY (customerMobile) REFERENCES customer (mobileNo) ON DELETE CASCADE,
                        FOREIGN KEY (khataBookId) REFERENCES customer_khata_book (khataBookId) ON DELETE CASCADE,
                        FOREIGN KEY (linkedPreOrderId) REFERENCES pre_order (preOrderId) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_transaction_linkedPreOrderId ON customer_transaction (linkedPreOrderId)")
            }
        }
    }
}
