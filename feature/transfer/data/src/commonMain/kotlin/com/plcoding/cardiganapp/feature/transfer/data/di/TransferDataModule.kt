package com.plcoding.cardiganapp.feature.transfer.data.di

import com.plcoding.cardiganapp.feature.transfer.data.FixtureBoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.data.JsonExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val transferDataModule = module {
    singleOf(::JsonExportFileChecker) bind ExportFileChecker::class
}

/** TEMPORARY: debug builds only, until mob-import-export-connect adds the real repository (task group 10 removes it). */
val transferFixtureDataModule = module {
    singleOf(::FixtureBoardTransferRepository) bind BoardTransferRepository::class
}
