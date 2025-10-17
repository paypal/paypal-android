package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalSerializationApi
@Serializable
data class GraphQLExtension(
    val correlationId: String,
    val code: String? = null
)
