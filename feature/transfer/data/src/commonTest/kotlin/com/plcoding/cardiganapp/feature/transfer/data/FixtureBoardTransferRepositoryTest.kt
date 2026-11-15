package com.plcoding.cardiganapp.feature.transfer.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.transfer.domain.CheckedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class FixtureBoardTransferRepositoryTest {

    private val repository = FixtureBoardTransferRepository()

    @Test
    fun fixtureExportPassesTheDeviceCheck() = runTest {
        val file = (repository.exportBoard(FixtureBoardTransferRepository.FIXTURE_BOARD_ID) as Result.Success).data
        assertThat(JsonExportFileChecker().check(file.bytes)).isEqualTo(CheckedFile(key = "FIX"))
        assertThat(file.fileName).isEqualTo("FIX-2026-11-04.json")
    }

    @Test
    fun usedKeyClashes() = runTest {
        val file = exportJson(key = "\"crd\"").encodeToByteArray()
        assertThat(repository.importBoard(file, key = null)).isEqualTo(Result.Error(ImportError.KeyAlreadyUsed))
        assertThat(repository.importBoard(file, key = "CRDB")).isInstanceOf(Result.Success::class)
    }
}
