package com.plcoding.cardiganapp.core.data.networking

import com.plcoding.cardiganapp.core.data.BuildKonfig
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlin.reflect.typeOf

suspend inline fun <reified Response> HttpClient.get(
    route: String,
    queryParameters: Map<String, Any?> = mapOf(),
): Result<Response, DataError.Network> {
    return safeCall {
        get {
            url(constructRoute(route))
            queryParameters.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified Request, reified Response> HttpClient.post(
    route: String,
    body: Request,
): Result<Response, DataError.Network> {
    return safeCall {
        post {
            url(constructRoute(route))
            setBody(body)
        }
    }
}

suspend inline fun <reified Request, reified Response> HttpClient.patch(
    route: String,
    body: Request,
): Result<Response, DataError.Network> {
    return safeCall {
        patch {
            url(constructRoute(route))
            setBody(body)
        }
    }
}

suspend inline fun <reified Response> HttpClient.delete(
    route: String,
    queryParameters: Map<String, Any?> = mapOf(),
): Result<Response, DataError.Network> {
    return safeCall {
        delete {
            url(constructRoute(route))
            queryParameters.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified T> safeCall(
    noinline execute: suspend () -> HttpResponse,
): Result<T, DataError.Network> {
    return platformSafeCall(
        execute = execute,
        handleResponse = { response -> responseToResult(response) },
    )
}

/**
 * Runs the request and maps exceptions to [DataError.Network]. Connectivity exceptions differ
 * per engine (OkHttp on Android, Darwin on iOS), so each platform adds its own mapping on top of
 * [toCommonNetworkError]. [CancellationException] is always rethrown.
 */
expect suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Result<T, DataError.Network>,
): Result<T, DataError.Network>

/**
 * Maps a response to a [Result] by HTTP status only. Error bodies are never parsed, so bodies
 * that aren't problem details (e.g. from a proxy) are handled the same way.
 */
suspend inline fun <reified T> responseToResult(
    response: HttpResponse,
): Result<T, DataError.Network> {
    return when (response.status.value) {
        in 200..299 -> {
            if (typeOf<T>() == typeOf<Unit>()) {
                Result.Success(Unit as T)
            } else {
                Result.Success(response.body<T>())
            }
        }
        400 -> Result.Error(DataError.Network.BAD_REQUEST)
        401 -> Result.Error(DataError.Network.UNAUTHORIZED)
        403 -> Result.Error(DataError.Network.FORBIDDEN)
        404 -> Result.Error(DataError.Network.NOT_FOUND)
        408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
        409 -> Result.Error(DataError.Network.CONFLICT)
        413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
        415 -> Result.Error(DataError.Network.UNSUPPORTED_MEDIA_TYPE)
        422 -> Result.Error(DataError.Network.UNPROCESSABLE_ENTITY)
        429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
        503 -> Result.Error(DataError.Network.SERVICE_UNAVAILABLE)
        in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
        else -> Result.Error(DataError.Network.UNKNOWN)
    }
}

/**
 * Joins [baseUrl] and [route]. A route that already starts with [baseUrl] is returned unchanged.
 */
fun constructRoute(route: String, baseUrl: String = BuildKonfig.BASE_URL): String {
    val base = baseUrl.trimEnd('/')
    return when {
        route.startsWith(base) -> route
        route.startsWith("/") -> base + route
        else -> "$base/$route"
    }
}

internal fun Exception.toCommonNetworkError(): DataError.Network {
    return when (this) {
        is CancellationException -> throw this
        is HttpRequestTimeoutException -> DataError.Network.REQUEST_TIMEOUT
        // Ktor wraps body decoding failures in JsonConvertException, not SerializationException.
        is SerializationException, is ContentConvertException -> DataError.Network.SERIALIZATION
        else -> DataError.Network.UNKNOWN
    }
}
