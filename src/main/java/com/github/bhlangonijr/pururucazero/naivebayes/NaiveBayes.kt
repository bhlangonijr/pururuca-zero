package com.github.bhlangonijr.pururucazero.naivebayes

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

class NaiveBayes {

    fun train(dataSet: DataSet): DataStats {

        val stats = DataStats(labels = dataSet.labels, labelDescriptions = dataSet.labelDescriptions)
        return train(stats, dataSet)
    }

    fun train(
        stats: DataStats,
        dataSet: DataSet
    ): DataStats {

        return train(stats, dataSet.features, dataSet.labels, dataSet.rowHeaders, dataSet.colIndex)
    }

    fun train(
        features: FloatArray,
        labels: FloatArray,
        rowIndex: LongArray,
        colIndex: IntArray,
        labelDescriptions: Map<Float, String>
    ): DataStats {

        val stats = DataStats(labels = labels, labelDescriptions = labelDescriptions)
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

    fun classify(
        dataSet: DataSet,
        stats: DataStats,
        completeClassification: Boolean
    ): BatchClassification {

        val features = mutableListOf<Float>()
        val classifications = FloatArray(dataSet.labels.size)
        val fullResult = mutableListOf<Classification>()
        var rows = 0
        for ((i, v) in dataSet.features.withIndex()) {
            features.add(v)
            if (i + 1 >= dataSet.rowHeaders[rows + 1]) {
                val samples = features.toFloatArray()
                val classification = classify(samples, dataSet.colIndex, stats)
                classifications[rows] = classification.predict()
                if (completeClassification) {
                    fullResult.add(classification)
                }
                rows++
                features.clear()
            }
        }
        return BatchClassification(dataSet.labels, classifications, fullResult)
    }

    fun classify(features: FloatArray, colIndex: IntArray, stats: DataStats): Classification {

        val classification = Classification()
        val logSum = mutableListOf<Double>()
        stats.getValues().forEach { s ->
            val prediction = Prediction(s.classId)
            prediction.classLikelihood = 0.0
            val values = features.withIndex()
                .filter { s.isAvailable(colIndex[it.index]) }
                .map {
                    val value = it.value
                    val featureId = colIndex[it.index]
                    val result = logPosterior(
                        value, s.getFeatureStats(featureId).mean,
                        s.getFeatureStats(featureId).variance
                    )
                    prediction.featureLikelihood[featureId] = exp(result)
                    result
                }

            prediction.classLikelihood = ln(s.prior.toDouble()) + values.sum()
            classification.predictions[s.classId] = prediction
            logSum.add(prediction.classLikelihood)
        }
        classification.predictions.values.forEach {
            it.probability = exp(it.classLikelihood - logSumExp(logSum))
        }
        return classification
    }

    private fun logSumExp(values: List<Double>): Double {

        val maxValue = values.maxOrNull() ?: 0.0
        return maxValue + ln(values.sumOf { exp(it - maxValue) })
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

        stats.getValues().forEachIndexed { _, s ->
            for (featureStats in s.getValues()) {
                featureStats.mean = featureStats.sum / featureStats.count
                featureStats.variance = featureStats.sumDeltas / (featureStats.count - 1)
            }
            s.prior = (s.count.toFloat()) / (stats.count)
        }
        return stats
    }

    private fun posterior(feature: Float, mean: Float, variance: Float): Double =
        (1.0 / sqrt(2.0 * Math.PI * variance)) *
                exp(-(feature - mean).pow(2.0f) / (2.0 * variance))

    private fun logPosterior(feature: Float, mean: Float, variance: Float): Double =
        -0.5 * ln(2.0 * Math.PI * ni(variance)) - ((feature.toDouble() - mean).pow(2.0)) / (2.0 * ni(variance))

    private fun ni(v: Float) = if (v == 0f) Float.MIN_VALUE else v
}