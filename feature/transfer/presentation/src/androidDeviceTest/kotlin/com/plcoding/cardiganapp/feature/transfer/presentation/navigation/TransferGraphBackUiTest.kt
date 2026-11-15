package com.plcoding.cardiganapp.feature.transfer.presentation.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.import_dialog_title
import cardiganapp.feature.transfer.presentation.generated.resources.importing_title
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.validPick
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.getString

/** Android back press on the real dialog destinations. iOS has no back press to dialogs. */
@OptIn(ExperimentalTestApi::class)
class TransferGraphBackUiTest {

    private val host = TransferGraphTestHost()

    @BeforeTest
    fun setUp() = host.start()

    @AfterTest
    fun tearDown() = host.stop()

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    }

    @Test
    fun backInTheErrorDialogEndsTheImport() = runComposeUiTest {
        host.checker.result = null
        host.openImport(this)
        host.picker.pick(validPick())
        host.awaitText(this, getString(Res.string.import_dialog_title))

        pressBack()
        waitUntil { host.doneCount == 1 }
        waitUntil { onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun backWhileUploadingDoesNothing() = runComposeUiTest {
        host.repository.gate = CompletableDeferred()
        host.openImport(this)
        host.picker.pick(validPick())
        val title = getString(Res.string.importing_title, "board-backup.json")
        host.awaitText(this, title)

        pressBack()
        waitForIdle()
        onNodeWithText(title).assertIsDisplayed()
        assertThat(host.doneCount).isEqualTo(0)
    }
}
