package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import java.util.*
import java.util.stream.Collectors

class Mcts constructor(private val params: SearchParams,
                       private val board: Board,
                       private val search: Search) {

    fun rooSearch() {

        val node = Node(Move(Square.NONE, Square.NONE))
        var bestMove: Move? = Move(Square.NONE, Square.NONE)
        val initialTime = System.currentTimeMillis()
        var nodes = 0L
        while (!search.shouldStop(params, nodes, initialTime)) {
            nodes++
            bestMove = node.selectMove(board)
        }
        node.children!!.forEach { println(it) }
        println("bestmove $bestMove")
        println("info string total time ${System.currentTimeMillis() - initialTime}")
    }

    class Node(val move: Move) {

        companion object {
            val PAWN_VALUE = 1.0
            val BISHOP_VALUE = 3.0
            val KNIGHT_VALUE = 3.0
            val ROOK_VALUE = 5.0
            val QUEEN_VALUE = 9.0
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

            var selected: Node ?= null
            var best = Double.NEGATIVE_INFINITY
            for (node in children!!) {
                val score =  score / (node.hits + EPSILON) +
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
            while (!isLeaf(node)) {
                node = node.select()
                board.doMove(node.move)
                visited.add(node)
                count++
            }
            node.expand(board)
            if (!isLeaf(node)) {
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

        private fun isLeaf(node: Node) = node.children == null || node.children?.size == 0

        private fun eval(board: Board): Double {

            return when {
                board.isMated -> -1.0
                board.isDraw -> 0.0
                else -> random.nextGaussian()
            }
        }

        private fun scoreMaterial(board: Board): Double {

            val side = board.sideToMove
            val other = side.flip()
            return countMaterial(board, side) - countMaterial(board, other)
        }

        private fun countMaterial(board: Board, side: Side) =
                bitCount(board.getBitboard(Piece.make(side, PieceType.PAWN))) * PAWN_VALUE +
                        bitCount(board.getBitboard(Piece.make(side, PieceType.BISHOP))) * BISHOP_VALUE +
                        bitCount(board.getBitboard(Piece.make(side, PieceType.KNIGHT))) * KNIGHT_VALUE +
                        bitCount(board.getBitboard(Piece.make(side, PieceType.ROOK))) * ROOK_VALUE +
                        bitCount(board.getBitboard(Piece.make(side, PieceType.QUEEN))) * QUEEN_VALUE

        private fun bitCount(bb: Long) = java.lang.Long.bitCount(bb).toDouble()

        override fun toString(): String {
            return "Node(move=$move, hits=$hits, score=$score)"
        }
    }

}