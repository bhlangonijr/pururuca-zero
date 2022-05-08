package com.github.bhlangonijr.pururucazero.util

object Utils {

    @JvmStatic
    fun arrayToCsr(array: FloatArray): Pair<FloatArray, IntArray> {

        val colIndex = arrayListOf<Int>()
        val data = mutableListOf<Float>()
        for ((idx, feature) in array.withIndex()) {
            if (!feature.isNaN()) {
                colIndex.add(idx)
                data.add(feature)
            }
        }
        return Pair(data.toFloatArray(), colIndex.toIntArray())
    }

    // https://www.chessprogramming.org/Flipping_Mirroring_and_Rotating
    @JvmStatic
    fun flipVertical(x: Long): Long {
        return ((x shl 56)) or
                ((x shl 40) and 0x00ff000000000000L) or
                ((x shl 24) and 0x0000ff0000000000L) or
                ((x shl 8) and 0x000000ff00000000L) or
                ((x shr 8) and 0x00000000ff000000L) or
                ((x shr 24) and 0x0000000000ff0000L) or
                ((x shr 40) and 0x000000000000ff00L) or
                ((x shr 56))
    }
}