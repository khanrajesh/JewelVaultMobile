package com.velox.jewelvault.data.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.velox.jewelvault.data.roomdb.dao.CategoryDao
import com.velox.jewelvault.data.roomdb.dao.CustomerDao
import com.velox.jewelvault.data.roomdb.dao.ItemDao
import com.velox.jewelvault.data.roomdb.dao.MasterDao
import com.velox.jewelvault.data.roomdb.dao.OrderDao
import com.velox.jewelvault.data.roomdb.dao.PurchaseDao
import com.velox.jewelvault.data.roomdb.dao.StoreDao
import com.velox.jewelvault.data.roomdb.dao.SubCategoryDao
import com.velox.jewelvault.data.roomdb.dao.UsersDao
import com.velox.jewelvault.data.roomdb.entity.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.entity.UsersEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderItemEntity
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
        CustomerEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        FirmEntity::class,
        SellerEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        MetalExchangeEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun masterDao(): MasterDao
    abstract fun userDao(): UsersDao
    abstract fun storeDao(): StoreDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun orderDao(): OrderDao
    abstract fun purchaseDao(): PurchaseDao
}
