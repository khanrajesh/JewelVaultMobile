package com.velox.jewelvault.data.roomdb.entity.purchase

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "firm"
)
data class FirmEntity(
    @PrimaryKey(autoGenerate = true)
    val firmId: Int = 0,
    val firmName: String,
    val firmMobileNumber: String,
    val gstNumber: String,
    val address: String
)

@Entity(
    tableName = "seller",
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
    @PrimaryKey(autoGenerate = true)
    val sellerId: Int = 0,
    val firmId: Int,
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
