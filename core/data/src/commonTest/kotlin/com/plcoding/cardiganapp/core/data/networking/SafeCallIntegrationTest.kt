package com.plcoding.cardiganapp.core.data.networking

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.plcoding.cardiganapp.core.data.BuildKonfig
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test

class SafeCallIntegrationTest {

    @Serializable
    private data class ItemFixture(val id: String, val name: String)

    @Serializable
    private data class RenameFixture(val name: String)

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun clientWith(handler: MockRequestHandler) =
        HttpClientFactory.create(MockEngine(handler))

    private fun clientRespondingWith(status: HttpStatusCode, content: String = "") =
        clientWith { respond(content = content, status = status, headers = jsonHeaders) }

    @Test
    fun `every status from the error catalog maps to its network error`() = runTest {
        val expected = mapOf(
            400 to DataError.Network.BAD_REQUEST,
            401 to DataError.Network.UNAUTHORIZED,
            403 to DataError.Network.FORBIDDEN,
            404 to DataError.Network.NOT_FOUND,
            408 to DataError.Network.REQUEST_TIMEOUT,
            409 to DataError.Network.CONFLICT,
            413 to DataError.Network.PAYLOAD_TOO_LARGE,
            415 to DataError.Network.UNSUPPORTED_MEDIA_TYPE,
            422 to DataError.Network.UNPROCESSABLE_ENTITY,
            429 to DataError.Network.TOO_MANY_REQUESTS,
            500 to DataError.Network.SERVER_ERROR,
            503 to DataError.Network.SERVICE_UNAVAILABLE,
        )

        expected.forEach { (status, error) ->
            val result = clientRespondingWith(HttpStatusCode.fromValue(status)).get<ItemFixture>("/items")

            assertThat(result, name = "status $status").isEqualTo(Result.Error(error))
        }
    }

    @Test
    fun `other 5xx statuses map to server error`() = runTest {
        listOf(502, 504).forEach { status ->
            val result = clientRespondingWith(HttpStatusCode.fromValue(status)).get<ItemFixture>("/items")

            assertThat(result, name = "status $status").isEqualTo(Result.Error(DataError.Network.SERVER_ERROR))
        }
    }

    @Test
    fun `unlisted 4xx statuses map to unknown`() = runTest {
        listOf(418, 451).forEach { status ->
            val result = clientRespondingWith(HttpStatusCode.fromValue(status)).get<ItemFixture>("/items")

            assertThat(result, name = "status $status").isEqualTo(Result.Error(DataError.Network.UNKNOWN))
        }
    }

    @Test
    fun `204 without body maps to unit success`() = runTest {
        val client = clientWith { respond(content = "", status = HttpStatusCode.NoContent) }

        val result = client.delete<Unit>("/items/1")

        assertThat(result).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun `unknown fields in a success body are ignored`() = runTest {
        val client = clientRespondingWith(
            status = HttpStatusCode.OK,
            content = """{"id":"1","name":"Backlog","position":"a0","nested":{"x":[1,2]}}""",
        )

        val result = client.get<ItemFixture>("/items/1")

        assertThat(result).isEqualTo(Result.Success(ItemFixture(id = "1", name = "Backlog")))
    }

    @Test
    fun `malformed json in a success body maps to serialization error`() = runTest {
        val client = clientRespondingWith(status = HttpStatusCode.OK, content = """{"id":"1","name":""")

        val result = client.get<ItemFixture>("/items/1")

        assertThat(result).isEqualTo(Result.Error(DataError.Network.SERIALIZATION))
    }

    @Test
    fun `error body that is not problem details still maps by status`() = runTest {
        val client = clientWith {
            respond(
                content = "<html><body>Bad Gateway</body></html>",
                status = HttpStatusCode.BadGateway,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString()),
            )
        }

        val result = client.get<ItemFixture>("/items")

        assertThat(result).isEqualTo(Result.Error(DataError.Network.SERVER_ERROR))
    }

    @Test
    fun `request timeout exception maps to request timeout`() = runTest {
        val client = clientWith { request -> throw HttpRequestTimeoutException(request) }

        val result = client.get<ItemFixture>("/items")

        assertThat(result).isEqualTo(Result.Error(DataError.Network.REQUEST_TIMEOUT))
    }

    @Test
    fun `unexpected exception maps to unknown`() = runTest {
        val client = clientWith { throw IllegalStateException("engine broke") }

        val result = client.get<ItemFixture>("/items")

        assertThat(result).isEqualTo(Result.Error(DataError.Network.UNKNOWN))
    }

    @Test
    fun `cancellation is rethrown instead of mapped`() = runTest {
        val client = clientWith { throw CancellationException("cancelled") }

        assertFailure {
            client.get<ItemFixture>("/items")
        }.isInstanceOf(CancellationException::class)
    }

    @Test
    fun `post patch and delete send their method and json body`() = runTest {
        val requests = mutableListOf<String>()
        val client = clientWith { request ->
            requests += "${request.method.value} ${request.body.contentType} ${request.body.toByteArray().decodeToString()}"
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        client.post<RenameFixture, Unit>("/items", RenameFixture("New"))
        client.patch<RenameFixture, Unit>("/items/1", RenameFixture("Renamed"))
        client.delete<Unit>("/items/1")

        assertThat(requests).containsExactly(
            "${HttpMethod.Post.value} application/json {\"name\":\"New\"}",
            "${HttpMethod.Patch.value} application/json {\"name\":\"Renamed\"}",
            "${HttpMethod.Delete.value} null ",
        )
    }

    @Test
    fun `routes are joined to the base url with exactly one slash`() = runTest {
        val urls = mutableListOf<String>()
        val client = clientWith { request ->
            urls += request.url.toString()
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        client.get<Unit>("/boards")
        client.get<Unit>("boards")
        client.get<Unit>("${BuildKonfig.BASE_URL}/boards")
        client.get<Unit>("/boards", queryParameters = mapOf("q" to "a b&c", "skip" to null))

        val base = BuildKonfig.BASE_URL
        assertThat(urls).containsExactly(
            "$base/boards",
            "$base/boards",
            "$base/boards",
            "$base/boards?q=a+b%26c",
        )
    }

    @Test
    fun `base url with trailing slash is joined without a double slash`() {
        val base = "http://localhost:8080/api/v1"

        assertThat(constructRoute("/boards", baseUrl = "$base/")).isEqualTo("$base/boards")
        assertThat(constructRoute("boards", baseUrl = "$base/")).isEqualTo("$base/boards")
    }
}
