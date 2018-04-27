package com.github.bhlangonijr.chesslib

const val MIN_DEPTH = 1

class SearchParams(val whiteTime: Long,
                   val blackTime: Long,
                   val whiteIncrement: Long,
                   val blackIncrement: Long,
                   val moveTime: Long,
                   val depth: Int,
                   val movesToGo: Int,
                   val nodes: Long,
                   val searchMoves: String, //TODO implement this
                   val infinite: Boolean,
                   val ponder: Boolean) {

    override fun toString(): String {

        return "SearchParams(whiteTime=$whiteTime, " +
                "blackTime=$blackTime, " +
                "whiteIncrement=$whiteIncrement, " +
                "blackIncrement=$blackIncrement, " +
                "moveTime=$moveTime, " +
                "depth=$depth, " +
                "movesToGo=$movesToGo, " +
                "nodes=$nodes, " +
                "searchMoves=$searchMoves, " +
                "infinite=$infinite, " +
                "ponder=$ponder)"
    }
}