package com.plcoding.cardiganapp.feature.transfer.presentation.fakes

import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.FileSharer

class FakeFileSharer : FileSharer {
    val shared = mutableListOf<ExportedFile>()

    override suspend fun share(file: ExportedFile) {
        shared += file
    }
}
