package com.github.bhlangonijr.chesslib.eval

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side

const val PAWN_VALUE = 100
const val BISHOP_VALUE = 300
const val KNIGHT_VALUE = 300
const val ROOK_VALUE = 500
const val QUEEN_VALUE = 950
const val MAX_VALUE = 40000
const val MATE_VALUE = 39000

class Eval

fun scoreMaterial(board: Board) = scoreMaterial(board, board.sideToMove)

fun scoreMaterial(board: Board, player: Side): Int {

    return countMaterial(board, player) - countMaterial(board, player.flip())
}

private fun countMaterial(board: Board, side: Side) =
        bitCount(board.getBitboard(Piece.make(side, PieceType.PAWN))) * PAWN_VALUE +
                bitCount(board.getBitboard(Piece.make(side, PieceType.BISHOP))) * BISHOP_VALUE +
                bitCount(board.getBitboard(Piece.make(side, PieceType.KNIGHT))) * KNIGHT_VALUE +
                bitCount(board.getBitboard(Piece.make(side, PieceType.ROOK))) * ROOK_VALUE +
                bitCount(board.getBitboard(Piece.make(side, PieceType.QUEEN))) * QUEEN_VALUE

private fun bitCount(bb: Long) = java.lang.Long.bitCount(bb)