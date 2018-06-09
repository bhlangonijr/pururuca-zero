package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.mcts.Mcts
import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Test

class MctsTest {

    @Test
    fun testSearchWithNodes() {

        val board = Board()
        board.loadFromFen("r2qkb1r/pp2nppp/3p4/2pNN1B1/2BnP3/3P4/PPP2PPP/R2bK2R w KQkq - 1 0")
        println(board)
        val params = SearchParams(nodes = 2000000)
        val state = SearchState(params, board)

        val bestMove = Mcts().rooSearch(state)

        assertEquals(Move(Square.D5, Square.F6), bestMove)
    }

    @Test
    fun testSearchWithNodes2() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P1P1/7P/RNBK1BNR b kq - 0 20")
        println(board)
        val params = SearchParams(nodes = 2000000)
        val state = SearchState(params, board)

        val bestMove = Mcts().rooSearch(state)
        assertEquals(Move(Square.E4, Square.F2), bestMove)
    }

}



