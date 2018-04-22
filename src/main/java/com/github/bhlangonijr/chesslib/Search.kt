package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.MoveList

class Search {

    private val board = Board()

    fun reset() {

        board.clear()
    }

    fun setupPosition(fen: String, moves: String) {

        val moveList = MoveList(fen)
        moveList.loadFromText(moves)
        board.loadFromFen(moveList.getFen(moveList.size))
    }

    fun setupPosition(moves: String) {

        val moveList = MoveList()
        moveList.loadFromText(moves)
        board.loadFromFen(moveList.getFen(moveList.size))
    }

    fun start(params: SearchParams): Boolean {

        // search
        return true
    }

    fun stop(): Boolean {

        return true
    }

}