package com.plcoding.cardiganapp.di

import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.transfer.domain.BoardKeyOwnerLookup
import com.plcoding.cardiganapp.session.AppViewModel
import com.plcoding.cardiganapp.toast.AppToastController
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module

val appModule = module {
    singleOf(::AppToastController) binds arrayOf(AppToastController::class, ToastController::class)
    viewModel { AppViewModel(get(), getAll()) }
    single<BoardKeyOwnerLookup> {
        val repository = get<BoardRepository>()
        BoardKeyOwnerLookup { key -> repository.boards.value?.firstOrNull { it.key == key }?.name }
    }
}
