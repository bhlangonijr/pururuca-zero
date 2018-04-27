package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.MoveList
import java.util.concurrent.Executors

class Search {

    private val executor = Executors.newSingleThreadExecutor()
    private val board = Board()
    private var stopped = true

    fun reset() {

        board.loadFromFen(board.context.startFEN)
    }

    fun setupPosition(fen: String, moves: String) {

        if (moves.isNotBlank()) {
            val moveList = MoveList(fen)
            moveList.loadFromText(moves)
            board.loadFromFen(moveList.getFen(moveList.size))
        } else {
            board.loadFromFen(fen)
        }
    }

    fun setupPosition(moves: String) {

        if (moves.isNotBlank()) {
            val moveList = MoveList()
            moveList.loadFromText(moves)
            board.loadFromFen(moveList.getFen(moveList.size))
        } else {
            reset()
        }
    }

    fun start(params: SearchParams): Boolean {

        stopped = false
        executor.submit({ Mcts(params, board, this).rooSearch() })
        return true
    }

    fun stop(): Boolean {

        stopped = true
        return true
    }

    fun shouldStop(params: SearchParams, nodes: Long, initialTime: Long): Boolean {

        if (stopped || nodes >= params.nodes) {
            return true
        }
        val elapsed = System.currentTimeMillis() - initialTime
        return elapsed >= timeLeft(params)
    }

    private fun timeLeft(params: SearchParams): Long {

        return when (board.sideToMove) {
            Side.WHITE -> params.whiteTime / 40 + params.whiteIncrement
            else -> params.blackTime / 40 + params.blackIncrement
        }
    }
}


