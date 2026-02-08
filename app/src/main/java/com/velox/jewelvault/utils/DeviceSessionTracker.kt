package com.velox.jewelvault.utils

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.velox.jewelvault.MainActivity
import com.velox.jewelvault.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DeviceSessionTracker(
    private val dataStoreManager: DataStoreManager,
    private val firestore: FirebaseFirestore,
    private val app: Application
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private val wasInBackground = AtomicBoolean(false)


    override fun onStart(owner: LifecycleOwner) {
//        if (wasInBackground.getAndSet(false)) {
//            // Force restart flow from beginning
//            val i = Intent(app, MainActivity::class.java).apply {
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                putExtra("RESET_FLOW", true)
//            }
//            app.startActivity(i)
////            updateDeviceActiveFlag(isActive = true)
////             startHeartbeat() // enable if you really want periodic keepalive only in foreground
//        }
    }



    override fun onStop(owner: LifecycleOwner) {
        wasInBackground.set(true)
        stopHeartbeat()
        updateDeviceActiveFlag(isActive = false)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        wasInBackground.set(false)
    }

    fun updateDeviceActiveFlag(isActive: Boolean) {
        scope.launch {
            try {
                val adminMobile = dataStoreManager.getAdminInfo().third.first()
                if (adminMobile.isBlank()) return@launch
                val deviceId = buildDeviceId(adminMobile)
                val devicesRef = firestore.collection("users")
                    .document(adminMobile)
                    .collection("devices")

                val payload = mapOf(
                    "deviceId" to deviceId,
                    "manufacturer" to Build.MANUFACTURER,
                    "model" to Build.MODEL,
                    "isActive" to isActive,
                    "lastSeenAt" to System.currentTimeMillis()
                )

                if (isActive) {
                    val activeSnapshot = devicesRef
                        .whereEqualTo("isActive", true)
                        .get()
                        .await()

                    firestore.runBatch { batch ->
                        activeSnapshot.documents.forEach { doc ->
                            if (doc.id != deviceId) {
                                batch.update(doc.reference, "isActive", false)
                            }
                        }
                        batch.set(devicesRef.document(deviceId), payload, SetOptions.merge())
                    }.await()
                } else {
                    devicesRef.document(deviceId).set(payload, SetOptions.merge()).await()
                }
            } catch (_: Exception) {
                // Best-effort; ignore failures to avoid blocking app lifecycle.
            }
        }
    }

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (true) {
                updateDeviceActiveFlag(isActive = true)
                delay(TimeUnit.MINUTES.toMillis(1))
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
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
