package com.paypal.android.card

import org.junit.Assert.assertEquals
import org.junit.Test

class DateParserUnitTest {

    @Test
    fun `parses MM YY formatted dates`() {
        val sut = com.paypal.android.ui.card.DateParser()
        val result = sut.parseExpirationDate("01/22")

        assertEquals(1, result.month)
        assertEquals(2022, result.year)
    }
}
