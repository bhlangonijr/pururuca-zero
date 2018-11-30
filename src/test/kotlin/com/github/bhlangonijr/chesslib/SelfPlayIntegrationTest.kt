package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.Constants.startStandardFENPosition
import com.github.bhlangonijr.chesslib.abts.Abts
import com.github.bhlangonijr.chesslib.mcts.Mcts
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import org.junit.Test

class SelfPlayIntegrationTest {

    @Test
    fun `Match Abts and Mcts engines`() {

        val board = Board()
        board.loadFromFen(startStandardFENPosition)

        val abts = Abts()
        val mcts = Mcts()

        val moves = MoveList(board.fen)
        while (!board.isDraw && !board.isMated) {
            val move = play(board, abts, mcts)
            if (move != Move(Square.NONE, Square.NONE) && board.doMove(move)) {
                moves += move
                println("Played: $move = ${board.fen}")
            }
        }
        printResult(moves, board)
    }

    @Test
    fun `Match Mcts engine with different parameters`() {

        val board = Board()
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -")

        val mcts1 = Mcts(1.4)
        val mcts2 = Mcts()

        val moves = MoveList(board.fen)
        while (!board.isDraw && !board.isMated) {
            val move = play(board, mcts1, mcts2)
            if (move != Move(Square.NONE, Square.NONE) && board.doMove(move)) {
                moves += move
                println("Played: $move = ${board.fen}")
            }
        }

        printResult(moves, board)
    }

}