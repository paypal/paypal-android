package com.paypal.android.corepayments.graphql.common

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class GraphQLExtension(
    val correlationId: String,
    val code: String? = null
)
