package com.paypal.android.corepayments.graphql.common

import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class GraphQLResponse(
    val data: JSONObject? = null,
    val extensions: List<Extension>? = null,
    val errors: List<Error>? = null,
    val correlationId: String? = null
)
