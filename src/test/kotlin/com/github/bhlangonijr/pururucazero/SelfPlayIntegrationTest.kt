package com.github.bhlangonijr.pururucazero

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Constants.startStandardFENPosition
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.pururucazero.alphabeta.AlphaBetaSearch
import com.github.bhlangonijr.pururucazero.eval.StatsEval
import com.github.bhlangonijr.pururucazero.ml.NaiveBayes
import com.github.bhlangonijr.pururucazero.ml.PgnConverter.Companion.pgnToDataSet
import com.github.bhlangonijr.pururucazero.montecarlo.MonteCarloSearch
import org.junit.Test

@ExperimentalStdlibApi
class SelfPlayIntegrationTest {

    @Test
    fun `Match Abts and Mcts engines`() {

        val board = Board()
        board.loadFromFen(startStandardFENPosition)

        val abts = AlphaBetaSearch()
        val mcts = MonteCarloSearch()

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
    fun `Search mate KQ vs K`() {

        val board = Board()
        board.loadFromFen("8/8/3k4/8/4K3/5Q2/8/8 w - - 0 1")

        val abts = AlphaBetaSearch()
        val moves = MoveList(board.fen)
        val state = SearchState(SearchParams(depth = 4), board)
        while (!board.isDraw && !board.isMated) {
            println("Search: ${board.fen} - \n$board")
            val move = abts.rooSearch(state)
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

        val mcts1 = MonteCarloSearch(1.4)
        val mcts2 = MonteCarloSearch()

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

    @Test
    fun `Test naive bayes training`() {

        //val data = pgnToDataSet("/Users/bhlangonijr/Downloads/CCRL-4040.[1162348].pgn")
        val data = pgnToDataSet("src/test/resources/Stockfish_DD_64-bit_4CPU.pgn")
        val nb = NaiveBayes()
        val stats = nb.train(data)
        println(stats)  

    }

    @Test
    fun `Match Alpha beta engine with statistical assisted playing`() {

        //val data = pgnToDataSet("/Users/bhlangonijr/Downloads/CCRL-4040.[1162348].pgn")
        val data = pgnToDataSet("src/test/resources/Stockfish_DD_64-bit_4CPU.pgn")
        val nb = NaiveBayes()
        val stats = nb.train(data)
        println(stats)

        val board = Board()
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -")

        val mcts1 = MonteCarloSearch(1.5, StatsEval(stats))
        val mcts2 = AlphaBetaSearch()

        val moves = MoveList(board.fen)
        while (!board.isDraw && !board.isMated) {
            val move = play(board, mcts1, mcts2, 60000)
            if (move != Move(Square.NONE, Square.NONE) && board.doMove(move)) {
                moves += move
                println("Played: $move = ${board.fen}")
            }
        }

        printResult(moves, board)
    }

    @Test
    fun `Match Mcts engine with statistical assisted playing`() {

        //val data = pgnToDataSet("/Users/bhlangonijr/Downloads/CCRL-4040.[1162348].pgn")
        val data = pgnToDataSet("src/test/resources/Stockfish_DD_64-bit_4CPU.pgn")
        val nb = NaiveBayes()
        val stats = nb.train(data)
        println(stats)

        val board = Board()
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -")

        val mcts1 = MonteCarloSearch(1.5, StatsEval(stats))
        val mcts2 = MonteCarloSearch()

        val moves = MoveList(board.fen)
        while (!board.isDraw && !board.isMated) {
            val move = play(board, mcts1, mcts2, 60000)
            if (move != Move(Square.NONE, Square.NONE) && board.doMove(move)) {
                moves += move
                println("Played: $move = ${board.fen}")
            }
        }

        printResult(moves, board)
    }

}