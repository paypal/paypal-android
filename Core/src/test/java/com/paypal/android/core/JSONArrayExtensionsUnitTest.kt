package com.paypal.android.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class JSONArrayExtensionsUnitTest {

    private data class ValueHolder(val value: String)

    @Test
    fun `it should map a JSONArray of objects into a given type`() {
        val jsonArray = JSONArray()
            .put(
                0, JSONObject()
                    .put("value", "sample-value")
            )

        val result = jsonArray.map { ValueHolder(it.getString("value")) }
        assertEquals(1, result.size)

        assertEquals("sample-value", result[0].value)
    }
}