package com.paypal.android.card

import org.junit.Assert.assertEquals
import org.junit.Test

class DateParserUnitTest {

    @Test
    fun `parses MM YY formatted dates`() {
        val sut = DateParser()
        val result = sut.parseCardExpiry("01/22")

        assertEquals("01", result.month)
        assertEquals("2022", result.year)
    }
}