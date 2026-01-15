package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.velox.jewelvault.data.roomdb.TableNames
import com.velox.jewelvault.data.roomdb.dto.PreOrderSummary
import com.velox.jewelvault.data.roomdb.entity.preorder.PreOrderEntity
import com.velox.jewelvault.data.roomdb.entity.preorder.PreOrderItemEntity
import kotlinx.coroutines.flow.Flow
import java.sql.Timestamp

@Dao
interface PreOrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreOrder(preOrder: PreOrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreOrderItems(items: List<PreOrderItemEntity>): List<Long>

    @Update
    suspend fun updatePreOrder(preOrder: PreOrderEntity): Int

    @Update
    suspend fun updatePreOrderItem(item: PreOrderItemEntity): Int

    @Query("UPDATE `${TableNames.PRE_ORDER}` SET status = :status, updatedAt = :updatedAt WHERE preOrderId = :preOrderId")
    suspend fun updatePreOrderStatus(preOrderId: String, status: String, updatedAt: Timestamp): Int

    @Query("DELETE FROM `${TableNames.PRE_ORDER}` WHERE preOrderId = :preOrderId")
    suspend fun deletePreOrderById(preOrderId: String): Int

    @Query("DELETE FROM `${TableNames.PRE_ORDER_ITEM}` WHERE preOrderItemId = :preOrderItemId")
    suspend fun deletePreOrderItemById(preOrderItemId: String): Int

    @Query("SELECT * FROM `${TableNames.PRE_ORDER}` WHERE preOrderId = :preOrderId")
    fun observePreOrder(preOrderId: String): Flow<PreOrderEntity?>

    @Query("SELECT * FROM `${TableNames.PRE_ORDER_ITEM}` WHERE preOrderId = :preOrderId")
    fun observePreOrderItems(preOrderId: String): Flow<List<PreOrderItemEntity>>

    @Query(
        """
        SELECT 
            po.preOrderId AS preOrderId,
            po.orderDate AS orderDate,
            po.deliveryDate AS deliveryDate,
            po.status AS status,
            po.customerMobile AS customerMobile,
            c.name AS customerName,
            (
                SELECT GROUP_CONCAT(DISTINCT catName)
                FROM `${TableNames.PRE_ORDER_ITEM}`
                WHERE preOrderId = po.preOrderId
            ) AS categories,
            (
                SELECT COALESCE(SUM(quantity), 0)
                FROM `${TableNames.PRE_ORDER_ITEM}`
                WHERE preOrderId = po.preOrderId
            ) AS totalQuantity,
            (
                SELECT COALESCE(SUM(estimatedGrossWt * quantity), 0)
                FROM `${TableNames.PRE_ORDER_ITEM}`
                WHERE preOrderId = po.preOrderId
            ) AS estimatedWeight,
            (
                SELECT COALESCE(SUM(estimatedPrice * quantity), 0)
                FROM `${TableNames.PRE_ORDER_ITEM}`
                WHERE preOrderId = po.preOrderId
            ) AS estimatedPrice,
            (
                SELECT COALESCE(SUM(
                    CASE 
                        WHEN transactionType = 'credit' THEN amount
                        WHEN transactionType = 'debit' THEN -amount
                        ELSE 0
                    END
                ), 0)
                FROM `${TableNames.CUSTOMER_TRANSACTION}`
                WHERE linkedPreOrderId = po.preOrderId
            ) AS advanceAmount
        FROM `${TableNames.PRE_ORDER}` AS po
        LEFT JOIN `${TableNames.CUSTOMER}` AS c ON c.mobileNo = po.customerMobile
        ORDER BY po.deliveryDate ASC, po.orderDate DESC
        """
    )
    fun observePreOrderSummaries(): Flow<List<PreOrderSummary>>

    @Query(
        """
        SELECT 
            po.preOrderId AS preOrderId,
            po.orderDate AS orderDate,
            po.deliveryDate AS deliveryDate,
            po.status AS status,
            po.customerMobile AS customerMobile,
            c.name AS customerName,
            (
                SELECT GROUP_CONCAT(DISTINCT catName)
                FROM `${TableNames.PRE_ORDER_ITEM}`
                WHERE preOrderId = po.preOrderId
            ) AS categories,
            (
                SELECT COALESCE(SUM(quantity), 0)
                FROM `${TableNames.PRE_ORDER_ITEM}`
                WHERE preOrderId = po.preOrderId
            ) AS totalQuantity,
            (
                SELECT COALESCE(SUM(estimatedGrossWt * quantity), 0)
                FROM `${TableNames.PRE_ORDER_ITEM}`
                WHERE preOrderId = po.preOrderId
            ) AS estimatedWeight,
            (
                SELECT COALESCE(SUM(estimatedPrice * quantity), 0)
                FROM `${TableNames.PRE_ORDER_ITEM}`
                WHERE preOrderId = po.preOrderId
            ) AS estimatedPrice,
            (
                SELECT COALESCE(SUM(
                    CASE 
                        WHEN transactionType = 'credit' THEN amount
                        WHEN transactionType = 'debit' THEN -amount
                        ELSE 0
                    END
                ), 0)
                FROM `${TableNames.CUSTOMER_TRANSACTION}`
                WHERE linkedPreOrderId = po.preOrderId
            ) AS advanceAmount
        FROM `${TableNames.PRE_ORDER}` AS po
        LEFT JOIN `${TableNames.CUSTOMER}` AS c ON c.mobileNo = po.customerMobile
        ORDER BY po.deliveryDate ASC, po.orderDate DESC
        LIMIT :limit
        """
    )
    fun observeUpcomingPreOrders(limit: Int): Flow<List<PreOrderSummary>>
}
