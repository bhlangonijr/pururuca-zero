package com.github.bhlangonijr.pururucazero.abts

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.pururucazero.SearchParams
import com.github.bhlangonijr.pururucazero.SearchState
import org.junit.Assert.assertEquals
import org.junit.Test

class AbtsTest {

    @Test
    fun `Search best move avoiding lose rook`() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P3/6PP/RNBK1BNR w kq - 0 19")

        val params = SearchParams(depth = 5)
        val state = SearchState(params, board)

        val bestMove = Abts().rooSearch(state)
        assertEquals(Move(Square.D1, Square.E1), bestMove)
    }

    @Test
    fun `Search best move wining quality`() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P1P1/7P/RNBK1BNR b kq - 0 20")

        val params = SearchParams(depth = 5)
        val state = SearchState(params, board)

        val bestMove = Abts().rooSearch(state)
        assertEquals(Move(Square.E4, Square.F2), bestMove)
    }

    @Test
    fun `Search best move force knight trade`() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/8/PPP1P1P1/5n1P/RNBK1BNR w kq - 1 21")

        val params = SearchParams(depth = 4)
        val state = SearchState(params, board)

        val bestMove = Abts().rooSearch(state)
        assertEquals(Move(Square.D1, Square.E2), bestMove)
    }

    @Test
    fun `Search best move capturing the rook`() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/8/PPP1P1P1/5n1P/RNB1KBNR b kq - 2 22")

        val params = SearchParams(depth = 4)
        val state = SearchState(params, board)

        val bestMove = Abts().rooSearch(state)
        assertEquals(Move(Square.F2, Square.H1), bestMove)
    }

}