package com.github.bhlangonijr.chesslib.mcts

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors

val random = Random()
const val DEFAULT_EPSILON = 1.47
const val NUM_THREADS = 4

class Mcts(private val epsilon: Double = DEFAULT_EPSILON) : SearchEngine {

    override fun rooSearch(state: SearchState): Move {

        val executor = Array(NUM_THREADS) { Executors.newFixedThreadPool(NUM_THREADS) }
        val node = Node(Move(Square.NONE, Square.NONE), state.board.sideToMove)
        val boards = Array(NUM_THREADS) { state.board.clone() }
        val simulations = AtomicLong(0)

        for (i in 1 until NUM_THREADS) {
            executor[i].submit {
                while (!state.shouldStop()) {
                    val score = searchMove(node, state, boards[i], boards[i].sideToMove, 0)
                    node.updateStats(score)
                    simulations.incrementAndGet()
                }
            }
        }

        var timestamp = System.currentTimeMillis()
        while (!state.shouldStop()) {
            val score = searchMove(node, state, boards[0], boards[0].sideToMove, 0)
            node.updateStats(score)
            simulations.incrementAndGet()
            if ((System.currentTimeMillis() - timestamp) > 5000L) {
                println("info string total nodes ${state.nodes.get()}")
                timestamp = System.currentTimeMillis()
            }
        }

        node.children!!.forEach { println(it) }
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
            moves.size == 0 && isKingAttacked -> -1
            moves.size == 0 && !isKingAttacked -> 0
            node.isLeaf() -> {
                if (ply == 0 && state.params.searchMoves.isNotBlank()) {
                    val searchMoves = MoveList()
                    searchMoves.addAll(moves
                            .stream()
                            .filter { state.params.searchMoves.contains(it.toString()) }
                            .collect(Collectors.toList()))
                    node.expand(searchMoves, board.sideToMove)
                } else {
                    node.expand(moves, board.sideToMove)
                }
                val childNode = node.select(epsilon, board, player)
                board.doMove(childNode.move)
                val score = -playOut(state, board, ply + 1, player, childNode.move)
                childNode.updateStats(score)
                board.undoMove()
                score
            }
            else -> {
                val childNode = node.select(epsilon, board, player)
                board.doMove(childNode.move)
                val score = -searchMove(childNode, state, board, player, ply + 1)
                childNode.updateStats(score)
                board.undoMove()
                score
            }
        }
    }
}

fun playOut(state: SearchState, board: Board, ply: Int, player: Side, lastMove: Move): Long {

    var m: Move? = null
    return try {

        val moves = MoveGenerator.generateLegalMoves(board)
        val isKingAttacked = board.isKingAttacked
        when {
            moves.size == 0 && isKingAttacked -> -1
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
                m = move
                board.doMove(move)
                state.nodes.incrementAndGet()
                val playOutScore = -playOut(state, board, ply + 1, player, move)
                board.undoMove()
                return playOutScore
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message} - $m")
        println("FEN error pos: ${board.fen}")
        println(board)
        e.printStackTrace()
        0
    }

}

private fun selectMove(state: SearchState, moves: MoveList): Move {
    return moves[random.nextInt(moves.size)]
}
