package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.dto.CustomerBalanceSummary
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import com.velox.jewelvault.data.roomdb.TableNames
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
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE customerMobile = :customerMobile ORDER BY transactionDate DESC")
    fun getCustomerTransactions(customerMobile: String): Flow<List<CustomerTransactionEntity>>
    
    // Get transactions by category
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE customerMobile = :customerMobile AND category = :category ORDER BY transactionDate DESC")
    fun getCustomerTransactionsByCategory(customerMobile: String, category: String): Flow<List<CustomerTransactionEntity>>
    
    // Get khata book related transactions
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE customerMobile = :customerMobile AND (category = 'khata_book' OR transactionType LIKE 'khata_%') ORDER BY transactionDate DESC")
    fun getCustomerKhataTransactions(customerMobile: String): Flow<List<CustomerTransactionEntity>>
    
    // Get outstanding balance transactions
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE customerMobile = :customerMobile AND category = 'outstanding' ORDER BY transactionDate DESC")
    fun getCustomerOutstandingTransactions(customerMobile: String): Flow<List<CustomerTransactionEntity>>
    
    // Get regular payment transactions
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE customerMobile = :customerMobile AND category = 'regular_payment' ORDER BY transactionDate DESC")
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
        FROM ${TableNames.CUSTOMER_TRANSACTION} 
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
        FROM ${TableNames.CUSTOMER_TRANSACTION} 
        WHERE userId = :userId AND storeId = :storeId AND category = 'outstanding'
    """)
    suspend fun getTotalOutstandingAmount(userId: String, storeId: String): Double
    
    // Get khata book payment transactions for a specific khata book
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment' ORDER BY monthNumber ASC")
    fun getKhataBookPayments(khataBookId: String): Flow<List<CustomerTransactionEntity>>
    
    // Get total paid amount for a khata book
    @Query("SELECT COALESCE(SUM(amount), 0) FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment'")
    suspend fun getKhataBookTotalPaidAmount(khataBookId: String): Double
    
    // Get paid months count for a khata book
    @Query("SELECT COUNT(*) FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment'")
    suspend fun getKhataBookPaidMonthsCount(khataBookId: String): Int
    
    // Get all transactions for a store/user
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE userId = :userId AND storeId = :storeId ORDER BY transactionDate DESC")
    fun getAllTransactions(userId: String, storeId: String): Flow<List<CustomerTransactionEntity>>
    
    // Get transactions by date range
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE userId = :userId AND storeId = :storeId AND transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getTransactionsByDateRange(userId: String, storeId: String, startDate: Long, endDate: Long): Flow<List<CustomerTransactionEntity>>
    
    // Get transactions by type
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION} WHERE userId = :userId AND storeId = :storeId AND transactionType = :transactionType ORDER BY transactionDate DESC")
    fun getTransactionsByType(userId: String, storeId: String, transactionType: String): Flow<List<CustomerTransactionEntity>>
    
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
        FROM ${TableNames.CUSTOMER_TRANSACTION} 
        WHERE userId = :userId AND storeId = :storeId AND category = 'outstanding'
        GROUP BY customerMobile
        HAVING balance > 0
        ORDER BY balance DESC
    """)
    suspend fun getCustomersWithOutstandingBalance(userId: String, storeId: String): List<CustomerBalanceSummary>
    
    @Query("SELECT * FROM ${TableNames.CUSTOMER_TRANSACTION}")
    suspend fun getAllTransactions(): List<CustomerTransactionEntity>
}

