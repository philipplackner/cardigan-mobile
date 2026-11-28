package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object BoardListRoute

@Serializable
data object NewBoardRoute

@Serializable
data class RenameBoardRoute(
    val boardId: String,
)

@Serializable
data class DeleteBoardRoute(
    val boardId: String,
)
