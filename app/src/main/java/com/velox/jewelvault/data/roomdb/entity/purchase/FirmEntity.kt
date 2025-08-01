package com.velox.jewelvault.data.roomdb.entity.purchase

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(
    tableName = TableNames.FIRM
)
data class FirmEntity(
    @PrimaryKey
    val firmId: String,
    val firmName: String,
    val firmMobileNumber: String,
    val gstNumber: String,
    val address: String
)

@Entity(
    tableName = TableNames.SELLER,
    foreignKeys = [
        ForeignKey(
            entity = FirmEntity::class,
            parentColumns = ["firmId"],
            childColumns = ["firmId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("firmId")]
)

data class SellerEntity(
    @PrimaryKey
    val sellerId:String,
    val firmId: String,
    val name: String,
    val mobileNumber: String
)


data class FirmWithSellers(
    @Embedded val firm: FirmEntity,
    @Relation(
        parentColumn = "firmId",
        entityColumn = "firmId"
    )
    val sellers: List<SellerEntity>
)
