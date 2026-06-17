package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalSerializationApi
@Serializable
data class GraphQLError(
    val message: String,
    // extensions can be an object OR an array depending on the server — use JsonElement to accept both
    val extensions: JsonElement? = null
)
