package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move
import junit.framework.Assert.assertEquals
import org.junit.Test

class MctsTest {


    @Test
    fun testSearchWithNodes() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P3/6PP/RNBK1BNR w kq - 0 19")

        val params = SearchParams(nodes = 100)
        val state = SearchState(params, board)

        val bestMove = Mcts().rooSearch(state)

        assertEquals(Move(Square.D1, Square.E1), bestMove)

    }


}



