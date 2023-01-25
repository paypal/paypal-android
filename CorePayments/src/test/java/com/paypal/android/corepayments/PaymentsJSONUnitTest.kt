package com.paypal.android.corepayments

import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

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
              "jsonArray": [
                {
                  "arrayElement0": "array-element-0-value"
                }
              ],
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
    fun `it should return an optional JSONObject if one exists at a given keypath`() {
        val json = sut.optGetObject("a.b")

        val expected = JSONObject()
            .put("c", "c-value")
        JSONAssert.assertEquals(expected, json?.json, true)
    }

    @Test
    fun `it should return null when an optional JSONObject does not exist at a given keypath`() {
        val json = sut.optGetObject("a.b.c.d")
        assertNull(json)
    }

    @Test
    fun `it should return a link href for a given rel if one exists`() {
        val href = sut.getLinkHref("sample-rel")
        assertEquals("/sample/href", href)
    }

    @Test
    fun `it should return null if the link does not exist`() {
        val href = sut.getLinkHref("another-rel")
        assertNull(href)
    }
}
