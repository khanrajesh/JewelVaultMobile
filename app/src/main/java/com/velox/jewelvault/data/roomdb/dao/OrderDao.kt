package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
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
    SELECT itemAddName,
           catName AS category,
           subCatName AS subCategory,
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


    @Query("""
    SELECT 
        c.mobileNo,
        c.name,
        c.address,
        c.gstin_pan,

        oi.id,
        oi.orderId,
        oi.orderDate,
        oi.itemId,
        oi.customerMobile,
        oi.catId,
        oi.catName,
        oi.itemAddName,
        oi.subCatId,
        oi.subCatName,
        oi.entryType,
        oi.quantity,
        oi.gsWt,
        oi.ntWt,
        oi.fnWt,
        oi.fnMetalPrice,
        oi.purity,
        oi.crgType,
        oi.crg,
        oi.othCrgDes,
        oi.othCrg,
        oi.cgst,
        oi.sgst,
        oi.igst,
        oi.huid,
        oi.addDesKey,
        oi.addDesValue,
        oi.price,
        oi.charge,
        oi.tax

    FROM OrderItemEntity AS oi
    INNER JOIN CustomerEntity AS c ON oi.customerMobile = c.mobileNo

    WHERE (:start IS NULL OR oi.orderDate >= :start)
      AND (:end IS NULL OR oi.orderDate <= :end)
      AND (:category IS NULL OR oi.catName = :category)
      AND (:subCategory IS NULL OR oi.subCatName = :subCategory)

    ORDER BY oi.orderDate DESC
    LIMIT :limit
""")
    suspend fun getIndividualSellItems(
        start: Timestamp? = null,
        end: Timestamp? = null,
        category: String? = null,
        subCategory: String? = null,
        limit: Int = 10
    ): List<IndividualSellItem>

    @Query("""
    SELECT 
        subCatName,
        SUM(price+charge+tax) AS totalPrice,
        SUM(fnWt) AS totalFnWt
    FROM OrderItemEntity
    WHERE (:start IS NULL OR orderDate >= :start)
      AND (:end IS NULL OR orderDate <= :end)
    GROUP BY subCatName
    ORDER BY totalPrice DESC
    LIMIT 10
""")
    suspend fun getTopSellingSubcategories(
        start: Timestamp? = null,
        end: Timestamp? = null
    ): List<TopSubCategory>


    @Query("""
    SELECT 
        catName AS category,
        subCatName,
        itemAddName,
        SUM(fnWt) AS totalFnWt
    FROM OrderItemEntity
    WHERE (:start IS NULL OR orderDate >= :start)
      AND (:end IS NULL OR orderDate <= :end)
    GROUP BY catName, itemAddName
    ORDER BY category ASC, totalFnWt DESC
""")
    suspend fun getGroupedItemWeights(
        start: Timestamp? = null,
        end: Timestamp? = null
    ): List<TopItemByCategory>



    @Query("""
    SELECT 
        catName AS category,
        subCatName,
        itemAddName,
        SUM(fnWt) AS totalFnWt
    FROM OrderItemEntity
    WHERE (:start IS NULL OR orderDate >= :start)
      AND (:end IS NULL OR orderDate <= :end)
    GROUP BY catName, subCatName
    ORDER BY totalFnWt DESC
    LIMIT 5
    """)
    suspend fun getTopSellingItemsByCategory(
        start: Timestamp? = null,
        end: Timestamp? = null
    ): List<TopItemByCategory>

    @Query("""
    SELECT 
        COUNT(DISTINCT orderId) AS invoiceCount,
        SUM(totalAmount+totalCharge+totalTax) AS totalAmount
    FROM OrderEntity
    WHERE (:start IS NULL OR orderDate >= :start)
      AND (:end IS NULL OR orderDate <= :end)
""")
    suspend fun getTotalSalesSummary(
        start: Timestamp? = null,
        end: Timestamp? = null
    ): SalesSummary

}


fun getTimestampDaysAgo(days: Int): Timestamp {
    val millis = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
    return Timestamp(millis)
}

fun TimeRange.range(): Pair<Timestamp, Timestamp> = getTimeRange(this)

fun getTimeRange(range: TimeRange): Pair<Timestamp, Timestamp> {
    val now = today
    val start = when (range) {
        TimeRange.WEEKLY -> getTimestampDaysAgo(7)
        TimeRange.MONTHLY -> getTimestampDaysAgo(30)
        TimeRange.THREE_MONTHS -> getTimestampDaysAgo(90)
        TimeRange.SIX_MONTHS -> getTimestampDaysAgo(180)
        TimeRange.YEARLY -> getTimestampDaysAgo(365)
    }
    return start to now
}

fun getStartOfMonth(): Timestamp = Timestamp(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)

val today: Timestamp = Timestamp(System.currentTimeMillis())

enum class TimeRange {
    WEEKLY,
    MONTHLY,
    THREE_MONTHS,
    SIX_MONTHS,
    YEARLY
}

data class IndividualSellItem(
    //customer details
    val mobileNo: String,
    val name: String,
    val address: String? = null,
    val gstin_pan: String? = null,
    //item details
    val id: Int = 0,
    val orderId: Int,
    val orderDate: Timestamp,
    val itemId: Int,
    val customerMobile:String,
    val catId: Int,
    val catName: String,
    val itemAddName: String,
    val subCatId: Int,
    val subCatName: String,
    val entryType: String,
    val quantity: Int,
    val gsWt: Double,
    val ntWt: Double,
    val fnWt: Double,
    val fnMetalPrice: Double,
    val purity: String,
    val crgType: String,
    val crg: Double,
    val othCrgDes: String,
    val othCrg: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val huid: String,
    val addDesKey:String,
    val addDesValue:String,
    val price: Double,
    val charge: Double,
    val tax: Double,

)

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

data class TopItemByCategory(
    val category: String,
    val subCatName: String,
    val itemAddName: String,
    val totalFnWt: Double
)

data class SalesSummary(
    val invoiceCount: Int,
    val totalAmount: Double
)

data class TopSubCategory(
    val subCatName: String,
    val totalPrice: Double,
    val totalFnWt: Double
)