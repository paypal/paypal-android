package com.paypal.android.corepayments

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import org.junit.Test

class SessionIdRepositoryUnitTest {

    @Test
    fun `sessionId is the formatted UUID from the UUIDHelper`() {
        val uuidHelper = mockk<UUIDHelper> {
            every { formattedUUID } returns "0123456789abcdef0123456789abcdef"
        }

        val sut = SessionIdRepository(uuidHelper)

        assertEquals("0123456789abcdef0123456789abcdef", sut.sessionId)
    }

    @Test
    fun `sessionId is stable across multiple reads on the same instance`() {
        val sut = SessionIdRepository()

        assertEquals(sut.sessionId, sut.sessionId)
    }

    @Test
    fun `instance is a singleton with a stable sessionId`() {
        assertSame(SessionIdRepository.instance, SessionIdRepository.instance)
        assertEquals(
            SessionIdRepository.instance.sessionId,
            SessionIdRepository.instance.sessionId
        )
    }
}
