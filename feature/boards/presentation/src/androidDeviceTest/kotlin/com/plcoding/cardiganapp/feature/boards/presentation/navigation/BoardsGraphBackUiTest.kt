package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import android.view.KeyEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_save
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_title
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.getString

/** Android back press on the real board dialog destinations. iOS has no back press to dialogs. */
@OptIn(ExperimentalTestApi::class)
class BoardsGraphBackUiTest {

    private val host = BoardsGraphTestHost()

    @BeforeTest
    fun setUp() = host.start()

    @AfterTest
    fun tearDown() = host.stop()

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    }

    @Test
    fun backClosesTheDialogWithoutAResult() = runComposeUiTest {
        host.show(this, startAtList = false)
        runOnIdle { host.navController.navigateToRenameBoard(host.raytracer.id.toString()) }
        val title = getString(Res.string.rename_board_title)
        host.awaitText(this, title)

        pressBack()
        host.awaitGone(this, title)
        assertThat(host.renamed).isEmpty()
    }

    @Test
    fun backWhileSavingKeepsTheDialog() = runComposeUiTest {
        host.repository.gate = CompletableDeferred()
        host.show(this, startAtList = false)
        runOnIdle { host.navController.navigateToRenameBoard(host.raytracer.id.toString()) }
        val title = getString(Res.string.rename_board_title)
        host.awaitText(this, title)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Path tracer")
        onNodeWithText(getString(Res.string.rename_board_save)).performClick()

        pressBack()
        waitForIdle()
        onNodeWithText(title).assertExists()

        host.repository.gate!!.complete(Unit)
        waitUntil { host.renamed.isNotEmpty() }
        assertThat(host.renamed).containsExactly("Path tracer")
    }
}
