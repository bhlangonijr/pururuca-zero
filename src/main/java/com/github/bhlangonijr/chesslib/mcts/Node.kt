package com.github.bhlangonijr.chesslib.mcts

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors

class Node(val move: Move, val side: Side) {

    val hits = AtomicLong(0)
    val wins = AtomicLong(0)
    val losses = AtomicLong(0)
    val score = AtomicLong(0)
    var children: List<Node>? = null

    fun pickBest(): Node {

        var selected = children!![0]
        for (node in children!!) {
            val nodeScore = node.score.get() / (node.hits.get() + 1.0)
            val currentScore = selected.score.get() / (selected.hits.get() + 1.0)
            if (nodeScore > currentScore) {
                selected = node
            }
        }
        return selected
    }

    fun expand(board: Board): List<Node>? {

        if (children == null) {
            children = MoveGenerator
                    .generateLegalMoves(board)
                    .stream()
                    .map { Node(it, board.sideToMove) }
                    .collect(Collectors.toList())
        }
        return children
    }

    fun select(): Node {

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

    fun updateStats(score: Long) {
        this.hits.incrementAndGet()
        if (score > 0.0) this.wins.incrementAndGet()
        if (score < 0.0) this.losses.incrementAndGet()
        this.score.accumulateAndGet(score, { left, right -> left + right })
    }

    fun isLeaf() = children == null || children?.size == 0

    override fun toString(): String {
        return "Node(move=$move, side=$side, hits=$hits, wins=$wins, losses=$losses, score=$score"
    }
}