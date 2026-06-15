package com.paypal.android.corepayments.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ButtonSessionStoreUnitTest {

    @Test
    fun `buttonSessionId is initialized with a non-blank value`() {
        assertFalse(ButtonSessionStore.buttonSessionId.isBlank())
    }

    @Test
    fun `resetSession changes buttonSessionId`() {
        val before = ButtonSessionStore.buttonSessionId
        ButtonSessionStore.resetSession()
        assertNotEquals(before, ButtonSessionStore.buttonSessionId)
    }

    @Test
    fun `buttonSessionId is non-blank after resetSession`() {
        ButtonSessionStore.resetSession()
        assertFalse(ButtonSessionStore.buttonSessionId.isBlank())
    }
}
