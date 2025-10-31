package com.paypal.android.corepayments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// NOTE: by design, this regex fails when matched against empty lines
// Ref: https://stackoverflow.com/a/5885097
// language=RegExp
private val BASE64_REGEX =
    "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$".toRegex()

@RunWith(RobolectricTestRunner::class)
class SessionStoreUnitTest {

    @Test
    fun `it should return null for property reads when a value for the property does not exist`() {
        val sut = SessionStore()
        assertNull(sut["example_key"])
    }

    @Test
    fun `it should allow property writes`() {
        val sut = SessionStore()
        sut["example_key"] = "example_value"
        assertEquals(sut["example_key"], "example_value")
    }

    @Test
    fun `it should allow all properties to be cleared`() {
        val sut = SessionStore()
        sut["example_key1"] = "example_value1"
        sut["example_key2"] = "example_value2"
        sut.clear()
        assertNull(sut["example_key1"])
        assertNull(sut["example_key2"])
    }

    @Test
    fun `it should serialize to a base64 encoded string`() {
        val sut = SessionStore()
        sut["example_key"] = "example_value"
        assertTrue(BASE64_REGEX.matches(sut.toBase64EncodedJSON()))
    }

    @Test
    fun `it should restore base64 encoded JSON properties`() {
        val launchWithUrlStore = SessionStore()
        launchWithUrlStore["example_key1"] = "example_value1"
        launchWithUrlStore["example_key2"] = "example_value2"
        val encodedState = launchWithUrlStore.toBase64EncodedJSON()

        val sut = SessionStore()
        sut.restore(encodedState)
        assertEquals(sut["example_key1"], "example_value1")
        assertEquals(sut["example_key2"], "example_value2")
    }

    @Test
    fun `it should not throw when restoring input is not valid base64`() {
        val sut = SessionStore()
        val invalidBase64 = "invalidBase64WithTooMuchPadding======="
        sut.restore(invalidBase64)
        assertNull(sut["example_key"])
    }

    @Test
    fun `it should not throw when restoring input is not valid json`() {
        // NOTE: contains base64 string == "invalid json"
        val base64EncodingOfInvalidJSON = "aW52YWxpZCBqc29u"

        val sut = SessionStore()
        sut.restore(base64EncodingOfInvalidJSON)
        assertNull(sut["example_key"])
    }
}
