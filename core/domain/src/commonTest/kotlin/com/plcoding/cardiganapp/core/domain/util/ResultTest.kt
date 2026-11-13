package com.plcoding.cardiganapp.core.domain.util

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.fail

class ResultTest {

    private enum class TestError : Error { BOOM }

    @Test
    fun `actions never run on the wrong branch`() {
        val error: Result<Int, TestError> = Result.Error(TestError.BOOM)
        val success: Result<Int, TestError> = Result.Success(1)

        error
            .map { fail("map must not run on Error") }
            .onSuccess { fail("onSuccess must not run on Error") }
        success.onFailure { fail("onFailure must not run on Success") }
    }

    @Test
    fun `map on error keeps the original error`() {
        val result: Result<Int, TestError> = Result.Error(TestError.BOOM)

        val mapped = result.map { it.toString() }

        assertThat(mapped).isEqualTo(Result.Error(TestError.BOOM))
    }

    @Test
    fun `onSuccess and onFailure return the same instance they were called on`() {
        val success: Result<Int, TestError> = Result.Success(1)
        val error: Result<Int, TestError> = Result.Error(TestError.BOOM)

        assertThat(success.onSuccess { }).isSameInstanceAs(success)
        assertThat(success.onFailure { }).isSameInstanceAs(success)
        assertThat(error.onSuccess { }).isSameInstanceAs(error)
        assertThat(error.onFailure { }).isSameInstanceAs(error)
    }

    @Test
    fun `chained calls run in order on the mapped value and only one branch fires`() {
        val calls = mutableListOf<String>()

        Result.Success(2)
            .map { it * 10 }
            .onSuccess { calls += "success:$it" }
            .onFailure { calls += "failure:$it" }
            .map { it + 1 }
            .onSuccess { calls += "success:$it" }

        assertThat(calls).containsExactly("success:20", "success:21")
    }

    @Test
    fun `asEmptyResult turns success data into Unit and keeps errors`() {
        val success: Result<String, TestError> = Result.Success("data")
        val error: Result<String, TestError> = Result.Error(TestError.BOOM)

        assertThat(success.asEmptyResult()).isEqualTo(Result.Success(Unit))
        assertThat(error.asEmptyResult()).isEqualTo(Result.Error(TestError.BOOM))
    }

    @Test
    fun `null data is still a success`() {
        val result: Result<String?, TestError> = Result.Success(null)
        var received = false

        val mapped = result
            .onSuccess {
                assertThat(it).isNull()
                received = true
            }
            .onFailure { fail("null data must not be treated as failure") }
            .map { it ?: "fallback" }

        assertThat(received).isTrue()
        assertThat(mapped).isEqualTo(Result.Success("fallback"))
    }

    @Test
    fun `exception thrown by mapper propagates instead of becoming an error`() {
        val result: Result<Int, TestError> = Result.Success(1)

        assertFailure {
            result.map { throw IllegalStateException("mapper failed") }
        }.isInstanceOf(IllegalStateException::class)
    }
}
