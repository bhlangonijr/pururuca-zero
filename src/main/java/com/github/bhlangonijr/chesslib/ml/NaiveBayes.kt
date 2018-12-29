package com.github.bhlangonijr.chesslib.ml

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class NaiveBayes {

    fun train(features: FloatArray, colIndex: IntArray, rowIndex: IntArray, labels: IntArray): Map<Float, ClassStats> {

        val stats = mutableMapOf<Float, ClassStats>()

        for(n in labels) {
            stats.compute(n.toFloat()) { _, s ->
                if (s == null) {
                    ClassStats(n.toFloat())
                } else {
                    s.count++
                    s
                }
            }
        }

        var r = 0
        var rows = 0
        for ((i, v) in features.withIndex()) {
            val featureId = colIndex[i]
            val classId = labels[rows].toFloat()
            stats.computeIfPresent(classId) { _, classStat ->
                classStat.featureStats.compute(featureId) { _, stat ->
                    val feat = stat ?: FeatureStats(featureId)
                    feat.sum += v
                    feat.count++
                    feat
                }
                classStat
            }
            r++
            if (r > rowIndex[rows]) {
                rows++
            }
        }

        r = 0
        rows = 0
        for ((i, v) in features.withIndex()) {
            val featureId = colIndex[i]
            val classId = labels[rows].toFloat()
            stats.computeIfPresent(classId) { _, classStat ->
                classStat.featureStats.computeIfPresent(featureId) { _, feat ->
                    feat.mean = feat.sum / feat.count
                    feat.sumDeltas += (v - feat.mean).pow(2.0f)
                    feat
                }
                classStat
            }
            r++
            if (r > rowIndex[rows]) {
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

    fun train(dataSet: DataSet): Map<Float, ClassStats> {

        val stats = mutableMapOf<Float, ClassStats>()

        dataSet.samples.forEachIndexed { _, feat ->
            stats.compute(feat.getClass()) { _, stats ->
                if (stats == null) {
                    ClassStats(feat.getClass())
                } else {
                    stats.count++
                    stats
                }
            }
        }

        stats.values.forEach { s ->
            dataSet.featureIds.forEach { id ->
                val mean = dataSet.mean(id) { f -> f.getClass() == s.classId }
                val variance = dataSet.variance(id, mean) { f -> f.getClass() == s.classId }
                s.featureStats[id]!!.mean = mean
                s.featureStats[id]!!.variance = variance
            }
            s.prior = s.count.toFloat() / dataSet.samples.size
        }
        return stats
    }

    fun classify(testFeatureSet: FeatureSet, stats: Map<Float, ClassStats>): Classification {

        val classification = Classification()
        var sum = 0.0f
        stats.values.forEach { s ->
            val prediction = Prediction(s.classId)

            prediction.classLikelihood = s.prior
            testFeatureSet.featureIdMap.keys
                    .filter { s.isAvailable(it) }
                    .forEach { name ->
                        val result = posterior(testFeatureSet.get(name), s.featureMean(name), s.featureVariance(name))
                        if (!result.isNaN()) {
                            prediction.classLikelihood *= result
                            prediction.featureLikelihood[name] = result
                        } else {
                            println("NaN returned: $name: ${s.featureMean(name)} - ${s.featureVariance(name)}")
                        }
                    }
            classification.predictions[s.classId] = prediction
            sum += prediction.classLikelihood
        }
        classification.predictions.values.forEach {
            it.probability = it.classLikelihood / sum
        }
        return classification
    }

    fun classify(features: FloatArray, colIndex: IntArray, stats: Map<Float, ClassStats>): Classification {

        val classification = Classification()
        var sum = 0.0f
        stats.values.forEach { s ->
            val prediction = Prediction(s.classId)

            prediction.classLikelihood = s.prior

            features.withIndex()
                    .filter { s.isAvailable(colIndex[it.index]) }
                    .forEach {
                        val value = it.value
                        val featureId = colIndex[it.index]
                        val result = posterior(value, s.featureMean(featureId), s.featureVariance(featureId))
                        if (!result.isNaN()) {
                            prediction.classLikelihood *= result
                            prediction.featureLikelihood[featureId] = result
                        } else {
                            println("NaN returned: $featureId: ${s.featureMean(featureId)} - ${s.featureVariance(featureId)}")
                        }
                    }
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
    var classLikelihood: Float = 0.0f
    var probability: Float = 0.0f
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
}