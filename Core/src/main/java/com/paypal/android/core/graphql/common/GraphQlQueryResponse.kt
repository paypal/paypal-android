package com.paypal.android.core.graphql.common

data class GraphQlQueryResponse<T>
    (
    val data: T? = null,
    val extensions: List<Extension>? = null,
    val errors: List<Error>? = null
)
