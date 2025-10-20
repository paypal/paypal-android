package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class GraphQLRequest<V>(
    val query: String,
    val variables: V? = null,
    @kotlinx.serialization.Transient
    val operationName: String? = null
)
