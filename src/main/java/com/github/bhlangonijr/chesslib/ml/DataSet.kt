package com.github.bhlangonijr.chesslib.ml

import kotlin.math.pow

class DataSet(val samples: List<FeatureSet>, val featureNames: List<String>) {

    fun mean(featureId: String, predicate: (FeatureSet) -> Boolean): Double {

        var sum = 0.0
        var count = 0
        samples.filter { predicate.invoke(it) }.forEach {
            sum += it.get(featureId)
            count++
        }
        return sum / count
    }

    fun variance(featureId: String, mean: Double, predicate: (FeatureSet) -> Boolean): Double {

        var sum = 0.0
        var count = 0
        samples.filter { predicate.invoke(it) }.forEach {
            sum += (it.get(featureId) - mean).pow(2.0)
            count++
        }
        return sum / (count - 1)
    }

    override fun toString(): String {
        return "DataSet(samples=\n$samples, featureNames=$featureNames)"
    }
}

class FeatureSet(val id: Int, val features: List<Double>, val featureNameMap: Map<String, Int>) {

    fun get(featureId: String): Double {
        val idx = featureNameMap[featureId]
        return features[idx!!]
    }

    fun getClass(): Double {
        return features[0]
    }

    override fun toString(): String {
        return "FeatureSet(classId=$id, features=$features, featureNameMap=$featureNameMap)\n"
    }

}
