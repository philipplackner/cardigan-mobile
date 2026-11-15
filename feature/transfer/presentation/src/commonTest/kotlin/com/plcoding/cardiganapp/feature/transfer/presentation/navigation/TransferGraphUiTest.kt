package com.plcoding.cardiganapp.feature.transfer.presentation.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.import_dialog_cancel
import cardiganapp.feature.transfer.presentation.generated.resources.importing_title
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.validPick
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.getString

/** The real dialog destinations. Back-press checks are Android-only, in `TransferGraphBackUiTest`. */
@OptIn(ExperimentalTestApi::class)
class TransferGraphUiTest {

    private val host = TransferGraphTestHost()

    @BeforeTest
    fun setUp() = host.start()

    @AfterTest
    fun tearDown() = host.stop()

    @Test
    fun importRouteOpensThePickerAndDrawsNoDialogContent() = runComposeUiTest {
        host.openImport(this)
        onAllNodes(hasText("", substring = true)).assertCountEquals(0)
    }

    @Test
    fun firstPickerCancelPopsTheRoute() = runComposeUiTest {
        host.openImport(this)
        host.picker.cancel()
        waitUntil { host.doneCount == 1 }
        waitUntil { onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun cancelInTheErrorDialogPopsTheRoute() = runComposeUiTest {
        host.checker.result = null
        host.openImport(this)
        host.picker.pick(validPick())
        val cancel = getString(Res.string.import_dialog_cancel)
        host.awaitText(this, cancel)

        onNodeWithText(cancel).performClick()
        waitUntil { host.doneCount == 1 }
        waitUntil { onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun uploadingShowsTheProgressDialogInsideTheDestination() = runComposeUiTest {
        host.repository.gate = CompletableDeferred()
        host.openImport(this)
        host.picker.pick(validPick())
        val title = getString(Res.string.importing_title, "board-backup.json")
        host.awaitText(this, title)

        onNodeWithText(title).assertIsDisplayed()
        assertThat(host.doneCount).isEqualTo(0)
    }
}
