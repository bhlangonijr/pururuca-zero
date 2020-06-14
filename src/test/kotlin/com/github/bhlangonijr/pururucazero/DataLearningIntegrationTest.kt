package com.github.bhlangonijr.pururucazero

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.pururucazero.encoder.Matrix.Companion.arrayToCsr
import com.github.bhlangonijr.pururucazero.encoder.PositionStatsEncoder
import com.github.bhlangonijr.pururucazero.ml.NaiveBayes
import com.github.bhlangonijr.pururucazero.ml.PgnConverter.Companion.pgnToDataSet
import org.junit.Ignore
import org.junit.Test

@Ignore
class DataLearningIntegrationTest {

    private val statsEncoder = PositionStatsEncoder()

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
        val features = arrayToCsr(statsEncoder.encode(board))

        val classification = nb.classify(features.first, features.second, stats)

        println(classification.predictions.values.sortedBy { it.probability }.reversed())

    }
}