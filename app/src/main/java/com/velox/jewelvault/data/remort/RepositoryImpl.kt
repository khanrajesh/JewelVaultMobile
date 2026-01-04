package com.velox.jewelvault.data.remort

import com.velox.jewelvault.data.remort.model.MetalRatesResponseDto
import com.velox.jewelvault.utils.handler.KtorResource
import com.velox.jewelvault.utils.handler.handleNetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.Flow

class RepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    ) {
    fun requestMetalRate(
        url: String
    ): Flow<KtorResource<MetalRatesResponseDto>> = handleNetworkResponse {
        httpClient.post(url) {
            setBody(body)
        }
    }

}