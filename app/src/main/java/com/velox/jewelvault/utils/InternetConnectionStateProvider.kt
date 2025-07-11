package com.velox.jewelvault.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
import androidx.compose.runtime.MutableState
import com.velox.jewelvault.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun monitorInternetConnection(
    baseViewModel: BaseViewModel,
    speedMonitorJob: MutableState<Job?>,
    coroutineScope: CoroutineScope,
    context: Context,
) {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            baseViewModel.isConnectedState.value = true
            startSpeedMonitor(speedMonitorJob, coroutineScope,context) // Start speed check
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

private fun startSpeedMonitor(speedMonitorJob: MutableState<Job?>, coroutineScope: CoroutineScope,context: Context) {
    stopSpeedMonitor(speedMonitorJob) // Clear previous job if running

    speedMonitorJob.value = coroutineScope.launch {
        while (isActive) {
            val isSlow = !isInternetFast()
            if (isSlow) {
                withMain {
                    Toast.makeText(context, "Internet is slow", Toast.LENGTH_SHORT).show()
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