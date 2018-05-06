package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.mcts.Mcts
import com.github.bhlangonijr.chesslib.uci.Uci
import kotlin.system.exitProcess

const val VERSION = "0.1.0"
const val NAME = "pururuca-zero"
const val AUTHOR = "bhlangonijr"

class Main

fun main(args: Array<String>) {

    val search = Search(Board(), Mcts())
    val uci = Uci(search)
    while (uci.exec(readLine()!!)) {
    }
    exitProcess(0)
}
