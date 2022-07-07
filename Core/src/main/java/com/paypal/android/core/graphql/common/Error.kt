package com.paypal.android.core.graphql.common

data class Error(
    val message: String,
    val extensions: List<Extension>? = null
)
