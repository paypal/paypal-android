package com.paypal.android.core.graphql.common

import junit.framework.TestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class UtilsTest : TestCase() {

    @Test
    fun testToStringsList() {
        val jsonArray = JSONArray("[\"9\",\"0\"]")
        var result = jsonArray.toStringsList()
        assertEquals(result, listOf("9", "0"))
        val nullJsonArray: JSONArray? = null
        result = nullJsonArray.toStringsList()
        assertEquals(result, emptyList<String>())
    }

    @Test
    fun testGetJSONArrayOrNull() {
        val jsonObject = JSONObject("{\"array\": [9,0]}")
        var result = jsonObject.getJSONArrayOrNull("array")
        val expected = JSONArray(arrayOf(9, 0))
        assertEquals(result.toString(), expected.toString())
        result = JSONObject("{}").getJSONArrayOrNull("array")
        assertNull(result)
    }
}
