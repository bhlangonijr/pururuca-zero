package com.github.bhlangonijr.chesslib.ml

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class NaiveBayesTest {

    @Test
    fun testNaiveBayes() {

        val featureNames = mapOf("height" to 1, "weight" to 2, "foot size" to 3)
        val data = DataSet(
                arrayListOf(
                        FeatureSet(1, arrayListOf(0.0, 6.0, 180.0, 12.0), featureNames),
                        FeatureSet(2, arrayListOf(0.0, 5.92, 190.0, 11.0), featureNames),
                        FeatureSet(3, arrayListOf(0.0, 5.58, 170.0, 12.0), featureNames),
                        FeatureSet(4, arrayListOf(0.0, 5.92, 165.0, 10.0), featureNames),
                        FeatureSet(5, arrayListOf(1.0, 5.0, 100.0, 6.0), featureNames),
                        FeatureSet(6, arrayListOf(1.0, 5.5, 150.0, 8.0), featureNames),
                        FeatureSet(7, arrayListOf(1.0, 5.42, 130.0, 7.0), featureNames),
                        FeatureSet(8, arrayListOf(1.0, 5.75, 150.0, 9.0), featureNames)
                ),
                featureNames.keys.toList()
        )
        val nb = NaiveBayes()

        val test = FeatureSet(1, arrayListOf(-1.0, 6.0, 130.0, 8.0), featureNames)

        val stats = nb.train(data)

        val classification = nb.classify(test, stats)

        assertTrue(classification.predictions.values.sortedBy { it.probability }.reversed()[0].probability > 0.99)
        assertEquals(1.0, classification.predict())
    }
}



