package com.github.bhlangonijr.chesslib

import com.github.bhlangonijr.chesslib.move.Move

interface SearchEngine {

    fun rooSearch(state: SearchState): Move

}