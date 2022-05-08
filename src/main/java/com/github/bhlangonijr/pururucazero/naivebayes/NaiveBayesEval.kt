package com.github.bhlangonijr.pururucazero.naivebayes

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.pururucazero.SearchState
import com.github.bhlangonijr.pururucazero.util.Utils.arrayToCsr
import com.github.bhlangonijr.pururucazero.eval.Evaluator

class NaiveBayesEval constructor(var stats: DataStats) : Evaluator {

    private val nb = NaiveBayes()
    private val statsEncoder = NaiveBayesEncoder()

    override fun evaluate(state: SearchState, board: Board): Long {

        return try {
            val moves = MoveGenerator.generateLegalMoves(board)
            val isKingAttacked = board.isKingAttacked
            when {
                moves.size == 0 && isKingAttacked -> -1L
                moves.size == 0 && !isKingAttacked -> 0L
                board.isDraw -> 0L
                else -> {
                    state.nodes.incrementAndGet()
                    val encoded = statsEncoder.encode(board)
                    val features = arrayToCsr(encoded)
                    val prediction = nb.classify(features.first, features.second, stats).predict()
                    return when {
                        prediction == 1.0f && board.sideToMove == Side.WHITE -> 1L
                        prediction == 1.0f && board.sideToMove == Side.BLACK -> -1L
                        prediction == 2.0f && board.sideToMove == Side.BLACK -> 1L
                        prediction == 2.0f && board.sideToMove == Side.WHITE -> -1L
                        else -> 0L
                    }
                }
            }
        } catch (e: Exception) {
            println("Error in position ${e.message}")
            println("FEN error pos: ${board.fen}")
            println(board)
            e.printStackTrace()
            0L
        }
    }

    override fun pieceStaticValue(piece: Piece): Long {
        return 0L
    }

    override fun pieceSquareStaticValue(piece: Piece, square: Square): Long {
        return 0L
    }
}