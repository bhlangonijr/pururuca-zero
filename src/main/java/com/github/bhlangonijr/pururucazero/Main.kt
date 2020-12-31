package com.github.bhlangonijr.pururucazero

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.pururucazero.montecarlo.MonteCarloSearch
import com.github.bhlangonijr.pururucazero.uci.Uci
import kotlin.system.exitProcess

const val VERSION = "0.1.1"
const val NAME = "pururuca-zero"
const val AUTHOR = "bhlangonijr"

class Main

@ExperimentalStdlibApi
fun main() {

    val search = Search(Board(), MonteCarloSearch())
    val uci = Uci(search)
    while (uci.exec(readLine()!!)) {
    }
    exitProcess(0)
}
