package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import com.velox.jewelvault.utils.generateId

@Entity(tableName = TableNames.USERS)
data class UsersEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String?=null,
    val mobileNo: String,
    val token: String?=null,
    val pin: String? = null
)