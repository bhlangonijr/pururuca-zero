package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.Eval.Companion.MATE_VALUE
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator


class Abts : SearchEngine {

    override fun rooSearch(state: SearchState): Move {
        val fen = state.board.fen
        val score = search(state.board, Int.MIN_VALUE, Int.MAX_VALUE, state.params.depth, 0, state)
        if (state.board.fen != fen) {
            println("info string board state error: initial fen [$fen], final fen[${state.board.fen}]")
        }
        println("bestmove ${state.pv[0]}")
        println("info string eval $score moves ${state.pvLine()}")
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        return state.pv[0]
    }

    private fun search(board: Board, alpha: Int, beta: Int, depth: Int, ply: Int, state: SearchState): Int {

        if (depth <= 0 || state.shouldStop()) {
            return scoreMaterial(board)
        }

        var bestScore = Int.MIN_VALUE
        var newAlpha = alpha
        val moves = MoveGenerator.generatePseudoLegalMoves(board)
        for (move in moves) {
            if (!board.doMove(move)) {
                continue
            }
            val score = -search(board, -beta, -newAlpha, depth - 1, ply + 1, state)
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
            if (ply == 0) println("info string score $move = $score")
        }

        if (bestScore == Int.MIN_VALUE) {
            return if (board.isKingAttacked) -MATE_VALUE + ply else 0
        }
        return bestScore
    }
}