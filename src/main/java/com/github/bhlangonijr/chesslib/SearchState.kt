package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move
import java.util.concurrent.atomic.AtomicLong

class SearchState(val params: SearchParams, val board: Board) {

    val MAX_DEPTH = 100

    @Volatile
    var stopped = false
    val nodes = AtomicLong()

    val pv = Array(MAX_DEPTH, { Move(Square.NONE, Square.NONE) })

    fun shouldStop(): Boolean {

        if (stopped || nodes.get() >= params.nodes) {
            return true
        }
        val elapsed = System.currentTimeMillis() - params.initialTime
        return elapsed >= timeLeft(params)
    }

    private fun timeLeft(params: SearchParams): Long {

        return when (board.sideToMove) {
            Side.WHITE -> params.whiteTime / 40 + params.whiteIncrement
            else -> params.blackTime / 40 + params.blackIncrement
        }
    }

}