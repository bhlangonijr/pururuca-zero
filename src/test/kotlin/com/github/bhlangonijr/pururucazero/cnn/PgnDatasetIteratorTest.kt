package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.pgn.PgnIterator
import org.junit.Test

class PgnDatasetIteratorTest {

    @Test
    fun testPgnLoading() {

        val filename = "src/test/resources/Stockfish_DD_64-bit_4CPU.pgn"

        val pgnIterator = PgnDatasetIterator(
            pgnFile = filename,
            batchSize = 32,
            labelNames = mutableListOf("win", "lost", "draw")
        )

        val totalGames = PgnIterator(filename).sumOf { 1L }
        val totalMoves = PgnIterator(filename).sumOf { it.halfMoves.size }

        println(totalGames)
        println(totalMoves)

        var count = 0
        while (pgnIterator.hasNext()) {
            val dataset = pgnIterator.next()
            println(dataset.features.shape().contentToString())
            count += pgnIterator.batchSize
        }
        println(count)

    }

}