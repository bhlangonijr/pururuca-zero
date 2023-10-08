package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.pgn.PgnIterator
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.dataset.api.DataSetPreProcessor
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.indexing.NDArrayIndex
import java.util.*

class PgnDatasetIterator(
    val pgnFile: String,
    val batchSize: Int = 32,
    val labelNames: MutableList<String>
) : DataSetIterator, AutoCloseable {

    private var pgnIterator = PgnIterator(pgnFile)
    private var currentIterator = pgnIterator.iterator()
    private var dataSetPreProcessor: DataSetPreProcessor? = null
    private var count: Int = 0
    @Volatile
    private var eof = false
    private val datasetQueue = LinkedList<DataStack>()

    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun hasNext() = !eof

    override fun next(rows: Int): DataSet {

        if (!eof && datasetQueue.size == 0) {
            datasetQueue.add(DataStack(rows, labelNames))
        }
        var endOfFile = true
        if (datasetQueue.peekFirst().lines < rows) {
            while (currentIterator.hasNext()) {
                val game = currentIterator.next()
                try {
                    val moves = game.halfMoves
                    val board = Board()
                    for (move in moves) {
                        board.doMove(move)
                        val result = mapGameResult(game.result.description, board.sideToMove)
                        val dataStack = datasetQueue.peekLast()
                        dataStack.add(result, board)
                        if (dataStack.lines >= rows) {
                            datasetQueue.add(DataStack(rows, labelNames))
                        }
                    }
                    val dataStack = datasetQueue.peekFirst()
                    if (dataStack.lines >= rows) {
                        endOfFile = false
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    println(game.toString())
                }
            }
        } else {
            endOfFile = false
        }

        count++
        val dataStack = datasetQueue.pollFirst()
        val dataSet = DataSet(
            dataStack.inputs,
            dataStack.outputs
        )
        dataSet.labelNames = labelNames
        eof = endOfFile
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

        pgnIterator.close()
        pgnIterator = PgnIterator(pgnFile)
        currentIterator = pgnIterator.iterator()
        count = 0
        eof = false
        datasetQueue.clear()
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
            result == "1-0" && side == Side.WHITE -> -1
            result == "0-1" && side == Side.WHITE -> +1
            result == "0-1" && side == Side.BLACK -> -1
            result == "1-0" && side == Side.BLACK -> +1
            else -> 0
        }
    }

    data class DataStack(
        val rows: Int,
        val labelNames: MutableList<String>,
        val inputs: INDArray = Nd4j.create(
            rows,
            Nd4jEncoder.numberOfPlanes,
            Nd4jEncoder.size,
            Nd4jEncoder.size
        ),
        val outputs: INDArray = Nd4j.create(
            rows, labelNames.size
        ),
        var lines: Int = 0
    ) {
        fun add(
            label: Int,
            board: Board
        ) {
            if (lines >= rows) {
                throw IllegalArgumentException("INDArray is full. Size: $lines")
            }
            val vector = Nd4j.zeros(1, labelNames.size)
            vector.putScalar(label.toLong(), 1)
            outputs.putRow(lines.toLong(), vector)
            val features = Nd4jEncoder.encode(board)
            inputs.put(NDArrayIndex.indexesFor(lines.toLong()), features)
            lines++
        }
    }

    override fun close() {

        pgnIterator.close()
        datasetQueue.clear()
    }
}

