package com.github.bhlangonijr.chesslib.mcts

import com.github.bhlangonijr.chesslib.Board
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
            if (node.wins.get() / (node.hits.get() + 1.0) > selected.wins.get() / (selected.hits.get() + 1.0)) {
                selected = node
            }
        }
        return selected
    }

    fun expand(moves: MoveList, side: Side): List<Node>? {

        if (children == null) {
            synchronized(this as Any) {
                if (children == null) {
                    children = moves
                            .stream()
                            .map { Node(it, side) }
                            .collect(Collectors.toList())
                }
            }
        }

        return children
    }

    fun select(explorationFactor: Double, board: Board, player: Side): Node {

        var selected: Node? = null
        var best = Double.NEGATIVE_INFINITY
        for (node in children!!) {
            //board.doMove(node.move)
            val winRate = node.wins.get() / (node.hits.get() + explorationFactor)
            //val winProb = (winProbability(scoreMaterial(board, player).toDouble()))
            //println("Winprob [$winProb] vs [${scoreMaterial(board, player)}]")
            val exploration = Math.sqrt(Math.log((hits.get() + 1.0)) / (node.hits.get() + explorationFactor))
            val score = winRate + exploration + random.nextDouble() * explorationFactor
            //println("move [$move] prob: [$winProb] rate: [$winRate] exploration: [$exploration] score: [$score] hits: [$hits] nodes.hit: [${node.hits}]")
            //println("$score / $best")
            if (score > best) {
                selected = node
                best = score
            }
            //board.undoMove()
        }
        return selected!!
    }

    private fun winProbability(score: Double) = 1.0 / (1.0 + Math.exp(-10.0 * (score / 40000)))

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