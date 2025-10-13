package com.velox.jewelvault.data.bluetooth

import kotlinx.serialization.Serializable

@Serializable
data class PrinterInfo(
    val name: String?,
    val address: String,
    val method: String, // "RFCOMM" | "GATT" | "A2DP" | "HEADSET" | "HID" | "BONDED"
    val isDefault: Boolean = false,
    val lastConnectedAt: Long? = null,
    val supportedLanguages: List<String> = emptyList(),
    val currentLanguage: String? = null
)
