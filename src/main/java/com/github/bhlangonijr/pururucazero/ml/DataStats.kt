package com.github.bhlangonijr.pururucazero.ml

import kotlin.math.pow

class DataStats {

    var count = 0
    private val classStatsMap = mutableMapOf<Float, ClassStats>()

    fun getClassStats(classId: Float): ClassStats =
        classStatsMap.computeIfAbsent(classId) { ClassStats(classId) }

    fun getFeatureStats(classId: Float, featureId: Int): FeatureStats =
        getClassStats(classId).getFeatureStats(featureId)

    fun getValues() = classStatsMap.values

    fun merge(dataStats: DataStats): DataStats {

        val newStats = DataStats()
        newStats.count = this.count + dataStats.count
        this.getValues().forEach { classStats ->

            val mergingClassStats = dataStats.getClassStats(classStats.classId)
            val newClassStats = newStats.getClassStats(classStats.classId)
            newClassStats.count = classStats.count + mergingClassStats.count
            newClassStats.prior = newClassStats.count.toFloat() / newStats.count

            classStats.getValues().forEach { featureStats ->

                val mergingFeatureStats = dataStats
                    .getFeatureStats(classStats.classId, featureStats.featureId)
                val newFeatureStats = newClassStats.getFeatureStats(featureStats.featureId)
                newFeatureStats.count = featureStats.count + mergingFeatureStats.count
                newFeatureStats.sum = featureStats.sum + mergingFeatureStats.sum

                newFeatureStats.mean = combinedMean(
                    featureStats.count, featureStats.mean,
                    mergingFeatureStats.count, mergingFeatureStats.mean
                )

                newFeatureStats.variance = combinedVariance(
                    featureStats.count, featureStats.mean,
                    featureStats.variance, mergingFeatureStats.count, mergingFeatureStats.mean,
                    mergingFeatureStats.variance, newFeatureStats.mean
                )

            }
        }
        return newStats
    }

    private fun combinedMean(size1: Int, mean1: Float, size2: Int, mean2: Float): Float =
        (size1 * mean1 + size2 * mean2) / (size1 + size2)

    private fun combinedVariance(
        size1: Int, mean1: Float, variance1: Float,
        size2: Int, mean2: Float, variance2: Float, mean3: Float
    ): Float =
        (size1 * (variance1 + (mean1 - mean3).pow(2.0f)) + size2 * (variance2 + (mean2 - mean3).pow(2.0f))) /
                (size1 + size2)

    override fun toString(): String {
        return "DataStats(count=$count, \nclassStatsMap=$classStatsMap)\n"
    }

}

class ClassStats(val classId: Float) {

    var count = 0
    var prior = 0.0f
    private val featureStatsMap = mutableMapOf<Int, FeatureStats>()

    fun getFeatureStats(featureId: Int): FeatureStats =
        featureStatsMap.computeIfAbsent(featureId) { FeatureStats(featureId) }

    fun getValues() = featureStatsMap.values

    fun isAvailable(featureId: Int) = featureStatsMap[featureId] != null

    override fun toString(): String {
        return "ClassStats(classId=$classId, count=$count, prior=$prior, \nfeatureStats=\n $featureStatsMap)\n"
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
                "mean=$mean, variance=$variance)\n"
    }

}