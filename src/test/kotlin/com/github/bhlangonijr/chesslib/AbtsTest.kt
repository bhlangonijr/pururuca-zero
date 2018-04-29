package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move
import junit.framework.Assert.assertEquals
import org.junit.Test

class AbtsTest {


    @Test
    fun testSearchWithDepth() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P3/6PP/RNBK1BNR w kq - 0 19")

        val params = SearchParams(
                whiteTime = 60000,
                blackTime = 60000,
                whiteIncrement = 1000,
                blackIncrement = 1000,
                moveTime = 0,
                movesToGo = 1,
                depth = 100,
                nodes = 50000000,
                infinite = false,
                ponder = false,
                searchMoves = "")

        val state = SearchState(params, board)

        val bestMove = Abts().rooSearch(state)

        assertEquals(Move(Square.D1, Square.E1), bestMove)

    }


}



