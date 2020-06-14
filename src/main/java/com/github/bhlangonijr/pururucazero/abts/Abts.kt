package com.github.bhlangonijr.pururucazero.abts

import com.github.bhlangonijr.chesslib.Board
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

class Abts constructor(var evaluator: Evaluator = MaterialEval()) : SearchEngine {

    override fun rooSearch(state: SearchState): Move {
        val fen = state.board.fen

        state.moveScore.clear()
        for (i in 1..min(MAX_DEPTH, state.params.depth)) {
            val score = search(state.board, -MAX_VALUE, MAX_VALUE, i, 0, state)
            println("info string eval $score moves ${state.pvLine()} depth $i")
            if (state.shouldStop()) break
        }
        println("bestmove ${state.pv[0]}")
        if (state.board.fen != fen) {
            println("info string board state error: initial fen [$fen], final fen[${state.board.fen}]")
        }
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        return state.pv[0]
    }

    private fun search(board: Board, alpha: Long, beta: Long, depth: Int, ply: Int, state: SearchState): Long {

        if (depth <= 0 || state.shouldStop()) {
            return evaluator.evaluate(state, board)
        }

        var bestScore = -Long.MAX_VALUE
        var newAlpha = alpha
        val moves =
                if (ply == 0) orderMoves(state, MoveGenerator.generatePseudoLegalMoves(board))
                else MoveGenerator.generatePseudoLegalMoves(board)

        val isKingAttacked = board.isKingAttacked
        for (move in moves) {

            val newDepth = if (isKingAttacked) depth else depth - 1
            if (!board.doMove(move)) {
                continue
            }
            val score = -search(board, -beta, -newAlpha, newDepth, ply + 1, state)
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
            if (ply == 0) {
                state.moveScore[move] = score
            }
        }

        if (bestScore == -Long.MAX_VALUE) {
            return if (board.isKingAttacked) -MATE_VALUE + ply else 0L
        }
        return bestScore
    }

    private fun orderMoves(state: SearchState, moves: MoveList): MoveList {

        if (state.moveScore.size == 0) return moves
        val sorted = MoveList()
        sorted.addAll(moves.sortedWith(Comparator { o1, o2 -> (moveScore(o1, state) - moveScore(o2, state)).toInt() }).reversed())
        return sorted
    }

    private fun moveScore(move: Move, state: SearchState) = state.moveScore[move] ?: 0L

}