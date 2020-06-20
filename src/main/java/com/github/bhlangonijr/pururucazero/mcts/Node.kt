package com.github.bhlangonijr.pururucazero.mcts

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

class Node(val move: Move,
           val side: Side) {

    val hits = AtomicLong(0)
    val wins = AtomicLong(0)
    val losses = AtomicLong(0)
    val score = AtomicLong(0)
    var children: List<Node> = emptyList()
    var terminal = AtomicBoolean(false)
    var result = AtomicLong(0)

    fun pickBest(): Node {

        var selected = children[0]
        for (node in children) {
            if (node.wins.get() / (node.hits.get() + 1.0) > selected.wins.get() / (selected.hits.get() + 1.0)) {
                selected = node
            }
        }
        return selected
    }

    fun expand(moves: MoveList, side: Side): List<Node>? {

        if (children.isEmpty()) {
            synchronized(this as Any) {
                if (children.isEmpty()) {
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

        var selected: Node = children[0]
        var best = Double.NEGATIVE_INFINITY
        for (node in children) {
            //board.doMove(node.move)
            val winRate = node.wins.get() / (node.hits.get() + explorationFactor)
            //val winProb = (winProbability(scoreMaterial(board, player).toDouble()))
            //println("Winprob [$winProb] vs [${scoreMaterial(board, player)}]")
            val exploration = sqrt(ln(hits.get().toDouble()) / node.hits.get())
            val score = winRate + explorationFactor * exploration
            //println("move [$move] prob: [$winProb] rate: [$winRate] exploration: [$exploration] score: [$score] hits: [$hits] nodes.hit: [${node.hits}]")
            //println("$score / $best")
            if (score > best) {
                selected = node
                best = score
            }
            //board.undoMove()
        }
        return selected
    }

    private fun winProbability(score: Double) = 1.0 / (1.0 + exp(-10.0 * (score / 40000)))

    fun updateStats(score: Long) {
        this.hits.incrementAndGet()
        if (score > 0.0) wins.incrementAndGet()
        if (score < 0.0) losses.incrementAndGet()
        this.score.getAndAdd(score)
    }

    fun terminate(result: Long) {
        this.terminal.set(true)
        this.result.set(result)
    }

    fun isLeaf() = children.isEmpty()

    override fun toString(): String {
        return "Node(move=$move, side=$side, hits=$hits, wins=$wins, losses=$losses, score=$score"
    }
}