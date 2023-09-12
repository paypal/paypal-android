package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class GraphQLExtension(
    val correlationId: String,
    val code: String? = null
)
