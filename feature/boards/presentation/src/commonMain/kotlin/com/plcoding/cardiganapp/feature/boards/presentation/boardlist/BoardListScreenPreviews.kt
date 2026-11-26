package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.presentation.util.UiText
import androidx.compose.ui.tooling.preview.Preview

private val previewBoards = listOf(
    BoardItemUi("1", "Cardigan v1", "CRD", UiText.DynamicString("12 cards"), UiText.DynamicString("2 hours ago")),
    BoardItemUi("2", "Raytracer", "RAY", UiText.DynamicString("23 cards"), UiText.DynamicString("Yesterday")),
    BoardItemUi("3", "Conference talk", "TALK", UiText.DynamicString("7 cards"), UiText.DynamicString("Oct 9")),
    BoardItemUi("4", "Home server", "HOME", UiText.DynamicString("9 cards"), UiText.DynamicString("Sep 30")),
    BoardItemUi("5", "Dotfiles cleanup", "DOT", UiText.DynamicString("4 cards"), UiText.DynamicString("Sep 28")),
    BoardItemUi("6", "Reading list", "READ", UiText.DynamicString("15 cards"), UiText.DynamicString("Aug 30")),
)

private val previewLoaded = BoardListState(content = BoardListContent.Loaded(previewBoards), boardCount = previewBoards.size)

@Composable
private fun BoardListPreview(state: BoardListState, windowClass: WindowClass, darkTheme: Boolean = false) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        BoardListScreen(state = state, onAction = {})
    }
}

@Preview(name = "Boards · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun BoardsPhonePreview() = BoardListPreview(previewLoaded, WindowClass.Phone)

@Preview(name = "Boards · Tablet", widthDp = 834, heightDp = 1112)
@Composable
private fun BoardsTabletPreview() = BoardListPreview(previewLoaded, WindowClass.Tablet)

@Preview(name = "Boards · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun BoardsDesktopPreview() = BoardListPreview(previewLoaded, WindowClass.Desktop)

@Preview(name = "Loading · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun LoadingPhonePreview() = BoardListPreview(BoardListState(content = BoardListContent.Loading), WindowClass.Phone)

@Preview(name = "Loading · Tablet", widthDp = 834, heightDp = 1112)
@Composable
private fun LoadingTabletPreview() = BoardListPreview(BoardListState(content = BoardListContent.Loading), WindowClass.Tablet)

@Preview(name = "Loading · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun LoadingDesktopPreview() = BoardListPreview(BoardListState(content = BoardListContent.Loading), WindowClass.Desktop)

@Preview(name = "No boards yet · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun NoboardsyetPhonePreview() = BoardListPreview(BoardListState(content = BoardListContent.Loaded(emptyList())), WindowClass.Phone)

@Preview(name = "No boards yet · Tablet", widthDp = 834, heightDp = 1112)
@Composable
private fun NoboardsyetTabletPreview() = BoardListPreview(BoardListState(content = BoardListContent.Loaded(emptyList())), WindowClass.Tablet)

@Preview(name = "No boards yet · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun NoboardsyetDesktopPreview() = BoardListPreview(BoardListState(content = BoardListContent.Loaded(emptyList())), WindowClass.Desktop)

@Preview(name = "Load error · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun LoaderrorPhonePreview() = BoardListPreview(BoardListState(content = BoardListContent.Error), WindowClass.Phone)

@Preview(name = "Load error · Tablet", widthDp = 834, heightDp = 1112)
@Composable
private fun LoaderrorTabletPreview() = BoardListPreview(BoardListState(content = BoardListContent.Error), WindowClass.Tablet)

@Preview(name = "Load error · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun LoaderrorDesktopPreview() = BoardListPreview(BoardListState(content = BoardListContent.Error), WindowClass.Desktop)

@Preview(name = "Refreshing · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun RefreshingPhonePreview() = BoardListPreview(previewLoaded.copy(isRefreshing = true), WindowClass.Phone)

@Preview(name = "Refreshing · Tablet", widthDp = 834, heightDp = 1112)
@Composable
private fun RefreshingTabletPreview() = BoardListPreview(previewLoaded.copy(isRefreshing = true), WindowClass.Tablet)

@Preview(name = "Refreshing · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun RefreshingDesktopPreview() = BoardListPreview(previewLoaded.copy(isRefreshing = true), WindowClass.Desktop)

@Preview(name = "Phone menu · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun PhonemenuPhonePreview() = BoardListPreview(previewLoaded.copy(isMenuOpen = true, accountEmail = "ada@example.com"), WindowClass.Phone)

@Preview(name = "Boards · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun BoardsDarkPhonePreview() = BoardListPreview(previewLoaded, WindowClass.Phone, darkTheme = true)

@Preview(name = "Boards · Tablet · Dark", widthDp = 834, heightDp = 1112)
@Composable
private fun BoardsDarkTabletPreview() = BoardListPreview(previewLoaded, WindowClass.Tablet, darkTheme = true)

@Preview(name = "Boards · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun BoardsDarkDesktopPreview() = BoardListPreview(previewLoaded, WindowClass.Desktop, darkTheme = true)
