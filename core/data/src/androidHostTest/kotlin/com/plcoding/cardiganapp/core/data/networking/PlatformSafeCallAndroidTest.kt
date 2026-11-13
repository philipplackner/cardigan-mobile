package com.plcoding.cardiganapp.core.data.networking

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlin.test.Test

class PlatformSafeCallAndroidTest {

    private suspend fun resultFor(exception: Exception): Result<Unit, DataError.Network> {
        val client = HttpClientFactory.create(MockEngine { throw exception })
        return client.get<Unit>("/items")
    }

    @Test
    fun `unknown host maps to no internet`() = runTest {
        assertThat(resultFor(UnknownHostException("cardigan.local")))
            .isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }

    @Test
    fun `unresolved address maps to no internet`() = runTest {
        assertThat(resultFor(UnresolvedAddressException()))
            .isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }

    @Test
    fun `refused connection maps to no internet`() = runTest {
        assertThat(resultFor(ConnectException("Connection refused")))
            .isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }

    @Test
    fun `socket timeout maps to request timeout`() = runTest {
        assertThat(resultFor(SocketTimeoutException("timeout")))
            .isEqualTo(Result.Error(DataError.Network.REQUEST_TIMEOUT))
    }
}
