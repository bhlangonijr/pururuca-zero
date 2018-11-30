package com.github.bhlangonijr.chesslib.eval

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.ml.FeatureSet
import kotlin.math.abs
import kotlin.math.max

class StatEval {

    private val pieceList = Piece.values().filter { it != Piece.NONE }
    private val maxDistanceSize = Rank.values().size - 1
    private val allBb = 0L.inv()
    private val pieceTypeValues = mapOf(
            PieceType.NONE to 0.0,
            PieceType.PAWN to 10.0,
            PieceType.KNIGHT to 30.0,
            PieceType.BISHOP to 70.0,
            PieceType.ROOK to 190.0,
            PieceType.QUEEN to 530.0,
            PieceType.KING to 1.0)

    fun predict(board: Board): Double {

        return 1.0
    }

    fun getFeatureSet(id: Int, board: Board, classId: Double): FeatureSet {

        val features = extractFeatures(board)
        val map = mutableMapOf<String, Int>()

        var sparseIdx = 0
        val sparseFeatures = arrayListOf<Double>()
        sparseFeatures.add(0, classId)
        for ((idx, feature) in features.withIndex()) {
            if (!feature.isNaN()) {
                sparseIdx++
                map["$idx"] = sparseIdx
                sparseFeatures.add(sparseIdx, feature)
            }
        }
        return FeatureSet(id, sparseFeatures, map)
    }

    fun extractFeatures(board: Board): Array<Double> {

        // histogram of chebyshev distances between pieces of same color weighted by piece type
        val distanceSide = DoubleArray(pieceList.size * maxDistanceSize) { Double.NaN }
        // histogram of chebyshev distances between pieces of different color weighted by piece type
        val distanceOther = DoubleArray(pieceList.size * maxDistanceSize) { Double.NaN }
        // pieces supported by other pieces of same color
        val attacksSide = DoubleArray(pieceList.size) { Double.NaN }
        // piece attacked by pieces of other color
        val attacksOther = DoubleArray(pieceList.size) { Double.NaN }
        // how many squares a piece type can move
        val moves = DoubleArray(pieceList.size) { Double.NaN }
        // squares surrounding piece supported by other pieces of same color
        val closeAttacksSide = DoubleArray(pieceList.size) { Double.NaN }
        // squares surrounding piece attacked by pieces of other color
        val closeAttacksOther = DoubleArray(pieceList.size) { Double.NaN }
        // number of white pawns occupying a given rank
        val whitePawnRank = DoubleArray(Rank.values().size - 1) { Double.NaN }
        // number of black pawns occupying a given rank
        val blackPawnRank = DoubleArray(Rank.values().size - 1) { Double.NaN }

        // piece counts
        val pieceCount = DoubleArray(pieceList.size) { Double.NaN }

        val pieceLocation = pieceList
                .filter { board.getBitboard(it) > 0 }
                .associateBy({ it }, { board.getPieceLocation(it) })

        val pieceAttacks = pieceLocation
                .entries
                .associateBy({ it.key }, { attacks(board, it.key, it.value, it.key.pieceSide) })

        pieceLocation.entries.forEach { (p1, squares) ->
            initArray(pieceCount, p1.ordinal)
            pieceCount[p1.ordinal] += bitCount(board.getBitboard(p1)).toDouble()
            squares.forEach { sq1 ->
                if (p1.pieceType == PieceType.PAWN) {
                    if (p1.pieceSide == Side.WHITE) {
                        initArray(whitePawnRank, sq1.rank.ordinal)
                        whitePawnRank[sq1.rank.ordinal] += 1.0
                    } else {
                        initArray(blackPawnRank, sq1.rank.ordinal)
                        blackPawnRank[sq1.rank.ordinal] += 1.0
                    }
                }
                pieceLocation.entries.forEach { (p2, squares) ->
                    squares.forEach { sq2 ->
                        val idx = p1.ordinal * maxDistanceSize + distance(sq1, sq2)
                        if (p2.pieceSide == p1.pieceSide) {
                            initArray(distanceSide, idx)
                            distanceSide[idx] += pieceTypeValues[p2.pieceType]!!
                        } else {
                            initArray(distanceOther, idx)
                            distanceOther[idx] += pieceTypeValues[p2.pieceType]!!
                        }
                    }
                }
            }
        }

        val allAttacks = pieceAttacks.entries.associateBy({it.key}, {it.value.map { it.second }.fold(0L) {l1, l2 -> l1 or l2}})

        val whiteAttacks = allAttacks.filter { it.key.pieceSide == Side.WHITE }.values.fold(0L) { l1, l2 -> l1 or l2}
        val blackAttacks = allAttacks.filter { it.key.pieceSide == Side.BLACK }.values.fold(0L) { l1, l2 -> l1 or l2}
        val allSideAttacks = mapOf(Side.WHITE to whiteAttacks, Side.BLACK to blackAttacks)

        val whitePawnMoves = pawnMoves(board, pieceLocation[Piece.WHITE_PAWN] ?: emptyList(), Side.WHITE)
        val blackPawnMoves = pawnMoves(board, pieceLocation[Piece.BLACK_PAWN] ?: emptyList(), Side.BLACK)
        val pawnMoves = mapOf(Piece.WHITE_PAWN to whitePawnMoves, Piece.BLACK_PAWN to blackPawnMoves)

        //TODO Pinned pieces cannot move
        pieceAttacks.entries.forEach { e1 ->
            if (e1.key.pieceType == PieceType.PAWN) {
                initArray(moves, e1.key.ordinal)
                moves[e1.key.ordinal] += bitCount(pawnMoves[e1.key] ?: 0L and board.bitboard.inv()).toDouble()
            }
            e1.value.forEach { pair ->
                val ownPiece = pair.first.bitboard
                val attackedSquares = pair.second
                if (e1.key.pieceType == PieceType.KING) {
                    val allowedSquares = attackedSquares and allSideAttacks[e1.key.pieceSide.flip()]!!.inv()
                    initArray(moves, e1.key.ordinal)
                    moves[e1.key.ordinal] += bitCount(allowedSquares and board.bitboard.inv()).toDouble()
                } else if (e1.key.pieceType != PieceType.PAWN) {
                    initArray(moves, e1.key.ordinal)
                    moves[e1.key.ordinal] += bitCount(attackedSquares and board.bitboard.inv()).toDouble()
                }
                pieceAttacks.entries.filter { board.getBitboard(it.key) and ownPiece.inv() > 0L }.forEach { e2 ->
                    val piece = board.getBitboard(e2.key) and ownPiece.inv()
                    if (e2.key.pieceSide == e1.key.pieceSide) {
                        initArray(attacksSide, e2.key.ordinal)
                        initArray(closeAttacksSide, e2.key.ordinal)
                        attacksSide[e2.key.ordinal] += pieceTypeValues[e1.key.pieceType]!! *
                                bitCount(piece and attackedSquares).toDouble()
                        closeAttacksSide[e2.key.ordinal] += pieceTypeValues[e1.key.pieceType]!! *
                                bitCount(surround(piece) and attackedSquares).toDouble()
                    } else {
                        initArray(attacksOther, e2.key.ordinal)
                        initArray(closeAttacksOther, e2.key.ordinal)
                        attacksOther[e2.key.ordinal] += pieceTypeValues[e1.key.pieceType]!! *
                                bitCount(piece and attackedSquares).toDouble()
                        closeAttacksOther[e2.key.ordinal] += pieceTypeValues[e1.key.pieceType]!! *
                                bitCount(surround(piece) and attackedSquares).toDouble()
                    }
                }
            }
        }

        val boardState = arrayOf(board.sideToMove.ordinal + 1.0, board.moveCounter.toDouble(),
                if (board.halfMoveCounter == 0) Double.NaN else board.halfMoveCounter.toDouble())
/*
                pieceCount.toTypedArray() +
                boardState

                whitePawnRank.toTypedArray() +
                blackPawnRank.toTypedArray()

                distanceSide.toTypedArray() +
                distanceOther.toTypedArray()

 */
        return moves.toTypedArray() +
                attacksSide.toTypedArray() +
                attacksOther.toTypedArray() +
                closeAttacksSide.toTypedArray() +
                closeAttacksOther.toTypedArray() +
                whitePawnRank.toTypedArray() +
                blackPawnRank.toTypedArray() +
                boardState +
                pieceCount.toTypedArray()
    }

    private fun initArray(array: DoubleArray, idx: Int) {

        if (array[idx].isNaN()) {
            array[idx] = 0.0
        }
    }

    private fun surround(piece: Long) = Bitboard.bbToSquareList(piece)
            .map { Bitboard.getKingAttacks(it, allBb)}.reduce {l1, l2 -> l1 or l2}

    private fun attacks(board: Board, piece: Piece, squares: List<Square>, side: Side): ArrayList<Pair<Square, Long>> {

        val result = ArrayList<Pair<Square, Long>>(squares.size)
        for (sq in squares) {
            val attacks = when(piece.pieceType) {
                PieceType.PAWN -> Bitboard.getPawnCaptures(side, sq,
                        allBb, board.enPassantTarget)
                PieceType.KNIGHT -> Bitboard.getKnightAttacks(sq, allBb)
                PieceType.BISHOP -> Bitboard.getBishopAttacks(board.bitboard, sq)
                PieceType.ROOK -> Bitboard.getRookAttacks(board.bitboard, sq)
                PieceType.QUEEN -> Bitboard.getRookAttacks(board.bitboard, sq)
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