package com.github.bhlangonijr.pururucazero.ml

import kotlin.math.*

class NaiveBayes {

    fun train(dataSet: DataSet): Map<Float, ClassStats> {

        return train(dataSet.features, dataSet.labels, dataSet.rowHeaders, dataSet.colIndex)
    }

    fun train(features: FloatArray, labels: FloatArray, rowIndex: LongArray, colIndex: IntArray): Map<Float, ClassStats> {

        val stats = mutableMapOf<Float, ClassStats>()

        for (n in labels) {
            stats.compute(n) { _, s ->
                if (s == null) {
                    ClassStats(n)
                } else {
                    s.count++
                    s
                }
            }
        }

        var rows = 0
        for ((i, v) in features.withIndex()) {
            val featureId = colIndex[i]
            val classId = labels[rows]
            stats.computeIfPresent(classId) { _, classStat ->
                classStat.featureStats.compute(featureId) { _, stat ->
                    val feat = stat ?: FeatureStats(featureId)
                    feat.sum += v
                    feat.count++
                    feat
                }
                classStat
            }
            if (i + 1 > rowIndex[rows + 1]) {
                rows++
            }
        }

        rows = 0
        for ((i, v) in features.withIndex()) {
            val featureId = colIndex[i]
            val classId = labels[rows]
            stats.computeIfPresent(classId) { _, classStat ->
                classStat.featureStats.computeIfPresent(featureId) { _, feat ->
                    feat.mean = feat.sum / feat.count
                    feat.sumDeltas += (v - feat.mean).pow(2.0f)
                    feat
                }
                classStat
            }
            if (i + 1 > rowIndex[rows + 1]) {
                rows++
            }
        }

        stats.values.forEach { s ->
            for (featStats in s.featureStats.values) {
                val id = featStats.featureId
                val mean = featStats.sum / featStats.count
                val variance = featStats.sumDeltas / max(1, (featStats.count - 1))
                s.featureStats[id]!!.mean = mean
                s.featureStats[id]!!.variance = variance
            }
            s.prior = s.count.toFloat() / rows
        }
        return stats
    }

    fun classify(features: FloatArray, colIndex: IntArray, stats: Map<Float, ClassStats>): Classification {

        val classification = Classification()
        var sum = 0.0
        stats.values.forEach { s ->
            val prediction = Prediction(s.classId)
            prediction.classLikelihood = 0.0
            var logSum = 0.0
            features.withIndex()
                    .filter { s.isAvailable(colIndex[it.index]) }
                    .forEach {
                        val value = it.value
                        val featureId = colIndex[it.index]
                        val result = posterior(value, s.featureMean(featureId), s.featureVariance(featureId))
                        if (!result.isNaN()) {
                            prediction.featureLikelihood[featureId] = result
                            logSum += ln(result.toDouble())
                        } else {
                            println("NaN returned: $featureId: ${s.featureMean(featureId)} - ${s.featureVariance(featureId)}")
                        }
                    }
            prediction.classLikelihood = s.prior.toDouble() * Math.exp(logSum)
            classification.predictions[s.classId] = prediction
            sum += prediction.classLikelihood
        }
        classification.predictions.values.forEach {
            it.probability = it.classLikelihood / sum
        }
        return classification
    }


    private fun posterior(feature: Float, mean: Float, variance: Float): Float =
            (1.0f / sqrt(2.0f * Math.PI.toFloat() * variance)) * exp(-(feature - mean).pow(2.0f) / (2.0f * variance))
}

class ClassStats(val classId: Float) {

    var count = 1
    var prior = 0.0f
    val featureStats = mutableMapOf<Int, FeatureStats>()

    fun featureMean(featureId: Int) =
            featureStats[featureId]!!.mean

    fun featureVariance(featureId: Int) =
            featureStats[featureId]!!.variance

    fun isAvailable(featureId: Int) =
            featureStats[featureId] != null &&
                    !featureMean(featureId).isNaN() && !featureVariance(featureId).isNaN() &&
                    featureMean(featureId) != 0.0f && featureVariance(featureId) != 0.0f

    override fun toString(): String {
        return "ClassStats(classId=$classId, count=$count, prior=$prior, featureStats=$featureStats)"
    }

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

class FeatureStats(val featureId: Int) {

    var count = 0
    var sum = 0.0f
    var sumDeltas = 0.0f
    var mean = 0.0f
    var variance = 0.0f
    override fun toString(): String {
        return "FeatureStats(featureId=$featureId, count=$count, sum=$sum, sumDeltas=$sumDeltas, " +
                "mean=$mean, variance=$variance)"
    }

}