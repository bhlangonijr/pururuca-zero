package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.chesslib.pgn.PgnIterator
import com.github.bhlangonijr.pururucazero.cnn.Nd4jDecoder.decode
import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.encode
import org.junit.Assert.assertEquals
import org.junit.Test

class Nd4jDecoderTest {

    @Test
    fun testDecode1Board() {

        val moves = MoveList()
        moves.loadFromSan("1. e4 Nf6 2. e5 d5 3. Bc4 Nc6 4. Bf1 Nb8 5. Bc4 Nc6 6. Bf1 Nb8")

        val board = Board()
        moves.forEach { move ->
            board.doMove(move)
            val encoded = encode(board)
            val decoded = decode(encoded)
            compareAndIgnoreEnPassant(board, decoded)
        }
    }

    @Test
    fun testDecode2Board() {

        val moves = MoveList()
        moves.loadFromSan("1. d4 d5 2. Nf3 Nf6 3. c4 e6 4. Bg5 Nbd7 5. e3 Be7 6. Nc3 O-O 7. Rc1 b6 8. cxd5 exd5 " +
                "9. Qa4 c5 10. Qc6 Rb8 11. Nxd5 Bb7 12. Nxe7+ Qxe7 13. Qa4 Rbc8 14. Qa3 Qe6 15. Bxf6 Qxf6 " +
                "16. Ba6 Bxf3 17. Bxc8 Rxc8 18. gxf3 Qxf3 19. Rg1 Re8 20. Qd3 g6 21. Kf1 Re4 22. Qd1 Qh3+ " +
                "23. Rg2 Nf6 24. Kg1 cxd4 25. Rc4 dxe3 26. Rxe4 Nxe4 27. Qd8+ Kg7 28. Qd4+ Nf6 29. fxe3 Qe6 " +
                "30. Rf2 g5 31. h4 gxh4 32. Qxh4 Ng4 33. Qg5+ Kf8 34. Rf5 h5 35. Qd8+ Kg7 36. Qg5+ Kf8 " +
                "37. Qd8+ Kg7 38. Qg5+ Kf8")

        val board = Board()
        moves.forEach { move ->
            board.doMove(move)
            val encoded = encode(board)
            val decoded = decode(encoded)
            compareAndIgnoreEnPassant(board, decoded)
        }
    }
    @Test
    fun testDecode3Board() {

        val moves = MoveList()
        moves.loadFromSan("1.e4 e6 2.d4 d5 3.Nc3 Nf6 4.Bg5 c5 5.exd5 exd5 6.Bxf6 Qxf6 7.Nxd5 Qe6+ 8.Qe2 " +
                "Kd8 9.O-O-O cxd4 10.Qh5 Nc6 11.Nf3 Qh6+ 12.Qxh6 gxh6 13.Nxd4 Nxd4 14.Rxd4 Bg7 " +
                "15.Rd2 Bd7 16.Bc4 Rc8 17.Bb3 Rc5 18.Rhd1 Bc6 19.Nb4+ Kc8 20.Nxc6 Rxc6 21.Bxf7 " +
                "Rf8 22.Bd5 Rb6 23.c3 h5 24.Re2 Bh6+ 25.Kc2 Kb8 26.g3 Bg5 27.f4 Bd8 28.h4 Rg6 " +
                "29.Rd3 Bf6 30.Re6 Rg7 31.b4 Rc7 32.c4 h6 33.Kb3 Rcf7 34.Bf3 Bxh4 35.gxh4 Rxf4 " +
                "36.Bd5 Rxh4 37.Re7 b6 38.Rb7+ Kc8 39.Rxa7 Rf6 40.Rg7 Rh3 41.Rxh3 h4 42.Rxh4 " +
                "b5 43.cxb5 Rd6 44.Rc4+ Kd8 45.b6 Rd7 46.b7 Rxg7 47.b8=Q+ Ke7 48.Qe5+ Kd8 " +
                "49.Qd6+")

        val board = Board()
        moves.forEach { move ->
            board.doMove(move)
            val encoded = encode(board)
            val decoded = decode(encoded)
            compareAndIgnoreEnPassant(board, decoded)
        }
    }

    @Test
    fun testDecode4Board() {

        val moves = MoveList()
        moves.loadFromSan("1.d4 Nf6 2.Nf3 g6 3.g3 d5 4.Bg2 Bg7 5.Nc3 Ne4 6.Nxe4 dxe4 7.Ng5 Qxd4 8.Qxd4 " +
                " Bxd4 9.Nxe4 Nc6 10.c3 Bb6 11.Bh6 Bd7 12.O-O f6 13.Bg7 Rg8 14.Bxf6 exf6 " +
                " 15.Nxf6+ Ke7 16.Nxg8+ Rxg8 17.Rfd1 Rf8 18.e3 Bg4 19.Rd5 Be6 20.Rd2 Na5 21.Re2 " +
                " Bg4 22.Rc2 Bf5 23.Re2 Bg4 24.Rc2 Bf5 25.Rcc1 Nc4 26.b3 Nd6 27.a3 a5 28.Ra2 a4 " +
                " 29.bxa4 Ra8 30.c4 Bc5 31.e4 Bc8 32.e5 Nf7 33.Re1 Rxa4 34.Bd5 c6 35.Bxf7 Kxf7 " +
                " 36.Re4 Bf5 37.Rh4 h5 38.Re2 Rxa3 39.Rb2 g5 40.Rxh5 Kg6 41.Rh8 g4 42.Kg2 Be4+ " +
                " 43.Kf1 Bd3+ 44.Ke1 Bd4 45.Rg8+ Kh7 46.f4 Kxg8 47.Rxb7 Be3 48.Rb8+ Kh7 49.Rb1 " +
                " Bxb1 50.Ke2 Be4 51.Ke1 Bb1 52.h4 Bd3 53.e6")

        val board = Board()
        moves.forEach { move ->
            board.doMove(move)
            println(board.fen)
            val encoded = encode(board)
            val decoded = decode(encoded)
            compareAndIgnoreEnPassant(board, decoded)
        }
    }


    @Test
    fun testRookEncode() {

        val board = Board()
        board.loadFromFen("7R/1p6/2p3k1/2b1Pbp1/2P5/r5P1/1R3P1P/6K1 b - - 2 41")
        val encoded = encode(board)
        val decoded = decode(encoded)
        compareAndIgnoreEnPassant(board, decoded)
    }


    @Test
    fun testDecodeDatabase() {

        val pgnIterator = PgnIterator("src/test/resources/pt54.pgn")

        pgnIterator.forEach {game ->
            val moves = game.halfMoves
            val board = Board()
            println(game.moveText)
            moves.forEach { move ->
                board.doMove(move)
                val encoded = encode(board)
                val decoded = decode(encoded)
                compareAndIgnoreEnPassant(board, decoded)
            }

        }
    }

    private fun compareAndIgnoreEnPassant(board1: Board, board2: Board) {
        assertEquals(getFenNoEnPassant(board1.fen), getFenNoEnPassant(board2.fen))
    }

    private fun getFenNoEnPassant(fen: String): String {
        val parts = fen.split(" ")
        return "${parts[0]} ${parts[1]} ${parts[2]} ${parts[4]} ${parts[5]}"
    }

}