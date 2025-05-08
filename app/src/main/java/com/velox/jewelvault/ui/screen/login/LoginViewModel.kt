package com.velox.jewelvault.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.UsersEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {


    fun signup(usersEntity: UsersEntity, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            try {
                val result = appDatabase.userDao().insertUser(usersEntity)
                result?.let { onSuccess() } ?: onFailure()
            } catch (e: Exception) {
                onFailure()
            }
        }
    }

    fun login(phone: String, pass: String, onSuccess: (UsersEntity) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = appDatabase.userDao().getUserByMobile(phone)
                result?.let {
                    if (it.pin!= null && it.pin == pass){
                        onSuccess(it)
                    }else{
                        onFailure("Wrong Password")
                    }
                } ?: onFailure("Invalid User")
            } catch (e: Exception) {
                onFailure("${e.message}")
            }
        }
    }
}