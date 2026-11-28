package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reloads the board list from anywhere in the app (pull to refresh, after an import), so they share
 * one request and one [isReloading] flag. The request runs in the app-wide [scope]: a caller that
 * leaves doesn't cancel it. Callers that overlap join the running reload and get the same result.
 * Showing a failure is up to the caller, for example with [boardsReloadFailedToast].
 */
class BoardListReloader(
    private val repository: BoardRepository,
    private val scope: CoroutineScope,
) {
    private val _isReloading = MutableStateFlow(false)
    val isReloading: StateFlow<Boolean> = _isReloading.asStateFlow()

    private val lock = Mutex()
    private var running: Deferred<EmptyResult<DataError.Network>>? = null

    suspend fun reload(): EmptyResult<DataError.Network> {
        val reload =
            lock.withLock {
                running?.takeIf { it.isActive } ?: startReload().also { running = it }
            }
        return reload.await()
    }

    private fun startReload(): Deferred<EmptyResult<DataError.Network>> {
        _isReloading.value = true
        return scope
            .async(start = CoroutineStart.LAZY) {
                try {
                    repository.refresh()
                } finally {
                    _isReloading.value = false
                }
            }.also { it.start() }
    }
}
