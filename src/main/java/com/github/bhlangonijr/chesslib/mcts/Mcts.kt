package com.github.bhlangonijr.chesslib.mcts

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList
import java.util.*
import java.util.concurrent.Executors

val random = Random()
val EPSILON = Math.sqrt(2.0)
const val NUM_THREADS = 1

class Mcts : SearchEngine {

    private val executor = Executors.newFixedThreadPool(NUM_THREADS)

    override fun rooSearch(state: SearchState): Move {

        val node = Node(Move(Square.NONE, Square.NONE), state.board.sideToMove)
        val boards = Array(NUM_THREADS, {state.board.clone()})

        for (i in 1 until NUM_THREADS) {
            executor.submit({
                while (!state.shouldStop()) {
                    searchMove(node, state, boards[i], boards[i].sideToMove, state.params.depth)
                }
            })
        }
        var counter = 0L
        while (!state.shouldStop()) {
            searchMove(node, state, boards[0], boards[0].sideToMove, state.params.depth)
            if ((state.nodes.get() - counter) > 50000L) {
                println("info string total nodes ${state.nodes.get()}")
                counter = state.nodes.get()
            }
        }

        node.children!!.forEach { println(it) }
        println("bestmove ${node.pickBest().move}")
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        println("info string total nodes ${state.nodes.get()}")
        return node.pickBest().move
    }

    private fun searchMove(node: Node, state: SearchState, board: Board, player: Side, ply: Int): Long {

        state.nodes.incrementAndGet()

        return when {
            board.isMated -> if (board.sideToMove == player) -1 else 1
            board.isDraw -> 0
            node.isLeaf() -> {
                synchronized(node as Any, { node.expand(board) })
                val childNode = node.select()
                board.doMove(childNode.move)
                val score = playOut(state, board, ply + 1, player)
                childNode.updateStats(score)
                board.undoMove()
                score
            }
            else -> {
                val childNode = node.select()
                board.doMove(childNode.move)
                val score = searchMove(childNode, state, board, player, ply + 1)
                childNode.updateStats(score)
                board.undoMove()
                score
            }
        }
    }
}

fun playOut(state: SearchState, board: Board, ply: Int, player: Side): Long {

    return try {
        val moves = MoveGenerator.generateLegalMoves(board)
        val isKingAttacked = board.isKingAttacked
        when {
            moves.size == 0 && isKingAttacked -> if (board.sideToMove == player) -1 else 1
            moves.size == 0 && !isKingAttacked -> 0
            board.isDraw -> 0
            else -> {
                val move = selectMove(state, moves)
                board.doMove(move)
                state.nodes.incrementAndGet()
                val playOutScore = playOut(state, board,ply + 1, player)
                board.undoMove()
                return playOutScore
            }
        }
    } catch (e: Exception) {
        println("Error ${e.message}")
        println(board.fen)
        println(board)
        e.printStackTrace()
        0
    }

}

//private fun winProbability(score: Double) = if ((2.0/(1.0 + Math.exp(-10.0 * (score / 3000))) - 1.0) > 0.5) 1L else -1L

private fun selectMove(state: SearchState, moves: MoveList): Move {
    return  moves[random.nextInt(moves.size)]
}
