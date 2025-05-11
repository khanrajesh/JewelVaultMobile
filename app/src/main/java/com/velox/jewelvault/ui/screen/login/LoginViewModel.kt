package com.velox.jewelvault.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.UsersEntity
import com.velox.jewelvault.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val _appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager
) : ViewModel() {



    fun signup(usersEntity: UsersEntity, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            try {
                val result = _appDatabase.userDao().insertUser(usersEntity)
                result?.let { onSuccess() } ?: onFailure()
            } catch (e: Exception) {
                onFailure()
            }
        }
    }

    fun login(phone: String, pass: String, onSuccess: (UsersEntity) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = _appDatabase.userDao().getUserByMobile(phone)
                result?.let {
                    if (it.pin!= null && it.pin == pass){
                        try {
                            _dataStoreManager.setValue(DataStoreManager.USER_ID_KEY,it.id)
                            onSuccess(it)
                        }catch (e:Exception){
                            onFailure("Unable to store the user data")
                        }
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