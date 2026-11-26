package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.hasSize
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.boards_refresh_failed
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertResource
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeClock
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.board
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListReloader
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone

/*
 * Break vectors:
 * - first load: loading while it runs, loaded after, empty list, error when the store stays null
 * - retry: back to loading, then loaded or error again
 * - refresh failure with a list shown keeps the items and shows the D12 toast once; success shows none
 * - the store changes (create, delete, clear) after loading
 * - the refreshing flag follows the reloader
 * - open a board passes id, name and key; an unknown id does nothing
 * - the phone menu opens and closes, and every menu action closes it
 * - actions the Root handles (new, rename, delete) leave the state alone
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BoardListViewModelTest {

    private val repository = FakeBoardRepository()
    private val toasts = FakeToastController()
    private val cardigan = board(id = "0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", name = "Cardigan v1", key = "CRD", cardCount = 12)
    private val raytracer = board(name = "Raytracer", key = "RAY")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): Pair<BoardListViewModel, BoardListReloader> {
        val reloader = BoardListReloader(repository, backgroundScope)
        return BoardListViewModel(repository, reloader, toasts, FakeClock(), TimeZone.UTC) to reloader
    }

    private val BoardListViewModel.content get() = state.value.content

    @Test
    fun loadingThenLoaded() = runTest {
        repository.gate = CompletableDeferred()
        repository.serverBoards = listOf(cardigan, raytracer)
        val (viewModel) = viewModel()
        assertThat(viewModel.content).isEqualTo(BoardListContent.Loading)

        repository.gate!!.complete(Unit)

        val content = viewModel.content as BoardListContent.Loaded
        assertThat(content.boards.map { it.name }).isEqualTo(listOf("Cardigan v1", "Raytracer"))
        assertThat(viewModel.state.value.boardCount).isEqualTo(2)
        assertThat(repository.ensureLoadedCalls).isEqualTo(1)
    }

    @Test
    fun emptyListIsLoadedWithNoItems() = runTest {
        repository.serverBoards = emptyList()
        val (viewModel) = viewModel()
        assertThat(viewModel.content).isEqualTo(BoardListContent.Loaded(emptyList()))
        assertThat(viewModel.state.value.boardCount).isEqualTo(0)
    }

    @Test
    fun loadErrorThenRetry() = runTest {
        repository.refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        val (viewModel) = viewModel()
        assertThat(viewModel.content).isEqualTo(BoardListContent.Error)

        repository.refreshResult = Result.Success(Unit)
        repository.serverBoards = listOf(cardigan)
        repository.gate = CompletableDeferred()
        viewModel.onAction(BoardListAction.OnRetry)
        assertThat(viewModel.content).isEqualTo(BoardListContent.Loading)

        repository.gate!!.complete(Unit)
        assertThat(viewModel.content).isInstanceOf<BoardListContent.Loaded>()
    }

    @Test
    fun retryThatFailsShowsTheErrorAgain() = runTest {
        repository.refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        val (viewModel) = viewModel()
        viewModel.onAction(BoardListAction.OnRetry)
        assertThat(viewModel.content).isEqualTo(BoardListContent.Error)
        assertThat(toasts.messages).hasSize(0)
    }

    @Test
    fun refreshFailureKeepsTheItems() = runTest {
        repository.serverBoards = listOf(cardigan, raytracer)
        val (viewModel) = viewModel()
        repository.refreshResult = Result.Error(DataError.Network.NO_INTERNET)

        viewModel.onAction(BoardListAction.OnRefresh)
        testScheduler.runCurrent()

        assertThat((viewModel.content as BoardListContent.Loaded).boards).hasSize(2)
        assertThat(toasts.messages).hasSize(1)
        assertThat(viewModel.state.value.isRefreshing).isFalse()
    }

    @Test
    fun refreshSuccessShowsNoToast() = runTest {
        repository.serverBoards = listOf(cardigan)
        val (viewModel) = viewModel()
        repository.serverBoards = listOf(cardigan, raytracer)

        viewModel.onAction(BoardListAction.OnRefresh)
        testScheduler.runCurrent()

        assertThat((viewModel.content as BoardListContent.Loaded).boards).hasSize(2)
        assertThat(toasts.messages).hasSize(0)
    }

    @Test
    fun refreshFailureToastUsesTheD12Text() = runTest {
        repository.serverBoards = listOf(cardigan)
        val (viewModel) = viewModel()
        repository.refreshResult = Result.Error(DataError.Network.SERVER_ERROR)

        viewModel.onAction(BoardListAction.OnRefresh)
        testScheduler.runCurrent()

        val toast = toasts.messages.single()
        assertResource(toast.text, Res.string.boards_refresh_failed)
        assertThat(toast.type).isEqualTo(ToastType.Error)
    }

    @Test
    fun refreshFailureInTheErrorStateKeepsTheErrorState() = runTest {
        repository.refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        val (viewModel) = viewModel()

        viewModel.onAction(BoardListAction.OnRefresh)
        testScheduler.runCurrent()

        assertThat(viewModel.content).isEqualTo(BoardListContent.Error)
        assertThat(toasts.messages).hasSize(1)
    }

    @Test
    fun refreshingFlagFollowsTheReloader() = runTest {
        repository.serverBoards = listOf(cardigan)
        val (viewModel) = viewModel()
        repository.gate = CompletableDeferred()

        viewModel.onAction(BoardListAction.OnRefresh)
        testScheduler.runCurrent()
        assertThat(viewModel.state.value.isRefreshing).isTrue()

        repository.gate!!.complete(Unit)
        testScheduler.runCurrent()
        assertThat(viewModel.state.value.isRefreshing).isFalse()
    }

    @Test
    fun listUpdatesWhenTheStoreChanges() = runTest {
        repository.serverBoards = listOf(cardigan)
        val (viewModel) = viewModel()

        repository.state.value = listOf(raytracer, cardigan)
        assertThat((viewModel.content as BoardListContent.Loaded).boards.map { it.key }).isEqualTo(listOf("RAY", "CRD"))
        assertThat(viewModel.state.value.boardCount).isEqualTo(2)

        repository.state.value = null
        assertThat(viewModel.content).isEqualTo(BoardListContent.Loading)
    }

    @Test
    fun openBoardPassesIdNameAndKey() = runTest {
        repository.serverBoards = listOf(cardigan)
        val (viewModel) = viewModel()
        viewModel.events.test {
            viewModel.onAction(BoardListAction.OnBoardClick(cardigan.id.toString()))
            assertThat(awaitItem()).isEqualTo(BoardListEvent.OpenBoard(cardigan.id.toString(), "Cardigan v1", "CRD"))

            viewModel.onAction(BoardListAction.OnBoardClick("00000000-0000-0000-0000-000000000000"))
            expectNoEvents()
        }
    }

    @Test
    fun menuOpensAndEveryMenuActionClosesIt() = runTest {
        repository.serverBoards = listOf(cardigan)
        val (viewModel) = viewModel()
        listOf(
            BoardListAction.OnMenuDismiss,
            BoardListAction.OnImportClick,
            BoardListAction.OnLabelsClick,
            BoardListAction.OnLogOutClick,
        ).forEach { close ->
            viewModel.onAction(BoardListAction.OnMenuClick)
            assertThat(viewModel.state.value.isMenuOpen).isTrue()
            viewModel.onAction(close)
            assertThat(viewModel.state.value.isMenuOpen).isFalse()
        }
    }

    @Test
    fun actionsTheRootHandlesLeaveTheStateAlone() = runTest {
        repository.serverBoards = listOf(cardigan)
        val (viewModel) = viewModel()
        val before = viewModel.state.value
        listOf(
            BoardListAction.OnNewBoardClick,
            BoardListAction.OnRenameClick(cardigan.id.toString()),
            BoardListAction.OnDeleteClick(cardigan.id.toString()),
        ).forEach { action ->
            viewModel.onAction(action)
            assertThat(viewModel.state.value, name = "$action").isEqualTo(before)
        }
    }
}
