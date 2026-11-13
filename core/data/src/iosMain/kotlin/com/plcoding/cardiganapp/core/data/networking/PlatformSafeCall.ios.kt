package com.plcoding.cardiganapp.core.data.networking

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.client.statement.HttpResponse
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

actual suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError.Network>,
): Result<T, DataError.Network> {
    return try {
        handleResponse(execute())
    } catch (e: Exception) {
        val error = when (e) {
            is DarwinHttpRequestException -> e.origin.toNetworkError()
            else -> e.toCommonNetworkError()
        }
        Result.Error(error)
    }
}

internal fun NSError.toNetworkError(): DataError.Network {
    if (domain != NSURLErrorDomain) {
        return DataError.Network.UNKNOWN
    }
    return when (code) {
        NSURLErrorNotConnectedToInternet,
        NSURLErrorNetworkConnectionLost,
        NSURLErrorCannotFindHost,
        NSURLErrorCannotConnectToHost,
        NSURLErrorDNSLookupFailed -> DataError.Network.NO_INTERNET
        NSURLErrorTimedOut -> DataError.Network.REQUEST_TIMEOUT
        else -> DataError.Network.UNKNOWN
    }
}
