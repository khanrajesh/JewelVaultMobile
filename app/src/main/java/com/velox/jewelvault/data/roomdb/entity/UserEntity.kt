package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UsersEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String?=null,
    val mobileNo: String,
    val token: String?=null,
    val pin: String? = null
)
