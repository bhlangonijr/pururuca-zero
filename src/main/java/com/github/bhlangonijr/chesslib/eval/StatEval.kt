package com.github.bhlangonijr.chesslib.eval

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import kotlin.math.abs
import kotlin.math.max

class StatEval {

    private val pieces = Piece.values().filter { it != Piece.NONE }

    fun predict(board: Board): Double {

        return 1.0
    }


    fun extractFeatures(board: Board): Array<Double> {

        val result = DoubleArray(pieces.size * pieces.size) { -1.0 }

        val pieceLocation = pieces
                .filter { board.getBitboard(it) > 0 }
                .associateBy({ it }, { board.getPieceLocation(it) })

        pieceLocation.entries.forEach { (p1, squares) ->
            squares.forEach { sq1 ->
                pieceLocation.entries.forEach { (p2, squares) ->
                    squares.forEach { sq2 ->
                        result[p1.ordinal * pieces.size + p2.ordinal] += distance(sq1, sq2)
                    }
                }
            }
        }
        return result.toTypedArray()
    }

    private fun distance(sq1: Square, sq2: Square) =
            max(abs(sq1.file.ordinal - sq2.file.ordinal), abs(sq1.rank.ordinal - sq2.rank.ordinal)).toDouble()

    private fun bitCount(bb: Long) = java.lang.Long.bitCount(bb)

}