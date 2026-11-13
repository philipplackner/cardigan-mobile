package com.plcoding.cardiganapp.core.data.di

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.data.BuildKonfig
import com.plcoding.cardiganapp.core.data.networking.get
import com.plcoding.cardiganapp.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test

class CoreDataModuleIntegrationTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `http client from koin sends requests through the provided engine to the base url`() = runTest {
        val engine = MockEngine { respond(content = "", status = HttpStatusCode.NoContent) }
        val koin = startKoin {
            modules(
                coreDataModule,
                module { single<HttpClientEngine> { engine } },
            )
        }.koin

        val result = koin.get<HttpClient>().get<Unit>("/ping")

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(engine.requestHistory.map { it.url.toString() })
            .containsExactly("${BuildKonfig.BASE_URL}/ping")
    }
}
