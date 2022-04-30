package com.github.bhlangonijr.pururucazero.ml

class BatchClassification(
    private val labels: FloatArray,
    val classifications: FloatArray,
    val fullResults: MutableList<Classification>
) {

    private val classes = labels.distinct()
    fun accuracy(): Float {
        return (classes.sumOf { accuracy(it).toDouble() } / classes.size).toFloat()
    }

    fun precision(): Float {
        return (classes.sumOf { precision(it).toDouble() } / classes.size).toFloat()
    }

    fun recall(): Float {
        return (classes.sumOf { recall(it).toDouble() } / classes.size).toFloat()
    }

    fun f1Score(): Float {
        return 2 * (precision() * recall() / (precision() + recall()))
    }

    fun accuracy(classId: Float): Float {

        var tp = 0f
        var tn = 0f
        var fp = 0f
        var fn = 0f
        labels.forEachIndexed { index, label ->
            val predicted = classifications[index]
            when {
                label == classId && predicted == classId -> tp++
                label != classId && predicted != classId -> tn++
                label != classId && predicted == classId -> fp++
                label == classId && predicted != classId -> fn++
            }
        }
        return (tp + tn) / (tp + tn + fp + fn)
    }

    fun precision(classId: Float): Float {

        var tp = 0f
        var fp = 0f
        labels.forEachIndexed { index, label ->
            val predicted = classifications[index]
            tp += if (label == classId && predicted == classId) 1 else 0
            fp += if (label != classId && predicted == classId) 1 else 0
        }
        return tp / (tp + fp)
    }

    fun recall(classId: Float): Float {

        var tp = 0f
        var fn = 0f
        labels.forEachIndexed { index, label ->
            val predicted = classifications[index]
            tp += if (label == classId && predicted == classId) 1 else 0
            fn += if (label == classId && predicted != classId) 1 else 0
        }
        return tp / (tp + fn)
    }

    override fun toString(): String {
        return "BatchClassification(classifications=$classifications, fullResult=$fullResults)"
    }

}

class Classification {

    val predictions = mutableMapOf<Float, Prediction>()
    fun predict() =
        predictions.values.sortedBy { it.classLikelihood }.reversed()[0].classId

    override fun toString(): String {
        return "Classification(predictions=$predictions)"
    }
}

class Prediction(val classId: Float) {

    val featureLikelihood = mutableMapOf<Int, Double>()
    var classLikelihood: Double = 0.0
    var probability: Double = 0.0
    override fun toString(): String {
        return "\nPrediction(classId=$classId, probability=$probability, " +
                "classLikelihood=$classLikelihood, featureLikelihood=$featureLikelihood)"
    }
}

fun wrapClassifications(
    predicts: Array<FloatArray>,
    labels: FloatArray,
    completeClassification: Boolean
): BatchClassification {

    val fullClassifications = mutableListOf<Classification>()
    val classifications = FloatArray(predicts.size)
    for (i in predicts.indices) {
        val predictedClassId = sortAndGet(predicts[i])
        classifications[i] = predictedClassId
        if (completeClassification) {
            val classification = Classification()
            for (n in predicts[i].indices) {
                val classId = n.toFloat()
                val prediction = Prediction(classId)
                prediction.classLikelihood = predicts[i][n].toDouble()
                prediction.probability = predicts[i][n].toDouble()
                classification.predictions[classId] = prediction
            }
            fullClassifications.add(classification)
        }
    }
    return BatchClassification(labels, classifications, fullClassifications)
}

fun sortAndGet(vector: FloatArray): Float {

    var majorIndex = 0
    var maxProb = Float.MIN_VALUE
    for (i in vector.indices) {
        if (vector[i] > maxProb) {
            maxProb = vector[i]
            majorIndex = i
        }
    }
    return majorIndex.toFloat()
}