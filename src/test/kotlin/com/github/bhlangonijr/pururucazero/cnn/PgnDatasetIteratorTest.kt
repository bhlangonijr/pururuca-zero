package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.pgn.PgnIterator
import com.github.bhlangonijr.pururucazero.cnn.Nd4jDecoder.decode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nd4j.linalg.indexing.NDArrayIndex
import org.nd4j.linalg.string.NDArrayStrings

class PgnDatasetIteratorTest {

    @Test
    fun testPgnLoading() {

        val filename = "src/test/resources/one-win.pgn"
        val batchSize = 32
        val pgnIterator = PgnDatasetIterator(
            pgnFile = filename,
            batchSize = batchSize,
            labelNames = mutableListOf("win prob")
        )

        //val totalGames = PgnIterator(filename).sumOf { 1L }
        val totalMoves = PgnIterator(filename).sumOf { it.halfMoves.size }

        var count = 0
        while (pgnIterator.hasNext()) {
            val dataset = pgnIterator.next()
            count += dataset.features.shape()[0].toInt()
            val rows = dataset.features.size(0)
            assertEquals(batchSize, rows.toInt())
            assertEquals(119, dataset.features.shape()[1])
            assertEquals(8, dataset.features.shape()[2])
            println(dataset.features.shape().toList())
            println(dataset.labels.shape().toList())
            println(dataset.labels)
            for (i in 0 until batchSize) {
                val features = dataset.features.get(NDArrayIndex.point(i.toLong()) ,NDArrayIndex.interval(0, 119))
                //println("Features shape: ${features.shape().toList()}")
                val board = decode(features)
                println("$count-$i: ${board.fen}")
            }
            //println(dataset.features.toString(NDArrayStrings(100000000L, false, 2)))
        }
        assertTrue(count >= totalMoves)
    }

}