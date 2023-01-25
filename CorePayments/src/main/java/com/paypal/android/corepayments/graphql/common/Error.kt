package com.paypal.android.corepayments.graphql.common

data class Error(
    val message: String,
    val extensions: List<Extension>? = null
)
