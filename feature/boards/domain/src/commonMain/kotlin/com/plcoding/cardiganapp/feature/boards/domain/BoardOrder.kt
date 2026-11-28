package com.plcoding.cardiganapp.feature.boards.domain

/**
 * The board list order from cardigan-shared-specs `api-conventions` "Board list order": updated
 * time descending, then created time descending, then id ascending. Only used to place one board
 * into a loaded list; the loaded list itself keeps the server's order.
 */
object BoardOrder : Comparator<BoardSummary> {
    override fun compare(
        a: BoardSummary,
        b: BoardSummary,
    ): Int =
        b.updatedAt.compareTo(a.updatedAt).takeIf { it != 0 }
            ?: b.createdAt.compareTo(a.createdAt).takeIf { it != 0 }
            ?: a.id.toString().compareTo(b.id.toString())

    /** Inserts [board] before the first board it sorts before, or at the end. */
    fun insert(
        boards: List<BoardSummary>,
        board: BoardSummary,
    ): List<BoardSummary> {
        val index = boards.indexOfFirst { compare(board, it) < 0 }.let { if (it == -1) boards.size else it }
        return boards.toMutableList().apply { add(index, board) }
    }

    /** Removes the board with [board]'s id and inserts [board] by this order. */
    fun replace(
        boards: List<BoardSummary>,
        board: BoardSummary,
    ): List<BoardSummary> = insert(boards.filterNot { it.id == board.id }, board)
}
