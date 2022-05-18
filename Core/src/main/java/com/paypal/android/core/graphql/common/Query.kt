package com.paypal.android.core.graphql.common

import org.json.JSONObject

abstract class Query<T> {
    fun requestBody(): JSONObject {
        val query = """query { $queryName(${queryParameters()})
            $dataFieldsForResponse }
        """
        return JSONObject().apply {
            put("query", query)
        }
    }

    abstract val queryParams: Map<String, Any>
    abstract val queryName: String
    abstract val dataFieldsForResponse: String
    abstract fun parse(jsonObject: JSONObject): T

    private fun queryParameters(): String {
        return StringBuilder().apply {
            for (pair in queryParams) {
                append("\n ${pair.key}: ${pair.value.encodedValue()} \n ")
            }
        }.toString()
    }
}

private fun Any.encodedValue(): Any =
    when (this) {
        is String -> """"$this""""
        else -> this
    }
