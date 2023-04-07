package com.paypal.android.corepayments.graphql.common

import org.json.JSONObject

internal data class GraphQLQueryResponse(
    val data: JSONObject? = null,
    val extensions: List<Extension>? = null,
    val errors: List<Error>? = null,
    val correlationId: String? = null
)
