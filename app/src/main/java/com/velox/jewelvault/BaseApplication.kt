package com.velox.jewelvault

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.firestore.FirebaseFirestore
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.AppLogger
import com.velox.jewelvault.utils.DeviceSessionTracker
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class BaseApplication : Application(), Configuration.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeviceSessionEntryPoint {
        fun dataStoreManager(): DataStoreManager
        fun firestore(): FirebaseFirestore
    }

    private var deviceSessionTracker: DeviceSessionTracker? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        // WorkManager will be initialized via workManagerConfiguration
        val entryPoint = EntryPointAccessors.fromApplication(
            this,
            DeviceSessionEntryPoint::class.java
        )
        deviceSessionTracker = DeviceSessionTracker(
            dataStoreManager = entryPoint.dataStoreManager(),
            firestore = entryPoint.firestore(),
            app = this
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(deviceSessionTracker!!)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
