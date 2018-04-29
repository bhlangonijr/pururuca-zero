package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator


class Abts : SearchEngine {

    override fun rooSearch(state: SearchState): Move {

        search(state.board, Int.MIN_VALUE, Int.MAX_VALUE, state.params.depth, 0, state)

        println("bestmove ${state.pv[0]}")
        println("info string eval ${state.pv[0]}")
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        return state.pv[0]
    }

    private fun search(board: Board, alpha: Int, beta: Int, depth: Int, ply: Int, state: SearchState): Int {

        if (depth == 0 || state.shouldStop()) {
            return scoreMaterial(board)
        }

        if (board.isDraw) {
            return 0
        }

        if (board.isMated) {
            return -Eval.KING_VALUE
        }

        val moves = MoveGenerator.generatePseudoLegalMoves(board)
        var bestScore = Int.MIN_VALUE
        var newAlpha = alpha
        for (move in moves) {
            if (!board.doMove(move, true)) {
                continue
            }
            val score = -search(board, -beta, -newAlpha, depth - 1, ply + 1, state)
            board.undoMove()
            if (score >= beta) {
                bestScore = score
                state.pv[ply] = move
                break
            }
            if (score > bestScore) {
                bestScore = score
                state.pv[ply] = move
                if (score > newAlpha) {
                    newAlpha = score
                }
            }
        }

        return bestScore
    }
}