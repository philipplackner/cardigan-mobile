package com.plcoding.cardiganapp.pitest.fixtures

inline fun twice(block: () -> Unit) {
    block()
    block()
}

fun twice(times: Int): Int = times * 2

suspend inline fun <T> retried(block: suspend () -> T): T = block()

inline fun <reified T> nameOf(): String = T::class.simpleName ?: "unknown"

suspend inline fun <T> wrapped(crossinline block: suspend () -> T): T = runSuspend { block() }

suspend fun <T> runSuspend(block: suspend () -> T): T = block()

fun plain(value: Int): Int = value + 1

object InlineObject {
    inline fun double(value: Int): Int = value * 2

    fun triple(value: Int): Int = value * 3
}
