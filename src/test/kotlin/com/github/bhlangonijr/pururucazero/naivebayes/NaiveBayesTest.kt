package com.github.bhlangonijr.pururucazero.naivebayes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaiveBayesTest {

    @Test
    fun testNaiveBayesCsr() {

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

        val labelDescriptions = mapOf(Pair(0.0f, "male"), Pair(1.0f, "female"))
        val test = arrayListOf(6.0f, 130.0f, 8.0f).toFloatArray()
        val testColIndex = intArrayOf(0, 1, 2)

        val labels = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f)
        val colIndex = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2)
        val rowIndex = longArrayOf(0, 3, 6, 9, 12, 15, 18, 21, 24)

        val nb = NaiveBayes()

        val stats = nb.train(data, labels, rowIndex, colIndex, labelDescriptions)
        val classification = nb.classify(test, testColIndex, stats)

        println(classification.predictions.values.sortedBy { it.probability }.reversed())
        assertTrue(classification.predictions.values.sortedBy { it.probability }.reversed()[0].probability > 0.70)
        assertEquals(1.0f, classification.predict())
    }

    @Test
    fun testBatchNaiveBayesCsr() {

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

        val labelDescriptions = mapOf(Pair(0.0f, "male"), Pair(1.0f, "female"))
        val test = arrayListOf(6.0f, 130.0f, 8.0f).toFloatArray()
        val testColIndex = intArrayOf(0, 1, 2)

        val labels1 = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
        val colIndex1 = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2)
        val rowIndex1 = longArrayOf(0, 3, 6, 9, 12)

        val labels2 = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
        val colIndex2 = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2)
        val rowIndex2 = longArrayOf(0, 3, 6, 9, 12)

        val stats1 = DataStats(labels = labels1, labelDescriptions = labelDescriptions)
        val stats2 = DataStats(labels = labels2, labelDescriptions = labelDescriptions)
        val nb = NaiveBayes()

        nb.train(stats1, data1, labels1, rowIndex1, colIndex1)
        nb.train(stats2, data2, labels2, rowIndex2, colIndex2)

        val stats = stats1.merge(stats2)
        println(stats)
        val classification = nb.classify(test, testColIndex, stats)

        println(classification.predictions.values.sortedBy { it.probability }.reversed())

        assertTrue(classification.predictions.values.sortedBy { it.probability }.reversed()[0].probability > 0.7)
        assertEquals(1.0f, classification.predict())
    }

    @Test
    fun testNaiveBayesFeiosoCsr() {

        val data = arrayListOf(
            8f,3f,2f,
            2f,2f,2f,
            2f,1f,2f,
            1f,4f,2f,
            8f,3f,1f,
            4f,2f,1f,
            2f,1f,1f,
            1f,4f,1f,
            6f,1f,2f,
            2f,2f,2f,
            1f,3f,2f
        ).toFloatArray()

        val labelDescriptions = mapOf(Pair(0.0f, "home"), Pair(1.0f, "work"), Pair(2.0f, "other"))

        val test = arrayListOf(10f, 3f, 2f).toFloatArray()
        val testColIndex = intArrayOf(0, 1, 2)

        val labels = floatArrayOf(0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 2.0f, 2.0f)
        val colIndex = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1,
            2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2)
        val rowIndex = longArrayOf(0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33)

        val nb = NaiveBayes()

        val stats = nb.train(data, labels, rowIndex, colIndex, labelDescriptions)

        println(stats)
        val classification = nb.classify(test, testColIndex, stats)

        println(classification.predictions.values.sortedBy { it.probability }.reversed())
        //assertTrue(classification.predictions.values.sortedBy { it.probability }.reversed()[0].probability > 0.70)
        //assertEquals(1.0f, classification.predict())
        println("${labelDescriptions[classification.predict()]}")
    }
}