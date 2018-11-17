package com.github.bhlangonijr.chesslib.eval

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side

class StatEval

fun predict(board: Board) = predict(board, board.sideToMove)

fun predict(board: Board, player: Side): Double {

    return 1.0
}


fun extractFeatures(board: Board, player: Side): Array<Double> {

    return arrayOf(1.0)
}


private fun bitCount(bb: Long) = java.lang.Long.bitCount(bb)