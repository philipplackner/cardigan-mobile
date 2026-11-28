package com.plcoding.cardiganapp.feature.transfer.data.di

import com.plcoding.cardiganapp.feature.transfer.data.JsonExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val transferDataModule =
    module {
        singleOf(::JsonExportFileChecker) bind ExportFileChecker::class
    }
