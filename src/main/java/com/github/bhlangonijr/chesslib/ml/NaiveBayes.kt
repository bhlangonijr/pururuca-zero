package com.github.bhlangonijr.chesslib.ml

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

class NaiveBayes {

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
            dataSet.featureNames.forEach { name ->
                val mean = dataSet.mean(name) { f -> f.getClass() == s.classId }
                val variance = dataSet.variance(name, mean) { f -> f.getClass() == s.classId }
                s.featureMean[name] = mean
                s.featureVariance[name] = variance
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
            testFeatureSet.featureNameMap.keys
                    .filter { s.isAvailable(it) }
                    .forEach { name ->
                        val result = posterior(testFeatureSet.get(name), s.featureMean[name]!!, s.featureVariance[name]!!)
                        if (!result.isNaN()) {
                            prediction.classLikelihood *= result
                            prediction.featureLikelihood[name] = result
                        } else {
                            println("NaN returned: $name: ${s.featureMean[name]} - ${s.featureVariance[name]}")
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
    val featureMean = mutableMapOf<String, Float>()
    val featureVariance = mutableMapOf<String, Float>()

    fun isAvailable(featureId: String) =
            featureMean[featureId] != null && featureVariance[featureId] != null &&
                    !featureMean[featureId]!!.isNaN() && !featureVariance[featureId]!!.isNaN() &&
                    featureMean[featureId]!! != 0.0f && featureVariance[featureId]!! != 0.0f

    override fun toString(): String {
        return "ClassStats(classId=$classId, count=$count, prior=$prior, featureMean=$featureMean, featureVariance=$featureVariance)\n"
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

    val featureLikelihood = mutableMapOf<String, Float>()
    var classLikelihood: Float = 0.0f
    var probability: Float = 0.0f
    override fun toString(): String {
        return "Prediction(classId=$classId, probability=$probability, classLikelihood=$classLikelihood, featureLikelihood=$featureLikelihood)\n"
    }
}