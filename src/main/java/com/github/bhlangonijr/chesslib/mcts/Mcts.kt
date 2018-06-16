package com.github.bhlangonijr.chesslib.mcts

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList
import com.sun.tools.internal.xjc.reader.gbind.Expression.EPSILON
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors

val random = Random()
const val DEFAULT_EPSILON = 1.45
const val DEFAULT_COOLING_FACTOR = 0.99997
const val NUM_THREADS = 4

class Mcts(private val epsilon: Double = DEFAULT_EPSILON,
           private val coolingFactor: Double = DEFAULT_COOLING_FACTOR) : SearchEngine {



    private val executor: ExecutorService = Executors.newFixedThreadPool(NUM_THREADS)

    override fun rooSearch(state: SearchState): Move {

        val node = Node(Move(Square.NONE, Square.NONE), state.board.sideToMove)
        val boards = Array(NUM_THREADS, { state.board.clone() })
        val simulations = AtomicLong(0)

        for (i in 1 until NUM_THREADS) {
            executor.submit({
                var explorationFactor: Double = epsilon
                while (!state.shouldStop()) {
                    explorationFactor *= coolingFactor
                    searchMove(node, state, boards[i], boards[i].sideToMove, 0, epsilon)
                    simulations.incrementAndGet()
                }
            })
        }
        var timestamp = System.currentTimeMillis()
        var explorationFactor: Double = epsilon
        while (!state.shouldStop()) {
            explorationFactor *= coolingFactor
            searchMove(node, state, boards[0], boards[0].sideToMove, 0, epsilon)
            simulations.incrementAndGet()
            if ((System.currentTimeMillis() - timestamp) > 5000L) {
                println("info string total nodes ${state.nodes.get()} - exploration: $explorationFactor")
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

    private fun searchMove(node: Node, state: SearchState, board: Board, player: Side, ply: Int, explorationFactor: Double): Long {

        state.nodes.incrementAndGet()
        val moves = MoveGenerator.generateLegalMoves(board)
        val isKingAttacked = board.isKingAttacked
        val explorationUpdate = exploration(explorationFactor)
        return when {
            moves.size == 0 && isKingAttacked -> if (board.sideToMove == player) -1 else 1
            moves.size == 0 && !isKingAttacked -> 0
            node.isLeaf() -> {
                if (ply == 0 && state.params.searchMoves.isNotBlank()) {
                    val searchMoves = MoveList()
                    searchMoves.addAll(moves
                            .stream()
                            .filter({ state.params.searchMoves.contains(it.toString()) })
                            .collect(Collectors.toList()))
                    node.expand(searchMoves, board.sideToMove)
                } else {
                    node.expand(moves, board.sideToMove)
                }
                val childNode = node.select(explorationUpdate)
                board.doMove(childNode.move)
                val score = playOut(state, board, ply + 1, player, childNode.move)
                childNode.updateStats(score)
                board.undoMove()
                score
            }
            else -> {
                val childNode = node.select(explorationUpdate)
                board.doMove(childNode.move)
                val score = searchMove(childNode, state, board, player, ply + 1, explorationUpdate)
                childNode.updateStats(score)
                board.undoMove()
                score
            }
        }
    }
    private fun exploration(explorationFactor: Double) = explorationFactor * coolingFactor
}

fun playOut(state: SearchState, board: Board, ply: Int, player: Side, lastMove: Move): Long {

    return try {

        val moves = MoveGenerator.generateLegalMoves(board)
        val isKingAttacked = board.isKingAttacked
        when {
            moves.size == 0 && isKingAttacked -> if (board.sideToMove == player) -1 else 1
            moves.size == 0 && !isKingAttacked -> 0
            board.isDraw -> 0
            else -> {
                val move = selectMove(state, moves)
                val kq = board.getKingSquare(board.sideToMove.flip())
                if (kq == move.to) {
                    println("FEN: ${board.fen}")
                    println("move: $move")
                    println("last lastMove: $lastMove")
                }
                board.doMove(move)
                state.nodes.incrementAndGet()
                val playOutScore = playOut(state, board, ply + 1, player, move)
                board.undoMove()
                return playOutScore
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        println("FEN error pos: ${board.fen}")
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
