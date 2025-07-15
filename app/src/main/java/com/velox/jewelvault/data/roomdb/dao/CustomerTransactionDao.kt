package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.CustomerTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerTransactionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CustomerTransactionEntity): Long
    
    @Update
    suspend fun updateTransaction(transaction: CustomerTransactionEntity): Int
    
    @Delete
    suspend fun deleteTransaction(transaction: CustomerTransactionEntity)
    
    // Get all transactions for a customer
    @Query("SELECT * FROM customer_transaction WHERE customerMobile = :customerMobile ORDER BY transactionDate DESC")
    fun getCustomerTransactions(customerMobile: String): Flow<List<CustomerTransactionEntity>>
    
    // Get transactions by category
    @Query("SELECT * FROM customer_transaction WHERE customerMobile = :customerMobile AND category = :category ORDER BY transactionDate DESC")
    fun getCustomerTransactionsByCategory(customerMobile: String, category: String): Flow<List<CustomerTransactionEntity>>
    
    // Get khata book related transactions
    @Query("SELECT * FROM customer_transaction WHERE customerMobile = :customerMobile AND (category = 'khata_book' OR transactionType LIKE 'khata_%') ORDER BY transactionDate DESC")
    fun getCustomerKhataTransactions(customerMobile: String): Flow<List<CustomerTransactionEntity>>
    
    // Get outstanding balance transactions
    @Query("SELECT * FROM customer_transaction WHERE customerMobile = :customerMobile AND category = 'outstanding' ORDER BY transactionDate DESC")
    fun getCustomerOutstandingTransactions(customerMobile: String): Flow<List<CustomerTransactionEntity>>
    
    // Get regular payment transactions
    @Query("SELECT * FROM customer_transaction WHERE customerMobile = :customerMobile AND category = 'regular_payment' ORDER BY transactionDate DESC")
    fun getCustomerPaymentTransactions(customerMobile: String): Flow<List<CustomerTransactionEntity>>
    
    // Calculate outstanding balance (debits - credits)
    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN transactionType IN ('debit', 'khata_debit') THEN amount
                WHEN transactionType IN ('credit', 'khata_payment') THEN -amount
                ELSE 0
            END
        ), 0) 
        FROM customer_transaction 
        WHERE customerMobile = :customerMobile AND category = 'outstanding'
    """)
    suspend fun getCustomerOutstandingBalance(customerMobile: String): Double
    
    // Calculate total outstanding amount for all customers
    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN transactionType IN ('debit', 'khata_debit') THEN amount
                WHEN transactionType IN ('credit', 'khata_payment') THEN -amount
                ELSE 0
            END
        ), 0) 
        FROM customer_transaction 
        WHERE userId = :userId AND storeId = :storeId AND category = 'outstanding'
    """)
    suspend fun getTotalOutstandingAmount(userId: Int, storeId: Int): Double
    
    // Get khata book payment transactions for a specific khata book
    @Query("SELECT * FROM customer_transaction WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment' ORDER BY monthNumber ASC")
    fun getKhataBookPayments(khataBookId: Int): Flow<List<CustomerTransactionEntity>>
    
    // Get total paid amount for a khata book
    @Query("SELECT COALESCE(SUM(amount), 0) FROM customer_transaction WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment'")
    suspend fun getKhataBookTotalPaidAmount(khataBookId: Int): Double
    
    // Get paid months count for a khata book
    @Query("SELECT COUNT(*) FROM customer_transaction WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment'")
    suspend fun getKhataBookPaidMonthsCount(khataBookId: Int): Int
    
    // Get all transactions for a store/user
    @Query("SELECT * FROM customer_transaction WHERE userId = :userId AND storeId = :storeId ORDER BY transactionDate DESC")
    fun getAllTransactions(userId: Int, storeId: Int): Flow<List<CustomerTransactionEntity>>
    
    // Get transactions by date range
    @Query("SELECT * FROM customer_transaction WHERE userId = :userId AND storeId = :storeId AND transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getTransactionsByDateRange(userId: Int, storeId: Int, startDate: Long, endDate: Long): Flow<List<CustomerTransactionEntity>>
    
    // Get transactions by type
    @Query("SELECT * FROM customer_transaction WHERE userId = :userId AND storeId = :storeId AND transactionType = :transactionType ORDER BY transactionDate DESC")
    fun getTransactionsByType(userId: Int, storeId: Int, transactionType: String): Flow<List<CustomerTransactionEntity>>
    
    // Get customer balance summary
    @Query("""
        SELECT 
            customerMobile,
            COALESCE(SUM(
                CASE 
                    WHEN transactionType IN ('debit', 'khata_debit') THEN amount
                    WHEN transactionType IN ('credit', 'khata_payment') THEN -amount
                    ELSE 0
                END
            ), 0) as balance
        FROM customer_transaction 
        WHERE userId = :userId AND storeId = :storeId AND category = 'outstanding'
        GROUP BY customerMobile
        HAVING balance > 0
        ORDER BY balance DESC
    """)
    suspend fun getCustomersWithOutstandingBalance(userId: Int, storeId: Int): List<CustomerBalanceSummary>
}

data class CustomerBalanceSummary(
    val customerMobile: String,
    val balance: Double
) 