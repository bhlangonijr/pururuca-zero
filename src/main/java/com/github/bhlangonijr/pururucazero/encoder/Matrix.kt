package com.github.bhlangonijr.pururucazero.encoder

class Matrix {

    companion object {

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
    }
}