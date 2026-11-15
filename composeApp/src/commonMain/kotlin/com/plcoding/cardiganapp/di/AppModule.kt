package com.plcoding.cardiganapp.di

import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.feature.transfer.domain.BoardKeyOwnerLookup
import com.plcoding.cardiganapp.toast.AppToastController
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module

val appModule = module {
    singleOf(::AppToastController) binds arrayOf(AppToastController::class, ToastController::class)
    // No loaded board list exists until mob-board-list-ui, so the key clash dialog uses the sentence without a name.
    single<BoardKeyOwnerLookup> { BoardKeyOwnerLookup { null } }
}
