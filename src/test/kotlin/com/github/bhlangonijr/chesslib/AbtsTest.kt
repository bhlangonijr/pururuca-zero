package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.abts.Abts
import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Test

class AbtsTest {

    @Test
    fun testSearchWithDepth1() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P3/6PP/RNBK1BNR w kq - 0 19")

        val params = SearchParams(depth = 6)
        val state = SearchState(params, board)

        val bestMove = Abts().rooSearch(state)
        assertEquals(Move(Square.D1, Square.E1), bestMove)
    }

    @Test
    fun testSearchWithDepth2() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P1P1/7P/RNBK1BNR b kq - 0 20")

        val params = SearchParams(depth = 6)
        val state = SearchState(params, board)

        val bestMove = Abts().rooSearch(state)
        assertEquals(Move(Square.E4, Square.F2), bestMove)
    }

    @Test
    fun testSearchWithDepth3() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/8/PPP1P1P1/5n1P/RNBK1BNR w kq - 1 21")

        val params = SearchParams(depth = 5)
        val state = SearchState(params, board)

        val bestMove = Abts().rooSearch(state)
        assertEquals(Move(Square.D1, Square.E2), bestMove)
    }

    @Test
    fun testSearchWithDepth4() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/8/PPP1P1P1/5n1P/RNB1KBNR b kq - 2 22")

        val params = SearchParams(depth = 5)
        val state = SearchState(params, board)

        val bestMove = Abts().rooSearch(state)
        assertEquals(Move(Square.F2, Square.H1), bestMove)
    }

}