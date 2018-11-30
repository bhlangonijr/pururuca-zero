package com.github.bhlangonijr.chesslib.ml

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

class NaiveBayes {

    fun train(dataSet: DataSet): Map<Double, ClassStats> {

        val stats = mutableMapOf<Double, ClassStats>()

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
            s.prior = s.count.toDouble() / dataSet.samples.size
        }
        return stats
    }

    fun classify(testFeatureSet: FeatureSet, stats: Map<Double, ClassStats>): Classification {

        val classification = Classification()
        var sum = 0.0
        stats.values.forEach  {s ->
            val prediction = Prediction(s.classId)

            prediction.classLikelihood = s.prior
            testFeatureSet.featureNameMap.keys
                    .filter { s.isAvailable(it) }
                    .forEach { name ->
                        val result = posterior(testFeatureSet.get(name), s.featureMean[name]!! + 0.0000001, s.featureVariance[name]!! + 0.0000001)
                        if (!result.isNaN()) {
                            prediction.classLikelihood *= result
                            prediction.featureLikelihood[name] = result
                        } else {
                            println("NaN returned: $name: ${s.featureMean[name]} - ${s.featureVariance[name]}" )
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

    private fun posterior(feature: Double, mean: Double, variance: Double) =
            (1.0 / sqrt(2.0 * Math.PI * variance)) * exp((-(feature - mean).pow(2.0)) / (2.0 * variance))
}

class ClassStats(val classId: Double) {

    var count = 1
    var prior = 0.0
    val featureMean = mutableMapOf<String, Double>()
    val featureVariance = mutableMapOf<String, Double>()

    fun isAvailable(featureId: String) =
            featureMean[featureId] != null && featureVariance[featureId] != null &&
                    !featureMean[featureId]!!.isNaN() && !featureVariance[featureId]!!.isNaN()

    override fun toString(): String {
        return "ClassStats(classId=$classId, count=$count, prior=$prior, featureMean=$featureMean, featureVariance=$featureVariance)\n"
    }
}

class Classification {

    val predictions = mutableMapOf<Double, Prediction>()
    fun predict() =
            predictions.values.sortedBy { it.probability }.reversed()[0].classId
    override fun toString(): String {
        return "Classification(predictions=$predictions)"
    }
}

class Prediction(val classId: Double) {

    val featureLikelihood = mutableMapOf<String, Double>()
    var classLikelihood: Double = 0.0
    var probability: Double = 0.0
    override fun toString(): String {
        return "Prediction(classId=$classId, probability=$probability, classLikelihood=$classLikelihood, featureLikelihood=$featureLikelihood)\n"
    }
}