package com.github.bhlangonijr.pururucazero.encoder

import com.github.bhlangonijr.chesslib.*
import kotlin.math.abs
import kotlin.math.max

class PositionStatsEncoder : BoardEncoder {

    private val pieceList = Piece.values().filter { it != Piece.NONE }
    private val allBb = 0L.inv()
    private val pieceTypeValues = mapOf(
            PieceType.NONE to 0.0f,
            PieceType.PAWN to 10.0f,
            PieceType.KNIGHT to 30.0f,
            PieceType.BISHOP to 70.0f,
            PieceType.ROOK to 190.0f,
            PieceType.QUEEN to 530.0f,
            PieceType.KING to 1.0f)

    override fun encode(board: Board): FloatArray {

        // histogram of chebyshev distances between pieces of same color weighted by piece type
        val distanceSide = FloatArray(pieceList.size * pieceList.size) { Float.NaN }
        // histogram of chebyshev distances between pieces of different color weighted by piece type
        val distanceOther = FloatArray(pieceList.size * pieceList.size) { Float.NaN }
        // pieces supported by other pieces of same color
        val attacks = FloatArray(Side.values().size) { Float.NaN }
        // piece attacked by pieces of other color
        //val attacksOther = FloatArray(Side.values().size) { Float.NaN }
        // how many squares a piece type can move
        val moves = FloatArray(Side.values().size) { Float.NaN }
        // squares surrounding piece supported by other pieces of same color
        val closeAttacks = FloatArray(Side.values().size) { Float.NaN }
        // squares surrounding piece attacked by pieces of other color
        //val closeAttacksOther = FloatArray(Side.values().size) { Float.NaN }
        // number of white pawns occupying a given rank
        val whitePawnRank = FloatArray(Rank.values().size - 1) { Float.NaN }
        // number of black pawns occupying a given rank
        val blackPawnRank = FloatArray(Rank.values().size - 1) { Float.NaN }

        // piece counts
        val pieceCount = FloatArray(pieceList.size) { Float.NaN }

        val pieceLocation = pieceList
                .filter { board.getBitboard(it) > 0 }
                .associateBy({ it }, { board.getPieceLocation(it) })

        val pieceAttacks = pieceLocation
                .entries
                .associateBy({ it.key }, { attacks(board, it.key, it.value, it.key.pieceSide) })

        pieceLocation.entries.forEach { (p1, squares) ->
            initArray(pieceCount, p1.ordinal)
            pieceCount[p1.ordinal] += bitCount(board.getBitboard(p1)).toFloat()
            squares.forEach { sq1 ->
                if (p1.pieceType == PieceType.PAWN) {
                    if (p1.pieceSide == Side.WHITE) {
                        initArray(whitePawnRank, sq1.rank.ordinal)
                        whitePawnRank[sq1.rank.ordinal] += 1.0f
                    } else {
                        initArray(blackPawnRank, sq1.rank.ordinal)
                        blackPawnRank[sq1.rank.ordinal] += 1.0f
                    }
                }
                pieceLocation.entries.forEach { (p2, squares) ->
                    squares.forEach { sq2 ->
                        val idx = p1.ordinal * pieceList.size + p2.ordinal
                        if (p2.pieceSide == p1.pieceSide) {
                            initArray(distanceSide, idx)
                            distanceSide[idx] += (8 - distance(sq1, sq2).toFloat())
                        } else {
                            initArray(distanceOther, idx)
                            distanceOther[idx] += (8 - distance(sq1, sq2).toFloat())
                        }
                    }
                }
            }
        }

        val allAttacks = pieceAttacks.entries.associateBy({ it.key },
                { it.value.map { it.second }.fold(0L) { l1, l2 -> l1 or l2 } })

        val whiteAttacks = allAttacks.filter { it.key.pieceSide == Side.WHITE }.values.fold(0L) { l1, l2 -> l1 or l2 }
        val blackAttacks = allAttacks.filter { it.key.pieceSide == Side.BLACK }.values.fold(0L) { l1, l2 -> l1 or l2 }
        val allSideAttacks = mapOf(Side.WHITE to whiteAttacks, Side.BLACK to blackAttacks)

        val whitePawnMoves = pawnMoves(board, pieceLocation[Piece.WHITE_PAWN] ?: emptyList(), Side.WHITE)
        val blackPawnMoves = pawnMoves(board, pieceLocation[Piece.BLACK_PAWN] ?: emptyList(), Side.BLACK)
        val pawnMoves = mapOf(Piece.WHITE_PAWN to whitePawnMoves, Piece.BLACK_PAWN to blackPawnMoves)

        //TODO Pinned pieces cannot move
        pieceAttacks.entries.forEach { e1 ->
            if (e1.key.pieceType == PieceType.PAWN) {
                initArray(moves, e1.key.pieceSide.ordinal)
                moves[e1.key.pieceSide.ordinal] += bitCount(pawnMoves[e1.key] ?: 0L
                and board.bitboard.inv()).toFloat()
            }
            e1.value.forEach { pair ->
                val ownPiece = pair.first.bitboard
                val attackedSquares = pair.second
                if (e1.key.pieceType == PieceType.KING) {
                    val allowedSquares = attackedSquares and allSideAttacks[e1.key.pieceSide.flip()]!!.inv()
                    initArray(moves, e1.key.pieceSide.ordinal)
                    moves[e1.key.pieceSide.ordinal] += bitCount(allowedSquares and board.bitboard.inv()).toFloat()
                } else if (e1.key.pieceType != PieceType.PAWN) {
                    initArray(moves, e1.key.pieceSide.ordinal)
                    moves[e1.key.pieceSide.ordinal] += bitCount(attackedSquares and board.bitboard.inv()).toFloat()
                }
                pieceAttacks.entries.filter { board.getBitboard(it.key) and ownPiece.inv() > 0L }.forEach { e2 ->
                    val piece = board.getBitboard(e2.key) and ownPiece.inv()
                    initArray(attacks, e2.key.pieceSide.ordinal)
                    initArray(closeAttacks, e2.key.pieceSide.ordinal)

                    attacks[e2.key.pieceSide.ordinal] += (pieceTypeValues[e1.key.pieceType]!!) *
                            bitCount(piece and attackedSquares).toFloat()
                    closeAttacks[e2.key.pieceSide.ordinal] += (pieceTypeValues[e1.key.pieceType]!!) *
                            bitCount(surround(piece) and attackedSquares).toFloat()
                }
            }

        }

        val boardState = FloatArray(5)
        boardState[0] = board.sideToMove.ordinal + 1.0f
        boardState[1] = board.moveCounter.toFloat()
        boardState[2] = board.halfMoveCounter.toFloat()
        boardState[3] = board.getCastleRight(Side.WHITE).ordinal + 1.0f
        boardState[4] = board.getCastleRight(Side.BLACK).ordinal + 1.0f
/*
                pieceCount.toTypedArray() +
                boardState

                whitePawnRank.toTypedArray() +
                blackPawnRank.toTypedArray()
                moves.toTypedArray() +
                distanceSide.toTypedArray() +
                distanceOther.toTypedArray()
                attacksSide.toTypedArray() +
                attacksOther.toTypedArray() +
                closeAttacksSide.toTypedArray() +
                closeAttacksOther.toTypedArray() +
                whitePawnRank.toTypedArray() +
                blackPawnRank.toTypedArray()

 */
        return boardState +
//                distanceSide +
//                distanceOther +
                moves +
                attacks +
                closeAttacks
//                whitePawnRank +
//                blackPawnRank
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
                PieceType.PAWN -> Bitboard.getPawnCaptures(side, sq,
                        allBb, board.enPassantTarget)
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