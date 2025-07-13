package com.velox.jewelvault.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

import androidx.compose.runtime.MutableState
import com.velox.jewelvault.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

fun monitorInternetConnection(
    baseViewModel: BaseViewModel,
    speedMonitorJob: MutableState<Job?>,
    coroutineScope: CoroutineScope,
    context: Context,
    dataStoreManager: DataStoreManager? = null,
) {

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            baseViewModel.isConnectedState.value = true
            // Check if speed monitoring is enabled before starting it
            coroutineScope.launch {
                try {
                        startSpeedMonitor(speedMonitorJob, coroutineScope, baseViewModel,dataStoreManager)
                } catch (e: Exception) {
                    // If we can't check the setting, don't start speed monitoring
                    e.printStackTrace()
                }
            }
        }

        override fun onLost(network: Network) {
            baseViewModel.isConnectedState.value = false
            stopSpeedMonitor(speedMonitorJob)
        }

        override fun onUnavailable() {
            baseViewModel.isConnectedState.value = false
            stopSpeedMonitor(speedMonitorJob)
        }
    })
}

private fun startSpeedMonitor(
    speedMonitorJob: MutableState<Job?>,
    coroutineScope: CoroutineScope,
    baseViewModel: BaseViewModel,
    dataStoreManager: DataStoreManager?
) {
    stopSpeedMonitor(speedMonitorJob) // Clear previous job if running

    speedMonitorJob.value = coroutineScope.launch {
        while (isActive) {
            val speedMonitoringEnabled = dataStoreManager?.getValue(DataStoreManager.NETWORK_SPEED_MONITORING, true)?.first() ?: true
            if (speedMonitoringEnabled) {
                val isSlow = !isInternetFast()
                if (isSlow) {
                    withMain {
                        baseViewModel.snackMessage = "Internet is slow"
                    }
                }

            }
            delay(5000L) // Repeat every 5 seconds
        }
    }
}

private fun stopSpeedMonitor(speedMonitorJob: MutableState<Job?>) {
    speedMonitorJob.value?.cancel()
    speedMonitorJob.value = null
}

private fun isInternetFast(): Boolean {
    return try {
        val start = System.currentTimeMillis()
        val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 google.com")
        val exit = process.waitFor()
        val duration = System.currentTimeMillis() - start
        exit == 0 && duration < 1000
    } catch (e: Exception) {
        false
    }
}