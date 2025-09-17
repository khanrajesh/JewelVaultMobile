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
import com.velox.jewelvault.data.roomdb.TableNames
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    // ------------------------
    // SellerFirmEntity
    // ------------------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFirm(firm: FirmEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSeller(seller: SellerEntity): Long

    @Query("SELECT * FROM ${TableNames.SELLER} WHERE sellerId = :sellerId")
    suspend fun getSellerById(sellerId: String): SellerEntity?

    @Transaction
    @Query("SELECT * FROM ${TableNames.FIRM}")
    suspend fun getAllFirmsWithSellers(): List<FirmWithSellers>

    // Step 2: Get firm by firmId
    @Query("SELECT * FROM ${TableNames.FIRM} WHERE firmId = :firmId LIMIT 1")
    suspend fun getFirmById(firmId: String): FirmEntity?

    @Query("SELECT * FROM ${TableNames.FIRM} WHERE firmMobileNumber = :firmMobileNumber LIMIT 1")
    suspend fun getFirmByMobile(firmMobileNumber: String): FirmEntity?

    // Step 1: Get seller by mobile number
    @Query("SELECT * FROM ${TableNames.SELLER} WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getSellerByMobile(mobile: String): SellerEntity?

    // ------------------------
    // PurchaseOrderEntity
    // ------------------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseOrder(order: PurchaseOrderEntity): Long

    @Query("SELECT * FROM ${TableNames.PURCHASE_ORDER} WHERE purchaseOrderId = :orderId")
    suspend fun getPurchaseOrderById(orderId: Long): PurchaseOrderEntity?

    @Query("SELECT * FROM ${TableNames.PURCHASE_ORDER}")
    suspend fun getAllPurchaseOrders(): List<PurchaseOrderEntity>

    @Delete
    suspend fun deleteOrder(order: PurchaseOrderEntity)

    @Query("SELECT * FROM ${TableNames.PURCHASE_ORDER} WHERE billDate = :billDate")
    suspend fun getOrdersByBillDate(billDate: String): List<PurchaseOrderEntity>

    @Query(
        """
    SELECT * FROM ${TableNames.PURCHASE_ORDER_ITEM} 
    WHERE purchaseOrderId = :orderId AND subCatId = :subCatId
    """
    )
    suspend fun getItemsByOrderIdAndSubCatId(
        orderId: String,
        subCatId: String
    ): List<PurchaseOrderItemEntity>


    // ------------------------
    // PurchaseOrderItemEntity
    // ------------------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrderItem(item: PurchaseOrderItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseOrderItem(item: PurchaseOrderItemEntity): Long

    @Query("SELECT * FROM ${TableNames.PURCHASE_ORDER_ITEM} WHERE purchaseOrderId = :orderId")
    suspend fun getItemsByOrderId(orderId: String): List<PurchaseOrderItemEntity>

    @Delete
    suspend fun deleteItem(item: PurchaseOrderItemEntity)


    // ------------------------
    // MetalExchangeEntity
    // ------------------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExchange(exchange: MetalExchangeEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMetalExchange(exchange: MetalExchangeEntity): Long

    @Query("SELECT * FROM ${TableNames.METAL_EXCHANGE} WHERE purchaseOrderId = :orderId")
    suspend fun getExchangeByOrderId(orderId: Long): List<MetalExchangeEntity>

    @Delete
    suspend fun deleteExchange(exchange: MetalExchangeEntity)


    // ------------------------
    // Full Relational Fetch
    // ------------------------
    @Transaction
    @Query("SELECT * FROM ${TableNames.PURCHASE_ORDER} WHERE purchaseOrderId = :orderId")
    suspend fun getOrderWithDetails(orderId: String): PurchaseOrderWithDetails?

    @Transaction
    @Query("SELECT * FROM ${TableNames.PURCHASE_ORDER}")
    suspend fun getAllOrdersWithDetails(): List<PurchaseOrderWithDetails>

    @Transaction
    @Query("SELECT * FROM ${TableNames.PURCHASE_ORDER} ORDER BY billDate ASC")
     fun getAllOrdersWithDetailsByBillDateAsc(): Flow<List<PurchaseOrderWithDetails>>

    @Transaction
    @Query("SELECT * FROM ${TableNames.PURCHASE_ORDER} ORDER BY billDate DESC")
     fun getAllOrdersWithDetailsByBillDateDesc(): Flow<List<PurchaseOrderWithDetails>>
     
    @Query("SELECT * FROM ${TableNames.FIRM}")
    suspend fun getAllFirms(): List<FirmEntity>

    
    @Query("SELECT * FROM ${TableNames.PURCHASE_ORDER_ITEM}")
    suspend fun getAllPurchaseOrderItems(): List<PurchaseOrderItemEntity>
    
    @Query("SELECT * FROM ${TableNames.METAL_EXCHANGE}")
    suspend fun getAllMetalExchanges(): List<MetalExchangeEntity>
}
