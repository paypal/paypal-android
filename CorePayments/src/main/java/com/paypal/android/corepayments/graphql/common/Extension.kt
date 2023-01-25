package com.paypal.android.corepayments.graphql.common

data class Extension(
    val correlationId: String,
    val code: String? = null
)
