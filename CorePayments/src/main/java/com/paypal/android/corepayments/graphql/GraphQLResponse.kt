package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import org.json.JSONObject

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class GraphQLResponse(
    val data: JSONObject? = null,
    val extensions: List<GraphQLExtension>? = null,
    val errors: List<GraphQLError>? = null,
    val correlationId: String? = null
)
