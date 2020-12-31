package com.github.bhlangonijr.pururucazero.ml

data class DataSet(
    val features: FloatArray, val labels: FloatArray,
    val rowHeaders: LongArray, val colIndex: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataSet

        if (!features.contentEquals(other.features)) return false
        if (!labels.contentEquals(other.labels)) return false
        if (!rowHeaders.contentEquals(other.rowHeaders)) return false
        if (!colIndex.contentEquals(other.colIndex)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = features.contentHashCode()
        result = 31 * result + labels.contentHashCode()
        result = 31 * result + rowHeaders.contentHashCode()
        result = 31 * result + colIndex.contentHashCode()
        return result
    }
}