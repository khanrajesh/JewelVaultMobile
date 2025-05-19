package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderItemEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderWithItems
import kotlinx.coroutines.flow.Flow
import java.sql.Timestamp

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert
    suspend fun insertItems(items: List<OrderItemEntity>): List<Long>

    @Transaction
    @Query("SELECT * FROM OrderEntity WHERE orderId = :id")
    suspend fun getOrderWithItems(id: Long): OrderWithItems

    @Query("SELECT * FROM OrderEntity ORDER BY orderDate DESC")
    fun getAllOrdersDesc(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM OrderEntity ORDER BY orderDate ASC")
    fun getAllOrdersAsc(): Flow<List<OrderEntity>>

    @Query("""
    SELECT o.orderId, o.orderDate, c.name AS customerName, c.mobileNo, 
           o.totalAmount, o.totalTax, o.totalCharge
    FROM OrderEntity o
    INNER JOIN CustomerEntity c ON o.customerMobile = c.mobileNo
    WHERE (:start IS NULL OR o.orderDate >= :start)
      AND (:end IS NULL OR o.orderDate <= :end)
    ORDER BY o.orderDate DESC
""")
    suspend fun getLedgerEntries(
        start: Timestamp? = null,
        end: Timestamp? = null
    ): List<LedgerEntry>

    @Query("""
    SELECT itemAddName, catName AS category, subCatName AS subCategory,
           SUM(quantity) AS totalQuantity,
           SUM(gsWt) AS totalWeight,
           SUM(price) AS totalRevenue
    FROM OrderItemEntity
    INNER JOIN OrderEntity ON OrderItemEntity.orderId = OrderEntity.orderId
    WHERE (:start IS NULL OR OrderEntity.orderDate >= :start)
      AND (:end IS NULL OR OrderEntity.orderDate <= :end)
      AND (:category IS NULL OR catName = :category)
      AND (:subCategory IS NULL OR subCatName = :subCategory)
    GROUP BY itemAddName, catName, subCatName
    ORDER BY totalRevenue DESC
""")
    suspend fun getItemSalesReport(
        start: Timestamp? = null,
        end: Timestamp? = null,
        category: String? = null,
        subCategory: String? = null
    ): List<ItemSalesReport>

    @Query("""
    SELECT c.name AS customerName, c.mobileNo,
           COUNT(o.orderId) AS totalOrders,
           SUM(o.totalAmount) AS totalSpent
    FROM OrderEntity o
    INNER JOIN CustomerEntity c ON o.customerMobile = c.mobileNo
    WHERE (:start IS NULL OR o.orderDate >= :start)
      AND (:end IS NULL OR o.orderDate <= :end)
    GROUP BY c.mobileNo, c.name
    ORDER BY totalSpent DESC
""")
    suspend fun getCustomerPurchaseSummary(
        start: Timestamp? = null,
        end: Timestamp? = null
    ): List<CustomerPurchaseSummary>

}

fun getStartOfMonth(): Timestamp = Timestamp(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)

fun getToday(): Timestamp = Timestamp(System.currentTimeMillis())


data class LedgerEntry(
    val orderId: Long,
    val orderDate: Timestamp,
    val customerName: String,
    val mobileNo: String,
    val totalAmount: Double,
    val totalTax: Double,
    val totalCharge: Double
)

data class ItemSalesReport(
    val itemAddName: String,
    val totalQuantity: Int,
    val totalWeight: Double,
    val totalRevenue: Double,
    val category: String,
    val subCategory: String
)

data class CustomerPurchaseSummary(
    val customerName: String,
    val mobileNo: String,
    val totalSpent: Double,
    val totalOrders: Int
)
