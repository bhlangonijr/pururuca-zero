package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.Eval.Companion.BISHOP_VALUE
import com.github.bhlangonijr.chesslib.Eval.Companion.KNIGHT_VALUE
import com.github.bhlangonijr.chesslib.Eval.Companion.PAWN_VALUE
import com.github.bhlangonijr.chesslib.Eval.Companion.QUEEN_VALUE
import com.github.bhlangonijr.chesslib.Eval.Companion.ROOK_VALUE

class Eval {

    companion object {
        val PAWN_VALUE = 1
        val BISHOP_VALUE = 3
        val KNIGHT_VALUE = 3
        val ROOK_VALUE = 5
        val QUEEN_VALUE = 9
        val KING_VALUE = 100000
        val MATE_VALUE = 200000
    }
}

fun scoreMaterial(board: Board): Int {

    val side = board.sideToMove
    val other = side.flip()
    return countMaterial(board, side) - countMaterial(board, other)
}

private fun countMaterial(board: Board, side: Side) =
        bitCount(board.getBitboard(Piece.make(side, PieceType.PAWN))) * PAWN_VALUE +
                bitCount(board.getBitboard(Piece.make(side, PieceType.BISHOP))) * BISHOP_VALUE +
                bitCount(board.getBitboard(Piece.make(side, PieceType.KNIGHT))) * KNIGHT_VALUE +
                bitCount(board.getBitboard(Piece.make(side, PieceType.ROOK))) * ROOK_VALUE +
                bitCount(board.getBitboard(Piece.make(side, PieceType.QUEEN))) * QUEEN_VALUE

private fun bitCount(bb: Long) = java.lang.Long.bitCount(bb)