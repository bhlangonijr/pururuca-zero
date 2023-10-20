package com.github.bhlangonijr.pururucazero.cnn

import org.junit.Test

class CnnTrainerIntegrationTest {

    @Test
    fun testTrainer() {

        val trainDatasetFile = "src/test/resources/Stockfish_DD_64-bit_4CPU.pgn"
        val testDatasetFile = "src/test/resources/pt54.pgn"

        val batchSize = 64
        val trainPgnIterator = PgnDatasetIterator(
            pgnFile = trainDatasetFile,
            batchSize = batchSize,
            labelNames = mutableListOf("win")
        )

        val testPgnIterator = PgnDatasetIterator(
            pgnFile = testDatasetFile,
            batchSize = batchSize,
            labelNames = mutableListOf("win")
        )

        val trainer = CnnTrainer("test.zip", null)
        val result = trainer.train(trainPgnIterator, testPgnIterator)

        println(result)

    }

}