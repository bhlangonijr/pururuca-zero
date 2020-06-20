package com.github.bhlangonijr.pururucazero.eval

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.pururucazero.SearchState

interface Evaluator {

    fun evaluate(state: SearchState, board: Board): Long

    fun pieceStaticValue(piece: Piece): Long

}