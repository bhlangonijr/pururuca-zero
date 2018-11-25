package com.github.bhlangonijr.chesslib.eval

import com.github.bhlangonijr.chesslib.Board
import org.junit.Test

class StatsEvalTest {

    @Test
    fun testEval() {

        val board = Board()
        board.loadFromFen("2k5/7R/4K3/8/8/8/8/8 w - - 1 0")
        println(board.fen)
        println(board)

        val stat = StatEval()

        println("features  = ${stat.extractFeatures(board).joinToString { "$it" }}")

    }
}



