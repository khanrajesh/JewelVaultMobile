package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.velox.jewelvault.data.roomdb.TableNames
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerKhataBookPlanEntity

@Dao
interface CustomerKhataBookPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: CustomerKhataBookPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: CustomerKhataBookPlanEntity): Int

    @Delete
    suspend fun deletePlan(plan: CustomerKhataBookPlanEntity)

    @Query("DELETE FROM ${TableNames.CUSTOMER_KHATA_BOOK_PLAN} WHERE planId = :planId")
    suspend fun deletePlanById(planId: String)

    @Query(
        """
        SELECT * FROM ${TableNames.CUSTOMER_KHATA_BOOK_PLAN}
        WHERE userId = :userId AND storeId = :storeId
        ORDER BY updatedAt DESC
        """
    )
    suspend fun getPlansByUserAndStore(
        userId: String,
        storeId: String
    ): List<CustomerKhataBookPlanEntity>

    @Query("SELECT * FROM ${TableNames.CUSTOMER_KHATA_BOOK_PLAN}")
    suspend fun getAllPlansList(): List<CustomerKhataBookPlanEntity>

    @Query("SELECT * FROM ${TableNames.CUSTOMER_KHATA_BOOK_PLAN} WHERE planId = :planId")
    suspend fun getPlanById(planId: String): CustomerKhataBookPlanEntity?
}
