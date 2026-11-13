package com.plcoding.cardiganapp.core.data.networking

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import io.ktor.client.statement.HttpResponse
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

actual suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError.Network>,
): Result<T, DataError.Network> {
    return try {
        handleResponse(execute())
    } catch (e: Exception) {
        val error = when (e) {
            is UnknownHostException,
            is UnresolvedAddressException,
            is ConnectException -> DataError.Network.NO_INTERNET
            is SocketTimeoutException -> DataError.Network.REQUEST_TIMEOUT
            else -> e.toCommonNetworkError()
        }
        Result.Error(error)
    }
}
