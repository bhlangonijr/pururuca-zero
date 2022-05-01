package com.github.bhlangonijr.pururucazero.encoder

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.pururucazero.eval.MaterialEval
import kotlin.math.abs
import kotlin.math.max

class PositionStatsEncoder {

    private val pieceList = Piece.values().filter { it != Piece.NONE }
    private val sideList = Side.values()
    private val allBb = 0L.inv()
    private val pieceTypeValues = mapOf(
        PieceType.NONE to 0.0f,
        PieceType.PAWN to 1.0f,
        PieceType.KNIGHT to 3.0f,
        PieceType.BISHOP to 3.0f,
        PieceType.ROOK to 5.0f,
        PieceType.QUEEN to 9.0f,
        PieceType.KING to 20.0f
    )
    private val materialEval = MaterialEval()


    fun encode(board: Board): FloatArray {

        // piece attacked by pieces of other color
        val attacks = FloatArray(sideList.size) { Float.NaN }
        // how many squares a piece type can move
        val moves = FloatArray(sideList.size) { Float.NaN }
        // squares surrounding piece supported by other pieces of same color
        val closeAttacks = FloatArray(sideList.size) { Float.NaN }

        val pieceLocation = pieceList
            .filter { board.getBitboard(it) != 0L }
            .associateBy({ it }, { board.getPieceLocation(it) })

        val pieceAttacks = pieceLocation
            .entries
            .associateBy({ it.key }, { attacks(board, it.key, it.value, it.key.pieceSide) })

        val allAttacks = pieceAttacks.entries.associateBy({ it.key },
            { it.value.map { it.second }.fold(0L) { l1, l2 -> l1.or(l2) } })

        val whiteAttacks = allAttacks.filter { it.key.pieceSide == Side.WHITE }.values.fold(0L) { l1, l2 -> l1 or l2 }
        val blackAttacks = allAttacks.filter { it.key.pieceSide == Side.BLACK }.values.fold(0L) { l1, l2 -> l1 or l2 }
        val allSideAttacks = mapOf(Side.WHITE to whiteAttacks, Side.BLACK to blackAttacks)
        //allSideAttacks.forEach { println("${it.key} \n${Bitboard.bitboardToString(it.value)}") }


        val whitePawnMoves = pawnMoves(board, pieceLocation[Piece.WHITE_PAWN] ?: emptyList(), Side.WHITE)
        val blackPawnMoves = pawnMoves(board, pieceLocation[Piece.BLACK_PAWN] ?: emptyList(), Side.BLACK)
        val pawnMoves = mapOf(Piece.WHITE_PAWN to whitePawnMoves, Piece.BLACK_PAWN to blackPawnMoves)


        allAttacks.forEach { entry ->
            initArray(attacks, entry.key.pieceSide.ordinal)
            initArray(closeAttacks, entry.key.pieceSide.ordinal)

            pieceList.filter { it.pieceSide != entry.key.pieceSide && board.getBitboard(it) > 0L }.forEach {
                val piece = board.getBitboard(it)
                //println("${entry.key} $it \n${Bitboard.bitboardToString(piece and entry.value)} - ${bitCount(surround(piece) and entry.value)}")
                attacks[entry.key.pieceSide.ordinal] += (pieceTypeValues[it.pieceType] ?: 0.0f) *
                        bitCount(piece and entry.value).toFloat()
                closeAttacks[entry.key.pieceSide.ordinal] += (pieceTypeValues[it.pieceType] ?: 0.0f) *
                        bitCount(surround(piece) and entry.value).toFloat()
            }
        }

        //TODO Pinned pieces cannot move
        pieceAttacks.entries.forEach { entry ->
            if (entry.key.pieceType == PieceType.PAWN) {
                initArray(moves, entry.key.pieceSide.ordinal)
                moves[entry.key.pieceSide.ordinal] += bitCount(
                    pawnMoves[entry.key] ?: 0L
                    and board.bitboard.inv()
                ).toFloat()
            }
            entry.value.forEach { pair ->
                val attackedSquares = pair.second
                if (entry.key.pieceType == PieceType.KING) {
                    val allowedSquares = attackedSquares and allSideAttacks[entry.key.pieceSide.flip()]!!.inv()
                    initArray(moves, entry.key.pieceSide.ordinal)
                    moves[entry.key.pieceSide.ordinal] += bitCount(allowedSquares and board.bitboard.inv()).toFloat()
                } else if (entry.key.pieceType != PieceType.PAWN) {
                    initArray(moves, entry.key.pieceSide.ordinal)
                    moves[entry.key.pieceSide.ordinal] += bitCount(attackedSquares and board.bitboard.inv()).toFloat()
                }
            }
        }

        val boardState = FloatArray(4)
        val whiteAdvantage = materialEval.scoreMaterial(board, Side.WHITE).toFloat()
        boardState[0] =
            (if (whiteAdvantage > 0.0f) whiteAdvantage else 0.0f) + (if (board.sideToMove == Side.WHITE) 10.0f else 1.0f)
        boardState[1] =
            (if (whiteAdvantage < 0.0f) -whiteAdvantage else 0.0f) + (if (board.sideToMove == Side.BLACK) 10.0f else 1.0f)
//        boardState[2] = board.sideToMove.ordinal + 1.0f
//        boardState[2] = board.moveCounter.toFloat()
//        boardState[3] = board.halfMoveCounter.toFloat()
//        boardState[4] = board.getCastleRight(Side.WHITE).ordinal + 1.0f
//        boardState[5] = board.getCastleRight(Side.BLACK).ordinal + 1.0f
        boardState[2] = materialEval.scoreMaterial(board, Side.WHITE).toFloat()
        boardState[3] = materialEval.scoreMaterial(board, Side.BLACK).toFloat()

        return boardState +
                moves +
                attacks +
                closeAttacks
    }

    private fun initArray(array: FloatArray, idx: Int) {

        if (array[idx].isNaN()) {
            array[idx] = 0.0f
        }
    }

    private fun surround(piece: Long) = Bitboard.bbToSquareList(piece)
        .map { Bitboard.getKingAttacks(it, allBb) }.reduce { l1, l2 -> l1 or l2 }

    private fun attacks(board: Board, piece: Piece, squares: List<Square>, side: Side): ArrayList<Pair<Square, Long>> {

        val result = ArrayList<Pair<Square, Long>>(squares.size)
        for (sq in squares) {
            val attacks = when (piece.pieceType) {
                PieceType.PAWN -> Bitboard.getPawnCaptures(
                    side, sq,
                    allBb, board.enPassantTarget
                )
                PieceType.KNIGHT -> Bitboard.getKnightAttacks(sq, allBb)
                PieceType.BISHOP -> Bitboard.getBishopAttacks(board.bitboard, sq)
                PieceType.ROOK -> Bitboard.getRookAttacks(board.bitboard, sq)
                PieceType.QUEEN -> Bitboard.getQueenAttacks(board.bitboard, sq)
                PieceType.KING -> Bitboard.getKingAttacks(sq, allBb)
                else -> 0
            }
            result.add(Pair(sq, attacks))
        }
        return result
    }

    private fun pawnMoves(board: Board, squares: List<Square>, side: Side): Long {

        var result = 0L
        for (sq in squares) {
            val attacks = Bitboard.getPawnMoves(side, sq, board.bitboard)
            result = result or attacks
        }
        return result
    }

    private fun distance(sq1: Square, sq2: Square) =
        max(abs(sq1.file.ordinal - sq2.file.ordinal), abs(sq1.rank.ordinal - sq2.rank.ordinal))

    private fun bitCount(bb: Long) = java.lang.Long.bitCount(bb)

}