package com.github.bhlangonijr.chesslib.ml

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.eval.StatEval
import com.github.bhlangonijr.chesslib.pgn.PgnHolder
import java.util.*

data class DataSet(val features: FloatArray, val labels: FloatArray,
                   val rowHeaders: LongArray, val colIndex: IntArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataSet

        if (!Arrays.equals(features, other.features)) return false
        if (!Arrays.equals(labels, other.labels)) return false
        if (!Arrays.equals(rowHeaders, other.rowHeaders)) return false
        if (!Arrays.equals(colIndex, other.colIndex)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(features)
        result = 31 * result + Arrays.hashCode(labels)
        result = 31 * result + Arrays.hashCode(rowHeaders)
        result = 31 * result + Arrays.hashCode(colIndex)
        return result
    }
}

fun pgnToDataSet(name: String): DataSet {

    val mapResult = mapOf("1-0" to 1.0f, "0-1" to 2.0f, "1/2-1/2" to 0.0f)
    val stat = StatEval()
    val pgn = PgnHolder(name)
    pgn.loadPgn()

    val rowHeaders = arrayListOf<Long>()
    val colIndex = arrayListOf<Int>()

    println("$name loading, games: ${pgn.game.size}")
    var lines = 0
    var rowHeader = 0L
    val data = mutableListOf<Float>()
    val labels = mutableListOf<Float>()
    rowHeaders.add(0)
    for ((idx0, game) in pgn.game.withIndex()) {
        try {
            game.loadMoveText()
            val moves = game.halfMoves
            val board = Board()
            for (move in moves) {
                board.doMove(move)
                val result = mapResult[game.result.description] ?: 0.0f
                val features = stat.extractFeatures(board)
                labels.add(result)
                lines++
                for ((idx, feature) in features.withIndex()) {
                    if (!feature.isNaN()) {
                        rowHeader++
                        colIndex.add(idx)
                        data.add(feature)
                    }
                }
                rowHeaders.add(rowHeader)
            }
            if (idx0 % 100 == 0) println("$name loaded more 100")
        } catch (e: Exception) {
            e.printStackTrace()
            println(game.toString())
        }
    }
    println("Number of lines [$lines] Row header [$rowHeader] " +
            "Labels.size [${labels.size}] features.size [${data.size}]")

    return DataSet(data.toFloatArray(), labels.toFloatArray(), rowHeaders.toLongArray(), colIndex.toIntArray())
}