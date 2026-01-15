package com.velox.jewelvault.utils.handler

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject



//region Ktor

inline fun <reified T> handleNetworkResponse(crossinline call: suspend () -> HttpResponse): Flow<KtorResource<T>> {
    return flow<KtorResource<T>> {
        emit(KtorResource.Loading(isLoading = true))
        try {
            val response = call.invoke()
            val status = response.status.value
            if (status == 200) {
                emit(KtorResource.Success(response.body()))
            } else {
                Log.e("Ktor", "ClientRequestException: ${response}")
                emit(KtorResource.Error("Call Exception", response.body(), status))
            }

        } catch (e: ClientRequestException) {
            Log.e("Ktor", "ClientRequestException: ${e.message}", e)
            emit(
                KtorResource.Error(
                    "ClientRequestException",
                    e.response.body(),
                    e.response.status.value
                )
            )
        } catch (e: ServerResponseException) {
            Log.e("Ktor", "ServerResponseException: ${e.message}", e)
            emit(
                KtorResource.Error(
                    "ServerResponseException",
                    e.response.body(),
                    e.response.status.value
                )
            )
        } catch (e: ResponseException) {
            Log.e("Ktor", "ResponseException: ${e.message}", e)
            emit(
                KtorResource.Error(
                    "ResponseException",
                    e.response.body(),
                    e.response.status.value
                )
            )
        } catch (e: Exception) {
            Log.e("Ktor", "Unknown network error", e)
            emit(KtorResource.Error(e.message ?: "Unknown Error"))
        }

        emit(KtorResource.Loading(isLoading = false))
    }.flowOn(Dispatchers.IO)
}



internal inline fun <T> handleFlowKtor(
    flow: Flow<KtorResource<T>>,
    crossinline onLoading: suspend (it: Boolean) -> Unit,
    crossinline onFailure: suspend (it: String?, errorObject: JsonObject?, code: Int?) -> Unit,
    crossinline onSuccess: suspend (it: T?) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        flow.collectLatest {
            when (it) {
                is KtorResource.Error -> {

                    CoroutineScope(Dispatchers.Main).launch {
                        onFailure.invoke(it.message, it.errorObject, it.code)
                    }
                }

                is KtorResource.Loading -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        onLoading.invoke(it.isLoading)
                    }
                }

                is KtorResource.Success -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        onSuccess.invoke(it.data)
                    }
                }
            }
        }
    }
}

//endregion

