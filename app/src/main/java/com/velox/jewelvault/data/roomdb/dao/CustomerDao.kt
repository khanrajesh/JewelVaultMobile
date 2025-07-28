package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    // Insert new customer, replace on conflict
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    // Insert multiple customers
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    // Update existing customer
    @Update
    suspend fun updateCustomer(customer: CustomerEntity): Int

    // Delete customer
    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    // Get all customers
    @Query("SELECT * FROM customer_entity ORDER BY addDate DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    // Get a customer by mobile number
    @Query("SELECT * FROM customer_entity WHERE mobileNo = :mobileNo LIMIT 1")
    suspend fun getCustomerByMobile(mobileNo: String): CustomerEntity?

    // Delete all customers
    @Query("DELETE FROM customer_entity")
    suspend fun deleteAllCustomers()

    // Search customers by name or mobile
    @Query("""
        SELECT * FROM customer_entity 
        WHERE (name LIKE '%' || :query || '%' OR mobileNo LIKE '%' || :query || '%')
        AND userId = :userId AND storeId = :storeId
        ORDER BY name ASC
    """)
    fun searchCustomers(query: String, userId: Int, storeId: Int): Flow<List<CustomerEntity>>

    // Get customers by user and store
    @Query("""
        SELECT * FROM customer_entity 
        WHERE userId = :userId AND storeId = :storeId
        ORDER BY addDate DESC
    """)
    fun getCustomersByUserAndStore(userId: Int, storeId: Int): Flow<List<CustomerEntity>>
    
    // Get all customers (suspend version for backup)
    @Query("SELECT * FROM customer_entity ORDER BY addDate DESC")
    suspend fun getAllCustomersList(): List<CustomerEntity>
}
