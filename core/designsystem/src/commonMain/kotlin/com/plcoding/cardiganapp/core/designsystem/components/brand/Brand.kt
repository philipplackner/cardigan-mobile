package com.plcoding.cardiganapp.core.designsystem.components.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cardiganapp.core.designsystem.generated.resources.Res
import cardiganapp.core.designsystem.generated.resources.brand_wordmark
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

private val LogoSize = 22.dp
private val LogoBarHeights = listOf(14.dp, 9.dp, 11.dp)

/** The Cardigan mark: three columns of different heights on a `text/primary` tile. Decorative. */
@Composable
fun Logo(modifier: Modifier = Modifier) {
    val barShape = RoundedCornerShape(AppTheme.radius.r1)
    Row(
        modifier =
            modifier
                .size(LogoSize)
                .background(AppTheme.colors.text.primary, RoundedCornerShape(AppTheme.radius.r6))
                .padding(AppTheme.space.s4),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s2),
        verticalAlignment = Alignment.Top,
    ) {
        LogoBarHeights.forEach { barHeight ->
            Spacer(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(barHeight)
                        .background(AppTheme.colors.background.app, barShape),
            )
        }
    }
}

enum class BrandSize {
    /** Tablet and desktop sidebar. */
    Sidebar,

    /** Phone app bar. */
    AppBar,

    /** Log in and register. */
    Auth,
}

/** Logo and wordmark. */
@Composable
fun Brand(
    size: BrandSize,
    modifier: Modifier = Modifier,
) {
    val space = AppTheme.space
    val padding =
        when (size) {
            BrandSize.AppBar -> PaddingValues(start = space.s4, end = space.s8, top = space.s4, bottom = space.s4)
            BrandSize.Sidebar, BrandSize.Auth -> PaddingValues(horizontal = space.s8, vertical = space.s4)
        }
    val textStyle =
        when (size) {
            BrandSize.Sidebar -> AppTheme.textStyles.sans15_5SemiBold100LsMinus2
            BrandSize.AppBar -> AppTheme.textStyles.sans17SemiBold100LsMinus2
            BrandSize.Auth -> AppTheme.textStyles.sans18SemiBold100LsMinus2
        }
    Row(
        modifier = modifier.padding(padding),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s9),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Logo()
        Text(
            text = stringResource(Res.string.brand_wordmark),
            style = textStyle,
            color = AppTheme.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BrandSamples() {
    Row(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s24),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Logo()
        BrandSize.entries.forEach { Brand(size = it) }
    }
}

@Preview(name = "Brand · Light")
@Composable
private fun BrandLightPreview() {
    AppTheme(darkTheme = false) { BrandSamples() }
}

@Preview(name = "Brand · Dark")
@Composable
private fun BrandDarkPreview() {
    AppTheme(darkTheme = true) { BrandSamples() }
}
