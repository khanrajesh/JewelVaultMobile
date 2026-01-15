package com.velox.jewelvault.data.remort.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetalRateDto(
    val metal: String,
    @SerialName("unit_gm") val unitGm: Double? = null,
    val price: Double? = null
)

@Serializable
data class MetalRatesDataDto(
    val timestamp: String,
    val rates: List<MetalRateDto> = emptyList()
)

@Serializable
data class MetalRatesResponseDto(
    val success: Boolean,
    val message: String? = null,
    val data: MetalRatesDataDto? = null
)
