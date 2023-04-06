package com.paypal.android.corepayments.graphql.common

internal data class GraphQLQueryResponse<T>(
    val data: T? = null,
    val extensions: List<Extension>? = null,
    val errors: List<Error>? = null,
    val correlationId: String? = null
)
