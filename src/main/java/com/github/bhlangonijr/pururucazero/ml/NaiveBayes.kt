package com.github.bhlangonijr.pururucazero.ml

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

class NaiveBayes {

    fun train(dataSet: DataSet): DataStats {

        val stats = DataStats()
        return train(stats, dataSet)
    }

    fun train(stats: DataStats, dataSet: DataSet): DataStats {

        return train(stats, dataSet.features, dataSet.labels, dataSet.rowHeaders, dataSet.colIndex)
    }

    fun train(
        features: FloatArray, labels: FloatArray, rowIndex: LongArray,
        colIndex: IntArray
    ): DataStats {

        val stats = DataStats()
        return train(stats, features, labels, rowIndex, colIndex)
    }

    fun train(
        stats: DataStats, features: FloatArray, labels: FloatArray, rowIndex: LongArray,
        colIndex: IntArray
    ): DataStats {

        updateCounters(stats, features, labels, rowIndex, colIndex)
        updateStats(stats, features, labels, rowIndex, colIndex)
        return stats
    }

    fun classify(features: FloatArray, colIndex: IntArray, stats: DataStats): Classification {

        val classification = Classification()
        var sum = 0.0
        stats.getValues().forEach { s ->
            val prediction = Prediction(s.classId)
            prediction.classLikelihood = 0.0
            var logSum = 0.0
            features.withIndex()
                .filter { s.isAvailable(colIndex[it.index]) }
                .forEach {
                    val value = it.value
                    val featureId = colIndex[it.index]
                    val result = posterior(
                        value, s.getFeatureStats(featureId).mean,
                        s.getFeatureStats(featureId).variance
                    )
                    if (!result.isNaN()) {
                        prediction.featureLikelihood[featureId] = result
                        logSum += ln(result.toDouble())
                    } else {
                        println(
                            "NaN returned[$value]: $featureId: ${s.getFeatureStats(featureId).mean} - " +
                                    "${s.getFeatureStats(featureId).variance}"
                        )
                        throw IllegalArgumentException("NaN returned")
                    }
                }
            prediction.classLikelihood = s.prior.toDouble() * exp(logSum)
            classification.predictions[s.classId] = prediction
            sum += prediction.classLikelihood
        }
        classification.predictions.values.forEach {
            it.probability = it.classLikelihood / sum
        }
        return classification
    }

    private fun updateCounters(
        stats: DataStats, features: FloatArray, labels: FloatArray,
        rowIndex: LongArray, colIndex: IntArray
    ) {

        for (classId in labels) {
            stats.count++
            stats.getClassStats(classId).count++
        }

        var rows = 0
        for ((i, v) in features.withIndex()) {
            val featureId = colIndex[i]
            val classId = labels[rows]
            val featureStats = stats.getFeatureStats(classId, featureId)
            featureStats.sum += v
            featureStats.count++
            if (i + 1 >= rowIndex[rows + 1]) {
                rows++
            }
        }
    }

    private fun updateStats(
        stats: DataStats, features: FloatArray, labels: FloatArray,
        rowIndex: LongArray, colIndex: IntArray
    ): DataStats {

        var rows = 0
        for ((i, v) in features.withIndex()) {
            val featureId = colIndex[i]
            val classId = labels[rows]
            val featureStats = stats.getFeatureStats(classId, featureId)
            val mean = featureStats.sum / featureStats.count
            featureStats.sumDeltas += (v - mean).pow(2.0f)
            if (i + 1 >= rowIndex[rows + 1]) {
                rows++
            }
        }

        stats.getValues().forEach { s ->
            for (featureStats in s.getValues()) {
                featureStats.mean = featureStats.sum / featureStats.count
                featureStats.variance = featureStats.sumDeltas / (featureStats.count - 1)
            }
            s.prior = s.count.toFloat() / stats.count
        }
        return stats
    }

    private fun posterior(feature: Float, mean: Float, variance: Float): Float =
        (1.0f / sqrt(2.0f * Math.PI.toFloat() * variance)) *
                exp(-(feature - mean).pow(2.0f) / (2.0f * variance))
}

class Classification {

    val predictions = mutableMapOf<Float, Prediction>()
    fun predict() =
        predictions.values.sortedBy { it.probability }.reversed()[0].classId

    override fun toString(): String {
        return "Classification(predictions=$predictions)"
    }
}

class Prediction(val classId: Float) {

    val featureLikelihood = mutableMapOf<Int, Float>()
    var classLikelihood: Double = 0.0
    var probability: Double = 0.0
    override fun toString(): String {
        return "Prediction(classId=$classId, probability=$probability, " +
                "classLikelihood=$classLikelihood, featureLikelihood=$featureLikelihood)\n"
    }
}
