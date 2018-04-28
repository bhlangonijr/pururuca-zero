package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator


class Abts constructor(private val params: SearchParams,
                       private val board: Board,
                       private val search: Search) {

    val MAX_DEPTH = 100

    fun rooSearch() {

        val pv = Array(MAX_DEPTH, { Move(Square.NONE, Square.NONE)})
        search(board, Int.MIN_VALUE, Int.MAX_VALUE, params.depth, 0, pv)

        println("bestmove ${pv[0]}")
        println("info string eval ${pv[0]}")
        println("info string total time ${System.currentTimeMillis() - params.initialTime}")
    }

    private fun search(board: Board, alpha: Int, beta: Int, depth: Int, ply: Int, pv: Array<Move>): Int {

        if (depth == 0 || search.shouldStop(params, 0)) {
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
            val score = -search(board, -beta, -newAlpha, depth - 1, ply + 1, pv)
            board.undoMove()
            if (score >= beta) {
                bestScore = score
                pv[ply] = move
                break
            }
            if (score > bestScore) {
                bestScore = score
                pv[ply] = move
                if (score > newAlpha) {
                    newAlpha = score
                }
            }
        }

        return bestScore
    }
}