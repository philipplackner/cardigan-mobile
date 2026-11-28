package com.plcoding.cardiganapp.feature.transfer.presentation.di

import com.plcoding.cardiganapp.feature.transfer.presentation.exportboard.ExportBoardViewModel
import com.plcoding.cardiganapp.feature.transfer.presentation.importboard.ImportBoardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val transferPresentationModule =
    module {
        viewModelOf(::ImportBoardViewModel)
        viewModel { (boardId: String, boardName: String) ->
            ExportBoardViewModel(boardId, boardName, get(), get())
        }
    }
