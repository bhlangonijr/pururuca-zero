package com.github.bhlangonijr.chesslib.eval

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.mcts.Mcts
import com.github.bhlangonijr.chesslib.ml.NaiveBayes
import com.github.bhlangonijr.chesslib.ml.pgnToDataSet
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.chesslib.play
import com.github.bhlangonijr.chesslib.printResult
import junit.framework.TestCase.assertEquals
import ml.dmlc.xgboost4j.java.DMatrix
import ml.dmlc.xgboost4j.java.XGBoost
import org.junit.Test
import java.util.*

class StatsEvalTest {

    @Test
    fun testEval() {

        val board = Board()
        board.loadFromFen("2k5/7R/4K3/8/8/8/8/8 w - - 1 0")

        val stat = StatEval()
        val expect = listOf(1.0, 190.0, 1.0, 1.0, 190.0, 1.0, 1.0, 1.0, 1.0, 190.0, 14.0, 7.0, 2.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 570.0, 0.0, 1.0, 571.0)
        assertEquals(expect, stat.extractFeatures(board))
    }

    @Test
    fun testEval2() {

        val board = Board()
        board.loadFromFen("5rkr/pp2Rp2/1b1p1Pb1/3P2Q1/2n3P1/2p5/P4P2/4R1K1 w - - 1 0")

        val stat = StatEval()
        val expect = listOf(1.0, 50.0, 1441.0, 250.0, 1501.0, 401.0, 611.0, 551.0, 380.0, 20.0, 541.0, 20.0, 550.0,
                30.0, 381.0, 530.0, 20.0, 190.0, 20.0, 191.0, 10.0, 1.0, 10.0, 190.0, 10.0, 540.0, 10.0, 200.0, 50.0,
                261.0, 140.0, 271.0, 220.0, 92.0, 71.0, 30.0, 10.0, 80.0, 30.0, 71.0, 140.0, 30.0, 41.0, 20.0, 50.0,
                151.0, 10.0, 1.0, 10.0, 70.0, 10.0, 30.0, 90.0, 10.0, 120.0, 221.0, 181.0, 351.0, 150.0, 82.0, 20.0,
                81.0, 140.0, 20.0, 150.0, 30.0, 1.0, 70.0, 10.0, 11.0, 40.0, 80.0, 10.0, 40.0, 150.0, 30.0, 1.0,
                400.0, 770.0, 770.0, 951.0, 801.0, 1113.0, 10.0, 10.0, 400.0, 541.0, 540.0, 210.0, 200.0, 40.0, 922.0,
                10.0, 200.0, 540.0, 10.0, 20.0, 191.0, 4.0, 18.0, 3.0, 4.0, 3.0, 6.0, 13.0, 1.0, 1061.0, 390.0, 0.0,
                190.0, 171.0, 0.0, 50.0, 0.0, 70.0, 0.0, 0.0, 0.0, 380.0, 0.0, 530.0, 0.0, 5382.0, 392.0, 20.0, 190.0,
                582.0, 220.0, 63.0, 140.0, 772.0, 292.0, 150.0, 70.0, 3350.0, 540.0, 1670.0, 200.0, 2.0, 1.0, 1.0,
                1.0, 1.0, 1.0, 3.0)
        assertEquals(expect, stat.extractFeatures(board))
    }

    @Test
    fun `Generate DataSet and validate`() {

        val data = pgnToDataSet("src/test/resources/pt54.pgn")

        val nb = NaiveBayes()
        val stats = nb.train(data)
        println(stats)

        val board = Board()
        board.loadFromFen("r2qkb1r/pp2nppp/3p4/2pNN1B1/2BnP3/3P4/PPP2PPP/R2bK2R w KQkq - 1 0")
        //board.loadFromFen("5k2/6p1/8/p7/P3p3/1pP1r1qP/1P6/5K2 w - - 1 55")
        val features = StatEval().extractFeatureSet(board)

        val classification = nb.classify(features.first, features.second, stats)

        println(classification.predictions.values.sortedBy { it.probability }.reversed())

    }

    @Test
    fun `Match Mcts engine with statistical assisted playing`() {

        val data = pgnToDataSet("src/test/resources/Stockfish_DD_64-bit_4CPU.pgn")

        val nb = NaiveBayes()
        val stats = nb.train(data)

        val board = Board()
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -")

        val mcts1 = Mcts(1.4, stats)
        val mcts2 = Mcts()

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
    fun testLearningPgnXgBoost() {

        val data = pgnToDMatrix("src/test/resources/Stockfish_DD_64-bit_4CPU.pgn")
        val test = pgnToDMatrix("src/test/resources/test.pgn")

        println(data.rowNum())
        println(test.rowNum())

        val params = HashMap<String, Any>()
        params["eta"] = 1.0
        params["max_depth"] = 5
        params["nthread"] = 5
        params["num_class"] = 3

        //params["eval_metric"] = "mlogloss"
        params["objective"] = "multi:softprob"

        val watches = HashMap<String, DMatrix>()
        watches["train"] = data
        watches["test"] = test

        val round = 10

        val booster = XGBoost.train(data, params, round, watches, null, null)

        val predicts = booster.predict(test)

        predicts.forEachIndexed { i1, floats ->
            floats.forEachIndexed { i2, m ->
                println("$i1,$i2 = $m [" + test.label[i1] + "]")
            }
        }
    }
}


fun pgnToDMatrix(name: String): DMatrix {

    val data = pgnToDataSet(name)
    val matrix = DMatrix(data.rowHeaders, data.colIndex,
            data.features, DMatrix.SparseType.CSR, data.labels.size)
    matrix.label = data.labels
    return matrix
}