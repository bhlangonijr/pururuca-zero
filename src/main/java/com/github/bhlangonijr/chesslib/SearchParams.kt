package com.github.bhlangonijr.chesslib

const val MIN_DEPTH = 1

class SearchParams(val whiteTime: Long ?= 0,
                   val blackTime: Long ?= 0,
                   val whiteIncrement: Long ?= 0,
                   val blackIncrement: Long ?= 0,
                   val moveTime: Long ?= 0,
                   val depth: Int ?= MIN_DEPTH,
                   val movesToGo: Int ?= 0,
                   val nodes: Long ?= 0,
                   val searchMoves: String ?= "", //TODO implement this
                   val infinite: Boolean ?= false,
                   val ponder: Boolean ?= false) {

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