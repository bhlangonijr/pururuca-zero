package com.github.bhlangonijr.chesslib.eval

import com.github.bhlangonijr.chesslib.*
import kotlin.math.abs
import kotlin.math.max

class StatEval {

    private val pieces = Piece.values().filter { it != Piece.NONE }
    private val distances = Rank.values().size - 1
    private val pieceTypeValues = mapOf(
            PieceType.NONE to 0.0,
            PieceType.PAWN to 1.0,
            PieceType.KNIGHT to 3.0,
            PieceType.BISHOP to 7.0,
            PieceType.ROOK to 19.0,
            PieceType.QUEEN to 53.0,
            PieceType.KING to 517.0)

    fun predict(board: Board): Double {

        return 1.0
    }


    fun extractFeatures(board: Board): Array<Double> {

        val distanceSide = DoubleArray(pieces.size * distances) { -1.0 }
        val distanceOther = DoubleArray(pieces.size * distances) { -1.0 }

        val pieceLocation = pieces
                .filter { board.getBitboard(it) > 0 }
                .associateBy({ it }, { board.getPieceLocation(it) })

        pieceLocation.entries.forEach { (p1, squares) ->
            squares.forEach { sq1 ->
                pieceLocation.entries.forEach { (p2, squares) ->
                    squares.forEach { sq2 ->
                        if (p2.pieceSide == p1.pieceSide) {
                            distanceSide[p1.ordinal * distances + distance(sq1, sq2)] += pieceTypeValues[p2.pieceType]!!
                        } else {
                            distanceOther[p1.ordinal * distances + distance(sq1, sq2)] += pieceTypeValues[p2.pieceType]!!
                        }
                    }
                }
            }
        }
        return distanceSide.toTypedArray() + distanceOther.toTypedArray()
    }

    private fun distance(sq1: Square, sq2: Square) =
            max(abs(sq1.file.ordinal - sq2.file.ordinal), abs(sq1.rank.ordinal - sq2.rank.ordinal))

    private fun bitCount(bb: Long) = java.lang.Long.bitCount(bb)

}