package com.github.bhlangonijr.pururucazero.encoder

import com.github.bhlangonijr.chesslib.Board

interface BoardEncoder {

    fun encode(board: Board): FloatArray

}