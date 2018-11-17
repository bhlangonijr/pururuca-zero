package com.github.bhlangonijr.chesslib.ml

import junit.framework.TestCase.assertEquals
import org.junit.Test

class DataSetTest {

    @Test
    fun testDataSetCreationAndCalculations() {

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

        val maleHeightMean = data.mean("height") { it.getClass() == 0.0 }
        val maleWeightMean = data.mean("weight") { it.getClass() == 0.0 }
        val maleFootSizeMean = data.mean("foot size") { it.getClass() == 0.0 }

        val maleHeightVariance = data.variance("height", maleHeightMean) { it.getClass() == 0.0 }
        val maleWeightVariance = data.variance("weight", maleWeightMean) { it.getClass() == 0.0 }
        val maleFootSizeVariance = data.variance("foot size", maleFootSizeMean) { it.getClass() == 0.0 }

        val femaleHeightMean = data.mean("height") { it.getClass() == 1.0 }
        val femaleWeightMean = data.mean("weight") { it.getClass() == 1.0 }
        val femaleFootSizeMean = data.mean("foot size") { it.getClass() == 1.0 }

        val femaleHeightVariance = data.variance("height", femaleHeightMean) { it.getClass() == 1.0 }
        val femaleWeightVariance = data.variance("weight", femaleWeightMean) { it.getClass() == 1.0 }
        val femaleFootSizeVariance = data.variance("foot size", femaleFootSizeMean) { it.getClass() == 1.0 }

        println("male mean:     $maleHeightMean, $maleWeightMean, $maleFootSizeMean ")
        println("male variance: $maleHeightVariance, $maleWeightVariance, $maleFootSizeVariance ")

        println("female mean:     $femaleHeightMean, $femaleWeightMean, $femaleFootSizeMean ")
        println("female variance: $femaleHeightVariance, $femaleWeightVariance, $femaleFootSizeVariance ")

        assertEquals(5.855, maleHeightMean)
        assertEquals(176.25, maleWeightMean)
        assertEquals(11.25, maleFootSizeMean)
        assertEquals(0.03503333333333331, maleHeightVariance)
        assertEquals(122.91666666666667, maleWeightVariance)
        assertEquals(0.9166666666666666, maleFootSizeVariance)

        assertEquals(5.4175, femaleHeightMean)
        assertEquals(132.5, femaleWeightMean)
        assertEquals(7.5, femaleFootSizeMean)
        assertEquals(0.097225, femaleHeightVariance)
        assertEquals(558.3333333333334, femaleWeightVariance)
        assertEquals(1.6666666666666667, femaleFootSizeVariance)

    }


}



