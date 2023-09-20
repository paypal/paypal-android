package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class GraphQLError(
    val message: String,
    val extensions: List<GraphQLExtension>? = null
)
