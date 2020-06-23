package com.github.bhlangonijr.pururucazero.abts

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.pururucazero.SearchEngine
import com.github.bhlangonijr.pururucazero.SearchState
import com.github.bhlangonijr.pururucazero.eval.Evaluator
import com.github.bhlangonijr.pururucazero.eval.MATE_VALUE
import com.github.bhlangonijr.pururucazero.eval.MAX_VALUE
import com.github.bhlangonijr.pururucazero.eval.MaterialEval
import kotlin.Comparator
import kotlin.math.max
import kotlin.math.min

const val MAX_DEPTH = 100

@ExperimentalStdlibApi
class Abts constructor(private var evaluator: Evaluator = MaterialEval(),
                       private var transpositionTable: TranspositionTable = TranspositionTable()) : SearchEngine {

    override fun rooSearch(state: SearchState): Move {
        val fen = state.board.fen

        transpositionTable.generation++
        state.moveScore.clear()
        var bestMove = Move(Square.NONE, Square.NONE)
        for (i in 1..min(MAX_DEPTH, state.params.depth)) {
            val score = search(state.board, -MAX_VALUE, MAX_VALUE, i, 0, state)
            if (state.shouldStop()) break
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

        if (state.shouldStop()) {
            return 0
        }
        state.nodes.incrementAndGet()

        if (depth <= 0) {
            return quiesce(board, alpha, beta, depth, ply, state)
        }

        var bestScore = -Long.MAX_VALUE
        var newAlpha = alpha
        var newBeta = beta
        var bestMove = Move(Square.NONE, Square.NONE)
        var hashMove = Move(Square.NONE, Square.NONE)

        val entry = transpositionTable.get(board.hashCode())
        if (entry != null && entry.depth >= depth) {
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

        val moves =
                if (ply == 0) orderRootMoves(state, MoveGenerator.generatePseudoLegalMoves(board))
                else orderMoves(state, hashMove, MoveGenerator.generatePseudoLegalMoves(board))

        val isKingAttacked = board.isKingAttacked
        for (move in moves) {

            val newDepth = if (isKingAttacked) depth else depth - 1
            if (!board.doMove(move, true)) {
                continue
            }
            val score = -search(board, -newBeta, -newAlpha, newDepth, ply + 1, state)
            board.undoMove()
            if (ply == 0) {
                state.moveScore[move.toString()] = score
            }
            if (score >= newBeta) {
                bestMove = move
                bestScore = score
                break
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
            bestScore >= beta -> TranspositionTable.NodeType.LOWERBOUND
            else -> TranspositionTable.NodeType.EXACT
        }

        transpositionTable.put(board.hashCode(), bestScore, depth, bestMove, nodeType)

        if (bestScore == -Long.MAX_VALUE) {
            return if (board.isKingAttacked) -MATE_VALUE + ply else 0L
        }
        return bestScore
    }

    private fun quiesce(board: Board, alpha: Long, beta: Long, depth: Int, ply: Int, state: SearchState): Long {

        if (state.shouldStop()) {
            return 0
        }
        state.nodes.incrementAndGet()

        var bestScore = -Long.MAX_VALUE
        var newAlpha = alpha
        var newBeta = beta
        var bestMove = Move(Square.NONE, Square.NONE)
        var hashMove = Move(Square.NONE, Square.NONE)

        val entry = transpositionTable.get(board.hashCode())
        if (entry != null && entry.depth >= depth) {
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
        val standPat = evaluator.evaluate(state, board)

        if (standPat >= newBeta) {
            return newBeta
        }

        if( alpha < standPat )
            newAlpha = standPat

        val moves = orderMoves(state, hashMove, MoveGenerator.generatePseudoLegalMoves(board))

        val isKingAttacked = board.isKingAttacked
        for (move in moves) {

            val newDepth = if (isKingAttacked) depth else depth - 1
            if (board.getPiece(move.to) == Piece.NONE || !board.doMove(move, true)) {
                continue
            }
            val score = -quiesce(board, -newBeta, -newAlpha, newDepth, ply + 1, state)
            board.undoMove()

            if (score >= newBeta) {
                bestScore = score
                bestMove = move
                break
            }
            if (score > bestScore) {
                bestScore = score
                if (score > newAlpha) {
                    newAlpha = score
                    bestMove = move
                    state.updatePv(move, ply)
                }
            }
        }

        val nodeType = when {
            bestScore <= alpha -> TranspositionTable.NodeType.UPPERBOUND
            bestScore >= beta -> TranspositionTable.NodeType.LOWERBOUND
            else -> TranspositionTable.NodeType.EXACT
        }

        transpositionTable.put(board.hashCode(), bestScore, depth, bestMove, nodeType)

        if (bestScore == -Long.MAX_VALUE) {
            return if (board.isKingAttacked) -MATE_VALUE + ply else 0L
        }
        return bestScore
    }

    private fun orderMoves(state: SearchState, hashMove: Move, moves: MoveList): List<Move> {

        if (state.moveScore.size == 0) return moves
        return moves.sortedWith(Comparator { o1, o2 -> (moveScore(o1, hashMove, state) -
                moveScore(o2, hashMove, state)).toInt() }).reversed()
    }

    private fun orderRootMoves(state: SearchState, moves: MoveList): List<Move> {

        if (state.moveScore.size == 0) return moves
        return moves.sortedWith(Comparator { o1, o2 -> (rootMoveScore(o1, state) -
                rootMoveScore(o2, state)).toInt() }).reversed()
    }

    private fun rootMoveScore(move: Move, state: SearchState) = state.moveScore[move.toString()] ?: -Long.MAX_VALUE

    //mvv-lva
    private fun moveScore(move: Move, hashMove: Move, state: SearchState) : Long {

        val attackedPiece = state.board.getPiece(move.to)
        val attackingPiece = state.board.getPiece(move.from)

        return when {
            move == hashMove -> MATE_VALUE
            attackedPiece != Piece.NONE -> evaluator.pieceStaticValue(attackedPiece) - evaluator.pieceStaticValue(attackingPiece)
            else -> 0L
        }
    }

}