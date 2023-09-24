package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.Bitboard.bitboardToString
import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.extraFeaturesSize
import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.totalBoardPlanes
import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.totalTimeSteps
import com.github.bhlangonijr.pururucazero.util.Utils
import com.github.bhlangonijr.pururucazero.util.Utils.flipVertical
import org.nd4j.linalg.api.ndarray.INDArray

import org.nd4j.linalg.indexing.NDArrayIndex
import java.lang.IllegalArgumentException

object Nd4jDecoder {

    private val pieceList = listOf(
        PieceType.KING,
        PieceType.PAWN,
        PieceType.BISHOP,
        PieceType.KNIGHT,
        PieceType.ROOK,
        PieceType.QUEEN
    )

    fun decode(encoded: INDArray): Board {
        val board = Board()
        board.clear()
        board.castleRight[Side.WHITE] = CastleRight.NONE
        board.castleRight[Side.BLACK] = CastleRight.NONE
        var row = 0
        var col = Nd4jEncoder.size

        val bitboards = mutableListOf<Long>()
        for (i in 1 .. totalTimeSteps) {
            for (j in 1..totalBoardPlanes) {
                val bb = planeToBitboard(encoded, row, col)
//                println("($i,$j)")
//                println(bitboardToString(bb))
                bitboards += bb
                row += Nd4jEncoder.size
                col += Nd4jEncoder.size
            }
        }
//        println(encoded.get(NDArrayIndex.interval(0, 8), NDArrayIndex.all()).toString())
//        println("----")
//        println(encoded.get(NDArrayIndex.interval(0+8, 8+8), NDArrayIndex.all()).toString())
//        println("----")
        val extraFeaturesList = mutableListOf<Float>()
        for (i in 1 .. extraFeaturesSize) {
            val number = planeToNumber(encoded, row, col)
            row += Nd4jEncoder.size
            col += Nd4jEncoder.size
            extraFeaturesList += number
            //println("($i)=$number")
        }
        var featureIndex = 0
        val side = Side.allSides[extraFeaturesList[featureIndex++].toInt()]
        board.sideToMove = side
        board.moveCounter = extraFeaturesList[featureIndex++].toInt()
        if (extraFeaturesList[featureIndex++] == 1f) {
            board.castleRight[Side.WHITE] = CastleRight.KING_SIDE
        }
        if (extraFeaturesList[featureIndex++] == 1f
            && board.castleRight[Side.WHITE] == CastleRight.NONE) {
            board.castleRight[Side.WHITE] = CastleRight.QUEEN_SIDE
        } else if (extraFeaturesList[featureIndex - 1] == 1f) {
            board.castleRight[Side.WHITE] = CastleRight.KING_AND_QUEEN_SIDE
        }
        if (extraFeaturesList[featureIndex++] == 1f) {
            board.castleRight[Side.BLACK] = CastleRight.KING_SIDE
        }
        if (extraFeaturesList[featureIndex++] == 1f
            && board.castleRight[Side.BLACK] == CastleRight.NONE) {
            board.castleRight[Side.BLACK] = CastleRight.QUEEN_SIDE
        } else if (extraFeaturesList[featureIndex - 1] == 1f) {
            board.castleRight[Side.BLACK] = CastleRight.KING_AND_QUEEN_SIDE
        }
        board.halfMoveCounter = extraFeaturesList[featureIndex].toInt()

        var planeIndex = 0
        for (pieceType in pieceList) {
            updateBoardWithBbb(board, bitboards[planeIndex++], pieceType, Side.WHITE)
        }
        for (pieceType in pieceList) {
            updateBoardWithBbb(board, bitboards[planeIndex++], pieceType, Side.BLACK)
        }
        return board
    }

    private fun updateBoardWithBbb(board: Board, bb: Long, pieceType: PieceType, side: Side) {
        if (bb > 0L) {
            val bitboard = if (side == Side.WHITE) bb else flipVertical(bb.toULong()).toLong()
            Bitboard.bbToSquareList(bitboard).forEach {
                board.setPiece(Piece.make(side, pieceType), it)
            }
        }
    }

    private fun planeToBitboard(plane: INDArray, row: Int, col: Int): Long {
        return planeToBitboard(plane.get(NDArrayIndex.interval(row, col), NDArrayIndex.all()))
    }

    private fun planeToBitboard(plane: INDArray): Long {
        if (!plane.shape().contentEquals(longArrayOf(8, 8))) {
            throw IllegalArgumentException("Plane should have shape 8x8")
        }
        var bitboard = 0UL

        for (i in 0..7) {
            for (j in 0..7) {
                val value = plane.getFloat(i, j)
                if (value > 0) {
                    bitboard = bitboard xor (1UL shl (j + i * Nd4jEncoder.size))
                }
            }
        }
        return bitboard.toLong()
    }

    private fun planeToNumber(plane: INDArray, row: Int, col: Int): Float {
        return planeToNumber(plane.get(NDArrayIndex.interval(row, col), NDArrayIndex.all()))
    }

    private fun planeToNumber(plane: INDArray): Float {
        if (!plane.shape().contentEquals(longArrayOf(8, 8))) {
            throw IllegalArgumentException("Plane should have shape 8x8")
        }
        return plane.getFloat(0, 0)
    }

}