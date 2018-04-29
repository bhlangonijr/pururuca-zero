package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.Eval.Companion.KING_VALUE
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import java.util.*
import java.util.stream.Collectors

class Mcts : SearchEngine {

    override fun rooSearch(state: SearchState): Move {

        val node = Node(Move(Square.NONE, Square.NONE))
        var bestMove: Move? = Move(Square.NONE, Square.NONE)
        var nodes = 0L
        while (!state.shouldStop()) {
            nodes++
            bestMove = node.selectMove(state.board)
        }
        node.children!!.forEach { println(it) }
        println("bestmove $bestMove")
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        return bestMove!!
    }

    class Node(val move: Move) {

        companion object {
            val EPSILON: Double = 1e-6
            val random = Random()
        }

        var hits: Long = 0
        var score: Double = 0.0
        var children: List<Node>? = null

        private fun pickBest() = children?.stream()
                ?.reduce { t: Node?, u: Node? ->
                    if (t!!.score > u!!.score) t else u
                }!!.orElse(Node(Move(Square.NONE, Square.NONE)))

        private fun expand(board: Board) {

            children = MoveGenerator
                    .generateLegalMoves(board)
                    .stream()
                    .map { Node(it) }
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

        fun selectMove(board: Board): Move {

            val side = board.sideToMove
            val visited = LinkedList<Node>()
            var node = this

            var count = 0
            while (!node.isLeaf()) {
                node = node.select()
                board.doMove(node.move)
                visited.add(node)
                count++
            }
            node.expand(board)
            if (!node.isLeaf()) {
                node = node.select()
                board.doMove(node.move)
                val lastSide = board.sideToMove
                count++
                visited.add(node)
                val value = if (side == lastSide) node.eval(board) else -node.eval(board)
                visited.forEach {
                    it.updateStats(value)
                }
            }

            for (i in 1..count) {
                board.undoMove()
            }
            return pickBest()?.move!!
        }

        private fun isLeaf() = children == null || children?.size == 0

        private fun eval(board: Board): Double {

            return when {
                board.isMated -> (-KING_VALUE).toDouble()
                board.isDraw -> 0.0
                else -> scoreMaterial(board).toDouble()
            }
        }

        override fun toString(): String {
            return "Node(move=$move, hits=$hits, score=$score)"
        }
    }

}