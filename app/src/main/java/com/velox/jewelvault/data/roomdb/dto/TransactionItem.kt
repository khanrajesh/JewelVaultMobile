package com.velox.jewelvault.data.roomdb.dto

import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import java.sql.Timestamp

data class TransactionItem(
    val title: String,
    val amount: Double,
    val date: Timestamp,
    val isOutstanding: Boolean,
    val transactionType: String,
    val category: String
) {
    companion object {
        fun fromTransaction(transaction: CustomerTransactionEntity): TransactionItem {
            val title = when (transaction.transactionType) {
                "debit" -> "Debit - ${transaction.description ?: "No description"}"
                "credit" -> "Credit - ${transaction.paymentMethod ?: "Payment"}"
                "khata_payment" -> "Khata Payment - Month ${transaction.monthNumber}"
                "khata_debit" -> "Khata Debit - ${transaction.notes ?: "Khata Book"}"
                else -> "${transaction.transactionType.uppercase()} - ${transaction.description ?: "Transaction"}"
            }

            return TransactionItem(
                title = title,
                amount = transaction.amount,
                date = transaction.transactionDate,
                isOutstanding = transaction.isDebit,
                transactionType = transaction.transactionType,
                category = transaction.category
            )
        }
    }
}