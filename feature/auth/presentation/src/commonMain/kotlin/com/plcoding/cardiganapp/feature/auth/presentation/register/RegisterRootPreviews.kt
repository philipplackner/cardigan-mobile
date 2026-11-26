package com.plcoding.cardiganapp.feature.auth.presentation.register

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass

private val previewStates = listOf(
    RegisterState(),
    RegisterState(
        email = "philipp@example.com",
        password = "Correct horse",
        repeatPassword = "Correct hors",
        passwordBlurred = true,
        repeatPasswordBlurred = true,
    ),
    RegisterState(
        email = "philipp@example.com",
        password = "Correct horse 7",
        repeatPassword = "Correct horse 7",
        emailAlreadyUsed = true,
    ),
    RegisterState(submitAttempted = true),
    RegisterState(
        email = "philipp@example.com",
        password = "Correct horse 7",
        repeatPassword = "Correct horse 7",
        submitAttempted = true,
        isLoading = true,
    ),
)

@Composable
private fun RegisterPreview(index: Int, windowClass: WindowClass, darkTheme: Boolean = false) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        RegisterScreen(state = previewStates[index], onAction = {})
    }
}

@Preview(name = "Register · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun RegisterPhonePreview() = RegisterPreview(0, WindowClass.Phone)

@Preview(name = "Register · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun RegisterPhoneDarkPreview() = RegisterPreview(0, WindowClass.Phone, darkTheme = true)

@Preview(name = "Register · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun RegisterDesktopPreview() = RegisterPreview(0, WindowClass.Desktop)

@Preview(name = "Register · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun RegisterDesktopDarkPreview() = RegisterPreview(0, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Live rules and mismatch · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun MismatchPhonePreview() = RegisterPreview(1, WindowClass.Phone)

@Preview(name = "Live rules and mismatch · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun MismatchDesktopDarkPreview() = RegisterPreview(1, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Email already registered · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun EmailUsedDesktopPreview() = RegisterPreview(2, WindowClass.Desktop)

@Preview(name = "Email already registered · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun EmailUsedPhoneDarkPreview() = RegisterPreview(2, WindowClass.Phone, darkTheme = true)

@Preview(name = "Empty submit · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun EmptySubmitPhonePreview() = RegisterPreview(3, WindowClass.Phone)

@Preview(name = "Empty submit · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun EmptySubmitDesktopDarkPreview() = RegisterPreview(3, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Creating account · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun CreatingPhonePreview() = RegisterPreview(4, WindowClass.Phone)

@Preview(name = "Creating account · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun CreatingDesktopDarkPreview() = RegisterPreview(4, WindowClass.Desktop, darkTheme = true)
