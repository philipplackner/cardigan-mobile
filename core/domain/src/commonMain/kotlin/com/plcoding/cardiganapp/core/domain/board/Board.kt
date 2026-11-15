package com.plcoding.cardiganapp.core.domain.board

import kotlin.uuid.Uuid

data class Board(
    val id: Uuid,
    val name: String,
    val key: String,
)
