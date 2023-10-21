package com.github.bhlangonijr.pururucazero.cnn

import org.junit.Test

class CnnTrainerIntegrationTest {

    @Test
    fun testTrainer() {

        val trainDatasetFile = "src/test/resources/pt54.pgn"
        val testDatasetFile = "src/test/resources/pt54.pgn"

        val batchSize = 32
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

        val trainer = CnnTrainer("test.zip")
        val result = trainer.train(trainPgnIterator, testPgnIterator, epochs = 3)

        testPgnIterator.labels
        println(result)

    }

    @Test
    fun testResNetTrainer() {

        val trainDatasetFile = "src/test/resources/pt54.pgn"
        val testDatasetFile = "src/test/resources/pt54.pgn"

        val batchSize = 32
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

        val trainer = ResNetTrainer("test.zip")
        val result = trainer.train(trainPgnIterator, testPgnIterator, epochs = 3)

        testPgnIterator.labels
        println(result)

    }
    @Test
    fun testIterators() {

        val testDatasetFile = "src/test/resources/pt53.pgn"

        val batchSize = 64

        val pgnIterator = PgnDatasetIterator(
            pgnFile = testDatasetFile,
            batchSize = batchSize,
            labelNames = mutableListOf("win")
        )


        val dataset = pgnIterator.next()
        println(dataset.labels.shape().toList())
        println(dataset.labels.toString())

    }


}