package com.github.bhlangonijr.chesslib.mcts

import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
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
            val nodeScore = node.score.get() * (node.hits.get() )
            val currentScore = selected.score.get() * (selected.hits.get() )
            if (nodeScore > currentScore) {
                selected = node
            }
        }
        return selected
    }

    fun expand(moves: MoveList, side: Side): List<Node>? {

        synchronized(this as Any, {
            if (children == null) {
                children = moves
                        .stream()
                        .map { Node(it, side) }
                        .collect(Collectors.toList())
            }
        })

        return children
    }

    fun select(explorationFactor: Double): Node {

        var selected: Node? = null
        var best = Double.MIN_VALUE
        for (node in children!!) {
            val score = node.wins.get() / (node.hits.get() + 1.0) +
                    explorationFactor * Math.sqrt(2 * Math.log((hits.get() + 1.0)) / (node.hits.get() + 1.0)) +
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
        if (score > 0.0) wins.incrementAndGet()
        if (score < 0.0) losses.incrementAndGet()
        this.score.getAndAdd(score)
    }

    fun isLeaf() = children == null || children?.size == 0

    override fun toString(): String {
        return "Node(move=$move, side=$side, hits=$hits, wins=$wins, losses=$losses, score=$score"
    }
}