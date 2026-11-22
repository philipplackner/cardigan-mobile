package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.boards_refresh_failed
import com.plcoding.cardiganapp.core.domain.util.onFailure
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Reloads the board list from anywhere in the app (pull to refresh, after an import) and shows the
 * refresh error toast when it fails. Runs in the app-wide [scope], so leaving a screen doesn't
 * cancel it. A call while a reload runs joins it.
 */
class BoardListReloader(
    private val repository: BoardRepository,
    private val toastController: ToastController,
    private val scope: CoroutineScope,
) {
    private val _isReloading = MutableStateFlow(false)
    val isReloading: StateFlow<Boolean> = _isReloading.asStateFlow()

    private var job: Job? = null

    fun reload() {
        if (job?.isActive == true) return
        _isReloading.value = true
        job = scope.launch {
            try {
                repository.refresh().onFailure {
                    toastController.show(ToastMessage(UiText.Resource(Res.string.boards_refresh_failed), ToastType.Error))
                }
            } finally {
                _isReloading.value = false
            }
        }
    }
}
