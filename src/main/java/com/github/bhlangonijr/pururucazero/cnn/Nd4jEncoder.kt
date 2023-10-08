package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.pururucazero.util.Utils.flipVertical
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

object Nd4jEncoder {

    const val size = 8
    const val totalTimeSteps = 8
    const val totalBoardPlanes = 14
    const val extraFeaturesSize = 7
    const val numberOfPlanes = totalBoardPlanes * totalTimeSteps + extraFeaturesSize // 119 as per alphazero paper
    private val emptyBoardPlaneCache = emptyBoardPlane()


    fun encode(board: Board): INDArray {

        val side = board.sideToMove
        val undoMoveList = mutableListOf<Move>()
        var planes = encodeToArray(board)

        for (i in 1 until totalTimeSteps) {
            planes += if (board.history.size > 1) {
                val move = board.undoMove()
                undoMoveList.add(move)
                encodeToArray(board)
            } else {
                emptyBoardPlaneCache
            }
        }

        for (i in undoMoveList.size - 1 downTo 0) {
            board.doMove(undoMoveList[i])
        }

        val whiteHasKingSide = hasCastleRight(board, Side.WHITE, CastleRight.KING_SIDE)
        val whiteHasQueenSide = hasCastleRight(board, Side.WHITE, CastleRight.QUEEN_SIDE)
        val blackHasKingSide = hasCastleRight(board, Side.BLACK, CastleRight.KING_SIDE)
        val blackHasQueenSide = hasCastleRight(board, Side.BLACK, CastleRight.QUEEN_SIDE)

        planes += sideToPlane(side) +
                numberToPlane(board.moveCounter) +
                numberToPlane(if (whiteHasKingSide) 1 else 0) +
                numberToPlane(if (whiteHasQueenSide) 1 else 0) +
                numberToPlane(if (blackHasKingSide) 1 else 0) +
                numberToPlane(if (blackHasQueenSide) 1 else 0) +
                numberToPlane(board.halfMoveCounter)
        return Nd4j.create(planes)
    }

    fun encodeToArray(board: Board): Array<Array<FloatArray>> {

        val result = Array(totalBoardPlanes) { index ->
            when (index) {
                0 -> getPlane(board, PieceType.KING, Side.WHITE)
                1 -> getPlane(board, PieceType.PAWN, Side.WHITE)
                2 -> getPlane(board, PieceType.BISHOP, Side.WHITE)
                3 -> getPlane(board, PieceType.KNIGHT, Side.WHITE)
                4 -> getPlane(board, PieceType.ROOK, Side.WHITE)
                5 -> getPlane(board, PieceType.QUEEN, Side.WHITE)
                6 -> getPlane(board, PieceType.KING, Side.BLACK)
                7 -> getPlane(board, PieceType.PAWN, Side.BLACK)
                8 -> getPlane(board, PieceType.BISHOP, Side.BLACK)
                9 -> getPlane(board, PieceType.KNIGHT, Side.BLACK)
                10 -> getPlane(board, PieceType.ROOK, Side.BLACK)
                11 -> getPlane(board, PieceType.QUEEN, Side.BLACK)
                12 -> repetitionToPlane(if (board.isRepetition(2)) 1 else 0)
                13 -> repetitionToPlane(if (board.isRepetition(3)) 1 else 0)
                else -> emptyPlane()
            }
        }
        return result
    }

    private fun emptyBoardPlane(): Array<Array<FloatArray>> {

        return Array(totalBoardPlanes) {
            Array(size) {
                FloatArray(size) { 0f }
            }
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
        val piece = Piece.make(side, pieceType)
        // view point should be flipped according to side to play
        val bb = board.getBitboard(piece)
        val bitboard = if (piece.pieceSide == Side.WHITE) {
            bb
        } else {
            flipVertical(bb.toULong()).toLong()
        }
        return bitboardToPlane(bitboard)
    }

    private fun numberToPlane(number: Int) =
        Array(1) {
            Array(size) {
                FloatArray(size) {
                    number.toFloat()
                }
            }
        }

    private fun sideToPlane(side: Side) =
        Array(1) {
            Array(size) {
                FloatArray(size) {
                    side.ordinal.toFloat()
                }
            }
        }

    private fun repetitionToPlane(repetition: Int) =
        Array(size) {
            FloatArray(size) {
                repetition.toFloat()
            }
        }

    fun bitboardToPlane(bb: Long) =
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