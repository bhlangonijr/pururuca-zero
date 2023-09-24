package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.encode
import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.encodeToArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class Nd4jEncoderTest {

    @Test
    fun testEncode1Board() {

        val moves = MoveList()
        moves.loadFromSan("1. e4 Nf6 2. e5 d5 3. Bc4 Nc6 4. Bf1 Nb8 5. Bc4 Nc6 6. Bf1 Nb8")

        val board = Board()
        moves.forEach { move ->
            board.doMove(move)
        }

        val encoded = encodeToArray(board)

        assertEquals(1f, encoded[0][4]) // side king position
        assertEquals(1f, encoded[5 * 8][3]) // opposite side king position
        assertEquals(1f, encoded[8 + 4][4]) // side pawn advanced
        assertEquals(1f, encoded[7 * 8 + 3][3]) // opposite pawn advanced
        assertEquals(1f, encoded[12 * 8][0]) // any square populated with first repetition
        assertEquals(0f, encoded[13 * 8][0]) // any square not populated with second repetition
    }

    @Test
    fun testEncode1BoardThreeFoldRepetition() {

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
        }
        println(board.fen)
        val encoded = encodeToArray(board)

        assertEquals(1f, encoded[0][6]) // side king position
        assertEquals(1f, encoded[6 * 8][5]) // opposite side king position
        assertEquals(1f, encoded[12 * 8][0]) // any square populated with first repetition
        assertEquals(1f, encoded[13 * 8][0]) // any square populated with second repetition
    }

    @Test
    fun testFullEncode() {

        val moves = MoveList()
        moves.loadFromSan("1. e4 Nf6 2. e5 d5 3. Bc4 Nc6 4. Bf1 Nb8 5. Bc4 Nc6 6. Bf1 Nb8")

        val board = Board()
        moves.forEach { move ->
            board.doMove(move)
        }
        val initialHash = board.incrementalHashKey
        val encoded = encode(board)
        assertEquals(initialHash, board.incrementalHashKey)

        assertEquals(1f, encoded.getRow(0).getFloat(4)) // side king position
        assertEquals(1f, encoded.getRow(5 * 8).getFloat(3)) // opposite side king position
        assertEquals(1f, encoded.getRow(8 + 4).getFloat(4)) // side pawn advanced
        assertEquals(1f, encoded.getRow(7 * 8 + 3).getFloat(3)) // opposite pawn advanced
        assertEquals(1f, encoded.getRow(12 * 8).getFloat(0)) // any square populated with first repetition
        assertEquals(0f, encoded.getRow(13 * 8).getFloat(0)) // any square not populated with second repetition
    }

    @Test
    fun testEncodePartialPlane() {

        val moves = MoveList()
        moves.loadFromSan("1. e4 Nf6")

        val board = Board()
        moves.forEach { move ->
            board.doMove(move)
        }
        val initialHash = board.incrementalHashKey
        val encoded = encode(board)
        assertEquals(initialHash, board.incrementalHashKey)

        assertEquals(1f, encoded.getRow(0).getFloat(4)) // side king position
        assertEquals(1f, encoded.getRow(5 * 8).getFloat(3)) // opposite side king position
        assertEquals(0f, encoded.getRow(12 * 8).getFloat(0)) // any square populated with first repetition
        assertEquals(0f, encoded.getRow(13 * 8).getFloat(0)) // any square not populated with second repetition

    }

    @Test
    fun testEmptyVsEncodedShapes() {

        val board = Board()

        val encoded = encode(board)

        val moves = MoveList()
        moves.loadFromSan("1. e4 Nf6 2. e5 d5 3. Bc4 Nc6 4. Bf1 Nb8 5. Bc4 Nc6 6. Bf1 Nb8")
        moves.forEach {
            board.doMove(it)
        }
        val encoded2 = encode(board)

        val empty = Nd4jEncoder.emptyPlanes

        assertTrue(Arrays.equals(encoded.shape(), empty.shape()))
        assertTrue(Arrays.equals(encoded.shape(), encoded2.shape()))

    }
}