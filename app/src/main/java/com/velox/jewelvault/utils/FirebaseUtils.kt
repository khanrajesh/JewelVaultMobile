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
    }
}