package com.velox.jewelvault.data.roomdb.entity.users

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(
    tableName = TableNames.USER_ADDITIONAL_INFO,
    foreignKeys = [
        ForeignKey(
            entity = UsersEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserAdditionalInfoEntity(
    @PrimaryKey val userId: String,
    val aadhaarNumber: String? = null,
    val address: String? = null,
    val emergencyContactPerson: String? = null,
    val emergencyContactNumber: String? = null,
    val governmentIdNumber: String? = null,
    val governmentIdType: String? = null, // PAN, Driving License, Passport, etc.
    val dateOfBirth: String? = null,
    val bloodGroup: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 