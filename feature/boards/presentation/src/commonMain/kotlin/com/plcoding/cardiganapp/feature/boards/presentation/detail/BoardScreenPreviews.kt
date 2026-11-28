package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.board.BoardContent
import com.plcoding.cardiganapp.core.domain.board.Card
import com.plcoding.cardiganapp.core.domain.board.Column
import com.plcoding.cardiganapp.core.domain.label.Label
import com.plcoding.cardiganapp.core.domain.label.LabelColor
import com.plcoding.cardiganapp.core.domain.text.NameError
import kotlin.uuid.Uuid

private val previewLabels =
    listOf(
        Label(Uuid.random(), "api", LabelColor.Blue),
        Label(Uuid.random(), "infra", LabelColor.Amber),
        Label(Uuid.random(), "web", LabelColor.Teal),
        Label(Uuid.random(), "mobile", LabelColor.Violet),
        Label(Uuid.random(), "design", LabelColor.Pink),
        Label(Uuid.random(), "bug", LabelColor.Red),
        Label(Uuid.random(), "docs", LabelColor.Slate),
    )

private fun labels(vararg names: String) = names.map { name -> previewLabels.first { it.name == name }.id }

private fun card(
    number: Long,
    title: String,
    vararg labels: String,
) = Card(Uuid.random(), number, title, labels(*labels))

private val previewBoard =
    BoardContent(
        board = Board(Uuid.random(), "Cardigan v1", "CRD"),
        columns =
            listOf(
                Column(
                    Uuid.random(),
                    "To do",
                    listOf(
                        card(118, "Decide card ordering: fractional ranks vs. integer positions", "api"),
                        card(121, "Define export file format v1", "api", "infra"),
                        card(124, "Empty state for the board list", "web", "mobile", "design"),
                        card(127, "Refresh token transport on web", "web", "api"),
                    ),
                ),
                Column(
                    Uuid.random(),
                    "In progress",
                    listOf(
                        card(109, "Single-flight token refresh in the Ktor client", "mobile", "api"),
                        card(112, "Column menu with move left and right", "web"),
                        card(115, "Long-press drag with auto-scroll between columns", "mobile"),
                    ),
                ),
                Column(
                    Uuid.random(),
                    "Review",
                    listOf(
                        card(104, "Import fails on a board with an empty column", "api", "bug"),
                        card(106, "Reverse proxy and TLS on the VPS", "infra"),
                    ),
                ),
                Column(
                    Uuid.random(),
                    "Done",
                    listOf(
                        card(101, "First draft of the REST contract", "api", "docs"),
                        card(102, "CMP project scaffolding with CI", "mobile", "infra"),
                        card(103, "React and TypeScript scaffold", "web"),
                    ),
                ),
            ),
    )

private val newBoard =
    BoardContent(
        board = Board(Uuid.random(), "Garden irrigation", "GARD"),
        columns = listOf("To do", "In progress", "Done").map { Column(Uuid.random(), it, emptyList()) },
    )

private val hint = BoardHeaderHint("Cardigan v1", "CRD")

private fun loaded(
    content: BoardContent = previewBoard,
    field: InlineField? = null,
    dialog: BoardDialog? = null,
) = BoardState(
    content = BoardLoadState.Loaded(content, previewLabels.associateBy { it.id }),
    headerHint = hint,
    inlineField = field,
    dialog = dialog,
)

private val boardState = loaded()
private val loadingState = BoardState(headerHint = hint)
private val errorState = BoardState(content = BoardLoadState.Error, headerHint = hint)
private val newBoardState = loaded(newBoard)
private val noColumnsState = loaded(previewBoard.copy(columns = emptyList()))
private val quickAddState =
    loaded(
        field = InlineField(InlineTarget.QuickAdd(previewBoard.columns[0].id), "Write empty-state copy for new boards"),
    )
private val quickAddErrorState =
    loaded(
        field =
            InlineField(
                InlineTarget.QuickAdd(previewBoard.columns[2].id),
                "Fix\nlogin",
                showErrors = true,
                error = NameError.InvalidCharacter,
            ),
    )
private val addColumnState = loaded(field = InlineField(InlineTarget.AddColumn, "Blocked"))
private val renameState = loaded(dialog = BoardDialog.RenameColumn(previewBoard.columns[2].id, "Review", "In review"))
private val deleteState = loaded(dialog = BoardDialog.DeleteColumn(previewBoard.columns[2].id, "Review", 2))
private val deleteEmptyState = loaded(newBoard, dialog = BoardDialog.DeleteColumn(newBoard.columns[0].id, "To do", 0))
private val discardState =
    loaded(
        field = InlineField(InlineTarget.QuickAdd(previewBoard.columns[0].id), "Write docs"),
        dialog = BoardDialog.DiscardInlineField(InlineTarget.QuickAdd(previewBoard.columns[3].id)),
    )

@Composable
private fun Preview(
    state: BoardState,
    windowClass: WindowClass,
    darkTheme: Boolean = false,
) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        BoardScreen(state = state, onAction = {})
    }
}

@Preview(name = "Board · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun BoardPhonePreview() = Preview(boardState, WindowClass.Phone)

@Preview(name = "Board · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun BoardPhoneDarkPreview() = Preview(boardState, WindowClass.Phone, darkTheme = true)

@Preview(name = "Board · Tablet", widthDp = 834, heightDp = 1112)
@Composable
private fun BoardTabletPreview() = Preview(boardState, WindowClass.Tablet)

@Preview(name = "Board · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun BoardDesktopPreview() = Preview(boardState, WindowClass.Desktop)

@Preview(name = "Board · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun BoardDesktopDarkPreview() = Preview(boardState, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Loading · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun LoadingPhonePreview() = Preview(loadingState, WindowClass.Phone)

@Preview(name = "Loading · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun LoadingDesktopPreview() = Preview(loadingState, WindowClass.Desktop)

@Preview(name = "Couldn’t load · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun ErrorPhonePreview() = Preview(errorState, WindowClass.Phone)

@Preview(name = "Couldn’t load · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun ErrorDesktopPreview() = Preview(errorState, WindowClass.Desktop)

@Preview(name = "New board · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun NewBoardPhonePreview() = Preview(newBoardState, WindowClass.Phone)

@Preview(name = "New board · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun NewBoardDesktopPreview() = Preview(newBoardState, WindowClass.Desktop)

@Preview(name = "No columns · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun NoColumnsPhonePreview() = Preview(noColumnsState, WindowClass.Phone)

@Preview(name = "No columns · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun NoColumnsDesktopPreview() = Preview(noColumnsState, WindowClass.Desktop)

@Preview(name = "Quick add · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun QuickAddPhonePreview() = Preview(quickAddState, WindowClass.Phone)

@Preview(name = "Quick add · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun QuickAddDesktopPreview() = Preview(quickAddState, WindowClass.Desktop)

@Preview(name = "Quick add error · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun QuickAddErrorDesktopPreview() = Preview(quickAddErrorState, WindowClass.Desktop)

@Preview(name = "Add column · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun AddColumnPhonePreview() = Preview(addColumnState, WindowClass.Phone)

@Preview(name = "Add column · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun AddColumnDesktopPreview() = Preview(addColumnState, WindowClass.Desktop)

@Preview(name = "Rename column · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun RenameDesktopPreview() = Preview(renameState, WindowClass.Desktop)

@Preview(name = "Delete column with cards · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun DeletePhonePreview() = Preview(deleteState, WindowClass.Phone)

@Preview(name = "Delete column with cards · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun DeleteDesktopPreview() = Preview(deleteState, WindowClass.Desktop)

@Preview(name = "Delete empty column · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun DeleteEmptyDesktopPreview() = Preview(deleteEmptyState, WindowClass.Desktop)

@Preview(name = "Discard changes · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun DiscardPhonePreview() = Preview(discardState, WindowClass.Phone)
