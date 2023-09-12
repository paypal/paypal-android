package com.paypal.android.corepayments.graphql.common

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class GraphQLError(
    val message: String,
    val extensions: List<GraphQLExtension>? = null
)
