package com.plcoding.cardiganapp.core.designsystem.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cardiganapp.core.designsystem.generated.resources.Res
import cardiganapp.core.designsystem.generated.resources.password_rule_met
import cardiganapp.core.designsystem.generated.resources.password_rule_not_met
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

private val PasswordRuleIconSize = 13.dp

/** Live password check: a cross until the rule is [met], then a check. */
@Composable
fun PasswordRule(
    text: String,
    met: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (met) AppTheme.colors.success.default else AppTheme.colors.text.muted
    val state = stringResource(if (met) Res.string.password_rule_met else Res.string.password_rule_not_met)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { stateDescription = state },
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = if (met) CardiganIcons.Check else CardiganIcons.CloseSmall,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(PasswordRuleIconSize),
        )
        Text(text = text, style = AppTheme.textStyles.sans12Regular145, color = color)
    }
}

@Composable
private fun PasswordRuleSamples() {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
    ) {
        PasswordRule(text = "12+ characters", met = true)
        PasswordRule(text = "12+ characters", met = false)
    }
}

@Preview(name = "Password rule · Light")
@Composable
private fun PasswordRuleLightPreview() {
    AppTheme(darkTheme = false) { PasswordRuleSamples() }
}

@Preview(name = "Password rule · Dark")
@Composable
private fun PasswordRuleDarkPreview() {
    AppTheme(darkTheme = true) { PasswordRuleSamples() }
}
