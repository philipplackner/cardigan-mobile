package com.plcoding.cardiganapp.core.data.networking

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorBadURL
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut
import kotlin.test.Test

class NSErrorMappingTest {

    private fun urlError(code: Long) = NSError(domain = NSURLErrorDomain, code = code, userInfo = null)

    @Test
    fun `not connected to internet maps to no internet`() {
        assertThat(urlError(NSURLErrorNotConnectedToInternet).toNetworkError())
            .isEqualTo(DataError.Network.NO_INTERNET)
    }

    @Test
    fun `network connection lost maps to no internet`() {
        assertThat(urlError(NSURLErrorNetworkConnectionLost).toNetworkError())
            .isEqualTo(DataError.Network.NO_INTERNET)
    }

    @Test
    fun `cannot find host maps to no internet`() {
        assertThat(urlError(NSURLErrorCannotFindHost).toNetworkError())
            .isEqualTo(DataError.Network.NO_INTERNET)
    }

    @Test
    fun `cannot connect to host maps to no internet`() {
        assertThat(urlError(NSURLErrorCannotConnectToHost).toNetworkError())
            .isEqualTo(DataError.Network.NO_INTERNET)
    }

    @Test
    fun `dns lookup failed maps to no internet`() {
        assertThat(urlError(NSURLErrorDNSLookupFailed).toNetworkError())
            .isEqualTo(DataError.Network.NO_INTERNET)
    }

    @Test
    fun `timed out maps to request timeout`() {
        assertThat(urlError(NSURLErrorTimedOut).toNetworkError())
            .isEqualTo(DataError.Network.REQUEST_TIMEOUT)
    }

    @Test
    fun `unlisted url error code maps to unknown`() {
        assertThat(urlError(NSURLErrorBadURL).toNetworkError())
            .isEqualTo(DataError.Network.UNKNOWN)
    }

    @Test
    fun `no internet code from a foreign domain maps to unknown`() {
        val error = NSError(domain = NSCocoaErrorDomain, code = NSURLErrorNotConnectedToInternet, userInfo = null)

        assertThat(error.toNetworkError()).isEqualTo(DataError.Network.UNKNOWN)
    }

    @Test
    fun `darwin exception thrown by the engine is mapped through its ns error`() = runTest {
        val client = HttpClientFactory.create(
            MockEngine { throw DarwinHttpRequestException(urlError(NSURLErrorNotConnectedToInternet)) }
        )

        assertThat(client.get<Unit>("/items")).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }
}
