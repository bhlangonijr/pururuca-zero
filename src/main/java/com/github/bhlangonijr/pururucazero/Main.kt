package com.github.bhlangonijr.pururucazero

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.pururucazero.mcts.Mcts
import com.github.bhlangonijr.pururucazero.uci.Uci
import kotlin.system.exitProcess

const val VERSION = "0.1.0"
const val NAME = "pururuca-zero"
const val AUTHOR = "bhlangonijr"

class Main

@ExperimentalStdlibApi
fun main() {

    val search = Search(Board(), Mcts())
    val uci = Uci(search)
    while (uci.exec(readLine()!!)) {
    }
    exitProcess(0)
}
