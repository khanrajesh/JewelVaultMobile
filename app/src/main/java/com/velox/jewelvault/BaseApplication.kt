package com.velox.jewelvault

import android.app.Application
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // WorkManager will be initialized via workManagerConfiguration
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}