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
const val NUM_THREADS = 2

class Mcts : SearchEngine {

    val executor = Executors.newFixedThreadPool(NUM_THREADS)
    override fun rooSearch(state: SearchState): Move {

        val node = Node(Move(Square.NONE, Square.NONE), state.board.sideToMove)
        var bestMove: Move? = Move(Square.NONE, Square.NONE)

        val boards = Array(NUM_THREADS, {state.board.clone()})

        for (i in 1 until NUM_THREADS) {
            executor.submit({
                println("Submitting thread $i")
                while (!state.shouldStop()) {
                    state.nodes.incrementAndGet()
                    node.selectMove(state, boards[i])
                }
            })
        }
        var counter = 0L
        while (!state.shouldStop()) {
            state.nodes.incrementAndGet()
            bestMove = node.selectMove(state, boards[0])
            if ((state.nodes.get() - counter) > 50000L) {
                println("info string total nodes ${state.nodes.get()}")
                counter = state.nodes.get()
            }
        }

        node.children!!.forEach { println(it) }
        println("bestmove $bestMove")
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        println("info string total nodes ${state.nodes.get()}")
        return bestMove!!
    }

    class Node(val move: Move, val side: Side) {

        val hits = AtomicLong(0)
        val wins = AtomicLong(0)
        val losses = AtomicLong(0)
        val score = AtomicLong(0)
        var children: List<Node>? = null

        private fun pickBest(): Node {

            var selected: Node = children!![0]
            for (node in children!!) {
                val nodeScore = node.score.get() / (node.hits.get() + 1.0)
                val currentScore = selected.score.get() / (selected.hits.get() + 1.0)
                if (nodeScore > currentScore) {
                    selected = node
                }
            }
            return selected
        }

        private fun expand(board: Board) : List<Node>? {

            if (children == null) {
                children = MoveGenerator
                        .generateLegalMoves(board)
                        .stream()
                        .map { Node(it, board.sideToMove) }
                        .collect(Collectors.toList())
            }
            return children
        }

        private fun select(): Node {

            var selected: Node? = null
            var best = Double.NEGATIVE_INFINITY
            for (node in children!!) {
                val score = node.wins.get() / (node.hits.get() + EPSILON) +
                        Math.sqrt(Math.log((hits.get() + 1.0)) / (node.hits.get() + EPSILON)) +
                        random.nextDouble() * EPSILON
                if (score > best) {
                    selected = node
                    best = score
                }
            }
            return selected!!
        }

        private fun updateStats(score: Long) {
            this.hits.incrementAndGet()
            if (score > 0.0) this.wins.incrementAndGet()
            if (score < 0.0) this.losses.incrementAndGet()
            this.score.accumulateAndGet(score, {left, right ->  left + right})
        }

        fun selectMove(state: SearchState, board: Board): Move {

            val visited = LinkedList<Node>()
            var node = this

            while (!node.isLeaf()) {
                node = node.select()
                board.doMove(node.move)
                state.nodes.incrementAndGet()
                visited += node
            }
            if (!board.isDraw && !board.isMated) {
                synchronized(this as Any, { node.expand(board) })
            }
            if (!node.isLeaf()) {
                node = node.select()
                board.doMove(node.move)
                state.nodes.incrementAndGet()
                visited += node
            }

            val value = playOut(state, board, 0, side)

            for (i in 0 until visited.size) {
                val n = visited[i]
                n.updateStats(value)
                board.undoMove()
            }

            return pickBest().move
        }

        private fun isLeaf() = children == null || children?.size == 0

        override fun toString(): String {
            return "Node(move=$move, side=$side, hits=$hits, wins=$wins, losses=$losses, score=$score"
        }
    }

}

fun playOut(state: SearchState, board: Board, ply: Int, side: Side): Long {

    return try {
        val moves = MoveGenerator.generateLegalMoves(board)
        val isKingAttacked = board.isKingAttacked
        //val score = scoreMaterial(state.board)
        when {
            moves.size == 0 && isKingAttacked -> if (board.sideToMove == side) -1 else 1
            moves.size == 0 && !isKingAttacked -> 0
            board.isDraw -> 0
            //ply > 10 && !isKingAttacked && Math.abs(score) >= 900 -> winProbability(side, score.toDouble())
            else -> {
                val move = selectMove(state, moves)
                board.doMove(move)
                state.nodes.incrementAndGet()
                val playOutScore = playOut(state, board,ply + 1, side)
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

private fun winProbability(score: Double) = if ((2.0/(1.0 + Math.exp(-10.0 * (score / 3000))) - 1.0) > 0.5) 1L else -1L
//
//private fun resolvePosition(state: SearchState, moves: MoveList, isCheck: Boolean, depth: Int, side: Side): Double {
//
//    val score = scoreMaterial(state.board)
//    //if (Math.abs(score) > 600) println("score = $score   = ${winProbability(score.toDouble())}")
//    return when {
//    // heuristic cut-off if side is up/down 900 points
//        depth <= 0 && !isCheck && Math.abs(score) >= 900 -> winProbability(score.toDouble())
//        else -> {
//            val move = selectMove(state, moves)
//            state.board.doMove(move)
//            state.nodes.incrementAndGet()
//            val playOutScore = -playOut(state, depth - 1, side)
//            state.board.undoMove()
//            return playOutScore
//        }
//    }
//}

private fun selectMove(state: SearchState, moves: MoveList): Move {
    return  moves[random.nextInt(moves.size)]
}
