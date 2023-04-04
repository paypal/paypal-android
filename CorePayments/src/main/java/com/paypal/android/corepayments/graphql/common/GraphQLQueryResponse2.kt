package com.paypal.android.corepayments.graphql.common

import com.paypal.android.corepayments.HttpResponse
import org.json.JSONObject

data class GraphQLQueryResponse2 (private val httpResponse: HttpResponse) {

    val isSuccessful = httpResponse.isSuccessful
    val data: JSONObject =
        JSONObject(httpResponse.body ?: "{}").getJSONObject("data") ?: JSONObject()

    val extensions: List<Extension> = emptyList()
    val errors: List<Error> = emptyList()
    val correlationID: String = httpResponse.headers[GraphQLClientImpl.PAYPAL_DEBUG_ID] ?: ""
}
