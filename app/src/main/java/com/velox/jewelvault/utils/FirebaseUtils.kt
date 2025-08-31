package com.velox.jewelvault.utils

import android.net.Uri
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseUtils {
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val STORE_DETAILS_SUBCOLLECTION = "stores"
        private const val STORE_IMAGES_FOLDER = "store_images"
        private const val APP_USERS_SUBCOLLECTION = "app_users"
        private const val USER_ADDITIONAL_INFO_SUBCOLLECTION = "user_additional_info"

        /**
         * Save store data to Firestore
         * Structure: users/{mobileNumber}/store_details/{storeId}
         */
        suspend fun saveOrUpdateStoreData(
            firestore: FirebaseFirestore,
            mobileNumber: String,
            storeData: Map<String, Any>,
            storeId: String? = null  // Can be null or blank for new
        ): Result<String> {
            return try {
                val collectionRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(mobileNumber)
                    .collection(STORE_DETAILS_SUBCOLLECTION)

                val finalStoreId: String
                val documentRef: DocumentReference

                if (!storeId.isNullOrBlank()) {
                    // Update existing document
                    documentRef = collectionRef.document(storeId)
                    documentRef.set(storeData).await()
                    finalStoreId = storeId
                } else {
                    // Create new document with auto ID
                    documentRef = collectionRef.add(storeData).await()
                    finalStoreId = documentRef.id
                }

                Result.success(finalStoreId)
            } catch (e: Exception) {
                log("Error saving/updating store data: ${e.message}")
                Result.failure(e)
            }
        }



        suspend fun getAllStores(
            firestore: FirebaseFirestore,
            mobileNumber: String
        ): Result<List<Pair<String, Map<String, Any>>>> {
            return try {
                val snapshot = firestore
                    .collection(USERS_COLLECTION)
                    .document(mobileNumber)
                    .collection(STORE_DETAILS_SUBCOLLECTION)
                    .get()
                    .await()

                val storeDocuments = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) doc.id to data else null
                }

                Result.success(storeDocuments)

            } catch (e: Exception) {
                log("Error fetching store documents: ${e.message}")
                Result.failure(e)
            }
        }


        /**
         * Get store data from Firestore
         */
        suspend fun getStoreDataFromFirestore(
            firestore: FirebaseFirestore,
            mobileNumber: String,
            storeId:String
        ): Result<Map<String, Any>?> {
            return try {
                val document = firestore
                    .collection(USERS_COLLECTION)
                    .document(mobileNumber)
                    .collection(STORE_DETAILS_SUBCOLLECTION)
                    .document(storeId)
                    .get()
                    .await()

                if (document.exists()) {
                    Result.success(document.data)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                log("Error getting store data from Firestore: ${e.message}")
                Result.failure(e)
            }
        }

        /**
         * Upload image to Firebase Storage and return download URL
         */
        suspend fun uploadImageToStorage(
            storage: FirebaseStorage,
            imageUri: Uri,
            mobileNumber: String
        ): Result<String> {
            return try {
                val fileName = "${STORE_IMAGES_FOLDER}/${mobileNumber}_${UUID.randomUUID()}.jpg"
                val storageRef: StorageReference = storage.reference.child(fileName)

                val uploadTask = storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await()

                log("Image uploaded successfully: ${downloadUrl}")
                Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                log("Error uploading image to Storage: ${e.message}")
                Result.failure(e)
            }
        }

        /**
         * Delete image from Firebase Storage
         */
        suspend fun deleteImageFromStorage(
            storage: FirebaseStorage,
            imageUrl: String
        ): Result<Unit> {
            return try {
                val storageRef = storage.getReferenceFromUrl(imageUrl)
                storageRef.delete().await()
                log("Image deleted successfully from Storage")
                Result.success(Unit)
            } catch (e: Exception) {
                log("Error deleting image from Storage: ${e.message}")
                Result.failure(e)
            }
        }

        /**
         * Convert StoreEntity to Firestore map
         */
        fun storeEntityToMap(storeEntity: com.velox.jewelvault.data.roomdb.entity.StoreEntity): Map<String, Any> {
            return mapOf(
                "storeId" to storeEntity.storeId,
                "userId" to storeEntity.userId,
                "proprietor" to storeEntity.proprietor,
                "name" to storeEntity.name,
                "email" to storeEntity.email,
                "phone" to storeEntity.phone,
                "address" to storeEntity.address,
                "registrationNo" to storeEntity.registrationNo,
                "gstinNo" to storeEntity.gstinNo,
                "panNo" to storeEntity.panNo,
                "image" to storeEntity.image,
                "invoiceNo" to storeEntity.invoiceNo,
                "upiId" to storeEntity.upiId,
                "lastUpdated" to System.currentTimeMillis()
            )
        }

        /**
         * Convert Firestore map to StoreEntity
         */
        fun mapToStoreEntity(data: Map<String, Any>): com.velox.jewelvault.data.roomdb.entity.StoreEntity {
            return com.velox.jewelvault.data.roomdb.entity.StoreEntity(
                storeId = (data["storeId"] as? String) ?: "",
                userId = (data["userId"] as? String) ?: "",
                proprietor = data["proprietor"] as? String ?: "",
                name = data["name"] as? String ?: "",
                email = data["email"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                registrationNo = data["registrationNo"] as? String ?: "",
                gstinNo = data["gstinNo"] as? String ?: "",
                panNo = data["panNo"] as? String ?: "",
                image = data["image"] as? String ?: "",
                invoiceNo = (data["invoiceNo"] as? Long)?.toInt() ?: 0,
                upiId = data["upiId"] as? String ?: ""
            )
        }

        // ==================== USER MANAGEMENT FIREBASE UTILITIES ====================

        /**
         * Save or update user data to Firestore
         * Structure: users/{userMobileNumber}/stores/{storeId}/app_users/{appUserMobileNumber}
         */
        suspend fun saveOrUpdateUserData(
            firestore: FirebaseFirestore,
            userMobileNumber: String,
            storeId: String,
            userData: Map<String, Any>,
            appUserMobileNumber: String
        ): Result<String> {
            return try {
                // Validate parameters
                if (userMobileNumber.isBlank() || storeId.isBlank() || appUserMobileNumber.isBlank()) {
                    log("Invalid parameters: userMobileNumber='$userMobileNumber', storeId='$storeId', appUserMobileNumber='$appUserMobileNumber'")
                    return Result.failure(IllegalArgumentException("Invalid parameters provided"))
                }
                
                log("Creating document reference: users/$userMobileNumber/stores/$storeId/app_users/$appUserMobileNumber")
                
                val documentRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userMobileNumber)
                    .collection(STORE_DETAILS_SUBCOLLECTION)
                    .document(storeId)
                    .collection(APP_USERS_SUBCOLLECTION)
                    .document(appUserMobileNumber)

                documentRef.set(userData).await()
                Result.success(appUserMobileNumber)
            } catch (e: Exception) {
                log("Error saving/updating user data: ${e.message}")
                Result.failure(e)
            }
        }

        /**
         * Save or update user additional info to Firestore
         * Structure: users/{userMobileNumber}/stores/{storeId}/app_users/{appUserMobileNumber}/user_additional_info/{appUserMobileNumber}
         */
        suspend fun saveOrUpdateUserAdditionalInfo(
            firestore: FirebaseFirestore,
            userMobileNumber: String,
            storeId: String,
            appUserMobileNumber: String,
            additionalInfoData: Map<String, Any>
        ): Result<Unit> {
            return try {
                // Validate parameters
                if (userMobileNumber.isBlank() || storeId.isBlank() || appUserMobileNumber.isBlank()) {
                    log("Invalid parameters: userMobileNumber='$userMobileNumber', storeId='$storeId', appUserMobileNumber='$appUserMobileNumber'")
                    return Result.failure(IllegalArgumentException("Invalid parameters provided"))
                }
                
                log("Creating document reference: users/$userMobileNumber/stores/$storeId/app_users/$appUserMobileNumber/user_additional_info/$appUserMobileNumber")
                
                val documentRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userMobileNumber)
                    .collection(STORE_DETAILS_SUBCOLLECTION)
                    .document(storeId)
                    .collection(APP_USERS_SUBCOLLECTION)
                    .document(appUserMobileNumber)
                    .collection(USER_ADDITIONAL_INFO_SUBCOLLECTION)
                    .document(appUserMobileNumber)

                documentRef.set(additionalInfoData).await()
                Result.success(Unit)
            } catch (e: Exception) {
                log("Error saving/updating user additional info: ${e.message}")
                Result.failure(e)
            }
        }

        /**
         * Get all users from Firestore
         */
        suspend fun getAllUsersFromFirestore(
            firestore: FirebaseFirestore,
            mobileNumber: String,
            storeId: String
        ): Result<List<Pair<String, Map<String, Any>>>> {
            return try {
                // Validate parameters
                if (mobileNumber.isBlank() || storeId.isBlank()) {
                    log("Invalid parameters: mobileNumber='$mobileNumber', storeId='$storeId'")
                    return Result.failure(IllegalArgumentException("Invalid parameters provided"))
                }
                
                log("Creating collection reference: users/$mobileNumber/stores/$storeId/app_users")
                
                val snapshot = firestore
                    .collection(USERS_COLLECTION)
                    .document(mobileNumber)
                    .collection(STORE_DETAILS_SUBCOLLECTION)
                    .document(storeId)
                    .collection(APP_USERS_SUBCOLLECTION)
                    .get()
                    .await()

                val userDocuments = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) doc.id to data else null
                }

                Result.success(userDocuments)
            } catch (e: Exception) {
                log("Error fetching user documents: ${e.message}")
                Result.failure(e)
            }
        }

        /**
         * Get user additional info from Firestore
         */
        suspend fun getUserAdditionalInfoFromFirestore(
            firestore: FirebaseFirestore,
            storeOwnerMobile: String,
            storeId: String,
            userMobileNumber: String
        ): Result<Map<String, Any>?> {
            return try {
                val document = firestore
                    .collection(USERS_COLLECTION)
                    .document(storeOwnerMobile)
                    .collection(STORE_DETAILS_SUBCOLLECTION)
                    .document(storeId)
                    .collection(APP_USERS_SUBCOLLECTION)
                    .document(userMobileNumber)
                    .collection(USER_ADDITIONAL_INFO_SUBCOLLECTION)
                    .document(userMobileNumber)
                    .get()
                    .await()

                if (document.exists()) {
                    Result.success(document.data)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                log("Error getting user additional info from Firestore: ${e.message}")
                Result.failure(e)
            }
        }

        /**
         * Delete user from Firestore
         */
        suspend fun deleteUserFromFirestore(
            firestore: FirebaseFirestore,
            storeOwnerMobile: String,
            storeId: String,
            userMobileNumber: String
        ): Result<Unit> {
            return try {
                // Validate parameters
                if (storeOwnerMobile.isBlank() || storeId.isBlank() || userMobileNumber.isBlank()) {
                    log("Invalid parameters: storeOwnerMobile='$storeOwnerMobile', storeId='$storeId', userMobileNumber='$userMobileNumber'")
                    return Result.failure(IllegalArgumentException("Invalid parameters provided"))
                }
                
                log("Deleting user: users/$storeOwnerMobile/stores/$storeId/app_users/$userMobileNumber")
                
                // Delete user additional info first (if exists)
                firestore
                    .collection(USERS_COLLECTION)
                    .document(storeOwnerMobile)
                    .collection(STORE_DETAILS_SUBCOLLECTION)
                    .document(storeId)
                    .collection(APP_USERS_SUBCOLLECTION)
                    .document(userMobileNumber)
                    .collection(USER_ADDITIONAL_INFO_SUBCOLLECTION)
                    .document(userMobileNumber)
                    .delete()
                    .await()

                // Delete main user document
                firestore
                    .collection(USERS_COLLECTION)
                    .document(storeOwnerMobile)
                    .collection(STORE_DETAILS_SUBCOLLECTION)
                    .document(storeId)
                    .collection(APP_USERS_SUBCOLLECTION)
                    .document(userMobileNumber)
                    .delete()
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                log("Error deleting user from Firestore: ${e.message}")
                Result.failure(e)
            }
        }

        /**
         * Convert UsersEntity to Firestore map
         */
        fun userEntityToMap(userEntity: com.velox.jewelvault.data.roomdb.entity.users.UsersEntity): Map<String, Any> {
            return mapOf(
                "userId" to userEntity.userId,
                "name" to userEntity.name,
                "email" to (userEntity.email ?: ""),
                "mobileNo" to userEntity.mobileNo,
                "pin" to (userEntity.pin ?: ""),
                "role" to userEntity.role,
            )
        }

        /**
         * Convert UserAdditionalInfoEntity to Firestore map
         */
        fun userAdditionalInfoEntityToMap(userInfoEntity: com.velox.jewelvault.data.roomdb.entity.users.UserAdditionalInfoEntity): Map<String, Any> {
            return mapOf(
                "userId" to userInfoEntity.userId,
                "aadhaarNumber" to (userInfoEntity.aadhaarNumber ?: ""),
                "address" to (userInfoEntity.address ?: ""),
                "emergencyContactPerson" to (userInfoEntity.emergencyContactPerson ?: ""),
                "emergencyContactNumber" to (userInfoEntity.emergencyContactNumber ?: ""),
                "governmentIdNumber" to (userInfoEntity.governmentIdNumber ?: ""),
                "governmentIdType" to (userInfoEntity.governmentIdType ?: ""),
                "dateOfBirth" to (userInfoEntity.dateOfBirth ?: ""),
                "bloodGroup" to (userInfoEntity.bloodGroup ?: ""),
                "isActive" to userInfoEntity.isActive,
                "createdAt" to userInfoEntity.createdAt,
                "updatedAt" to userInfoEntity.updatedAt
            )
        }

        /**
         * Convert Firestore map to UsersEntity
         */
        fun mapToUserEntity(data: Map<String, Any>): com.velox.jewelvault.data.roomdb.entity.users.UsersEntity {
            return com.velox.jewelvault.data.roomdb.entity.users.UsersEntity(
                userId = (data["userId"] as? String) ?: "",
                name = data["name"] as? String ?: "",
                email = data["email"] as? String,
                mobileNo = data["mobileNo"] as? String ?: "",
                pin = data["pin"] as? String,
                role = data["role"] as? String ?: "",
            )
        }

        /**
         * Convert Firestore map to UserAdditionalInfoEntity
         */
        fun mapToUserAdditionalInfoEntity(data: Map<String, Any>): com.velox.jewelvault.data.roomdb.entity.users.UserAdditionalInfoEntity {
            return com.velox.jewelvault.data.roomdb.entity.users.UserAdditionalInfoEntity(
                userId = (data["userId"] as? String) ?: "",
                aadhaarNumber = data["aadhaarNumber"] as? String,
                address = data["address"] as? String,
                emergencyContactPerson = data["emergencyContactPerson"] as? String,
                emergencyContactNumber = data["emergencyContactNumber"] as? String,
                governmentIdNumber = data["governmentIdNumber"] as? String,
                governmentIdType = data["governmentIdType"] as? String,
                dateOfBirth = data["dateOfBirth"] as? String,
                bloodGroup = data["bloodGroup"] as? String,
                isActive = data["isActive"] as? Boolean ?: true,
                createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}