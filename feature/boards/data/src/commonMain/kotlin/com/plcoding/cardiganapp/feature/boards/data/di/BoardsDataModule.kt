package com.plcoding.cardiganapp.feature.boards.data.di

import com.plcoding.cardiganapp.core.domain.session.SessionScopedStore
import com.plcoding.cardiganapp.feature.boards.data.InMemoryBoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.binds
import org.koin.dsl.module

val boardsDataModule = module {
    single { InMemoryBoardRepository(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) } binds arrayOf(BoardRepository::class, SessionScopedStore::class)
}
