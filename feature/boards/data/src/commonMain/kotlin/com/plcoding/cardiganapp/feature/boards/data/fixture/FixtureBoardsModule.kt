package com.plcoding.cardiganapp.feature.boards.data.fixture

import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import org.koin.dsl.module

/** TEMPORARY: binds [FixtureBoardDataSource] until `mob-boards-connect` binds the Ktor data source. */
val fixtureBoardsModule = module {
    single<RemoteBoardDataSource> { FixtureBoardDataSource() }
}
