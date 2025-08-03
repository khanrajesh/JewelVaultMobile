package com.velox.jewelvault.data.roomdb.entity.users

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(tableName = TableNames.USERS)
data class UsersEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String?=null,
    val mobileNo: String,
    val token: String?=null,
    val pin: String? = null,
    val role:String
)