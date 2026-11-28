package com.plcoding.cardiganapp.feature.auth.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthBanner

private val previewStates =
    listOf(
        LoginState(),
        LoginState(email = "philipp@example.com", password = "wrong password", banner = AuthBanner.WrongCredentials),
        LoginState(banner = AuthBanner.SessionExpired),
        LoginState(email = "philipp@example.com", password = "secret", banner = AuthBanner.RateLimited(12)),
        LoginState(email = "philipp@example.com", password = "secret", banner = AuthBanner.Unavailable),
        LoginState(email = "philipp@example.com", password = "correct horse", isLoading = true, submitAttempted = true),
        LoginState(submitAttempted = true),
    )

@Composable
private fun LoginPreview(
    index: Int,
    windowClass: WindowClass,
    darkTheme: Boolean = false,
) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        LoginScreen(state = previewStates[index], onAction = {})
    }
}

@Preview(name = "Log in · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun LoginPhonePreview() = LoginPreview(0, WindowClass.Phone)

@Preview(name = "Log in · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun LoginPhoneDarkPreview() = LoginPreview(0, WindowClass.Phone, darkTheme = true)

@Preview(name = "Log in · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun LoginDesktopPreview() = LoginPreview(0, WindowClass.Desktop)

@Preview(name = "Log in · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun LoginDesktopDarkPreview() = LoginPreview(0, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Wrong credentials · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun WrongCredentialsPhonePreview() = LoginPreview(1, WindowClass.Phone)

@Preview(name = "Wrong credentials · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun WrongCredentialsDesktopDarkPreview() = LoginPreview(1, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Session expired · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun SessionExpiredPhonePreview() = LoginPreview(2, WindowClass.Phone)

@Preview(name = "Session expired · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun SessionExpiredDesktopPreview() = LoginPreview(2, WindowClass.Desktop)

@Preview(name = "Rate limited · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun RateLimitedPhoneDarkPreview() = LoginPreview(3, WindowClass.Phone, darkTheme = true)

@Preview(name = "Rate limited · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun RateLimitedDesktopPreview() = LoginPreview(3, WindowClass.Desktop)

@Preview(name = "Unavailable · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun UnavailablePhonePreview() = LoginPreview(4, WindowClass.Phone)

@Preview(name = "Unavailable · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun UnavailableDesktopDarkPreview() = LoginPreview(4, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Logging in · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun LoggingInPhonePreview() = LoginPreview(5, WindowClass.Phone)

@Preview(name = "Logging in · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun LoggingInDesktopDarkPreview() = LoginPreview(5, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Empty submit · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun EmptySubmitPhonePreview() = LoginPreview(6, WindowClass.Phone)
