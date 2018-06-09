package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.abts.MAX_DEPTH
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import java.util.concurrent.atomic.AtomicLong

class SearchState(val params: SearchParams, val board: Board) {


    private val noMove = Move(Square.NONE, Square.NONE)

    @Volatile
    var stopped = false
    val nodes = AtomicLong()
    var pvPly = 0
    var moveScore = HashMap<Move, Int>()

    val pv = Array(MAX_DEPTH, { noMove })

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

    fun updatePv(move: Move, ply: Int) {

        if (pvPly > ply) pv.fill(noMove, ply, pvPly)
        pvPly = ply
        pv[ply] = move
    }

    fun pvLine(): MoveList {

        val moves = MoveList(board.fen)
        moves += pv.takeWhile { !(it.from == Square.NONE || it.to == Square.NONE) }
        return moves
    }

}