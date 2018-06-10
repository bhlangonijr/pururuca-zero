package com.github.bhlangonijr.chesslib.mcts

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.eval.scoreMaterial
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors

val random = Random()
val EPSILON = Math.sqrt(2.0)
const val NUM_THREADS = 4

class Mcts : SearchEngine {

    private val executor = Executors.newFixedThreadPool(NUM_THREADS)

    override fun rooSearch(state: SearchState): Move {

        val node = Node(Move(Square.NONE, Square.NONE), state.board.sideToMove)
        val boards = Array(NUM_THREADS, { state.board.clone() })
        val simulations = AtomicLong(0)
        for (i in 1 until NUM_THREADS) {
            executor.submit({
                while (!state.shouldStop()) {
                    searchMove(node, state, boards[i], boards[i].sideToMove, 0)
                    simulations.incrementAndGet()
                }
            })
        }
        var timestamp = System.currentTimeMillis()
        while (!state.shouldStop()) {
            searchMove(node, state, boards[0], boards[0].sideToMove, 0)
            simulations.incrementAndGet()
            if ((System.currentTimeMillis() - timestamp) > 5000L) {
                println("info string total nodes ${state.nodes.get()}")
                timestamp = System.currentTimeMillis()
            }
        }

        node.children!!.forEach {
            println(it)
        }
        println("bestmove ${node.pickBest().move}")
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        println("info string total nodes [${state.nodes.get()}], simulations[${simulations.get()}]")
        return node.pickBest().move
    }

    private fun searchMove(node: Node, state: SearchState, board: Board, player: Side, ply: Int): Long {

        state.nodes.incrementAndGet()
        val moves = MoveGenerator.generateLegalMoves(board)
        val isKingAttacked = board.isKingAttacked

        return when {
            moves.size == 0 && isKingAttacked -> if (board.sideToMove == player) -1 else 1
            moves.size == 0 && !isKingAttacked -> 0
            node.isLeaf() && ply > 4 && !isKingAttacked -> {
                val score = scoreMaterial(board)
                if (board.sideToMove == player) winProbability(score.toDouble()) else -winProbability(score.toDouble())
            }
            node.isLeaf() -> {
                if (ply == 0 && state.params.searchMoves.isNotBlank()) {
                    val searchMoves = MoveList()
                    searchMoves.addAll(moves
                            .stream()
                            .filter({state.params.searchMoves.contains(it.toString())})
                            .collect(Collectors.toList()))
                    node.expand(searchMoves, board.sideToMove)
                } else {
                    node.expand(moves, board.sideToMove)
                }
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
                val playOutScore = playOut(state, board, ply + 1, player)
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

private fun winProbability(score: Double): Long {

    val r = (2.0 / (1.0 + Math.exp(-10.0 * (score / 3000))) - 1.0)
    return when {
        r >= 0.3 -> 1L
        r <= -0.3 -> -1L
        else -> 0L
    }
}

private fun selectMove(state: SearchState, moves: MoveList): Move {
    return moves[random.nextInt(moves.size)]
}
