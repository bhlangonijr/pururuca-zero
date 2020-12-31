package com.github.bhlangonijr.pururucazero.ml

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class NaiveBayesTest {

    @Test
    fun testNaiveBayesCsr() {

        println("Naive Bayes Training")

        val data = arrayListOf(
            6.0f, 180.0f, 12.0f,
            5.92f, 190.0f, 11.0f,
            5.58f, 170.0f, 12.0f,
            5.92f, 165.0f, 10.0f,
            5.0f, 100.0f, 6.0f,
            5.5f, 150.0f, 8.0f,
            5.42f, 130.0f, 7.0f,
            5.75f, 150.0f, 9.0f
        ).toFloatArray()

        val test = arrayListOf(6.0f, 130.0f, 8.0f).toFloatArray()
        val testColIndex = intArrayOf(0, 1, 2)

        val labels = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f)
        val colIndex = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2)
        val rowIndex = longArrayOf(0, 3, 6, 9, 12, 15, 18, 21, 24)

        val nb = NaiveBayes()

        val stats = nb.train(data, labels, rowIndex, colIndex)
        println(stats)
        val classification = nb.classify(test, testColIndex, stats)

        println(classification.predictions.values.sortedBy { it.probability }.reversed())

        assertTrue(classification.predictions.values.sortedBy { it.probability }.reversed()[0].probability > 0.99)
        assertEquals(1.0f, classification.predict())
    }

    @Test
    fun testBatchNaiveBayesCsr() {

        println("Batch Naive Bayes Training")
        val data1 = arrayListOf(
            6.0f, 180.0f, 12.0f,
            5.58f, 170.0f, 12.0f,
            5.0f, 100.0f, 6.0f,
            5.42f, 130.0f, 7.0f
        ).toFloatArray()

        val data2 = arrayListOf(
            5.92f, 190.0f, 11.0f,
            5.92f, 165.0f, 10.0f,
            5.5f, 150.0f, 8.0f,
            5.75f, 150.0f, 9.0f
        ).toFloatArray()


        val test = arrayListOf(6.0f, 130.0f, 8.0f).toFloatArray()
        val testColIndex = intArrayOf(0, 1, 2)

        val labels1 = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
        val colIndex1 = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2)
        val rowIndex1 = longArrayOf(0, 3, 6, 9, 12)

        val labels2 = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
        val colIndex2 = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2)
        val rowIndex2 = longArrayOf(0, 3, 6, 9, 12)

        val stats1 = DataStats()
        val stats2 = DataStats()
        val nb = NaiveBayes()

        nb.train(stats1, data1, labels1, rowIndex1, colIndex1)
        nb.train(stats2, data2, labels2, rowIndex2, colIndex2)

        val stats = stats1.merge(stats2)
        println(stats)
        val classification = nb.classify(test, testColIndex, stats)

        println(classification.predictions.values.sortedBy { it.probability }.reversed())

        assertTrue(classification.predictions.values.sortedBy { it.probability }.reversed()[0].probability > 0.99)
        assertEquals(1.0f, classification.predict())
    }
}