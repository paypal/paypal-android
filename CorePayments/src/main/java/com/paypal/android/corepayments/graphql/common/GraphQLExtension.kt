package com.paypal.android.corepayments.graphql.common

data class GraphQLExtension(
    val correlationId: String,
    val code: String? = null
)
