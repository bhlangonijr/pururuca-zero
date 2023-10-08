package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.extraFeaturesSize
import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.totalBoardPlanes
import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.totalTimeSteps
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
        var index = 0
        val bitboards = mutableListOf<Long>()
        for (i in 0 until totalTimeSteps * totalBoardPlanes) {
            index = i
            val bb = planeToBitboard(encoded, index)
            bitboards += bb
        }
        index++
        val extraFeaturesList = mutableListOf<Float>()
        for (i in 1 .. extraFeaturesSize) {
            val number = planeToNumber(encoded, index)
            extraFeaturesList += number
            index += 1
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
            updateBoardWithBbb(board, bitboards[planeIndex++], pieceType, side)
        }
        for (pieceType in pieceList) {
            updateBoardWithBbb(board, bitboards[planeIndex++], pieceType, side.flip())
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

    private fun planeToBitboard(input: INDArray, index: Int): Long {
        val plane = input.get(NDArrayIndex.indices(index.toLong()))
        return planeToBitboard(plane)
    }

    private fun planeToBitboard(plane: INDArray): Long {
        var bitboard = 0UL

        for (i in 0..7) {
            for (j in 0..7) {
                val value = plane.getFloat(0, i, j)
                if (value > 0) {
                    bitboard = bitboard xor (1UL shl (j + i * Nd4jEncoder.size))
                }
            }
        }
        return bitboard.toLong()
    }

    private fun planeToNumber(input: INDArray, index: Int): Float {
        val plane = input.get(NDArrayIndex.indices(index.toLong()))
        return planeToNumber(plane)
    }

    private fun planeToNumber(plane: INDArray): Float {
        if (!plane.shape().contentEquals(longArrayOf(1, 8, 8))) {
            throw IllegalArgumentException("Plane should have shape 1x8x8")
        }
        return plane.getFloat(0, 0, 0)
    }

}