package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import java.util.*
import java.util.stream.Collectors

class Mcts : SearchEngine {

    override fun rooSearch(state: SearchState): Move {

        val node = Node(Move(Square.NONE, Square.NONE))
        var bestMove: Move? = Move(Square.NONE, Square.NONE)

        while (!state.shouldStop()) {
            state.nodes.incrementAndGet()
            bestMove = node.selectMove(state)
        }
        node.children!!.forEach { println(it) }
        println("bestmove $bestMove")
        println("info string total time ${System.currentTimeMillis() - state.params.initialTime}")
        println("info string total nodes ${state.nodes.get()}")
        return bestMove!!
    }

    class Node(val move: Move) {

        companion object {
            val EPSILON = 1.43
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

        fun selectMove(state: SearchState): Move {

            val side = state.board.sideToMove
            val visited = LinkedList<Node>()
            var node = this

            var count = 0
            while (!node.isLeaf()) {
                node = node.select()
                state.board.doMove(node.move)
                visited.add(node)
                count++
            }
            node.expand(state.board)
            if (!node.isLeaf()) {
                node = node.select()
                state.board.doMove(node.move)
                val lastSide = state.board.sideToMove
                count++
                visited.add(node)
                val value = if (side == lastSide) node.playOut(state) else -node.playOut(state)
                visited.forEach {
                    it.updateStats(value)
                }
            }

            for (i in 1..count) {
                state.board.undoMove()
            }
            return pickBest()?.move!!
        }

        private fun isLeaf() = children == null || children?.size == 0

        private fun playOut(state: SearchState): Double {

            val moves = MoveGenerator.generateLegalMoves(state.board)
            val isKingAttacked = state.board.isKingAttacked
            return when {
                moves.size == 0 && isKingAttacked -> -1.0
                moves.size == 0 && !isKingAttacked -> 0.0
                //moves.size > 0 && !isKingAttacked -> -10.0
                else -> 0.000//moves.size.toDouble()/100.0
            }
        }

        private fun playOut_(state: SearchState): Double {

            try {
                val moves = MoveGenerator.generateLegalMoves(state.board)
                val isKingAttacked = state.board.isKingAttacked
                return when {
                    moves.size == 0 && isKingAttacked -> -1.0
                    moves.size == 0 && !isKingAttacked -> 0.0
                    state.board.isDraw -> 0.0
                    else -> {
                        state.board.doMove(moves[random.nextInt(moves.size)])
                        state.nodes.incrementAndGet()
                        val score = playOut(state)
                        state.board.undoMove()
                        return score
                    }
                }
            } catch (e: Exception) {
                println("Error $e")
                println(state.board.fen)
                println(state.board)
                return 0.0
            }

        }

        override fun toString(): String {
            return "Node(move=$move, hits=$hits, score=$score)"
        }
    }

}