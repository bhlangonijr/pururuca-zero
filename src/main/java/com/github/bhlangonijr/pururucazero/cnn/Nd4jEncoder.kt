package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.pururucazero.util.Utils.flipVertical
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

object Nd4jEncoder {

    const val size = 8
    const val totalTimeSteps = 8
    val emptyPlanes = createEmptyPlanes()
    private val emptyBoardPlaneCache = emptyBoardPlane()
    private const val extraFeaturesSize = 7

    fun encode(board: Board): INDArray {

        val side = board.sideToMove
        val other = side.flip()
        val undoMoveList = mutableListOf<Move>()
        var planes = encodeToArray(board, side)

        for (i in 1 until totalTimeSteps) {
            planes += if (board.history.size > 1) {
                val move = board.undoMove()
                undoMoveList.add(move)
                encodeToArray(board, side)
            } else {
                emptyBoardPlaneCache
            }
        }

        for (i in undoMoveList.size - 1 downTo 0) {
            board.doMove(undoMoveList[i])
        }

        val sideHasKingSide = hasCastleRight(board, side, CastleRight.KING_SIDE)
        val sideHasQueenSide = hasCastleRight(board, side, CastleRight.QUEEN_SIDE)
        val otherHasKingSide = hasCastleRight(board, other, CastleRight.KING_SIDE)
        val otherHasQueenSide = hasCastleRight(board, other, CastleRight.QUEEN_SIDE)

        planes += sideToPlane(side) +
                numberToPlane(board.moveCounter) +
                numberToPlane(if (sideHasKingSide) 1 else 0) +
                numberToPlane(if (sideHasQueenSide) 1 else 0) +
                numberToPlane(if (otherHasKingSide) 1 else 0) +
                numberToPlane(if (otherHasQueenSide) 1 else 0) +
                numberToPlane(board.halfMoveCounter)
        return Nd4j.create(planes)
    }

    fun encodeToArray(board: Board, side: Side): Array<FloatArray> {

        val other = side.flip()

        return getPlane(board, PieceType.KING, side) +
                getPlane(board, PieceType.PAWN, side) +
                getPlane(board, PieceType.BISHOP, side) +
                getPlane(board, PieceType.KNIGHT, side) +
                getPlane(board, PieceType.ROOK, side) +
                getPlane(board, PieceType.QUEEN, side) +
                getPlane(board, PieceType.KING, other) +
                getPlane(board, PieceType.PAWN, other) +
                getPlane(board, PieceType.BISHOP, other) +
                getPlane(board, PieceType.KNIGHT, other) +
                getPlane(board, PieceType.ROOK, other) +
                getPlane(board, PieceType.QUEEN, other) +
                repetitionToPlane(if (board.isRepetition(2)) 1 else 0) +
                repetitionToPlane(if (board.isRepetition(3)) 1 else 0)
    }

    private fun createEmptyPlanes(): INDArray {

        return Nd4j.create(Array(
            size * 14 * totalTimeSteps +
                    size * extraFeaturesSize) {
            FloatArray(size) { 0f }
        })
    }

    private fun emptyBoardPlane(): Array<FloatArray> {

        return Array(size * 14) {
            FloatArray(size) { 0f }
        }
    }

    private fun emptyPlane() =
        Array(size) {
            FloatArray(size) { 0f }
        }

    private fun getPlane(
        board: Board,
        pieceType: PieceType,
        side: Side
    ): Array<FloatArray> {

        val sideToMove = board.sideToMove
        val piece = Piece.make(side, pieceType)
        // view point should be flipped according to side to play
        val bb = board.getBitboard(piece)
        val bitboard = if (sideToMove == Side.WHITE) {
            bb
        } else {
            flipVertical(bb)
        }
        return bitboardToPlane(bitboard)
    }

    private fun numberToPlane(number: Int) =
        Array(size) {
            FloatArray(size) {
                number.toFloat()
            }
        }

    private fun sideToPlane(side: Side) =
        Array(size) {
            FloatArray(size) {
                side.ordinal.toFloat()
            }
        }

    private fun repetitionToPlane(repetition: Int) =
        Array(size) {
            FloatArray(size) {
                repetition.toFloat()
            }
        }

    private fun bitboardToPlane(bb: Long) =
        Array(size) { i ->
            FloatArray(size) { j ->
                if ((1UL shl (j + i * size)) and bb.toULong() > 0UL) 1f else 0f
            }
        }

    private fun hasCastleRight(
        board: Board,
        side: Side,
        castleRight: CastleRight
    ): Boolean {

        val right = board.getCastleRight(side)
        return right == castleRight || right == CastleRight.KING_AND_QUEEN_SIDE
    }
}