package com.github.bhlangonijr.pururucazero

import com.github.bhlangonijr.chesslib.move.Move

interface SearchEngine {

    fun rooSearch(state: SearchState): Move

}