package com.github.bhlangonijr.pururucazero.eval

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.pururucazero.SearchState
import kotlin.math.min

const val PAWN_VALUE = 100L
const val BISHOP_VALUE = 320L
const val KNIGHT_VALUE = 330L
const val ROOK_VALUE = 500L
const val QUEEN_VALUE = 900L
const val MAX_VALUE = 40000L
const val MATE_VALUE = 39000L

val PAWN_PST = longArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0)

val KNIGHT_PST = longArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
)

val BISHOP_PST = longArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
)

val ROOK_PST = longArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  0,  5,  5,  0,  0,  0
)

val QUEEN_PST = longArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -5,  0,  5,  5,  5,  5,  0, -5,
        0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
)

val KING_OPENING_PST = longArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
)

val KING_END_PST = longArrayOf(
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
)

class MaterialEval : Evaluator, BoardEventListener {

    var linkedBoard: Board? = null
    var allPstValues = arrayOfNulls<Long>(2)
    var kingPstOpeningValues = arrayOfNulls<Long>(2)
    var kingPstEndValues = arrayOfNulls<Long>(2)

    override fun evaluate(state: SearchState, board: Board): Long {

        return scoreMaterial(board) + scorePieceSquare(board)
    }

    override fun pieceStaticValue(piece: Piece): Long {

        return when (piece.pieceType) {
            PieceType.PAWN -> PAWN_VALUE
            PieceType.BISHOP -> BISHOP_VALUE
            PieceType.KNIGHT -> KNIGHT_VALUE
            PieceType.ROOK -> ROOK_VALUE
            PieceType.QUEEN -> QUEEN_VALUE
            PieceType.KING -> MATE_VALUE
            else -> 0L
        }
    }

    override fun onEvent(event: BoardEvent?) {

        when (event?.type) {
            BoardEventType.ON_LOAD -> linkedBoard?.let { loadAllValues(event as Board) }
            BoardEventType.ON_MOVE -> updateDoMovePst(event as Move)
            BoardEventType.ON_UNDO_MOVE -> updateUndoMovePst(event as MoveBackup)
        }
    }

    fun scoreMaterial(board: Board) = scoreMaterial(board, board.sideToMove)

    private fun scoreMaterial(board: Board, player: Side): Long {

        return countMaterial(board, player) - countMaterial(board, player.flip())
    }

    fun scorePieceSquare(board: Board) = scorePieceSquare(board, board.sideToMove)

    fun scorePieceSquare(board: Board, player: Side): Long {

        if (linkedBoard == null || linkedBoard != board) {
            linkedBoard?.eventListener?.clear()
            linkedBoard = board
            loadAllValues(board)
            board.addEventListener(BoardEventType.ON_MOVE, this)
            board.addEventListener(BoardEventType.ON_UNDO_MOVE, this)
            board.addEventListener(BoardEventType.ON_LOAD, this)
        }
        val pstValues = allPstValues[player.ordinal]?.minus(allPstValues[player.flip().ordinal]!!) ?: 0L
        val sideKingPst = weightedAverage(board, kingPstOpeningValues[player.ordinal], kingPstEndValues[player.ordinal])
        val otherKingPst = weightedAverage(board, kingPstOpeningValues[player.flip().ordinal], kingPstEndValues[player.flip().ordinal])
        return pstValues.plus(sideKingPst.minus(otherKingPst))
    }

    private fun pieceSquareValue(piece: Piece, square: Square): Long {

        return when (piece.pieceType) {
            PieceType.PAWN -> PAWN_PST[getIndex(piece.pieceSide, square)]
            PieceType.KNIGHT -> KNIGHT_PST[getIndex(piece.pieceSide, square)]
            PieceType.BISHOP -> BISHOP_PST[getIndex(piece.pieceSide, square)]
            PieceType.ROOK -> ROOK_PST[getIndex(piece.pieceSide, square)]
            PieceType.QUEEN -> QUEEN_PST[getIndex(piece.pieceSide, square)]
            else -> 0L
        }
    }

    private fun updateDoMovePst(move: Move) {

        linkedBoard?.let {
            val movingPiece = it.backup.last.movingPiece
            val side = it.sideToMove
            val otherSide = side.flip()
            if (!it.backup.isEmpty() && it.backup.last.capturedPiece != null) {
                val capturedPiece = it.backup.last.capturedPiece
                val capturedSquare = it.backup.last.capturedSquare
                allPstValues[side.ordinal] = allPstValues[side.ordinal]
                        ?.minus(pieceSquareValue(capturedPiece, capturedSquare))
            }
            if (movingPiece.pieceType != PieceType.KING) {
                allPstValues[otherSide.ordinal] = allPstValues[otherSide.ordinal]
                        ?.minus(pieceSquareValue(movingPiece, move.from))
                allPstValues[otherSide.ordinal] = allPstValues[otherSide.ordinal]
                        ?.plus(pieceSquareValue(movingPiece, move.to))
            } else if (movingPiece.pieceType == PieceType.KING) {
                kingPstOpeningValues[otherSide.ordinal] = kingPstOpeningValues[otherSide.ordinal]
                        ?.minus(KING_OPENING_PST[getIndex(otherSide, move.from)])
                kingPstOpeningValues[otherSide.ordinal] = kingPstOpeningValues[otherSide.ordinal]
                        ?.plus(KING_OPENING_PST[getIndex(otherSide, move.to)])
            }
        }
    }

    private fun updateUndoMovePst(backup: MoveBackup) {

        linkedBoard?.let {
            val capturedPiece = backup.capturedPiece
            val capturedSquare = backup.capturedSquare
            val movingPiece = backup.movingPiece
            val otherSide = it.sideToMove
            val side = otherSide.flip()
            if (capturedPiece != null) {
                allPstValues[side.ordinal] = allPstValues[side.ordinal]
                        ?.plus(pieceSquareValue(capturedPiece, capturedSquare))
            }
            if (movingPiece.pieceType != PieceType.KING) {
                allPstValues[otherSide.ordinal] = allPstValues[otherSide.ordinal]
                        ?.minus(pieceSquareValue(movingPiece, backup.move.to))
                allPstValues[otherSide.ordinal] = allPstValues[otherSide.ordinal]
                        ?.plus(pieceSquareValue(movingPiece, backup.move.from))
            } else if (movingPiece.pieceType == PieceType.KING) {
                kingPstOpeningValues[otherSide.ordinal] = kingPstOpeningValues[otherSide.ordinal]
                        ?.minus(KING_OPENING_PST[getIndex(otherSide, backup.move.to)])
                kingPstOpeningValues[otherSide.ordinal] = kingPstOpeningValues[otherSide.ordinal]
                        ?.plus(KING_OPENING_PST[getIndex(otherSide, backup.move.from)])
            }
        }
    }

    private fun loadAllValues(board: Board) {

        allPstValues[Side.WHITE.ordinal] = calculatePieceSquare(board, Side.WHITE)
        allPstValues[Side.BLACK.ordinal] = calculatePieceSquare(board, Side.BLACK)
        kingPstOpeningValues[Side.WHITE.ordinal] = calculateKingOpeningPieceSquare(board, Side.WHITE)
        kingPstOpeningValues[Side.BLACK.ordinal] = calculateKingOpeningPieceSquare(board, Side.BLACK)
        kingPstEndValues[Side.WHITE.ordinal] = calculateKingEndPieceSquare(board, Side.WHITE)
        kingPstEndValues[Side.BLACK.ordinal] = calculateKingEndPieceSquare(board, Side.BLACK)
    }

    private fun weightedAverage(board: Board, openingValue: Long?, endingValue: Long?): Long {

        if (openingValue == null || endingValue == null) {
            return 0L
        }
        val maxMoves = 40
        val phase = min(maxMoves, board.moveCounter)
        return (maxMoves - phase) * openingValue / maxMoves +
                phase * endingValue / maxMoves

    }

    private fun calculateKingOpeningPieceSquare(board: Board, player: Side): Long {

        var sum = 0L
        board.getPieceLocation(Piece.make(player, PieceType.KING)).forEach {
            sum += KING_OPENING_PST[getIndex(player, it)]
        }
        return sum
    }

    private fun calculateKingEndPieceSquare(board: Board, player: Side): Long {

        var sum = 0L
        board.getPieceLocation(Piece.make(player, PieceType.KING)).forEach {
            sum += KING_END_PST[getIndex(player, it)]
        }
        return sum
    }

    private fun calculatePieceSquare(board: Board, player: Side): Long {

        var sum = 0L

        board.getPieceLocation(Piece.make(player, PieceType.PAWN)).forEach {
            sum += PAWN_PST[getIndex(player, it)]
        }
        board.getPieceLocation(Piece.make(player, PieceType.KNIGHT)).forEach {
            sum += KNIGHT_PST[getIndex(player, it)]
        }
        board.getPieceLocation(Piece.make(player, PieceType.BISHOP)).forEach {
            sum += BISHOP_PST[getIndex(player, it)]
        }
        board.getPieceLocation(Piece.make(player, PieceType.ROOK)).forEach {
            sum += ROOK_PST[getIndex(player, it)]
        }
        board.getPieceLocation(Piece.make(player, PieceType.QUEEN)).forEach {
            sum += QUEEN_PST[getIndex(player, it)]
        }
        return sum
    }

    private fun getIndex(side: Side, sq: Square) =
            if (side == Side.BLACK) sq.ordinal  else 63 - sq.ordinal

    private fun countMaterial(board: Board, side: Side) =
            bitCount(board.getBitboard(Piece.make(side, PieceType.PAWN))) * PAWN_VALUE +
                    bitCount(board.getBitboard(Piece.make(side, PieceType.BISHOP))) * BISHOP_VALUE +
                    bitCount(board.getBitboard(Piece.make(side, PieceType.KNIGHT))) * KNIGHT_VALUE +
                    bitCount(board.getBitboard(Piece.make(side, PieceType.ROOK))) * ROOK_VALUE +
                    bitCount(board.getBitboard(Piece.make(side, PieceType.QUEEN))) * QUEEN_VALUE

    private fun bitCount(bb: Long) = java.lang.Long.bitCount(bb).toLong()
}