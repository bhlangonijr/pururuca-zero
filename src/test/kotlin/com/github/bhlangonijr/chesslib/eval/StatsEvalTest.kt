package com.github.bhlangonijr.chesslib.eval

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.abts.Abts
import com.github.bhlangonijr.chesslib.mcts.Mcts
import com.github.bhlangonijr.chesslib.ml.DataSet
import com.github.bhlangonijr.chesslib.ml.FeatureSet
import com.github.bhlangonijr.chesslib.ml.NaiveBayes
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.chesslib.pgn.PgnHolder
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.util.*

class StatsEvalTest {

    @Test
    fun testEval() {

        val board = Board()
        board.loadFromFen("2k5/7R/4K3/8/8/8/8/8 w - - 1 0")

        val stat = StatEval()
        val expect = listOf(1.0, 190.0, 1.0, 1.0, 190.0, 1.0, 1.0, 1.0, 1.0, 190.0, 14.0, 7.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 570.0, 0.0, 1.0, 571.0)
        assertEquals(expect, stat.getFeatureSet(1, board, 1.0).features)

    }

    @Test
    fun testEval2() {

        val board = Board()
        board.loadFromFen("5rkr/pp2Rp2/1b1p1Pb1/3P2Q1/2n3P1/2p5/P4P2/4R1K1 w - - 1 0")

        val stat = StatEval()
        val expect = listOf(1.0, 50.0, 1441.0, 250.0, 1501.0, 401.0, 611.0, 551.0, 380.0, 20.0, 541.0, 20.0, 550.0, 30.0, 381.0, 530.0, 20.0, 190.0, 20.0, 191.0, 10.0, 1.0, 10.0, 190.0, 10.0, 540.0, 10.0, 200.0, 50.0, 261.0, 140.0, 271.0, 220.0, 92.0, 71.0, 30.0, 10.0, 80.0, 30.0, 71.0, 140.0, 30.0, 41.0, 20.0, 50.0, 151.0, 10.0, 1.0, 10.0, 70.0, 10.0, 30.0, 90.0, 10.0, 120.0, 221.0, 181.0, 351.0, 150.0, 82.0, 20.0, 81.0, 140.0, 20.0, 150.0, 30.0, 1.0, 70.0, 10.0, 11.0, 40.0, 80.0, 10.0, 40.0, 150.0, 30.0, 1.0, 400.0, 770.0, 770.0, 951.0, 801.0, 1113.0, 10.0, 10.0, 400.0, 541.0, 540.0, 210.0, 200.0, 40.0, 922.0, 10.0, 200.0, 540.0, 10.0, 20.0, 191.0, 4.0, 18.0, 3.0, 4.0, 3.0, 6.0, 13.0, 1.0, 1061.0, 390.0, 0.0, 190.0, 171.0, 0.0, 50.0, 0.0, 70.0, 0.0, 0.0, 0.0, 380.0, 0.0, 530.0, 0.0, 5382.0, 392.0, 20.0, 190.0, 582.0, 220.0, 63.0, 140.0, 772.0, 292.0, 150.0, 70.0, 3350.0, 540.0, 1670.0, 200.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 3.0)
        assertEquals(expect, stat.getFeatureSet(1, board, 1.0).features)
    }

    @Test
    fun `Match Mcts engine with statistical assisted playing`() {

        val data = pgnToDataSet("src/test/resources/Stockfish_9_64-bit_4CPU.commented.[1416].pgn")

        val nb = NaiveBayes()
        val stats = nb.train(data)

        val board = Board()
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -")

        val mcts1 = Mcts()
        val mcts2 = Mcts(1.4, stats)

        val moves = MoveList(board.fen)
        while (!board.isDraw && !board.isMated) {
            val move = play(board, mcts1, mcts2, 40000)
            if (move != Move(Square.NONE, Square.NONE) && board.doMove(move)) {
                moves += move
                println("Played: $move = ${board.fen}")
            }
        }

        printResult(moves, board)
    }

    private val featureNames = IntRange(0, 130).map { "$it" }

    @Test
    fun `Match Mcts engine with statistical assisted playing against Abts`() {

        val data = pgnToDataSet("src/test/resources/test.pgn")

        val eval = StatEval()
        val nb = NaiveBayes()

        var totalGames = 0
        var wonByMcts = 0
        var draw = 0
        val features = ArrayList(data.samples)
        for (i in 1..1000) {
            val board = Board()
            board.loadFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -")
            val moves = MoveList(board.fen)
            val partial = arrayListOf<FeatureSet>()

            val stats = nb.train(DataSet(features, featureNames))

            val player1 = if (i % 2 == 0) Mcts(1.4, stats) else Abts()
            val player2 = if (i % 2 != 0) Mcts(1.4, stats) else Abts()
            val mctsColor = if (i % 2 == 0) Side.WHITE else Side.BLACK
            try {
                while (!board.isDraw && !board.isMated) {
                    val move = play(board, player1, player2, 40000)
                    if (move != Move(Square.NONE, Square.NONE) && board.doMove(move)) {
                        moves += move
                        partial.add(eval.getFeatureSet(i, board, 0.0))
                        println("Played: $move = ${board.fen}")
                    }
                }

                partial.forEach {
                    it.features[0] = when {
                        board.isMated && board.sideToMove == Side.WHITE -> 2.0
                        board.isMated && board.sideToMove == Side.BLACK -> 1.0
                        else -> 0.0
                    }
                }
                println("partial ${partial.joinToString { "$it" }}")
                totalGames++
                if (board.isMated && board.sideToMove != mctsColor) {
                    wonByMcts++
                } else if (board.isDraw) {
                    draw++
                }
                features.addAll(partial)
                printResult(moves, board)
                println("Total = $totalGames, draws = $draw, won = $wonByMcts")
                println("====================================================")
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    @Test
    fun testLearningPgn() {

        val data = pgnToDataSet("src/test/resources/Stockfish_DD_64-bit_4CPU.pgn")
        val test = pgnToDataSet("src/test/resources/test.pgn")

        val nb = NaiveBayes()
        val stats = nb.train(data)

        var countTotal = 0
        var countCorrect = 0
        test.samples.forEach {

            val classification = nb.classify(it, stats)
            //println(it)
            //println("${classification.predictions}")
            println("${it.id}/${it.getClass()} = ${classification.predict()}")
            countTotal++
            if (it.getClass() == classification.predict()) {
                countCorrect++
            }
            //println()
        }
        println(stats)
        println("Totals ${data.samples.size}/${test.samples.size} = ${countCorrect.toDouble()/countTotal*100}%")

    }

    @Test
    fun testLearningPgnMultiple() {

        val testMap = pgnToDataSetList("src/test/resources/Stockfish_DD_64-bit_4CPU.pgn")
        val statMap = pgnToDataSetList("src/test/resourcestest.pgn")
                .entries
                .associateBy({it.key}, {NaiveBayes().train(it.value)})

        var countTotal = 0
        var countCorrect = 0
        testMap.entries.forEach { e ->
            val data = e.value
            val key = e.key
            data.samples.forEach { featureSet ->
                val classification = NaiveBayes().classify(featureSet, statMap[key]!!)
                //println(it)
                //println("${classification.predictions}")
                println("${featureSet.id}/${featureSet.getClass()} = ${classification.predict()}")
                countTotal++
                if (featureSet.getClass() == classification.predict()) {
                    countCorrect++
                }
                //println()
            }
            println("Totals ${data.samples.size} = ${countCorrect.toDouble()/countTotal*100}%")
        }
        println(statMap)



    }

    private fun pgnToDataSet(name: String): DataSet {

        val mapResult = mapOf("1-0" to 1.0, "0-1" to 2.0, "1/2-1/2" to 0.0)
        val stat = StatEval()
        val pgn = PgnHolder(name)
        pgn.loadPgn()

        println("$name loaded, games: ${pgn.game.size}")
        val list = mutableListOf<FeatureSet>()
        for ((idx, game) in pgn.game.withIndex()) {
            try {
                game.loadMoveText()
                val moves = game.halfMoves
                val board = Board()
                for (move in moves) {
                    board.doMove(move)
                    val featureSet = stat.getFeatureSet(idx, board, mapResult[game.result.description] ?: 3.0)
                    list.add(featureSet)
                    //println("${board.fen}: $featureSet")
                }
                if (idx % 100 == 0) println("$name loaded more 100")
            } catch (e: Exception) {
                e.printStackTrace()
                println(game.toString())
            }

        }
        return DataSet(list, featureNames)
    }

    private fun pgnToDataSetList(name: String): Map<String, DataSet> {

        val mapResult = mapOf("1-0" to 1.0, "0-1" to 2.0, "1/2-1/2" to 0.0)
        val stat = StatEval()
        val pgn = PgnHolder(name)
        val featureMap = mutableMapOf<String, MutableList<FeatureSet>>()

        pgn.loadPgn()

        println("$name loaded, games: ${pgn.game.size}")
        for ((idx, game) in pgn.game.withIndex()) {
            try {
                game.loadMoveText()
                val moves = game.halfMoves
                val board = Board()
                for (move in moves) {
                    board.doMove(move)
                    val featureSet = stat.getFeatureSet(idx, board, mapResult[game.result.description] ?: 3.0)

                    featureMap.computeIfAbsent(dataSetKey(board)) { mutableListOf(featureSet)}
                    featureMap.computeIfPresent(dataSetKey(board)) { _, list ->
                        list.add(featureSet)
                        list
                    }
                    //println("${board.fen}: $featureSet")
                }
                if (idx % 100 == 0) println("$name loaded more 100")
            } catch (e: Exception) {
                e.printStackTrace()
                println(game.toString())
            }

        }
        return featureMap.entries.associateBy({it.key}, {DataSet(it.value, featureNames)})
    }

    private fun dataSetKey(board: Board) =
            (if (board.getBitboard(Piece.WHITE_PAWN) > 0) "WP" else "__") +
                    (if (board.getBitboard(Piece.BLACK_PAWN) > 0) "BP" else "__") +
                    (if (board.sideToMove == Side.WHITE) "W" else "B")

}