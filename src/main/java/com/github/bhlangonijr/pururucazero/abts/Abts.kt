package com.github.bhlangonijr.pururucazero.abts

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.pururucazero.SearchEngine
import com.github.bhlangonijr.pururucazero.SearchState
import com.github.bhlangonijr.pururucazero.eval.Evaluator
import com.github.bhlangonijr.pururucazero.eval.MATE_VALUE
import com.github.bhlangonijr.pururucazero.eval.MAX_VALUE
import com.github.bhlangonijr.pururucazero.eval.MaterialEval
import com.sun.org.apache.xpath.internal.operations.Bool
import kotlin.math.max
import kotlin.math.min


const val MAX_DEPTH = 100

@ExperimentalStdlibApi
class Abts constructor(private var evaluator: Evaluator = MaterialEval(),
                       private var transpositionTable: TranspositionTable = TranspositionTable()) : SearchEngine {

    private val emptyMove = Move(Square.NONE, Square.NONE)

    override fun rooSearch(state: SearchState): Move {

        val fen = state.board.fen
        transpositionTable.generation++
        state.moveScore.clear()
        var bestMove = emptyMove
        for (i in 1..min(MAX_DEPTH, state.params.depth)) {
            val score = search(state.board, -MAX_VALUE, MAX_VALUE, i, 0, state)
            if (state.shouldStop() && bestMove != emptyMove) break
            bestMove = state.pv[0]
            val nodes = state.nodes.get()
            val time = System.currentTimeMillis() - state.params.initialTime
            val nps = nodes / (max(time / 1000, 1))
            println("info depth $i score cp $score time $time nodes $nodes nps $nps pv ${state.pvLine()}")
        }
        println("bestmove $bestMove")
        if (state.board.fen != fen) {
            println("info string board state error: initial fen [$fen], final fen[${state.board.fen}]")
        }
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        return bestMove
    }

    private fun search(board: Board, alpha: Long, beta: Long, depth: Int, ply: Int, state: SearchState): Long {

        if (depth <= 0 || ply >= MAX_DEPTH) {
            return quiesce(board, alpha, beta, depth, ply, state)
        }
        state.nodes.incrementAndGet()
        if (state.shouldStop()) {
            return 0L
        }
        if (isRepetition(board)) {
            return 0L
        }

        var bestScore = -Long.MAX_VALUE
        var newAlpha = alpha
        var newBeta = beta
        var bestMove = emptyMove
        var hashMove = emptyMove

        val entry = transpositionTable.get(board.hashCode())
        if (entry != null && entry.depth >= depth && ply > 0) {
            hashMove = entry.move
            when (entry.nodeType) {
                TranspositionTable.NodeType.EXACT -> {
                    return entry.value
                }
                TranspositionTable.NodeType.LOWERBOUND -> {
                    newAlpha = max(alpha, entry.value)
                }
                TranspositionTable.NodeType.UPPERBOUND -> {
                    newBeta = min(beta, entry.value)
                }
            }
        }

        val isKingAttacked = board.isKingAttacked

        if (ply > 0 && depth > 1 && beta <= evaluator.evaluate(state, board) &&
                !isKingAttacked && isNullMoveAllowed(board)) {

            board.doNullMove()
            val score = -search(board, -newBeta, -newBeta + 1, depth - 3, ply + 1, state)
            board.undoMove()
            if (score >= newBeta) {
                transpositionTable.put(board.hashCode(), score, depth, bestMove,
                        TranspositionTable.NodeType.LOWERBOUND)
                return score
            }
        }

        val moves =
                if (ply == 0) orderRootMoves(state, MoveGenerator.generatePseudoLegalMoves(board))
                else orderMoves(state, hashMove, MoveGenerator.generatePseudoLegalMoves(board))


        for (move in moves) {

            if (!board.doMove(move)) {
                continue
            }
            val newDepth = if (isKingAttacked) depth else depth - 1

            var score: Long

            if (bestScore == -Long.MAX_VALUE) {
                score = -search(board, -newBeta, -newAlpha, newDepth, ply + 1, state)
            } else {
                score = -search(board, -newAlpha - 1, -newAlpha, newDepth, ply + 1, state)
                if (score > newAlpha && score < newBeta) {
                    score = -search(board, -newBeta, -newAlpha, newDepth, ply + 1, state)
                }
            }

            board.undoMove()
            if (ply == 0) {
                state.moveScore[move.toString()] = score
            }
            if (score >= newBeta) {
                transpositionTable.put(board.hashCode(), score, depth, move,
                        TranspositionTable.NodeType.LOWERBOUND)
                return score
            }
            if (score > bestScore) {
                bestScore = score
                if (score > newAlpha) {
                    bestMove = move
                    newAlpha = score
                    state.updatePv(move, ply)
                }
            }
        }

        val nodeType = when {
            bestScore <= alpha -> TranspositionTable.NodeType.UPPERBOUND
            else -> TranspositionTable.NodeType.EXACT
        }
        transpositionTable.put(board.hashCode(), bestScore, depth, bestMove, nodeType)

        if (bestScore == -Long.MAX_VALUE) {
            return if (isKingAttacked) -MATE_VALUE + ply else 0L
        }
        return bestScore
    }

    fun quiesce(board: Board, alpha: Long, beta: Long, depth: Int, ply: Int, state: SearchState): Long {

        state.nodes.incrementAndGet()

        if (state.shouldStop()) {
            return 0
        }
        if (isRepetition(board)) {
            return 0L
        }
        var newAlpha = alpha

        var bestScore = evaluator.evaluate(state, board)
        if (bestScore >= beta) {
            return beta
        }
        if (alpha < bestScore) {
            newAlpha = bestScore
        }

        val moves = orderMoves(state, emptyMove, MoveGenerator.generatePseudoLegalMoves(board))
        val isKingAttacked = board.isKingAttacked
        for (move in moves) {
            if (!isKingAttacked && board.getBitboard(board.sideToMove.flip()).and(move.to.bitboard) == 0L) {
                continue
            }
            if (!board.doMove(move)) {
                continue
            }
            val score = -quiesce(board, -beta, -newAlpha, depth - 1, ply + 1, state)
            board.undoMove()
            if (score >= beta) {
                return score
            }
            if (score > bestScore) {
                bestScore = score
                if (score > newAlpha) {
                    newAlpha = score
                    state.updatePv(move, ply)
                }
            }
        }

        if (bestScore == -Long.MAX_VALUE && isKingAttacked) {
            return -MATE_VALUE + ply
        }
        return bestScore
    }

    private fun orderMoves(state: SearchState, hashMove: Move, moves: MoveList): List<Move> {

        if (state.moveScore.size == 0) return moves
        return moves.sortedWith(Comparator { o1, o2 ->
            (moveScore(o1, hashMove, state) -
                    moveScore(o2, hashMove, state)).toInt()
        }).reversed()
    }

    private fun orderRootMoves(state: SearchState, moves: MoveList): List<Move> {

        if (state.moveScore.size == 0) return moves
        return moves.sortedWith(Comparator { o1, o2 ->
            (rootMoveScore(o1, state) -
                    rootMoveScore(o2, state)).toInt()
        }).reversed()
    }

    private fun rootMoveScore(move: Move, state: SearchState) = state.moveScore[move.toString()] ?: -Long.MAX_VALUE

    //mvv-lva
    private fun moveScore(move: Move, hashMove: Move, state: SearchState): Long {

        val attackedPiece = state.board.getPiece(move.to)
        val attackingPiece = state.board.getPiece(move.from)

        return when {
            move == hashMove -> MATE_VALUE
            attackedPiece != Piece.NONE -> evaluator.pieceStaticValue(attackedPiece) - evaluator.pieceStaticValue(attackingPiece)
            else -> 0L
        }
    }

    private fun isNullMoveAllowed(board: Board): Boolean =
            board.getBitboard(Side.WHITE).xor(board.getBitboard(Piece.WHITE_PAWN)
                    .or(board.getBitboard(Piece.WHITE_KING)))  == 0L ||
                    board.getBitboard(Side.BLACK).xor(board.getBitboard(Piece.BLACK_PAWN)
                            .or(board.getBitboard(Piece.BLACK_KING)))  == 0L

    private fun isRepetition(board: Board): Boolean {

        val i: Int = min(board.history.size - 1, board.halfMoveCounter)
        var rept = 0
        if (board.history.size >= 4) {
            val lastKey: Int = board.history[board.history.size - 1]
            var x = 2
            while (x <= i) {
                val k: Int = board.history[board.history.size - x - 1]
                if (k == lastKey) {
                    rept++
                    if (rept >= 2) {
                        return true
                    }
                }
                x += 2
            }
        }
        return false
    }

}