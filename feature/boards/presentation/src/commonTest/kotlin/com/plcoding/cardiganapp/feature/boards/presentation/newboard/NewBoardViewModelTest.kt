package com.plcoding.cardiganapp.feature.boards.presentation.newboard

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
import cardiganapp.feature.boards.presentation.generated.resources.new_board_failed
import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.KeyFieldError
import com.plcoding.cardiganapp.feature.boards.domain.NameFieldError
import com.plcoding.cardiganapp.feature.boards.presentation.components.BoardKeyFieldError
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
 * - prefill follows the name until the key is edited, then never again
 * - lowercase key input shows uppercase
 * - no errors before the first submit, live after it
 * - name: whitespace only, line break, 101 code points, 99 letters + emoji
 * - key: empty, too short, digit, local clash with a loaded board
 * - no request when a check fails; normalized name sent
 * - saving: every action ignored, including Cancel; Create disabled
 * - server errors: key ALREADY_USED, TOO_LONG, unknown code, name invalid, name TOO_LONG with the server max
 * - network failure: toast, dialog stays, no automatic repeat
 * - success: Created with the server's values
 * - process death: fields restored after typing alone, keyEdited kept, errors shown again after a submit
 * - board list not loaded: no local clash check, the request goes out
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewBoardViewModelTest {

    private val repository = FakeBoardRepository(listOf(board(name = "Cardigan v1", key = "CRD")))
    private val toasts = FakeToastController()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = NewBoardViewModel(repository, toasts, handle)

    private val NewBoardViewModel.s get() = state.value

    private fun NewBoardViewModel.type(name: String? = null, key: String? = null) {
        name?.let { onAction(NewBoardAction.NameChange(it)) }
        key?.let { onAction(NewBoardAction.KeyChange(it)) }
    }

    @Test
    fun opensEmptyWithCreateEnabled() {
        val viewModel = viewModel()
        assertThat(viewModel.s).isEqualTo(NewBoardState())
        assertThat(viewModel.s.canCreate).isTrue()
    }

    @Test
    fun keyFollowsTheName() {
        val viewModel = viewModel()
        viewModel.type(name = "Garden irrigation")
        assertThat(viewModel.s.key).isEqualTo("GARD")
        viewModel.type(name = "Ärger")
        assertThat(viewModel.s.key).isEqualTo("ARGE")
        viewModel.type(name = "X 1")
        assertThat(viewModel.s.key).isEqualTo("")
    }

    @Test
    fun editedKeyIsKept() {
        val viewModel = viewModel()
        viewModel.type(name = "Garden irrigation", key = "GI")
        viewModel.type(name = "Something else")
        assertThat(viewModel.s.key).isEqualTo("GI")
        viewModel.type(key = "")
        viewModel.type(name = "Garden")
        assertThat(viewModel.s.key).isEqualTo("")
    }

    @Test
    fun lowercaseKeyShowsUppercase() {
        val viewModel = viewModel()
        viewModel.type(key = "gard")
        assertThat(viewModel.s.key).isEqualTo("GARD")
    }

    @Test
    fun noErrorsBeforeSubmit() {
        val viewModel = viewModel()
        viewModel.type(name = "Garden", key = "")
        assertThat(viewModel.s.keyError).isNull()
        assertThat(viewModel.s.canCreate).isTrue()
    }

    @Test
    fun emptySubmitShowsErrorsAndSendsNothing() {
        val viewModel = viewModel()
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.nameError).isEqualTo(NameError.Required)
        assertThat(viewModel.s.keyError).isEqualTo(BoardKeyFieldError.Rule(BoardKeyError.Required))
        assertThat(viewModel.s.canCreate).isFalse()
        assertThat(repository.createCalls).isEmpty()
    }

    @Test
    fun errorsUpdateLiveAfterSubmit() {
        val viewModel = viewModel()
        viewModel.onAction(NewBoardAction.Create)
        viewModel.type(name = "Garden irrigation")
        assertThat(viewModel.s.nameError).isNull()
        assertThat(viewModel.s.keyError).isNull()
        assertThat(viewModel.s.canCreate).isTrue()
        viewModel.type(key = "G")
        assertThat(viewModel.s.keyError).isEqualTo(BoardKeyFieldError.Rule(BoardKeyError.TooShort))
    }

    @Test
    fun nameRules() {
        val viewModel = viewModel()
        viewModel.type(key = "GARD")
        viewModel.type(name = "   ")
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.nameError).isEqualTo(NameError.Required)
        viewModel.type(name = "Line\nbreak")
        assertThat(viewModel.s.nameError).isEqualTo(NameError.InvalidCharacter)
        viewModel.type(name = "a".repeat(101))
        assertThat(viewModel.s.nameError).isEqualTo(NameError.TooLong(100))
        viewModel.type(name = "a".repeat(99) + "👍")
        assertThat(viewModel.s.nameError).isNull()
    }

    @Test
    fun digitInKey() {
        val viewModel = viewModel()
        viewModel.type(name = "Garden", key = "CR1")
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.keyError).isEqualTo(BoardKeyFieldError.Rule(BoardKeyError.InvalidFormat))
    }

    @Test
    fun localClashSendsNothing() {
        val viewModel = viewModel()
        viewModel.type(name = "Another", key = "crd")
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.keyError).isEqualTo(BoardKeyFieldError.AlreadyUsed("CRD"))
        assertThat(repository.createCalls).isEmpty()
    }

    @Test
    fun sendsTheNormalizedName() = runTest {
        val viewModel = viewModel()
        viewModel.type(name = "  Garden irrigation  ")
        viewModel.events.test {
            viewModel.onAction(NewBoardAction.Create)
            val created = awaitItem() as NewBoardEvent.Created
            assertThat(created.name).isEqualTo("Garden irrigation")
            assertThat(created.key).isEqualTo("GARD")
        }
        assertThat(repository.createCalls).containsExactly("Garden irrigation" to "GARD")
    }

    @Test
    fun createdUsesTheServerValues() = runTest {
        val server = board(id = "0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", name = "Garden", key = "GARD")
        repository.createResult = Result.Success(server)
        val viewModel = viewModel()
        viewModel.type(name = "Garden ")
        viewModel.events.test {
            viewModel.onAction(NewBoardAction.Create)
            assertThat(awaitItem()).isEqualTo(NewBoardEvent.Created(server.id.toString(), "Garden", "GARD"))
        }
        assertThat(toasts.messages).isEmpty()
    }

    @Test
    fun savingIgnoresEveryAction() = runTest {
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.type(name = "Garden")
        viewModel.events.test {
            viewModel.onAction(NewBoardAction.Create)
            assertThat(viewModel.s.isSaving).isTrue()
            assertThat(viewModel.s.canCreate).isFalse()

            viewModel.onAction(NewBoardAction.Cancel)
            viewModel.type(name = "Changed", key = "XY")
            viewModel.onAction(NewBoardAction.Create)
            expectNoEvents()
            assertThat(viewModel.s.name).isEqualTo("Garden")
            assertThat(repository.createCalls).hasSize(1)

            repository.gate!!.complete(Unit)
            val created = awaitItem() as NewBoardEvent.Created
            assertThat(created.name to created.key).isEqualTo("Garden" to "GARD")
        }
    }

    @Test
    fun serverKeyAlreadyUsed() {
        repository.createResult = Result.Error(BoardSaveError.InvalidFields(key = KeyFieldError.AlreadyUsed))
        val viewModel = viewModel()
        viewModel.type(name = "Garden")
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.keyError).isEqualTo(BoardKeyFieldError.AlreadyUsed("GARD"))
        assertThat(viewModel.s.isSaving).isFalse()
        assertThat(toasts.messages).isEmpty()
    }

    @Test
    fun serverKeyTooLongAndUnknownCode() {
        val viewModel = viewModel()
        viewModel.type(name = "Garden")
        repository.createResult = Result.Error(BoardSaveError.InvalidFields(key = KeyFieldError.Rule(BoardKeyError.TooLong)))
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.keyError).isEqualTo(BoardKeyFieldError.Rule(BoardKeyError.TooLong))

        viewModel.type(key = "GARDE")
        repository.createResult = Result.Error(BoardSaveError.InvalidFields(key = KeyFieldError.Invalid))
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.keyError).isEqualTo(BoardKeyFieldError.Rule(BoardKeyError.InvalidFormat))
    }

    @Test
    fun serverNameErrors() {
        val viewModel = viewModel()
        viewModel.type(name = "Garden")
        repository.createResult = Result.Error(BoardSaveError.InvalidFields(name = NameFieldError.Invalid))
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.nameError).isEqualTo(NameError.InvalidCharacter)

        viewModel.type(name = "Garden 2")
        repository.createResult = Result.Error(BoardSaveError.InvalidFields(name = NameFieldError.TooLong(80)))
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.nameError).isEqualTo(NameError.TooLong(80))
    }

    @Test
    fun networkFailureKeepsTheDialogAndDoesNotRepeat() = runTest {
        repository.createResult = Result.Error(BoardSaveError.Network(DataError.Network.NO_INTERNET))
        val viewModel = viewModel()
        viewModel.type(name = "Garden irrigation")
        viewModel.events.test {
            viewModel.onAction(NewBoardAction.Create)
            expectNoEvents()
        }
        assertThat(toasts.messages).hasSize(1)
        assertResource(toasts.messages.single().text, Res.string.new_board_failed)
        assertThat(viewModel.s.name).isEqualTo("Garden irrigation")
        assertThat(viewModel.s.key).isEqualTo("GARD")
        assertThat(viewModel.s.isSaving).isFalse()
        assertThat(repository.createCalls).hasSize(1)
    }

    @Test
    fun cancelCloses() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onAction(NewBoardAction.Cancel)
            assertThat(awaitItem()).isEqualTo(NewBoardEvent.Close)
        }
    }

    @Test
    fun restoresAfterProcessDeath() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.type(name = "Garden", key = "gi")
        first.onAction(NewBoardAction.Create)

        val restored = viewModel(handle)
        assertThat(restored.s.name).isEqualTo("Garden")
        assertThat(restored.s.key).isEqualTo("GI")
        assertThat(restored.s.submitted).isTrue()
        restored.type(name = "Other")
        assertThat(restored.s.key).isEqualTo("GI")
    }

    @Test
    fun typingAloneSurvivesProcessDeath() {
        val handle = SavedStateHandle()
        viewModel(handle).type(name = "Garden")
        val restored = viewModel(handle)
        assertThat(restored.s).isEqualTo(NewBoardState(name = "Garden", key = "GARD"))
    }

    @Test
    fun errorsShowAgainAfterProcessDeath() {
        val handle = SavedStateHandle()
        viewModel(handle).onAction(NewBoardAction.Create)
        val restored = viewModel(handle)
        assertThat(restored.s.nameError).isEqualTo(NameError.Required)
        assertThat(restored.s.keyError).isEqualTo(BoardKeyFieldError.Rule(BoardKeyError.Required))
    }

    @Test
    fun unloadedListSkipsTheLocalClashCheck() {
        repository.state.value = null
        val viewModel = viewModel()
        viewModel.type(name = "Cardigan")
        viewModel.onAction(NewBoardAction.Create)
        assertThat(viewModel.s.keyError).isNull()
        assertThat(repository.createCalls).containsExactly("Cardigan" to "CARD")
    }
}
