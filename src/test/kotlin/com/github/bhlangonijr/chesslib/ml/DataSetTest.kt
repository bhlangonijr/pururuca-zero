package com.github.bhlangonijr.chesslib.ml

import junit.framework.TestCase.assertEquals
import org.junit.Test

class DataSetTest {

    @Test
    fun testDataSetCreationAndCalculations() {

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

        val maleHeightMean = data.mean("height") { it.getClass() == 0.0f }
        val maleWeightMean = data.mean("weight") { it.getClass() == 0.0f }
        val maleFootSizeMean = data.mean("foot size") { it.getClass() == 0.0f }

        val maleHeightVariance = data.variance("height", maleHeightMean) { it.getClass() == 0.0f }
        val maleWeightVariance = data.variance("weight", maleWeightMean) { it.getClass() == 0.0f }
        val maleFootSizeVariance = data.variance("foot size", maleFootSizeMean) { it.getClass() == 0.0f }

        val femaleHeightMean = data.mean("height") { it.getClass() == 1.0f }
        val femaleWeightMean = data.mean("weight") { it.getClass() == 1.0f }
        val femaleFootSizeMean = data.mean("foot size") { it.getClass() == 1.0f }

        val femaleHeightVariance = data.variance("height", femaleHeightMean) { it.getClass() == 1.0f }
        val femaleWeightVariance = data.variance("weight", femaleWeightMean) { it.getClass() == 1.0f }
        val femaleFootSizeVariance = data.variance("foot size", femaleFootSizeMean) { it.getClass() == 1.0f }

        println("male mean:     $maleHeightMean, $maleWeightMean, $maleFootSizeMean ")
        println("male variance: $maleHeightVariance, $maleWeightVariance, $maleFootSizeVariance ")

        println("female mean:     $femaleHeightMean, $femaleWeightMean, $femaleFootSizeMean ")
        println("female variance: $femaleHeightVariance, $femaleWeightVariance, $femaleFootSizeVariance ")

        assertEquals(5.855f, maleHeightMean)
        assertEquals(176.25f, maleWeightMean)
        assertEquals(11.25f, maleFootSizeMean)
        assertEquals(0.035033356f, maleHeightVariance)
        assertEquals(122.91666666666667f, maleWeightVariance)
        assertEquals(0.9166666666666666f, maleFootSizeVariance)

        assertEquals(5.4175f, femaleHeightMean)
        assertEquals(132.5f, femaleWeightMean)
        assertEquals(7.5f, femaleFootSizeMean)
        assertEquals(0.09722499f, femaleHeightVariance)
        assertEquals(558.3333333333334f, femaleWeightVariance)
        assertEquals(1.6666666666666667f, femaleFootSizeVariance)

    }


}



