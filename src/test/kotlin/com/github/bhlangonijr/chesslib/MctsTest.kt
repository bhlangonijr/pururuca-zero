package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Test

class MctsTest {

    @Test
    fun testSearchWithNodes() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P3/6PP/RNBK1BNR w kq - 0 19")

        val params = SearchParams(nodes = 20000)
        val state = SearchState(params, board)

        val bestMove = Mcts().rooSearch(state)

        assertEquals(Move(Square.D1, Square.E2), bestMove)
    }

    @Test
    fun testSearchWithNodes2() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P1P1/7P/RNBK1BNR b kq - 0 20")

        val params = SearchParams(nodes = 10000)
        val state = SearchState(params, board)

        val bestMove = Mcts().rooSearch(state)
        assertEquals(Move(Square.E4, Square.F2), bestMove)
    }


}



