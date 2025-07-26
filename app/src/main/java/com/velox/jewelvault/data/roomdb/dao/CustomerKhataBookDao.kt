package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.dto.KhataBookSummary
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerKhataBookEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerKhataBookDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataBook(khataBook: CustomerKhataBookEntity): Long
    
    @Update
    suspend fun updateKhataBook(khataBook: CustomerKhataBookEntity): Int
    
    @Delete
    suspend fun deleteKhataBook(khataBook: CustomerKhataBookEntity)
    
    // Get all active khata books for a customer (now supports multiple)
    @Query("SELECT * FROM customer_khata_book WHERE customerMobile = :customerMobile AND status = 'active' ORDER BY startDate DESC")
    suspend fun getActiveKhataBooks(customerMobile: String): List<CustomerKhataBookEntity>
    
    // Get first active khata book (for backward compatibility)
    @Query("SELECT * FROM customer_khata_book WHERE customerMobile = :customerMobile AND status = 'active' LIMIT 1")
    suspend fun getFirstActiveKhataBook(customerMobile: String): CustomerKhataBookEntity?
    
    // Get all active khata books for store/user
    @Query("SELECT * FROM customer_khata_book WHERE userId = :userId AND storeId = :storeId AND status = 'active' ORDER BY startDate DESC")
    suspend fun getActiveKhataBooks(userId: Int, storeId: Int): List<CustomerKhataBookEntity>
    
    @Query("SELECT * FROM customer_khata_book WHERE userId = :userId AND storeId = :storeId AND status = 'completed'")
    suspend fun getMaturedKhataBooks(userId: Int, storeId: Int): List<CustomerKhataBookEntity>
    
    @Query("SELECT * FROM customer_khata_book WHERE userId = :userId AND storeId = :storeId")
    fun getAllKhataBooks(userId: Int, storeId: Int): Flow<List<CustomerKhataBookEntity>>
    
    @Query("SELECT * FROM customer_khata_book WHERE khataBookId = :khataBookId")
    suspend fun getKhataBookById(khataBookId: Int): CustomerKhataBookEntity?
    
    // Get khata books by plan name
    @Query("SELECT * FROM customer_khata_book WHERE customerMobile = :customerMobile AND planName = :planName AND status = 'active'")
    suspend fun getKhataBooksByPlan(customerMobile: String, planName: String): List<CustomerKhataBookEntity>
    
    // Get all khata books for a customer
    @Query("SELECT * FROM customer_khata_book WHERE customerMobile = :customerMobile ORDER BY startDate DESC")
    fun getCustomerKhataBooks(customerMobile: String): Flow<List<CustomerKhataBookEntity>>
    
    // Legacy methods for backward compatibility (will be removed in future)
    @Deprecated("Use getFirstActiveKhataBook instead")
    @Query("SELECT * FROM customer_khata_book WHERE customerMobile = :customerMobile AND status = 'active' LIMIT 1")
    suspend fun getActiveKhataBook(customerMobile: String): CustomerKhataBookEntity?
    
    // Updated queries using the new unified transaction system
    @Query("SELECT * FROM customer_transaction WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment' ORDER BY monthNumber ASC")
    fun getKhataBookPayments(khataBookId: Int): Flow<List<CustomerTransactionEntity>>
    
    @Query("SELECT * FROM customer_transaction WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment'")
    suspend fun getPaidMonths(khataBookId: Int): List<CustomerTransactionEntity>
    
    @Query("SELECT * FROM customer_transaction WHERE customerMobile = :customerMobile AND transactionType = 'khata_payment' ORDER BY transactionDate DESC")
    fun getCustomerKhataPayments(customerMobile: String): Flow<List<CustomerTransactionEntity>>
    
    @Query("SELECT SUM(amount) FROM customer_transaction WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment'")
    suspend fun getTotalPaidAmount(khataBookId: Int): Double?
    
    @Query("SELECT COUNT(*) FROM customer_transaction WHERE khataBookId = :khataBookId AND transactionType = 'khata_payment'")
    suspend fun getPaidMonthsCount(khataBookId: Int): Int
    
    @Query("""
        SELECT 
            kb.khataBookId,
            kb.customerMobile,
            kb.planName,
            kb.monthlyAmount,
            kb.totalMonths,
            kb.status,
            (kb.totalMonths - COALESCE(payment_counts.paid_count, 0)) as remainingMonths,
            (kb.totalMonths - COALESCE(payment_counts.paid_count, 0)) * kb.monthlyAmount as remainingAmount
        FROM customer_khata_book kb
        LEFT JOIN (
            SELECT khataBookId, COUNT(*) as paid_count
            FROM customer_transaction
            WHERE transactionType = 'khata_payment'
            GROUP BY khataBookId
        ) payment_counts ON kb.khataBookId = payment_counts.khataBookId
        WHERE kb.userId = :userId AND kb.storeId = :storeId
    """)
    suspend fun getKhataBookSummaries(userId: Int, storeId: Int): List<KhataBookSummary>
}

