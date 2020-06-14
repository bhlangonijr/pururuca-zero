package com.github.bhlangonijr.pururucazero.eval

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.pururucazero.encoder.Matrix
import com.github.bhlangonijr.pururucazero.encoder.PositionStatsEncoder
import junit.framework.TestCase.assertEquals
import org.junit.Ignore
import org.junit.Test

@Ignore
class StatsEvalTest {

    private val statsEncoder = PositionStatsEncoder()

    @Test
    fun testEval() {

        val board = Board()
        board.loadFromFen("2k5/7R/4K3/8/8/8/8/8 w - - 1 0")

        val expect = listOf(1.0, 190.0, 1.0, 1.0, 190.0, 1.0, 1.0, 1.0, 1.0, 190.0, 14.0, 7.0, 2.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 570.0, 0.0, 1.0, 571.0)
        assertEquals(expect, Matrix.arrayToCsr(statsEncoder.encode(board)))
    }

    @Test
    fun testEval2() {

        val board = Board()
        board.loadFromFen("5rkr/pp2Rp2/1b1p1Pb1/3P2Q1/2n3P1/2p5/P4P2/4R1K1 w - - 1 0")

        println(Matrix.arrayToCsr(statsEncoder.encode(board)))
        //val expect = listOf()
        //assertEquals(expect, stat.extractFeatures(board))
    }
}