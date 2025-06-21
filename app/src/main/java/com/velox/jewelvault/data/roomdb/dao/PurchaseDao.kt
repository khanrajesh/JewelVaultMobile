package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.velox.jewelvault.data.roomdb.dto.PurchaseOrderWithDetails
import com.velox.jewelvault.data.roomdb.entity.purchase.FirmEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.FirmWithSellers
import com.velox.jewelvault.data.roomdb.entity.purchase.MetalExchangeEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderItemEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.SellerEntity

@Dao
interface PurchaseDao {

    // ------------------------
    // SellerFirmEntity
    // ------------------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFirm(firm: FirmEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSeller(seller: SellerEntity): Long

    @Query("SELECT * FROM seller WHERE sellerId = :sellerId")
    suspend fun getSellerById(sellerId: Int): SellerEntity?



    @Transaction
    @Query("SELECT * FROM firm")
    suspend fun getAllFirmsWithSellers(): List<FirmWithSellers>

    @Transaction
    @Query("SELECT * FROM firm WHERE firmId = :firmId")
    suspend fun getFirmWithSellers(firmId: Int): FirmWithSellers?

    // Step 1: Get seller by mobile number
    @Query("SELECT * FROM seller WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getSellerByMobile(mobile: String): SellerEntity?

    // Step 2: Get firm by firmId
    @Query("SELECT * FROM firm WHERE firmId = :firmId LIMIT 1")
    suspend fun getFirmById(firmId: Int): FirmEntity?

    @Query("SELECT * FROM firm WHERE firmMobileNumber = :firmMobileNumber LIMIT 1")
    suspend fun getFirmByMobile(firmMobileNumber: String): FirmEntity?


    // ------------------------
    // PurchaseOrderEntity
    // ------------------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseOrder(order: PurchaseOrderEntity): Long

    @Query("SELECT * FROM purchase_orders WHERE purchaseOrderId = :orderId")
    suspend fun getPurchaseOrderById(orderId: Long): PurchaseOrderEntity?

    @Query("SELECT * FROM purchase_orders")
    suspend fun getAllPurchaseOrders(): List<PurchaseOrderEntity>

    @Delete
    suspend fun deleteOrder(order: PurchaseOrderEntity)

    @Query("SELECT * FROM purchase_orders WHERE billDate = :billDate")
    suspend fun getOrdersByBillDate(billDate: String): List<PurchaseOrderEntity>

    @Query(
        """
    SELECT * FROM purchase_order_items 
    WHERE purchaseOrderId = :orderId AND subCatName = :subCatName
    """
    )
    suspend fun getItemsByOrderIdAndSubCatName(
        orderId: Int,
        subCatName: String
    ): List<PurchaseOrderItemEntity>


    // ------------------------
    // PurchaseOrderItemEntity
    // ------------------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrderItem(item: PurchaseOrderItemEntity): Long

    @Query("SELECT * FROM purchase_order_items WHERE purchaseOrderId = :orderId")
    suspend fun getItemsByOrderId(orderId: Long): List<PurchaseOrderItemEntity>

    @Delete
    suspend fun deleteItem(item: PurchaseOrderItemEntity)


    // ------------------------
    // MetalExchangeEntity
    // ------------------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExchange(exchange: MetalExchangeEntity): Long

    @Query("SELECT * FROM metal_exchange WHERE purchaseOrderId = :orderId")
    suspend fun getExchangeByOrderId(orderId: Long): List<MetalExchangeEntity>

    @Delete
    suspend fun deleteExchange(exchange: MetalExchangeEntity)


    // ------------------------
    // Full Relational Fetch
    // ------------------------
    @Transaction
    @Query("SELECT * FROM purchase_orders WHERE purchaseOrderId = :orderId")
    suspend fun getOrderWithDetails(orderId: Long): PurchaseOrderWithDetails?

    @Transaction
    @Query("SELECT * FROM purchase_orders")
    suspend fun getAllOrdersWithDetails(): List<PurchaseOrderWithDetails>
}
