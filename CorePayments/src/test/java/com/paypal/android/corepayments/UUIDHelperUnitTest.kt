package com.paypal.android.corepayments

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UUIDHelperUnitTest {

    private val sut = UUIDHelper()

    @Test
    fun `formattedUUID is a 32 character lowercase hex string with no dashes`() {
        val uuid = sut.formattedUUID

        assertEquals(32, uuid.length)
        assertFalse(uuid.contains("-"))
        assertTrue(uuid.matches(Regex("[0-9a-f]{32}")))
    }

    @Test
    fun `formattedUUID returns a new random value on each access`() {
        assertNotEquals(sut.formattedUUID, sut.formattedUUID)
    }
}
