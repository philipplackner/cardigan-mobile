package com.plcoding.cardiganapp.feature.auth.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.brand.Brand
import com.plcoding.cardiganapp.core.designsystem.components.brand.BrandSize
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

private val AuthCardWidth = 380.dp

/**
 * The Figma "Auth" frame: the phone layout top-aligned, the 380dp card centered on tablet and
 * desktop. Both scroll, and the safe-drawing insets (including the keyboard) shrink the viewport,
 * so the focused field and the button stay reachable.
 */
@Composable
internal fun AuthLayout(
    title: String,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val space = AppTheme.space
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(AppTheme.colors.background.app)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val viewportHeight = maxHeight
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (AppTheme.windowClass == WindowClass.Phone) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = space.s20, vertical = space.s24)
                            .padding(top = space.s28),
                    verticalArrangement = Arrangement.spacedBy(space.s22),
                ) {
                    Brand(size = BrandSize.Auth)
                    Column(verticalArrangement = Arrangement.spacedBy(space.s18)) {
                        AuthTitle(title, large = true)
                        content()
                    }
                    footer()
                }
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = viewportHeight)
                            .padding(space.s24),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.width(AuthCardWidth),
                        verticalArrangement = Arrangement.spacedBy(space.s18),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Brand(size = BrandSize.Auth)
                        }
                        AuthCard {
                            AuthTitle(title, large = false)
                            content()
                        }
                        footer()
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(AppTheme.radius.r12)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .dropShadow(shape, AppTheme.shadows.small)
                .background(AppTheme.colors.background.surface, shape)
                .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
                .padding(AppTheme.space.s1 + AppTheme.space.s25),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
        content = content,
    )
}

@Composable
private fun AuthTitle(
    text: String,
    large: Boolean,
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
        style =
            if (large) AppTheme.textStyles.sans28SemiBold120LsMinus2 else AppTheme.textStyles.sans22SemiBold120LsMinus2,
        color = AppTheme.colors.text.primary,
    )
}

/** "No account yet? Create one" and "Already have an account? Log in": muted prompt, accent link. */
@Composable
internal fun AuthFooterLink(
    prompt: String,
    link: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$prompt ",
            style = AppTheme.textStyles.sans13_5Regular145,
            color = AppTheme.colors.text.muted,
        )
        Text(
            text = link,
            modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            style = AppTheme.textStyles.sans13_5Medium145,
            color = AppTheme.colors.accent.default,
        )
    }
}
