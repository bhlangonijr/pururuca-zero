package com.github.bhlangonijr.pururucazero.eval

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.pururucazero.abts.Abts
import com.github.bhlangonijr.pururucazero.mcts.Mcts
import com.github.bhlangonijr.pururucazero.ml.NaiveBayes
import com.github.bhlangonijr.pururucazero.ml.pgnToDataSet
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.pururucazero.play
import com.github.bhlangonijr.pururucazero.printResult
import junit.framework.TestCase.assertEquals
import ml.dmlc.xgboost4j.java.DMatrix
import ml.dmlc.xgboost4j.java.XGBoost
import org.junit.Ignore
import org.junit.Test

@Ignore
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
        println(stat.extractFeatures(board).toList())
        //val expect = listOf()
        //assertEquals(expect, stat.extractFeatures(board))
    }

    @Test
    fun `Generate DataSet and validate`() {

        val data = pgnToDataSet("src/test/resources/pt54.pgn")

        val nb = NaiveBayes()
        val stats = nb.train(data)
        println(stats)

        val board = Board()
        //board.loadFromFen("rnb1k1r1/ppppqppp/8/8/P3Pp2/n1b5/K7/8 w q - 10 23")
        //board.loadFromFen("r2qkb1r/pp2nppp/3p4/2pNN1B1/2BnP3/3P4/PPP2PPP/R2bK2R w KQkq - 1 0")
        board.loadFromFen("5k2/6p1/8/p7/P3p3/1pP1r1qP/1P6/5K2 w - - 1 55")
        val features = StatEval().extractFeatureSet(board)

        val classification = nb.classify(features.first, features.second, stats)

        println(classification.predictions.values.sortedBy { it.probability }.reversed())

    }

    @Test
    fun `Match Mcts engine with statistical assisted playing`() {

        val data = pgnToDataSet("src/test/resources/one-win.pgn")

        val nb = NaiveBayes()
        val stats = nb.train(data)

        val board = Board()
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -")

        val mcts1 = Mcts(1.3, stats)
        val mcts2 = Abts()

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
        val test = pgnToDMatrix("src/test/resources/pt54.pgn")

        println(data.rowNum())
        println(test.rowNum())

        val params = HashMap<String, Any>()
        params["booster"] = "dart"
        params["eta"] = 0.3
        params["max_depth"] = 3
        params["nthread"] = 5
        params["num_class"] = 3

        //params["eval_metric"] = "mlogloss"
        params["objective"] = "multi:softprob"

        val watches = HashMap<String, DMatrix>()
        watches["train"] = data
        watches["test"] = test

        val round = 50

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