package com.plcoding.cardiganapp.feature.boards.data.di

import com.plcoding.cardiganapp.core.domain.label.LabelSource
import com.plcoding.cardiganapp.core.domain.session.SessionScopedStore
import com.plcoding.cardiganapp.feature.boards.data.InMemoryBoardRepository
import com.plcoding.cardiganapp.feature.boards.data.NotConnectedRemoteBoardDataSource
import com.plcoding.cardiganapp.feature.boards.data.detail.NotConnectedBoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.data.detail.NotConnectedCardCreateRepository
import com.plcoding.cardiganapp.feature.boards.data.detail.NotConnectedLabelSource
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.binds
import org.koin.dsl.module

val boardsDataModule =
    module {
        // TODO(mob-boards-connect): bind the Ktor board data source.
        single<RemoteBoardDataSource> { NotConnectedRemoteBoardDataSource() }
        single { InMemoryBoardRepository(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) } binds
            arrayOf(BoardRepository::class, SessionScopedStore::class)
        // TODO(mob-boards-connect): bind the Ktor board detail repository.
        single<BoardDetailRepository> { NotConnectedBoardDetailRepository() }
        // TODO(mob-cards-connect): bind the Ktor card repository.
        single<CardCreateRepository> { NotConnectedCardCreateRepository() }
        // TODO(labels): bind the labels feature's LabelSource once mob-labels-ui lands.
        single<LabelSource> { NotConnectedLabelSource() }
    }
