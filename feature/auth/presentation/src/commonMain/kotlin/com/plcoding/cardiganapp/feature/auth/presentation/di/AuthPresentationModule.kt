package com.plcoding.cardiganapp.feature.auth.presentation.di

import com.plcoding.cardiganapp.feature.auth.presentation.login.LoginViewModel
import com.plcoding.cardiganapp.feature.auth.presentation.register.RegisterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModel { params -> LoginViewModel(params.get(), get(), get()) }
    viewModelOf(::RegisterViewModel)
}
