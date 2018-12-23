package com.github.bhlangonijr.chesslib.ml

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class NaiveBayesTest {

    @Test
    fun testNaiveBayes() {

        val featureNames = mapOf("height" to 1, "weight" to 2, "foot size" to 3)
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
}



