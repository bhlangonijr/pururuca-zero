package com.github.bhlangonijr.chesslib.ml

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class NaiveBayesTest {

    @Test
    fun testNaiveBayes() {

        val featureNames = mapOf(0 to 1, 1 to 2, 2 to 3)
        val data = DataSet(
                arrayListOf(
                        FeatureSet(1, arrayListOf(0.0f, 6.0f, 180.0f, 12.0f), featureNames),
                        FeatureSet(2, arrayListOf(0.0f, 5.92f, 190.0f, 11.0f), featureNames),
                        FeatureSet(3, arrayListOf(0.0f, 5.58f, 170.0f, 12.0f), featureNames),
                        FeatureSet(4, arrayListOf(0.0f, 5.92f, 165.0f, 10.0f), featureNames),
                        FeatureSet(5, arrayListOf(1.0f, 5.0f, 100.0f, 6.0f), featureNames),
                        FeatureSet(6, arrayListOf(1.0f, 5.5f, 150.0f, 8.0f), featureNames),
                        FeatureSet(7, arrayListOf(1.0f, 5.42f, 130.0f, 7.0f), featureNames),
                        FeatureSet(8, arrayListOf(1.0f, 5.75f, 150.0f, 9.0f), featureNames)
                ),
                featureNames.keys.toList()
        )
        val nb = NaiveBayes()

        val test = FeatureSet(1, arrayListOf(-1.0f, 6.0f, 130.0f, 8.0f), featureNames)

        val stats = nb.train(data)

        val classification = nb.classify(test, stats)

        assertTrue(classification.predictions.values.sortedBy { it.probability }.reversed()[0].probability > 0.99)
        assertEquals(1.0f, classification.predict())
    }

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
                5.75f, 150.0f, 9.0f).toFloatArray()

        val test = arrayListOf(6.0f, 130.0f, 8.0f).toFloatArray()
        val testColIndex = intArrayOf(0, 1, 2)

        val labels = intArrayOf(0, 0, 0, 0, 1, 1, 1, 1)
        val colIndex = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2)
        val rowIndex = intArrayOf(3, 6, 9, 12, 15, 18, 21, 24)

        val nb = NaiveBayes()

        val stats = nb.train(data, colIndex, rowIndex, labels)

        val classification = nb.classify(test, testColIndex, stats)

        println(classification.predictions.values.sortedBy { it.probability }.reversed())

        assertTrue(classification.predictions.values.sortedBy { it.probability }.reversed()[0].probability > 0.99)
        assertEquals(1.0f, classification.predict())
    }
}



