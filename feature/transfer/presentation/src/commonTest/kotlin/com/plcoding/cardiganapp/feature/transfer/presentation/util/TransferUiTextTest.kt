package com.plcoding.cardiganapp.feature.transfer.presentation.util

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.presentation.board.boardKeyAlreadyUsedText
import com.plcoding.cardiganapp.core.presentation.board.toUiText
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.transfer.presentation.importboard.KeyFieldError
import kotlin.test.Test

/*
 * Break vectors:
 * - AlreadyUsed must carry the clashing key into the text, not a blank or another key
 * - each rule error must map to that rule's text, not the already-used text
 */
class TransferUiTextTest {

    @Test
    fun alreadyUsedShowsTheSharedTextWithTheKey() {
        val text = KeyFieldError.AlreadyUsed("CRDB").toUiText() as UiText.Resource
        val expected = boardKeyAlreadyUsedText("CRDB") as UiText.Resource
        assertThat(text.id).isEqualTo(expected.id)
        assertThat(text.args.toList()).containsExactly("CRDB")
    }

    @Test
    fun ruleErrorsShowTheSharedRuleText() {
        BoardKeyError.entries.forEach { rule ->
            val text = KeyFieldError.Rule(rule).toUiText() as UiText.Resource
            val expected = rule.toUiText() as UiText.Resource
            assertThat(text.id, name = "rule=$rule").isEqualTo(expected.id)
            assertThat(text.args.toList(), name = "rule=$rule").isEqualTo(expected.args.toList())
        }
    }
}
