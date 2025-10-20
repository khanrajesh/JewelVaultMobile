package com.velox.jewelvault.data.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.velox.jewelvault.data.roomdb.dao.CategoryDao
import com.velox.jewelvault.data.roomdb.dao.CustomerDao
import com.velox.jewelvault.data.roomdb.dao.CustomerKhataBookDao
import com.velox.jewelvault.data.roomdb.dao.CustomerTransactionDao
import com.velox.jewelvault.data.roomdb.dao.FirmDao
import com.velox.jewelvault.data.roomdb.dao.ItemDao
import com.velox.jewelvault.data.roomdb.dao.MasterDao
import com.velox.jewelvault.data.roomdb.dao.OrderDao
import com.velox.jewelvault.data.roomdb.dao.PrinterDao
import com.velox.jewelvault.data.roomdb.dao.PurchaseDao
import com.velox.jewelvault.data.roomdb.dao.SellerDao
import com.velox.jewelvault.data.roomdb.dao.StoreDao
import com.velox.jewelvault.data.roomdb.dao.SubCategoryDao
import com.velox.jewelvault.data.roomdb.dao.UsersDao
import com.velox.jewelvault.data.roomdb.dao.UserAdditionalInfoDao
import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerKhataBookEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.entity.users.UsersEntity
import com.velox.jewelvault.data.roomdb.entity.users.UserAdditionalInfoEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderItemEntity
import com.velox.jewelvault.data.roomdb.entity.order.ExchangeItemEntity
import com.velox.jewelvault.data.roomdb.entity.printer.PrinterEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.FirmEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.MetalExchangeEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderItemEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.SellerEntity
import com.velox.jewelvault.utils.Converters



@Database(
    entities = [
        ItemEntity::class,
        CategoryEntity::class,
        SubCategoryEntity::class,
        StoreEntity::class,
        UsersEntity::class,
        UserAdditionalInfoEntity::class,
        CustomerEntity::class,
        CustomerKhataBookEntity::class,
        CustomerTransactionEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ExchangeItemEntity::class,
        FirmEntity::class,
        SellerEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        MetalExchangeEntity::class,
        PrinterEntity::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun masterDao(): MasterDao
    abstract fun userDao(): UsersDao
    abstract fun userAdditionalInfoDao(): UserAdditionalInfoDao
    abstract fun storeDao(): StoreDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun firmDao(): FirmDao
    abstract fun sellerDao(): SellerDao
    abstract fun itemDao(): ItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerKhataBookDao(): CustomerKhataBookDao
    abstract fun customerTransactionDao(): CustomerTransactionDao
    abstract fun orderDao(): OrderDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun printerDao(): PrinterDao
}
