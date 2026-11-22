package com.plcoding.cardiganapp.feature.boards.presentation.util

import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.boards_refresh_failed
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText

/** The D12 error toast for a failed board list reload, shown by every caller of `BoardListReloader.reload()`. */
fun boardsReloadFailedToast(): ToastMessage =
    ToastMessage(UiText.Resource(Res.string.boards_refresh_failed), ToastType.Error)
