package com.velox.jewelvault.utils

import android.os.Build
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreDeviceSessionManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    data class ActiveDeviceInfo(
        val deviceId: String,
        val manufacturer: String,
        val model: String,
        val lastLoginAt: Long
    )

    sealed class CheckResult {
        data object Allowed : CheckResult()
        data class Blocked(val info: ActiveDeviceInfo) : CheckResult()
        data class Error(val message: String?) : CheckResult()
    }

    suspend fun checkAndActivate(adminMobile: String, storeId: String): CheckResult {
        if (adminMobile.isBlank() || storeId.isBlank()) {
            return CheckResult.Error("Admin mobile or store id is missing")
        }

        return try {
            val timeoutMs = TimeUnit.MINUTES.toMillis(5)
            val now = System.currentTimeMillis()
            val deviceId = buildDeviceId(adminMobile)
            val devicesRef = firestore.collection("users")
                .document(adminMobile)
                .collection("stores")
                .document(storeId)
                .collection("devices")

            val snapshot = devicesRef.get(Source.SERVER).await()
            val activeDocs = mutableListOf<DocumentSnapshot>()
            var activeOther: ActiveDeviceInfo? = null

            snapshot.documents.forEach { doc ->
                val isActive = doc.getBoolean("isActive") == true
                if (!isActive || doc.id == deviceId) return@forEach

                val lastSeenAt = doc.getLong("lastSeenAt") ?: doc.getLong("lastLoginAt") ?: 0L
                val isExpired = lastSeenAt > 0 && now - lastSeenAt > timeoutMs

                activeDocs.add(doc)

                if (!isExpired && activeOther == null) {
                    activeOther = ActiveDeviceInfo(
                        deviceId = doc.id,
                        manufacturer = doc.getString("manufacturer") ?: "Unknown",
                        model = doc.getString("model") ?: "Unknown",
                        lastLoginAt = doc.getLong("lastLoginAt") ?: 0L
                    )
                }
            }

            if (activeOther != null) {
                return CheckResult.Blocked(activeOther!!)
            }

            val payload = mapOf(
                "deviceId" to deviceId,
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "isActive" to true,
                "lastLoginAt" to System.currentTimeMillis(),
                "lastSeenAt" to System.currentTimeMillis()
            )

            firestore.runBatch { batch ->
                activeDocs.forEach { doc ->
                    batch.update(doc.reference, "isActive", false)
                }
                batch.set(devicesRef.document(deviceId), payload, SetOptions.merge())
            }.await()

            CheckResult.Allowed
        } catch (e: Exception) {
            log("StoreDeviceSessionManager: checkAndActivate failed -> ${e.message}")
            CheckResult.Error(e.message)
        }
    }

    private fun buildDeviceId(adminMobile: String): String {
        val manufacturer = Build.MANUFACTURER.ifBlank { "unknown" }
        val model = Build.MODEL.ifBlank { "unknown" }
        val raw = "${manufacturer}_${model}_${adminMobile}"
        return raw.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }
}
