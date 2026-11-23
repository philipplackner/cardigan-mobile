package com.plcoding.cardiganapp.feature.boards.data.fixture

import com.plcoding.cardiganapp.core.domain.label.LabelSource
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import org.koin.dsl.module

/** TEMPORARY: binds the board view fixtures (D6) until the connect changes bind the real ones. */
val fixtureBoardViewModule = module {
    single { FixtureBoardStore(get()) }
    single<BoardDetailRepository> { FixtureBoardDetailRepository(get()) }
    single<CardCreateRepository> { FixtureCardCreateRepository(get()) }
    single<LabelSource> { FixtureLabelSource() }
}
