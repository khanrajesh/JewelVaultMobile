package com.velox.jewelvault.utils

import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import java.sql.Timestamp

object TransactionUtils {
    
    /**
     * Create an outstanding debt transaction
     */
    fun createOutstandingDebt(
        customerMobile: String,
        amount: Double,
        description: String? = null,
        notes: String? = null,
        userId: String,
        storeId: String
    ): CustomerTransactionEntity {
        return CustomerTransactionEntity(
            generateId(),
            customerMobile = customerMobile,
            transactionDate = Timestamp(System.currentTimeMillis()),
            amount = amount,
            transactionType = "debit",
            category = "outstanding",
            description = description,
            notes = notes,
            userId = userId,
            storeId = storeId
        )
    }
    
    /**
     * Create an outstanding payment transaction
     */
    fun createOutstandingPayment(
        customerMobile: String,
        amount: Double,
        paymentMethod: String? = null,
        referenceNumber: String? = null,
        notes: String? = null,
        userId: String,
        storeId: String
    ): CustomerTransactionEntity {
        return CustomerTransactionEntity(
            transactionId = generateId(),
            customerMobile = customerMobile,
            transactionDate = Timestamp(System.currentTimeMillis()),
            amount = amount,
            transactionType = "credit",
            category = "outstanding",
            paymentMethod = paymentMethod,
            referenceNumber = referenceNumber,
            notes = notes,
            userId = userId,
            storeId = storeId
        )
    }
    
    /**
     * Create a khata book payment transaction
     */
    fun createKhataPayment(
        customerMobile: String,
        khataBookId: String,
        monthNumber: Int,
        amount: Double,
        notes: String? = null,
        userId: String,
        storeId: String
    ): CustomerTransactionEntity {
        return CustomerTransactionEntity(
            generateId(),
            customerMobile = customerMobile,
            transactionDate = Timestamp(System.currentTimeMillis()),
            amount = amount,
            transactionType = "khata_payment",
            category = "khata_book",
            khataBookId = khataBookId,
            monthNumber = monthNumber,
            notes = notes,
            userId = userId,
            storeId = storeId
        )
    }
    
    /**
     * Create a khata book debit transaction (when khata book is created)
     */
    fun createKhataDebit(
        customerMobile: String,
        khataBookId: String,
        amount: Double,
        notes: String? = null,
        userId: String,
        storeId: String
    ): CustomerTransactionEntity {
        return CustomerTransactionEntity(
            generateId(),
            customerMobile = customerMobile,
            transactionDate = Timestamp(System.currentTimeMillis()),
            amount = amount,
            transactionType = "khata_debit",
            category = "khata_book",
            khataBookId = khataBookId,
            notes = notes,
            userId = userId,
            storeId = storeId
        )
    }
    
    /**
     * Create a regular payment transaction
     */
    fun createRegularPayment(
        customerMobile: String,
        amount: Double,
        paymentMethod: String? = null,
        referenceNumber: String? = null,
        notes: String? = null,
        userId: String,
        storeId: String
    ): CustomerTransactionEntity {
        return CustomerTransactionEntity(
            generateId(),
            customerMobile = customerMobile,
            transactionDate = Timestamp(System.currentTimeMillis()),
            amount = amount,
            transactionType = "credit",
            category = "regular_payment",
            paymentMethod = paymentMethod,
            referenceNumber = referenceNumber,
            notes = notes,
            userId = userId,
            storeId = storeId
        )
    }
    
    /**
     * Create an adjustment transaction
     */
    fun createAdjustment(
        customerMobile: String,
        amount: Double,
        isDebit: Boolean,
        description: String? = null,
        notes: String? = null,
        userId: String,
        storeId: String
    ): CustomerTransactionEntity {
        return CustomerTransactionEntity(
            generateId(),
            customerMobile = customerMobile,
            transactionDate = Timestamp(System.currentTimeMillis()),
            amount = amount,
            transactionType = if (isDebit) "debit" else "credit",
            category = "adjustment",
            description = description,
            notes = notes,
            userId = userId,
            storeId = storeId
        )
    }
    
    /**
     * Get transaction type display name
     */
    fun getTransactionTypeDisplayName(transactionType: String): String {
        return when (transactionType) {
            "debit" -> "Debit"
            "credit" -> "Credit"
            "khata_payment" -> "Khata Payment"
            "khata_debit" -> "Khata Debit"
            else -> transactionType.capitalize()
        }
    }
    
    /**
     * Get category display name
     */
    fun getCategoryDisplayName(category: String): String {
        return when (category) {
            "outstanding" -> "Outstanding"
            "khata_book" -> "Khata Book"
            "regular_payment" -> "Regular Payment"
            "adjustment" -> "Adjustment"
            else -> category.capitalize()
        }
    }
    
    /**
     * Get payment method display name
     */
    fun getPaymentMethodDisplayName(paymentMethod: String?): String {
        return when (paymentMethod) {
            "cash" -> "Cash"
            "bank" -> "Bank Transfer"
            "upi" -> "UPI"
            "card" -> "Card"
            "cheque" -> "Cheque"
            else -> paymentMethod ?: "Not specified"
        }
    }
} 