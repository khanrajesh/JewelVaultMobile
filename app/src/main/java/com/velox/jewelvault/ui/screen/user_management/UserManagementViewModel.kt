package com.velox.jewelvault.ui.screen.user_management

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.users.UsersEntity
import com.velox.jewelvault.data.roomdb.entity.users.UserAdditionalInfoEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.data.firebase.FirebaseUtils
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.SecurityUtils
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val database: AppDatabase,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
    private val _firestore: FirebaseFirestore,
    private val _dataStoreManager: DataStoreManager
) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = _dataStoreManager.getAdminInfo()
    /**
     * return Triple of Flow<String> for storeId, upiId, storeName
     * */
    val store: Triple<Flow<String>, Flow<String>, Flow<String>> = _dataStoreManager.getSelectedStoreInfo()
    private val _users = MutableStateFlow<List<UsersEntity>>(emptyList())
    val users: StateFlow<List<UsersEntity>> = _users.asStateFlow()

    private val _userAdditionalInfo = MutableStateFlow<List<UserAdditionalInfoEntity>>(emptyList())
    val userAdditionalInfo: StateFlow<List<UserAdditionalInfoEntity>> = _userAdditionalInfo.asStateFlow()

    val _operationSuccess = MutableStateFlow(false)
    val operationSuccess: StateFlow<Boolean> = _operationSuccess.asStateFlow()

    val snackBarState = _snackBarState

    // Form states for adding/editing users
    val userName = InputFieldState()
    val userMobile = InputFieldState()
    val userEmail = InputFieldState()
    val userPin = InputFieldState()
    val userRole = InputFieldState()
    val userAadhaar = InputFieldState()
    val userAddress = InputFieldState()
    val emergencyContactPerson = InputFieldState()
    val emergencyContactNumber = InputFieldState()
    val governmentIdNumber = InputFieldState()
    val governmentIdType = InputFieldState()
    val dateOfBirth = InputFieldState()
    val bloodGroup = InputFieldState()

    val editingUserId = mutableStateOf("")

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                val usersList = database.userDao().getAllUsers()
                _users.value = usersList
                
                val additionalInfoList = database.userAdditionalInfoDao().getAllActiveUserAdditionalInfo()
                _userAdditionalInfo.value = additionalInfoList
            } catch (e: Exception) {
                _snackBarState.value = "Error loading users: ${e.message}"
            }
        }
    }

    fun isDefaultUser(user: UsersEntity): Boolean {
        return user.role.lowercase() == "admin"
    }

    fun addUser() {
        log("Adding user - Form data: name='${userName.text}', mobile='${userMobile.text}', role='${userRole.text}', pin='${userPin.text}'")
        
        if (userName.text.isBlank() || userMobile.text.isBlank() || userRole.text.isBlank() || userPin.text.isBlank()) {
            _snackBarState.value = "Please fill all required fields (Name, Mobile, Role, PIN)"
            log("Form validation failed - missing required fields")
            return
        }
        
        // Validate email format if provided
        if (userEmail.text.isNotBlank() && !InputValidator.isValidEmail(userEmail.text)) {
            _snackBarState.value = "Please enter a valid email address"
            log("Form validation failed - invalid email format")
            return
        }
        
        // Validate PIN format (4-6 digits)
        if (!InputValidator.isValidPin(userPin.text)) {
            _snackBarState.value = "PIN must be 4-6 digits"
            log("Form validation failed - invalid PIN format")
            return
        }
        
        // Log the exact values being used for entity creation
        log("Creating entity with: name='${userName.text}', mobile='${userMobile.text}', role='${userRole.text}'")

        ioLaunch {
            try {
                val adminMobileNumber = admin.third.first()
                val storeId = store.first.first()

                val appUserMobileNumber = userMobile.text

                // Log values right before entity creation
                log("About to create entity - name='${userName.text}', mobile='${userMobile.text}', role='${userRole.text}', appUserMobileNumber='$appUserMobileNumber'")
                
                // Hash the PIN before storing
                val hashedPin = SecurityUtils.hashPin(userPin.text)
                
                // Create user entity with mobile number as userId
                val user = UsersEntity(
                    userId = appUserMobileNumber,
                    name = userName.text,
                    email = userEmail.text.takeIf { it.isNotBlank() },
                    mobileNo = appUserMobileNumber,
                    pin = hashedPin,
                    role = userRole.text
                )
                
                log("Created user entity: $user")
                
                // Create additional info entity
                val additionalInfo = UserAdditionalInfoEntity(
                    userId = appUserMobileNumber,
                    aadhaarNumber = userAadhaar.text.takeIf { it.isNotBlank() },
                    address = userAddress.text.takeIf { it.isNotBlank() },
                    emergencyContactPerson = emergencyContactPerson.text.takeIf { it.isNotBlank() },
                    emergencyContactNumber = emergencyContactNumber.text.takeIf { it.isNotBlank() },
                    governmentIdNumber = governmentIdNumber.text.takeIf { it.isNotBlank() },
                    governmentIdType = governmentIdType.text.takeIf { it.isNotBlank() },
                    dateOfBirth = dateOfBirth.text.takeIf { it.isNotBlank() },
                    bloodGroup = bloodGroup.text.takeIf { it.isNotBlank() }
                )

                // Save to Firestore first
                val userData = FirebaseUtils.userEntityToMap(user)
                val firestoreResult = FirebaseUtils.saveOrUpdateUserData(
                    _firestore,
                    adminMobileNumber,
                    storeId,
                    userData,
                    appUserMobileNumber
                )

                if (firestoreResult.isSuccess) {
                    // Save additional info to Firestore
                    val additionalInfoData = FirebaseUtils.userAdditionalInfoEntityToMap(additionalInfo)
                    val additionalInfoResult = FirebaseUtils.saveOrUpdateUserAdditionalInfo(
                        _firestore,
                        adminMobileNumber,
                        storeId,
                        appUserMobileNumber,
                        additionalInfoData
                    )

                    if (additionalInfoResult.isSuccess) {
                        // Only save to local database if both Firestore operations succeed
                        database.userDao().insertUser(user)
                        database.userAdditionalInfoDao().insertUserAdditionalInfo(additionalInfo)
                        
                        loadUsers()
                        _snackBarState.value = "User added successfully"
                        _operationSuccess.value = true
                        log("User data saved to both local database and Firestore")
                    } else {
                        _snackBarState.value = "Failed to save user additional info to cloud"
                        log("Failed to save additional info to Firestore: ${additionalInfoResult.exceptionOrNull()?.message}")
                    }
                } else {
                    _snackBarState.value = "Failed to save user to cloud"
                    log("Failed to save to Firestore: ${firestoreResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _snackBarState.value = "Error adding user: ${e.message}"
                log("Error adding user: ${e.message}")
            }
        }
    }

    fun updateUser() {
        if (editingUserId.value.isBlank() || userName.text.isBlank() || userMobile.text.isBlank() || userRole.text.isBlank() || userPin.text.isBlank()) {
            _snackBarState.value = "Please fill all required fields (Name, Mobile, Role, PIN)"
            return
        }
        
        // Validate email format if provided
        if (userEmail.text.isNotBlank() && !InputValidator.isValidEmail(userEmail.text)) {
            _snackBarState.value = "Please enter a valid email address"
            return
        }
        
        // Validate PIN format (4-6 digits)
        if (!InputValidator.isValidPin(userPin.text)) {
            _snackBarState.value = "PIN must be 4-6 digits"
            return
        }

        ioLaunch {
            try {
                val adminMobileNumber = admin.third.first()
                val storeId = store.first.first()
                val appUserMobileNumber = userMobile.text.trim()

                
                // Update user entity
                val existingUser = database.userDao().getUserById(editingUserId.value)
                if (existingUser != null) {
                    // Hash the PIN before storing
                    val hashedPin = SecurityUtils.hashPin(userPin.text)
                    
                    val updatedUser = existingUser.copy(
                        name = userName.text,
                        email = userEmail.text.takeIf { it.isNotBlank() },
                        mobileNo = appUserMobileNumber,
                        pin = hashedPin,
                        role = userRole.text
                    )
                    
                    // Update to Firestore first
                    val userData = FirebaseUtils.userEntityToMap(updatedUser)
                    val firestoreResult = FirebaseUtils.saveOrUpdateUserData(
                        _firestore,
                        adminMobileNumber,
                        storeId,
                        userData,
                        appUserMobileNumber
                    )

                    if (firestoreResult.isSuccess) {
                        // Update additional info entity
                        val existingAdditionalInfo = database.userAdditionalInfoDao().getUserAdditionalInfoById(editingUserId.value)
                        if (existingAdditionalInfo != null) {
                            val updatedAdditionalInfo = existingAdditionalInfo.copy(
                                aadhaarNumber = userAadhaar.text.takeIf { it.isNotBlank() },
                                address = userAddress.text.takeIf { it.isNotBlank() },
                                emergencyContactPerson = emergencyContactPerson.text.takeIf { it.isNotBlank() },
                                emergencyContactNumber = emergencyContactNumber.text.takeIf { it.isNotBlank() },
                                governmentIdNumber = governmentIdNumber.text.takeIf { it.isNotBlank() },
                                governmentIdType = governmentIdType.text.takeIf { it.isNotBlank() },
                                dateOfBirth = dateOfBirth.text.takeIf { it.isNotBlank() },
                                bloodGroup = bloodGroup.text.takeIf { it.isNotBlank() },
                                updatedAt = System.currentTimeMillis()
                            )
                            
                            // Update additional info to Firestore
                            val additionalInfoData = FirebaseUtils.userAdditionalInfoEntityToMap(updatedAdditionalInfo)
                            val additionalInfoResult = FirebaseUtils.saveOrUpdateUserAdditionalInfo(
                                _firestore,
                                adminMobileNumber,
                                storeId,
                                appUserMobileNumber,
                                additionalInfoData
                            )
                            
                            if (additionalInfoResult.isSuccess) {
                                // Only update local database if both Firestore operations succeed
                                database.userDao().updateUser(updatedUser)
                                database.userAdditionalInfoDao().updateUserAdditionalInfo(updatedAdditionalInfo)
                                
                                loadUsers()
                                _snackBarState.value = "User updated successfully"
                                _operationSuccess.value = true
                                log("User data updated in both local database and Firestore")
                            } else {
                                _snackBarState.value = "Failed to update user additional info to cloud"
                                log("Failed to update additional info to Firestore: ${additionalInfoResult.exceptionOrNull()?.message}")
                            }
                        } else {
                            // Create additional info if it doesn't exist
                            val additionalInfo = UserAdditionalInfoEntity(
                                userId = appUserMobileNumber,
                                aadhaarNumber = userAadhaar.text.takeIf { it.isNotBlank() },
                                address = userAddress.text.takeIf { it.isNotBlank() },
                                emergencyContactPerson = emergencyContactPerson.text.takeIf { it.isNotBlank() },
                                emergencyContactNumber = emergencyContactNumber.text.takeIf { it.isNotBlank() },
                                governmentIdNumber = governmentIdNumber.text.takeIf { it.isNotBlank() },
                                governmentIdType = governmentIdType.text.takeIf { it.isNotBlank() },
                                dateOfBirth = dateOfBirth.text.takeIf { it.isNotBlank() },
                                bloodGroup = bloodGroup.text.takeIf { it.isNotBlank() }
                            )
                            
                            // Save additional info to Firestore
                            val additionalInfoData = FirebaseUtils.userAdditionalInfoEntityToMap(additionalInfo)
                            val additionalInfoResult = FirebaseUtils.saveOrUpdateUserAdditionalInfo(
                                _firestore,
                                adminMobileNumber,
                                storeId,
                                appUserMobileNumber,
                                additionalInfoData
                            )
                            
                            if (additionalInfoResult.isSuccess) {
                                // Only update local database if both Firestore operations succeed
                                database.userDao().updateUser(updatedUser)
                                database.userAdditionalInfoDao().insertUserAdditionalInfo(additionalInfo)
                                
                                loadUsers()
                                _snackBarState.value = "User updated successfully"
                                _operationSuccess.value = true
                                log("User data updated in both local database and Firestore")
                            } else {
                                _snackBarState.value = "Failed to save user additional info to cloud"
                                log("Failed to save additional info to Firestore: ${additionalInfoResult.exceptionOrNull()?.message}")
                            }
                        }
                    } else {
                        _snackBarState.value = "Failed to update user to cloud"
                        log("Failed to update user in Firestore: ${firestoreResult.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _snackBarState.value = "Error updating user: ${e.message}"
                log("Error updating user: ${e.message}")
            }
        }
    }

    fun deleteUser(userId: String) {
        ioLaunch {
            try {
                val adminMobileNumber = admin.third.first()
                val storeId = store.first.first()
                val user = database.userDao().getUserById(userId)

                
                if (user != null) {
                    // Delete from Firestore first
                    val firestoreResult = FirebaseUtils.deleteUserFromFirestore(
                        _firestore,
                        adminMobileNumber,
                        storeId,
                        user.mobileNo
                    )

                    if (firestoreResult.isSuccess) {
                        // Only delete from local database if Firestore operation succeeds
                        database.userDao().deleteUser(user)
                        database.userAdditionalInfoDao().deleteUserAdditionalInfo(
                            UserAdditionalInfoEntity(userId = userId)
                        )
                        loadUsers()
                        _snackBarState.value = "User deleted successfully"
                        log("User deleted from both local database and Firestore")
                    } else {
                        _snackBarState.value = "Failed to delete user from cloud"
                        log("Failed to delete from Firestore: ${firestoreResult.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _snackBarState.value = "Error deleting user: ${e.message}"
                log("Error deleting user: ${e.message}")
            }
        }
    }

    fun getUser(userId: String) {
        viewModelScope.launch {
            try {
                val user = database.userDao().getUserById(userId)
                val additionalInfo = database.userAdditionalInfoDao().getUserAdditionalInfoById(userId)
                
                if (user != null) {
                    editingUserId.value = userId
                    userName.text = user.name
                    userMobile.text = user.mobileNo
                    userEmail.text = user.email ?: ""
                    userPin.text = "" // Clear PIN for security - user must enter new PIN
                    userRole.text = user.role
                    
                    if (additionalInfo != null) {
                        userAadhaar.text = additionalInfo.aadhaarNumber ?: ""
                        userAddress.text = additionalInfo.address ?: ""
                        emergencyContactPerson.text = additionalInfo.emergencyContactPerson ?: ""
                        emergencyContactNumber.text = additionalInfo.emergencyContactNumber ?: ""
                        governmentIdNumber.text = additionalInfo.governmentIdNumber ?: ""
                        governmentIdType.text = additionalInfo.governmentIdType ?: ""
                        dateOfBirth.text = additionalInfo.dateOfBirth ?: ""
                        bloodGroup.text = additionalInfo.bloodGroup ?: ""
                    }
                }
            } catch (e: Exception) {
                _snackBarState.value = "Error loading user data: ${e.message}"
            }
        }
    }

    fun clearForm() {
        userName.text = ""
        userMobile.text = ""
        userEmail.text = ""
        userPin.text = ""
        userRole.text = ""
        userAadhaar.text = ""
        userAddress.text = ""
        emergencyContactPerson.text = ""
        emergencyContactNumber.text = ""
        governmentIdNumber.text = ""
        governmentIdType.text = ""
        dateOfBirth.text = ""
        bloodGroup.text = ""
        editingUserId.value = ""
    }

    /**
     * Sync users from Firestore to local database
     */
    fun syncUsersFromFirestore() {
        ioLaunch {
            try {

                val adminMobileNumber = admin.third.first()
                val storeId = store.first.first()
                
                val firestoreResult = FirebaseUtils.getAllUsersFromFirestore(_firestore, adminMobileNumber, storeId)
                
                if (firestoreResult.isSuccess) {
                    val userDocuments = firestoreResult.getOrNull() ?: emptyList()
                    
                    for ((appUserMobileNumber, userData) in userDocuments) {
                        val userEntity = FirebaseUtils.mapToUserEntity(userData).copy(userId = appUserMobileNumber)
                        
                        // Check if user exists in local database
                        val existingUser = database.userDao().getUserById(appUserMobileNumber)
                        if (existingUser != null) {
                            // Update existing user
                            database.userDao().updateUser(userEntity)
                        } else {
                            // Insert new user
                            database.userDao().insertUser(userEntity)
                        }
                        
                        // Get additional info for this user
                        val additionalInfoResult = FirebaseUtils.getUserAdditionalInfoFromFirestore(
                            _firestore,
                            adminMobileNumber,
                            storeId,
                            appUserMobileNumber
                        )
                        
                        if (additionalInfoResult.isSuccess && additionalInfoResult.getOrNull() != null) {
                            val additionalInfoEntity = FirebaseUtils.mapToUserAdditionalInfoEntity(
                                additionalInfoResult.getOrNull()!!
                            ).copy(userId = appUserMobileNumber)
                            
                            // Check if additional info exists in local database
                            val existingAdditionalInfo = database.userAdditionalInfoDao().getUserAdditionalInfoById(appUserMobileNumber)
                            if (existingAdditionalInfo != null) {
                                database.userAdditionalInfoDao().updateUserAdditionalInfo(additionalInfoEntity)
                            } else {
                                database.userAdditionalInfoDao().insertUserAdditionalInfo(additionalInfoEntity)
                            }
                        }
                    }
                    
                    loadUsers()
                    _snackBarState.value = "Users synced from cloud successfully"
                    log("Users synced from Firestore to local database")
                } else {
                    _snackBarState.value = "Failed to sync users from cloud"
                    log("Failed to sync users from Firestore: ${firestoreResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _snackBarState.value = "Error syncing users: ${e.message}"
                log("Error syncing users: ${e.message}")
            }
        }
    }
}
