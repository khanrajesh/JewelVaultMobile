package com.velox.jewelvault.data.remort

import com.velox.jewelvault.data.remort.model.MetalRatesResponseDto
import com.velox.jewelvault.utils.handler.KtorResource
import com.velox.jewelvault.utils.handler.handleNetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow

class RepositoryImpl(
    private val httpClient: HttpClient,
) {
    companion object {
        private const val METAL_RATE_PATH = "https://jewelvaultbackend-44960140033.asia-south1.run.app/api/v1/metal-rate"
    }

    fun requestMetalRate(): Flow<KtorResource<MetalRatesResponseDto>> = handleNetworkResponse {
        httpClient.get(METAL_RATE_PATH)
    }
}
