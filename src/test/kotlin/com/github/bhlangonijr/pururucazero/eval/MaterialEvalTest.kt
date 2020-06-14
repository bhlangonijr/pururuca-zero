package com.github.bhlangonijr.pururucazero.eval

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.pururucazero.SearchParams
import com.github.bhlangonijr.pururucazero.SearchState
import org.junit.Assert.assertEquals
import org.junit.Test

class MaterialEvalTest {

    @Test
    fun testEval() {

        val eval = MaterialEval()
        val board = Board()
        val state = SearchState(SearchParams(), board)
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P3/6PP/RNBK1BNR w kq - 0 19")
        println(board.fen)
        println(eval.evaluate(state, board))
        assertEquals(0, eval.evaluate(state, board))

        board.doMove(Move(Square.G2, Square.G3))
        println(board.fen)
        println(eval.evaluate(state, board))
        assertEquals(0, eval.evaluate(state, board))

        board.doMove(Move(Square.E4, Square.F2))
        println(board.fen)
        println(eval.evaluate(state, board))
        println(board.isKingAttacked)
        println(MoveGenerator.generateLegalMoves(board))
        assertEquals(0, eval.evaluate(state, board))

        board.doMove(Move(Square.D1, Square.E1))
        println(board.fen)
        println(eval.evaluate(state, board))
        println(board.isKingAttacked)
        println(MoveGenerator.generateLegalMoves(board))
        assertEquals(0, eval.evaluate(state, board))

        board.doMove(Move(Square.F2, Square.H1))
        println(board.fen)
        println(eval.evaluate(state, board))
        println(board.isKingAttacked)
        assertEquals(-500, eval.evaluate(state, board))

    }
}


