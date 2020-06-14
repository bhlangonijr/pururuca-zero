package com.github.bhlangonijr.pururucazero.eval

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.pururucazero.SearchState

const val PAWN_VALUE = 100L
const val BISHOP_VALUE = 300L
const val KNIGHT_VALUE = 300L
const val ROOK_VALUE = 500L
const val QUEEN_VALUE = 950L
const val MAX_VALUE = 40000L
const val MATE_VALUE = 39000L

class MaterialEval : Evaluator {

    override fun evaluate(state: SearchState, board: Board): Long {

        return scoreMaterial(board)
    }

    private fun scoreMaterial(board: Board) = scoreMaterial(board, board.sideToMove)

    private fun scoreMaterial(board: Board, player: Side): Long {

        return countMaterial(board, player) - countMaterial(board, player.flip())
    }

    private fun countMaterial(board: Board, side: Side) =
            bitCount(board.getBitboard(Piece.make(side, PieceType.PAWN))) * PAWN_VALUE +
                    bitCount(board.getBitboard(Piece.make(side, PieceType.BISHOP))) * BISHOP_VALUE +
                    bitCount(board.getBitboard(Piece.make(side, PieceType.KNIGHT))) * KNIGHT_VALUE +
                    bitCount(board.getBitboard(Piece.make(side, PieceType.ROOK))) * ROOK_VALUE +
                    bitCount(board.getBitboard(Piece.make(side, PieceType.QUEEN))) * QUEEN_VALUE

    private fun bitCount(bb: Long) = java.lang.Long.bitCount(bb).toLong()
}

