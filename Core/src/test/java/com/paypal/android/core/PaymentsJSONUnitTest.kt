package com.paypal.android.core

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentsJSONUnitTest {

    private lateinit var sut: PaymentsJSON

    @Before
    fun beforeEach() {
        // language=JSON
        val json = """
            {
              "a": {
                "b": {
                  "c": "c-value"
                }
              },
              "links": [
                {
                  "rel": "sample-rel",
                  "href": "/sample/href"
                }
              ]
            }
        """.trimIndent()
        sut = PaymentsJSON(json)
    }

    @Test
    fun `it should return a string property by keypath`() {
        assertEquals("c-value", sut.getString("a.b.c"))
    }

    @Test(expected = JSONException::class)
    fun `it should throw when a string property does not exist at a given keypath`() {
        sut.getString("a.b.c.d")
    }

    @Test
    fun `it should return a link href for existing links`() {
        val href = sut.getLinkHref("sample-rel")
        assertEquals("/sample/href", href)
    }

    @Test
    fun `it should return null if the link does not exist`() {
        val href = sut.getLinkHref("another-rel")
        assertNull(href)
    }
}
