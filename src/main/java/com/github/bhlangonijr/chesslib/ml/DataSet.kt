package com.github.bhlangonijr.chesslib.ml

import kotlin.math.max
import kotlin.math.pow

class DataSet(val samples: List<FeatureSet>, val featureIds: List<Int>) {

    fun mean(featureId: Int, predicate: (FeatureSet) -> Boolean): Float {

        var sum = 0.0f
        var count = 0
        samples.filter { predicate.invoke(it) }
                .filter { it.exist(featureId) }
                .forEach {
                    sum += it.get(featureId)
                    count++
                }
        return sum / max(1, count)
    }

    fun variance(featureId: Int, mean: Float, predicate: (FeatureSet) -> Boolean): Float {

        var sum = 0.0f
        var count = 0
        samples.filter { predicate.invoke(it) }
                .filter { it.exist(featureId) }
                .forEach {
                    sum += (it.get(featureId) - mean).pow(2.0f)
                    count++
                }
        return sum / max(1, (count - 1))
    }

    override fun toString(): String {
        return "DataSet(samples=\n$samples, featureNames=$featureIds)"
    }
}

class FeatureSet(val id: Int, val features: ArrayList<Float>, val featureIdMap: Map<Int, Int>) {

    fun exist(featureId: Int): Boolean = featureIdMap[featureId] != null

    fun get(featureId: Int): Float {
        val idx = featureIdMap[featureId]
        return features[idx!!]
    }

    fun getClass(): Float {
        return features[0]
    }

    override fun toString(): String {
        return "FeatureSet(id=$id, features=$features, featureNameMap=$featureIdMap)\n"
    }

}