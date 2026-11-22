package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlin.test.Test

/** The destination window must not add a second scrim behind the design-system dialog. */
class BoardDialogDestinationPropertiesTest {

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun destinationDrawsNoScrimAndCantBeDismissed() {
        val properties = boardDialogDestinationProperties()
        assertThat(properties.scrimColor).isEqualTo(Color.Transparent)
        assertThat(properties.dismissOnBackPress).isFalse()
        assertThat(properties.dismissOnClickOutside).isFalse()
    }
}
