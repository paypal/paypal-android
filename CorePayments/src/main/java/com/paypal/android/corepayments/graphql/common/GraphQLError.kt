package com.paypal.android.corepayments.graphql.common

data class GraphQLError(
    val message: String,
    val extensions: List<GraphQLExtension>? = null
)
