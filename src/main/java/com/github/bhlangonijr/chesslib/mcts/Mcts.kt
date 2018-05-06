package com.github.bhlangonijr.chesslib.mcts

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.eval.scoreMaterial
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList
import java.util.*
import java.util.stream.Collectors

val random = Random()
const val EPSILON = 0.003

class Mcts : SearchEngine {

    override fun rooSearch(state: SearchState): Move {

        val node = Node(Move(Square.NONE, Square.NONE), state.board.sideToMove)
        var bestMove: Move? = Move(Square.NONE, Square.NONE)

        var count = 0
        while (!state.shouldStop()) {
            state.nodes.incrementAndGet()
            bestMove = node.selectMove(state)
            if ((count++ % 1000L) == 0L) println("info string total nodes ${state.nodes.get()}")
        }
        node.children!!.forEach { println(it) }
        println("bestmove $bestMove")
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        println("info string total nodes ${state.nodes.get()}")
        return bestMove!!
    }

    class Node(val move: Move, val side: Side) {

        var hits: Long = 0
        var score: Double = 0.0
        var children: List<Node>? = null

        private fun pickBest(): Node {

            var selected: Node = children!![0]
            for (node in children!!) {
                if (node.score > selected.score) {
                    selected = node
                }
            }
            return selected
        }

        private fun expand(board: Board) {

            children = MoveGenerator
                    .generateLegalMoves(board)
                    .stream()
                    .map { Node(it, board.sideToMove) }
                    .collect(Collectors.toList())
        }

        private fun select(): Node {

            var selected: Node? = null
            var best = Double.NEGATIVE_INFINITY
            for (node in children!!) {
                val score = score / (node.hits + EPSILON) +
                        Math.sqrt(Math.log((hits + 1).toDouble()) / (node.hits + EPSILON)) +
                        random.nextDouble() * EPSILON
                if (score > best) {
                    selected = node
                    best = score
                }
            }
            return selected!!
        }

        private fun updateStats(score: Double) {
            this.hits++
            this.score += score
        }

        fun selectMove(state: SearchState): Move {

            val visited = LinkedList<Node>()
            var node = this

            while (!node.isLeaf()) {
                node = node.select()
                state.board.doMove(node.move)
                state.nodes.incrementAndGet()
                visited += node
            }
            node.expand(state.board)
            if (!node.isLeaf()) {
                node = node.select()
                state.board.doMove(node.move)
                state.nodes.incrementAndGet()
                visited += node
            }
            val value = if (state.board.sideToMove == side) playOut(state, 40) else -playOut(state, 40)

            for (i in 0 until visited.size) {
                val n = visited[i]
                n.updateStats(value)
                state.board.undoMove()
            }

            return pickBest().move
        }

        private fun isLeaf() = children == null || children?.size == 0

        override fun toString(): String {
            return "Node(move=$move, hits=$hits, score=$score)"
        }
    }

}

fun playOut(state: SearchState, depth: Int): Double {

    return try {
        val moves = MoveGenerator.generateLegalMoves(state.board)
        val isKingAttacked = state.board.isKingAttacked
        when {
            //depth == 0 -> resolvePosition(state, moves)
            moves.size == 0 && isKingAttacked -> -1.0
            moves.size == 0 && !isKingAttacked -> 0.0
            state.board.isDraw -> 0.0
            else -> {
                val move = moves[random.nextInt(moves.size)]
                state.board.doMove(move)
                state.nodes.incrementAndGet()
                val playOutScore = playOut(state, depth - 1)
                state.board.undoMove()
                return playOutScore
            }
        }
    } catch (e: Exception) {
        println("Error $e")
        e.printStackTrace()
        println(state.board.fen)
        println(state.board)
        0.0
    }

}

private fun winProbability(score: Double) = 2.0/(1.0 + Math.exp(-10.0 * (score / 3000))) - 1.0

private fun resolvePosition(state: SearchState, moves: MoveList): Double {

    val score = scoreMaterial(state.board)
    //if (Math.abs(score) > 600) println("score = $score   = ${winProbability(score.toDouble())}")
    return when {
    // heuristic cut-off if side is up/down 500 points
        Math.abs(score) >= 0 -> winProbability(score.toDouble())
        else -> {
            val move = moves[random.nextInt(moves.size)]
            state.board.doMove(move)
            state.nodes.incrementAndGet()
            val playOutScore = playOut(state, 100)
            state.board.undoMove()
            return playOutScore
        }
    }
}

//        private fun selectMove(state: SearchState, moves: MoveList): Move {
//            var score = 1.0
//            for (move in moves) {
//                state.board.isAttackedBy(move)
//
//            }
//        }
