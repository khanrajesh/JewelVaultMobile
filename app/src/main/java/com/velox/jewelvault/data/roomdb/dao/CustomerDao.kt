package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.TableNames
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
    @Query("SELECT * FROM ${TableNames.CUSTOMER} ORDER BY addDate DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    // Get a customer by mobile number
    @Query("SELECT * FROM ${TableNames.CUSTOMER} WHERE mobileNo = :mobileNo LIMIT 1")
    suspend fun getCustomerByMobile(mobileNo: String): CustomerEntity?

    // Delete all customers
    @Query("DELETE FROM ${TableNames.CUSTOMER}")
    suspend fun deleteAllCustomers()

    // Search customers by name or mobile
    @Query("""
        SELECT * FROM ${TableNames.CUSTOMER} 
        WHERE (name LIKE '%' || :query || '%' OR mobileNo LIKE '%' || :query || '%')
        AND userId = :userId AND storeId = :storeId
        ORDER BY name ASC
    """)
    fun searchCustomers(query: String, userId: String, storeId: Int): Flow<List<CustomerEntity>>

    // Get customers by user and store
    @Query("""
        SELECT * FROM ${TableNames.CUSTOMER} 
        WHERE userId = :userId AND storeId = :storeId
        ORDER BY addDate DESC
    """)
    fun getCustomersByUserAndStore(userId: String, storeId: String): Flow<List<CustomerEntity>>
    
    // Get all customers (suspend version for backup)
    @Query("SELECT * FROM ${TableNames.CUSTOMER} ORDER BY addDate DESC")
    suspend fun getAllCustomersList(): List<CustomerEntity>
}
