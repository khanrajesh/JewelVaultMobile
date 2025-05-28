package com.velox.jewelvault.ui.screen.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.ioScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val _datastoreManage: DataStoreManager,
    private val appDatabase: AppDatabase,
) : ViewModel() {
    val shopName = InputFieldState()
    val propName = InputFieldState()
    val shopImage = InputFieldState()
    val address = InputFieldState()
    val registrationNo = InputFieldState()
    val gstinNo = InputFieldState()
    val panNumber = InputFieldState()

    val userEmail = InputFieldState()
    val userMobile = InputFieldState()

    val storeEntity = mutableStateOf<StoreEntity?>(null)


    fun getStoreData() {
        ioScope {
            val userId = _datastoreManage.userId.first()
            val userData = appDatabase.userDao().getUserById(userId)
            storeEntity.value = appDatabase.storeDao().getStoreById(userId)

            userData?.let {
                userEmail.text = it.email ?: ""
                userMobile.text = it.mobileNo?:""
            }

            storeEntity.value?.let {
                propName.text = it.proprietor
                userMobile.text = it.phone
                shopName.text = it.name
                shopImage.text = it.image
                address.text = it.address
                registrationNo.text = it.registrationNo
                gstinNo.text = it.gstinNo
                panNumber.text = it.panNo
            }
        }
    }

    fun saveStoreData(onSuccess: () -> Unit, onFailure: () -> Unit) {
        ioScope {
            try {
                val storeId = _datastoreManage.storeId.first()
                val userId = _datastoreManage.userId.first()

                if (storeId != -1) {
                    val storeEntity = StoreEntity(
                        storeId = storeId,
                        userId = userId,
                        proprietor = propName.text,
                        name = shopName.text,
                        phone = userMobile.text,
                        address = address.text,
                        registrationNo = registrationNo.text,
                        gstinNo = gstinNo.text,
                        panNo = panNumber.text,
                        image = "",
                    )
                    if (appDatabase.storeDao().updateStore(storeEntity) != -1) onSuccess() else onFailure()
                } else {
                    val storeEntity = StoreEntity(
                        userId = userId,
                        proprietor = propName.text,
                        name = shopName.text,
                        phone = userMobile.text,
                        address = address.text,
                        registrationNo = registrationNo.text,
                        gstinNo = gstinNo.text,
                        panNo = panNumber.text,
                        image = "",
                    )

                    val result = appDatabase.storeDao().insertStore(storeEntity)
                    if (result != -1L) {
                        _datastoreManage.setValue(DataStoreManager.STORE_ID_KEY, result.toInt())
                        onSuccess()
                    } else onFailure()
                }
            } catch (e: Exception) {

            }
        }
    }

}