package com.velox.jewelvault.data.roomdb.entity.customer

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import java.sql.Timestamp

@Entity(
    tableName = TableNames.CUSTOMER_TRANSACTION,
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["mobileNo"],
            childColumns = ["customerMobile"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomerTransactionEntity(
    @PrimaryKey
    val transactionId: String,
    val customerMobile: String,
    val transactionDate: Timestamp,
    val amount: Double, // Always positive, use transactionType to determine debit/credit
    val transactionType: String, // "debit", "credit", "khata_payment", "khata_debit"
    val category: String, // "outstanding", "khata_book", "regular_payment", "adjustment"
    val description: String? = null,
    val referenceNumber: String? = null, // Receipt number, transaction ID, etc.
    val paymentMethod: String? = null, // "cash", "bank", "upi", etc.
    val khataBookId: String? = null, // Reference to khata book if this is khata-related
    val monthNumber: Int? = null, // For khata payments, which month is being paid
    val notes: String? = null,
    val userId: String,
    val storeId: String
) {
    // Helper properties
    val isDebit: Boolean
        get() = transactionType == "debit" || transactionType == "khata_debit"
    
    val isCredit: Boolean
        get() = transactionType == "credit" || transactionType == "khata_payment"
    
    val isKhataRelated: Boolean
        get() = transactionType == "khata_payment" || transactionType == "khata_debit" || category == "khata_book"
    
    val isOutstandingRelated: Boolean
        get() = category == "outstanding"
    
    val isRegularPayment: Boolean
        get() = category == "regular_payment"
} 