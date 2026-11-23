package com.plcoding.cardiganapp.feature.auth.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import cardiganapp.feature.auth.presentation.generated.resources.Res
import cardiganapp.feature.auth.presentation.generated.resources.auth_error_rate_limited
import cardiganapp.feature.auth.presentation.generated.resources.auth_error_unavailable
import cardiganapp.feature.auth.presentation.generated.resources.login_error_wrong_credentials
import cardiganapp.feature.auth.presentation.generated.resources.login_session_expired
import com.plcoding.cardiganapp.core.designsystem.components.feedback.Banner
import com.plcoding.cardiganapp.core.designsystem.components.feedback.BannerType
import com.plcoding.cardiganapp.feature.auth.presentation.util.RetryTime
import com.plcoding.cardiganapp.feature.auth.presentation.util.toUiText
import org.jetbrains.compose.resources.stringResource

/** The one banner an auth screen shows at a time. */
sealed interface AuthBanner {
    data object SessionExpired : AuthBanner
    data object WrongCredentials : AuthBanner
    data class RateLimited(val seconds: Int) : AuthBanner
    data object Unavailable : AuthBanner
}

@Composable
internal fun AuthBannerView(banner: AuthBanner, modifier: Modifier = Modifier) {
    val message = when (banner) {
        AuthBanner.SessionExpired -> stringResource(Res.string.login_session_expired)
        AuthBanner.WrongCredentials -> stringResource(Res.string.login_error_wrong_credentials)
        is AuthBanner.RateLimited -> stringResource(
            Res.string.auth_error_rate_limited,
            RetryTime.of(banner.seconds).toUiText().asString(),
        )
        AuthBanner.Unavailable -> stringResource(Res.string.auth_error_unavailable)
    }
    Banner(
        message = message,
        type = if (banner == AuthBanner.SessionExpired) BannerType.Info else BannerType.Error,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
