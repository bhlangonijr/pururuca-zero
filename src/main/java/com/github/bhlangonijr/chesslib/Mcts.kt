package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList
import java.util.*
import java.util.stream.Collectors

class Mcts : SearchEngine {

    override fun rooSearch(state: SearchState): Move {

        val node = Node(Move(Square.NONE, Square.NONE), state.board.sideToMove)
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

    class Node(val move: Move, val side: Side) {

        companion object {
            const val EPSILON = 1.43
            val random = Random()
        }

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
                visited.add(node)
            }
            node.expand(state.board)
            if (!node.isLeaf()) {
                node = node.select()
                state.board.doMove(node.move)
                visited.add(node)

            }
            val value = if (state.board.sideToMove == side) node.playOut(state) else -node.playOut(state)

            visited.forEachIndexed { index, n ->
                n.updateStats(value)
                state.board.undoMove()
            }

            return pickBest().move
        }

        private fun isLeaf() = children == null || children?.size == 0

        private fun playOut(state: SearchState): Double {

            return try {
                val moves = MoveGenerator.generateLegalMoves(state.board)
                val isKingAttacked = state.board.isKingAttacked
                when {
                    moves.size == 0 && isKingAttacked -> 0.0
                    moves.size == 0 && !isKingAttacked -> 0.5
                    //moves.size >= 0 && isKingAttacked -> -1.0
                    state.board.isDraw -> 0.5
                    else -> winProbability(scoreMaterial(state.board).toDouble())//resolvePosition(state, moves)
                }
            } catch (e: Exception) {
                println("Error $e")
                println(state.board.fen)
                println(state.board)
                0.0
            }

        }

        fun winProbability(score: Double) = (1.0 + Math.exp(-5.0 * (score / 3000)))

        private fun resolvePosition(state: SearchState, moves: MoveList): Double {

            val score = scoreMaterial(state.board)
            //println("score = $score   = ${winProbability(score.toDouble())}")
            return when {
                // heuristic cut-off if side is up/down 300 points
                Math.abs(score) >= 400 -> winProbability(score.toDouble())
                else -> {
                    state.board.doMove(moves[random.nextInt(moves.size)])
                    state.nodes.incrementAndGet()
                    val playOutScore = playOut(state)
                    state.board.undoMove()
                    return playOutScore
                }
            }
        }



        override fun toString(): String {
            return "Node(move=$move, hits=$hits, score=$score)"
        }
    }

}