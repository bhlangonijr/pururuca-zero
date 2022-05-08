package com.github.bhlangonijr.pururucazero.encoder

import com.github.bhlangonijr.pururucazero.encoder.Utils.flipVertical
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    @Test
    fun testFlipVertical() {

        val bitboard = 1L shl 4

        val flipped = flipVertical(bitboard)
        assertEquals(bitboard, flipVertical(flipped))
    }
}