package com.plcoding.cardiganapp.core.designsystem.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

/** Material pull to refresh with the indicator in `background/surface` and `accent/default`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = AppTheme.colors.background.surface,
                color = AppTheme.colors.accent.default,
            )
        },
        content = content,
    )
}

@Composable
private fun RefreshingSample() {
    RefreshBox(
        isRefreshing = true,
        onRefresh = {},
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background.app),
    ) {
        Text(
            text = "Boards",
            modifier = Modifier.align(Alignment.Center),
            color = AppTheme.colors.text.primary,
        )
    }
}

@Preview(name = "Pull to refresh · Light", widthDp = 390, heightDp = 240)
@Composable
private fun RefreshBoxLightPreview() {
    AppTheme(darkTheme = false) { RefreshingSample() }
}

@Preview(name = "Pull to refresh · Dark", widthDp = 390, heightDp = 240)
@Composable
private fun RefreshBoxDarkPreview() {
    AppTheme(darkTheme = true) { RefreshingSample() }
}
