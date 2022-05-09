package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.pgn.PgnIterator
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.dataset.api.DataSetPreProcessor
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.factory.Nd4j


class PgnDatasetIterator(
    val pgnFile: String,
    val batchSize: Int = 32,
    val labelNames: MutableList<String>
) : DataSetIterator {

    private var dataSetPreProcessor: DataSetPreProcessor? = null
    private var count: Int = 0
    private var eof = false
    var curr = 0

    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun hasNext(): Boolean {

        return !eof
    }

    override fun next(rows: Int): DataSet {

        val inputs: INDArray = Nd4j.create(
            rows, Nd4jEncoder.size * 14 * Nd4jEncoder.totalTimeSteps +
                    Nd4jEncoder.size * 7, Nd4jEncoder.size
        )
        val outputs: INDArray = Nd4j.create(
            rows, labelNames.size
        )
        var lines = 0
        var internalCurr = 0
        PgnIterator(pgnFile).use { pgn ->
            for (game in pgn) {
                try {
                    val moves = game.halfMoves
                    val board = Board()
                    for (move in moves) {
                        board.doMove(move)
                        if (internalCurr++ < curr) {
                            continue
                        }
                        val result = mapGameResult(game.result.description, board.sideToMove)
                        addExample(inputs, outputs, result, board, lines)
                        lines++
                        curr++
                        if (lines >= rows) {
                            break
                        }
                    }
                    if (lines >= rows) {
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    println(game.toString())
                }
            }
        }

        count++
        val dataSet = DataSet(
            inputs,
            outputs
        )
        dataSet.labelNames = labelNames
        eof = lines == 0
        return dataSet
    }

    override fun next(): DataSet {

        return next(batchSize)
    }

    override fun inputColumns(): Int {
        TODO("Not yet implemented")
    }

    override fun totalOutcomes(): Int {

        return labelNames.size
    }

    override fun resetSupported(): Boolean {

        return true
    }

    override fun asyncSupported(): Boolean {

        return false
    }

    override fun reset() {

        count = 0
    }

    override fun batch(): Int {

        return batchSize
    }

    override fun setPreProcessor(preProcessor: DataSetPreProcessor) {

        this.dataSetPreProcessor = preProcessor
    }

    override fun getPreProcessor(): DataSetPreProcessor? {

        return this.dataSetPreProcessor
    }

    override fun getLabels(): MutableList<String> {
        return labelNames
    }

    private fun mapGameResult(result: String, side: Side): Int {

        return when {
            result == "1-0" && side == Side.WHITE -> 1
            result == "0-1" && side == Side.WHITE -> 2
            result == "0-1" && side == Side.BLACK -> 1
            result == "1-0" && side == Side.BLACK -> 2
            else -> 0
        }
    }

    private fun addExample(
        inputs: INDArray,
        outputs: INDArray,
        label: Int,
        board: Board,
        idx: Int
    ) {

        val vector = Nd4j.zeros(1, labelNames.size)
        vector.putScalar(label.toLong(), 1)
        outputs.putRow(idx.toLong(), vector)
        val features = Nd4jEncoder.encode(board)
        inputs.putRow(idx.toLong(), features)
    }
}