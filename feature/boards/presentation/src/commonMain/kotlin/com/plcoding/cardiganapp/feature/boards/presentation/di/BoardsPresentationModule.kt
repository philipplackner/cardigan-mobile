package com.plcoding.cardiganapp.feature.boards.presentation.di

import com.plcoding.cardiganapp.feature.boards.presentation.boardlist.BoardListViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.deleteboard.DeleteBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.detail.BoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListReloader
import com.plcoding.cardiganapp.feature.boards.presentation.newboard.NewBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.renameboard.RenameBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.sidebar.BoardsSidebarViewModel
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val boardsPresentationModule = module {
    single { BoardListReloader(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    single<Clock> { Clock.System }
    factory<TimeZone> { TimeZone.currentSystemDefault() }

    viewModelOf(::BoardListViewModel)
    viewModelOf(::NewBoardViewModel)
    viewModelOf(::BoardsSidebarViewModel)
    viewModelOf(::BoardViewModel)
    viewModel { params -> RenameBoardViewModel(params.get(), get(), get(), get()) }
    viewModel { params -> DeleteBoardViewModel(params.get(), get(), get()) }
}
