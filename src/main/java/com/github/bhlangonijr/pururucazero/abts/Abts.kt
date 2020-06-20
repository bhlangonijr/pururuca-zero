package com.github.bhlangonijr.pururucazero.abts

import com.github.bhlangonijr.chesslib.Board
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
import kotlin.math.min

const val MAX_DEPTH = 100

class Abts constructor(private var evaluator: Evaluator = MaterialEval()) : SearchEngine {

    override fun rooSearch(state: SearchState): Move {
        val fen = state.board.fen

        state.moveScore.clear()
        var bestMove = Move(Square.NONE, Square.NONE)
        for (i in 1..min(MAX_DEPTH, state.params.depth)) {
            val score = search(state.board, -MAX_VALUE, MAX_VALUE, i, 0, state)
            if (state.shouldStop()) break
            bestMove = state.pv[0]
            val nodes = state.nodes.get()
            val time = System.currentTimeMillis() - state.params.initialTime
            println("info depth $i score cp $score time $time nodes $nodes pv ${state.pvLine()}")
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
            return evaluator.evaluate(state, board)
        }

        var bestScore = -Long.MAX_VALUE
        var newAlpha = alpha
        val moves =
                if (ply == 0) orderRootMoves(state, MoveGenerator.generatePseudoLegalMoves(board))
                else MoveGenerator.generatePseudoLegalMoves(board)

        val isKingAttacked = board.isKingAttacked
        for (move in moves) {

            val newDepth = if (isKingAttacked) depth else depth - 1
            if (!board.doMove(move, true)) {
                continue
            }
            val score = -search(board, -beta, -newAlpha, newDepth, ply + 1, state)
            board.undoMove()
            if (ply == 0) {
                state.moveScore[move.toString()] = score
            }
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

        if (bestScore == -Long.MAX_VALUE) {
            return if (board.isKingAttacked) -MATE_VALUE + ply else 0L
        }
        return bestScore
    }

    private fun orderRootMoves(state: SearchState, moves: MoveList): List<Move> {

        if (state.moveScore.size == 0) return moves
        return moves.sortedWith(Comparator { o1, o2 -> (moveScore(o1, state) - moveScore(o2, state)).toInt() }).reversed()
    }

    private fun moveScore(move: Move, state: SearchState) = state.moveScore[move.toString()] ?: -Long.MAX_VALUE

}