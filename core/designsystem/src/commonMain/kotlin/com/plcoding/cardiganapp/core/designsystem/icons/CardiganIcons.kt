package com.plcoding.cardiganapp.core.designsystem.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import cardiganapp.core.designsystem.generated.resources.Res
import cardiganapp.core.designsystem.generated.resources.ic_alert
import cardiganapp.core.designsystem.generated.resources.ic_arrow_left
import cardiganapp.core.designsystem.generated.resources.ic_arrow_right
import cardiganapp.core.designsystem.generated.resources.ic_board
import cardiganapp.core.designsystem.generated.resources.ic_boards
import cardiganapp.core.designsystem.generated.resources.ic_check
import cardiganapp.core.designsystem.generated.resources.ic_chevron_down
import cardiganapp.core.designsystem.generated.resources.ic_chevron_left
import cardiganapp.core.designsystem.generated.resources.ic_chevron_right
import cardiganapp.core.designsystem.generated.resources.ic_close
import cardiganapp.core.designsystem.generated.resources.ic_close_small
import cardiganapp.core.designsystem.generated.resources.ic_cloud_off
import cardiganapp.core.designsystem.generated.resources.ic_columns
import cardiganapp.core.designsystem.generated.resources.ic_download
import cardiganapp.core.designsystem.generated.resources.ic_edit
import cardiganapp.core.designsystem.generated.resources.ic_file
import cardiganapp.core.designsystem.generated.resources.ic_filter
import cardiganapp.core.designsystem.generated.resources.ic_info
import cardiganapp.core.designsystem.generated.resources.ic_label
import cardiganapp.core.designsystem.generated.resources.ic_log_out
import cardiganapp.core.designsystem.generated.resources.ic_more
import cardiganapp.core.designsystem.generated.resources.ic_plus
import cardiganapp.core.designsystem.generated.resources.ic_refresh
import cardiganapp.core.designsystem.generated.resources.ic_rename
import cardiganapp.core.designsystem.generated.resources.ic_search
import cardiganapp.core.designsystem.generated.resources.ic_trash
import cardiganapp.core.designsystem.generated.resources.ic_upload
import org.jetbrains.compose.resources.painterResource

/** The 27 icons from the Figma Icons section, 16dp, tinted by `Icon`'s `tint` / `LocalContentColor`. */
object CardiganIcons {
    val More: Painter @Composable get() = painterResource(Res.drawable.ic_more)
    val Board: Painter @Composable get() = painterResource(Res.drawable.ic_board)
    val Plus: Painter @Composable get() = painterResource(Res.drawable.ic_plus)
    val Label: Painter @Composable get() = painterResource(Res.drawable.ic_label)
    val Trash: Painter @Composable get() = painterResource(Res.drawable.ic_trash)
    val Edit: Painter @Composable get() = painterResource(Res.drawable.ic_edit)
    val Refresh: Painter @Composable get() = painterResource(Res.drawable.ic_refresh)
    val LogOut: Painter @Composable get() = painterResource(Res.drawable.ic_log_out)
    val Boards: Painter @Composable get() = painterResource(Res.drawable.ic_boards)
    val Search: Painter @Composable get() = painterResource(Res.drawable.ic_search)
    val Close: Painter @Composable get() = painterResource(Res.drawable.ic_close)
    val ChevronDown: Painter @Composable get() = painterResource(Res.drawable.ic_chevron_down)
    val Upload: Painter @Composable get() = painterResource(Res.drawable.ic_upload)
    val ChevronLeft: Painter @Composable get() = painterResource(Res.drawable.ic_chevron_left)
    val CloseSmall: Painter @Composable get() = painterResource(Res.drawable.ic_close_small)
    val Filter: Painter @Composable get() = painterResource(Res.drawable.ic_filter)
    val Check: Painter @Composable get() = painterResource(Res.drawable.ic_check)
    val Alert: Painter @Composable get() = painterResource(Res.drawable.ic_alert)
    val Rename: Painter @Composable get() = painterResource(Res.drawable.ic_rename)
    val File: Painter @Composable get() = painterResource(Res.drawable.ic_file)
    val Download: Painter @Composable get() = painterResource(Res.drawable.ic_download)
    val CloudOff: Painter @Composable get() = painterResource(Res.drawable.ic_cloud_off)
    val Info: Painter @Composable get() = painterResource(Res.drawable.ic_info)
    val ArrowLeft: Painter @Composable get() = painterResource(Res.drawable.ic_arrow_left)
    val ArrowRight: Painter @Composable get() = painterResource(Res.drawable.ic_arrow_right)
    val Columns: Painter @Composable get() = painterResource(Res.drawable.ic_columns)
    val ChevronRight: Painter @Composable get() = painterResource(Res.drawable.ic_chevron_right)
}
