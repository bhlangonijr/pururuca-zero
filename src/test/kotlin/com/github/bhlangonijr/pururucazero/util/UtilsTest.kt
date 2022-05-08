package com.github.bhlangonijr.pururucazero.util

import com.github.bhlangonijr.pururucazero.util.Utils.flipVertical
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