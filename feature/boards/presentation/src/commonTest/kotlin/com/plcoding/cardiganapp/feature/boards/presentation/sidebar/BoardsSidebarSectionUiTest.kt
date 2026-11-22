package com.plcoding.cardiganapp.feature.boards.presentation.sidebar

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.boards_new_board
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.boards.domain.BoardOrder
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.board
import kotlin.test.Test
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class BoardsSidebarSectionUiTest {

    private val cardigan = board(name = "Cardigan v1", key = "CRD", updated = "2026-11-21T08:00:00Z")
    private val raytracer = board(name = "Raytracer", key = "RAY", updated = "2026-11-20T08:00:00Z")
    private val repository = FakeBoardRepository(listOf(cardigan, raytracer))
    private val opened = mutableListOf<Triple<String, String, String>>()
    private var newBoardClicks = 0

    @Test
    fun reflectsCreateRenameAndDelete() = runComposeUiTest {
        setContent {
            AppTheme(darkTheme = false, windowClass = WindowClass.Desktop) {
                Column {
                    BoardsSidebarSection(
                        onOpenBoard = { id, name, key -> opened += Triple(id, name, key) },
                        onNewBoardClick = { newBoardClicks++ },
                        viewModel = BoardsSidebarViewModel(repository),
                    )
                }
            }
        }
        onNodeWithText("Cardigan v1").assertExists()
        onNodeWithText("RAY").assertExists()

        val created = board(name = "Garden irrigation", key = "GARD", updated = "2026-11-21T09:00:00Z")
        repository.state.value = BoardOrder.insert(repository.state.value!!, created)
        waitUntil { onAllNodes(hasText("Garden irrigation")).fetchSemanticsNodes().isNotEmpty() }

        repository.state.value = BoardOrder.replace(repository.state.value!!, raytracer.copy(name = "Path tracer"))
        waitUntil { onAllNodes(hasText("Path tracer")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("Raytracer").assertCountEquals(0)

        repository.state.value = repository.state.value!!.filterNot { it.id == cardigan.id }
        waitUntil { onAllNodes(hasText("Cardigan v1")).fetchSemanticsNodes().isEmpty() }

        onNodeWithText("Path tracer").performClick()
        assertThat(opened).containsExactly(Triple(raytracer.id.toString(), "Path tracer", "RAY"))
        onNodeWithText(getString(Res.string.boards_new_board)).performClick()
        assertThat(newBoardClicks).isEqualTo(1)
    }

    @Test
    fun loadsTheBoardsWhenNothingLoadedThem() = runComposeUiTest {
        repository.state.value = null
        repository.serverBoards = listOf(cardigan)
        setContent {
            AppTheme(darkTheme = false, windowClass = WindowClass.Desktop) {
                Column {
                    BoardsSidebarSection(onOpenBoard = { _, _, _ -> }, onNewBoardClick = {}, viewModel = BoardsSidebarViewModel(repository))
                }
            }
        }
        waitUntil { onAllNodes(hasText("Cardigan v1")).fetchSemanticsNodes().isNotEmpty() }
        assertThat(repository.ensureLoadedCalls).isEqualTo(1)
    }
}
