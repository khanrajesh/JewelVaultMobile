package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    // Insert new customer, replace on conflict
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    // Insert multiple customers
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    // Update existing customer
    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    // Delete customer
    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    // Get all customers
    @Query("SELECT * FROM CustomerEntity ORDER BY addDate DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    // Get a customer by mobile number
    @Query("SELECT * FROM CustomerEntity WHERE mobileNo = :mobileNo LIMIT 1")
    suspend fun getCustomerByMobile(mobileNo: String): CustomerEntity?

    // Delete all customers
    @Query("DELETE FROM CustomerEntity")
    suspend fun deleteAllCustomers()
}
