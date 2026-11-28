package com.plcoding.cardiganapp.feature.boards.presentation.components

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.presentation.board.boardKeyAlreadyUsedText
import com.plcoding.cardiganapp.core.presentation.board.toUiText
import com.plcoding.cardiganapp.core.presentation.util.UiText
import kotlin.test.Test

/*
 * Break vectors: a rule error shows the shared rule text; a clash shows the shared clash text with
 * the key the user typed, not an empty one.
 */
class BoardFieldErrorsTest {
    // The core resource accessors are internal to :core:presentation, so compare against its helpers.
    private fun assertSameText(
        actual: UiText,
        expected: UiText,
    ) {
        assertThat(actual).isInstanceOf<UiText.Resource>()
        actual as UiText.Resource
        expected as UiText.Resource
        assertThat(actual.id).isEqualTo(expected.id)
        assertThat(actual.args.toList()).isEqualTo(expected.args.toList())
    }

    @Test
    fun ruleErrorUsesTheSharedRuleText() {
        assertSameText(BoardKeyFieldError.Rule(BoardKeyError.TooShort).toUiText(), BoardKeyError.TooShort.toUiText())
    }

    @Test
    fun clashNamesTheKey() {
        assertSameText(BoardKeyFieldError.AlreadyUsed("GARD").toUiText(), boardKeyAlreadyUsedText("GARD"))
    }
}
