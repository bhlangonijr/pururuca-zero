package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.abts.Abts
import com.github.bhlangonijr.chesslib.mcts.Mcts
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import org.junit.Test

class SelfPlayIntegrationTest {


    @Test
    fun `Match Abts and Mcts engines`() {

        val board = Board()
        board.loadFromFen("r1b1kb1r/ppp2ppp/8/4n3/4n3/PPP1P1P1/7P/RNBK1BNR b kq - 0 20")

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

    private fun printResult(moves: MoveList, board: Board) {

        if (board.isDraw) {
            println("result = 1/2 - 1/2")
        } else if (board.isMated && board.sideToMove == Side.BLACK) {
            println("result = 1 - 0")
        } else {
            println("result = 0 - 1")
        }

        println("move list: ${moves.toSan()}")
        println("final fen: ${board.fen}")
    }

    private fun play(board: Board,
                     player1: SearchEngine,
                     player2: SearchEngine): Move {

        val params1 = SearchParams(whiteTime = 180000, whiteIncrement = 0, blackTime = 180000, blackIncrement = 0)
        val params2 = SearchParams(whiteTime = 180000, whiteIncrement = 0, blackTime = 180000, blackIncrement = 0)

        return if (board.sideToMove == Side.WHITE) {
            player1.rooSearch(SearchState(params1, board))
        } else {
            player2.rooSearch(SearchState(params2, board))
        }
    }
}