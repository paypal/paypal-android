package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.PayPalSDKError
import kotlinx.serialization.InternalSerializationApi

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class GraphQLResult<out T> {

    @OptIn(InternalSerializationApi::class)
    data class Success<out T>(
        val response: GraphQLResponse<T>,
        val correlationId: String? = null
    ) : GraphQLResult<T>()

    data class Failure(val error: PayPalSDKError) : GraphQLResult<Nothing>()
}
