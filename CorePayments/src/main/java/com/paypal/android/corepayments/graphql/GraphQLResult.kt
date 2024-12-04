package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.PayPalSDKError
import org.json.JSONObject

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class GraphQLResult {

    data class Success(
        val data: JSONObject? = null,
        val extensions: List<GraphQLExtension>? = null,
        val errors: List<GraphQLError>? = null,
        val correlationId: String? = null
    ) : GraphQLResult()

    data class Failure(val error: PayPalSDKError) : GraphQLResult()
}
