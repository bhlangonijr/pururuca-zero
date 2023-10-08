package com.github.bhlangonijr.pururucazero.cnn

import org.junit.Test

class CnnTrainerIntegrationTest {

    @Test
    fun testTrainer() {

        val trainDatasetFile = "src/test/resources/pt54.pgn"
        val testDatasetFile = "src/test/resources/test.pgn"

        val batchSize = 32
        val trainPgnIterator = PgnDatasetIterator(
            pgnFile = trainDatasetFile,
            batchSize = batchSize,
            labelNames = mutableListOf("win", "lost", "draw")
        )

        val testPgnIterator = PgnDatasetIterator(
            pgnFile = testDatasetFile,
            batchSize = batchSize,
            labelNames = mutableListOf("win", "lost", "draw")
        )

        val trainer = CnnTrainner("test.zip", null)
        val result = trainer.train(trainPgnIterator, testPgnIterator)

        println(result)

    }

}