package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalSerializationApi
@Serializable
data class GraphQLResponse<out T> @OptIn(InternalSerializationApi::class) constructor(
    val data: T? = null,
    val extensions: JsonObject? = null,
    val errors: List<GraphQLError>? = null,
)
