package com.plcoding.cardiganapp.feature.boards.presentation.renameboard

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_no_longer_exists
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_failed
import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.NameFieldError
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.board
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertResource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/*
 * Break vectors:
 * - the board isn't loaded yet (loads first), is loaded (no second load), isn't in the list, or the
 *   load fails (closes); actions before the board is found
 * - unchanged name, including only added whitespace: closes, no request
 * - empty name: error, no request
 * - success reports the server's name
 * - 404: toast with the old name, closes, no Renamed
 * - network: toast, stays open with the input
 * - server name error under the field
 * - saving ignores Cancel and edits
 * - process death restores the typed name, and the error after a submit
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RenameBoardViewModelTest {

    private val raytracer = board(id = "0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", name = "Raytracer", key = "RAY")
    private val repository = FakeBoardRepository(listOf(raytracer))
    private val toasts = FakeToastController()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(id: String = raytracer.id.toString(), handle: SavedStateHandle = SavedStateHandle()) =
        RenameBoardViewModel(id, repository, toasts, handle)

    @Test
    fun showsTheCurrentNameAndKey() {
        val viewModel = viewModel()
        assertThat(viewModel.state.value).isEqualTo(RenameBoardState(boardName = "Raytracer", key = "RAY", name = "Raytracer"))
    }

    @Test
    fun loadsTheListFirstWhenItIsntLoaded() {
        repository.state.value = null
        repository.serverBoards = listOf(raytracer)
        val viewModel = viewModel()
        assertThat(repository.ensureLoadedCalls).isEqualTo(1)
        assertThat(viewModel.state.value.boardName).isEqualTo("Raytracer")
    }

    @Test
    fun loadedListIsNotLoadedAgain() {
        viewModel()
        assertThat(repository.ensureLoadedCalls).isEqualTo(0)
    }

    @Test
    fun actionsBeforeTheBoardIsFoundAreIgnored() = runTest {
        repository.state.value = null
        repository.serverBoards = listOf(raytracer)
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onAction(RenameBoardAction.NameChange("Path"))
            viewModel.onAction(RenameBoardAction.Save)
            viewModel.onAction(RenameBoardAction.Cancel)
            expectNoEvents()
        }
        assertThat(viewModel.state.value).isEqualTo(RenameBoardState())
        assertThat(repository.renameCalls).isEmpty()
    }

    @Test
    fun missingBoardCloses() = runTest {
        val viewModel = viewModel(id = "00000000-0000-0000-0000-000000000000")
        viewModel.events.test { assertThat(awaitItem()).isEqualTo(RenameBoardEvent.Close) }
        assertThat(viewModel.state.value.boardName).isNull()
    }

    @Test
    fun failedLoadCloses() = runTest {
        repository.state.value = null
        repository.refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        val viewModel = viewModel()
        viewModel.events.test { assertThat(awaitItem()).isEqualTo(RenameBoardEvent.Close) }
    }

    @Test
    fun unchangedNameClosesWithoutARequest() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(RenameBoardAction.NameChange("Raytracer "))
        viewModel.events.test {
            viewModel.onAction(RenameBoardAction.Save)
            assertThat(awaitItem()).isEqualTo(RenameBoardEvent.Close)
        }
        assertThat(repository.renameCalls).isEmpty()
    }

    @Test
    fun emptyNameShowsTheErrorAndSendsNothing() {
        val viewModel = viewModel()
        viewModel.onAction(RenameBoardAction.NameChange(""))
        assertThat(viewModel.state.value.nameError).isNull()
        viewModel.onAction(RenameBoardAction.Save)
        assertThat(viewModel.state.value.nameError).isEqualTo(NameError.Required)
        assertThat(viewModel.state.value.canSave).isFalse()
        viewModel.onAction(RenameBoardAction.NameChange("Path"))
        assertThat(viewModel.state.value.nameError).isNull()
        assertThat(repository.renameCalls).isEmpty()
    }

    @Test
    fun successReportsTheServerName() = runTest {
        repository.renameResult = Result.Success(raytracer.copy(name = "Path tracer"))
        val viewModel = viewModel()
        viewModel.onAction(RenameBoardAction.NameChange("  Path tracer "))
        viewModel.events.test {
            viewModel.onAction(RenameBoardAction.Save)
            assertThat(awaitItem()).isEqualTo(RenameBoardEvent.Renamed("Path tracer"))
        }
        assertThat(repository.renameCalls).containsExactly(raytracer.id to "Path tracer")
        assertThat(toasts.messages).isEmpty()
    }

    @Test
    fun notFoundShowsTheToastAndCloses() = runTest {
        repository.renameResult = Result.Error(BoardSaveError.NotFound)
        val viewModel = viewModel()
        viewModel.onAction(RenameBoardAction.NameChange("Path tracer"))
        viewModel.events.test {
            viewModel.onAction(RenameBoardAction.Save)
            assertThat(awaitItem()).isEqualTo(RenameBoardEvent.Close)
        }
        assertResource(toasts.messages.single().text, Res.string.board_no_longer_exists, "Raytracer")
    }

    @Test
    fun networkFailureShowsTheToastAndStays() = runTest {
        repository.renameResult = Result.Error(BoardSaveError.Network(DataError.Network.SERVER_ERROR))
        val viewModel = viewModel()
        viewModel.onAction(RenameBoardAction.NameChange("Path tracer"))
        viewModel.events.test {
            viewModel.onAction(RenameBoardAction.Save)
            expectNoEvents()
        }
        assertResource(toasts.messages.single().text, Res.string.rename_board_failed, "Raytracer")
        assertThat(viewModel.state.value.name).isEqualTo("Path tracer")
        assertThat(viewModel.state.value.isSaving).isFalse()
    }

    @Test
    fun serverNameErrorShowsUnderTheField() {
        repository.renameResult = Result.Error(BoardSaveError.InvalidFields(name = NameFieldError.Required))
        val viewModel = viewModel()
        viewModel.onAction(RenameBoardAction.NameChange("Path tracer"))
        viewModel.onAction(RenameBoardAction.Save)
        assertThat(viewModel.state.value.nameError).isEqualTo(NameError.Required)
    }

    @Test
    fun savingIgnoresCancelAndEdits() = runTest {
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.onAction(RenameBoardAction.NameChange("Path tracer"))
        viewModel.events.test {
            viewModel.onAction(RenameBoardAction.Save)
            assertThat(viewModel.state.value.isSaving).isTrue()
            viewModel.onAction(RenameBoardAction.Cancel)
            viewModel.onAction(RenameBoardAction.NameChange("Other"))
            expectNoEvents()
            assertThat(viewModel.state.value.name).isEqualTo("Path tracer")
            repository.gate!!.complete(Unit)
            assertThat(awaitItem()).isEqualTo(RenameBoardEvent.Renamed("Path tracer"))
        }
        assertThat(repository.renameCalls).hasSize(1)
    }

    @Test
    fun restoresTheTypedName() {
        val handle = SavedStateHandle()
        viewModel(handle = handle).onAction(RenameBoardAction.NameChange("Path"))
        assertThat(viewModel(handle = handle).state.value.name).isEqualTo("Path")
        assertThat(viewModel(handle = handle).state.value.canSave).isTrue()
    }

    @Test
    fun restoresTheErrorAfterASubmit() {
        val handle = SavedStateHandle()
        viewModel(handle = handle).apply {
            onAction(RenameBoardAction.NameChange(""))
            onAction(RenameBoardAction.Save)
        }
        val restored = viewModel(handle = handle).state.value
        assertThat(restored.submitted).isTrue()
        assertThat(restored.nameError).isEqualTo(NameError.Required)
    }
}
