package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isInstanceOf
import com.plcoding.cardiganapp.core.domain.board.BoardContent
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeCardCreateRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeLabelSource
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.boardContent
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.card
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.column
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.sampleBoardId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BoardViewModelTestBase {

    protected val todo = column("To do", card(101), card(102))
    protected val doing = column("In progress", card(103))
    protected val review = column("Review", card(104), card(105))
    protected val done = column("Done")
    protected val repository = FakeBoardDetailRepository(boardContent(todo, doing, review, done))
    protected val cards = FakeCardCreateRepository(repository)
    protected val labels = FakeLabelSource()
    protected val toasts = FakeToastController()

    @BeforeTest
    fun setUpMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDownMain() {
        Dispatchers.resetMain()
    }

    protected fun handle(name: String? = null, key: String? = null) = SavedStateHandle(
        buildMap {
            put(BoardViewModel.ARG_BOARD_ID, sampleBoardId.toString())
            name?.let { put(BoardViewModel.ARG_NAME, it) }
            key?.let { put(BoardViewModel.ARG_KEY, it) }
        },
    )

    protected fun viewModel(handle: SavedStateHandle = handle()) =
        BoardViewModel(repository, cards, labels, toasts, handle)

    /** Collects the events of [viewModel] for the rest of the test. */
    protected fun TestScope.events(viewModel: BoardViewModel): List<BoardEvent> {
        val events = mutableListOf<BoardEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        return events
    }

    /** Events other than focus requests, which every opened field sends. */
    protected val List<BoardEvent>.withoutFocus: List<BoardEvent> get() = filterNot { it is BoardEvent.FocusInlineField }

    protected val BoardViewModel.board: BoardContent
        get() {
            val content = state.value.content
            assertThat(content).isInstanceOf<BoardLoadState.Loaded>()
            return (content as BoardLoadState.Loaded).content
        }

    protected val BoardViewModel.columnNames: List<String> get() = board.columns.map { it.name }
}
