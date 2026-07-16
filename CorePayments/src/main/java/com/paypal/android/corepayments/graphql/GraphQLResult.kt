package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.HttpRequestTiming
import com.paypal.android.corepayments.PayPalSDKError
import kotlinx.serialization.InternalSerializationApi

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class GraphQLResult<out T> {

    /**
     * Round-trip timing of the underlying HTTP request, when available. Null when
     * the request never reached the network (e.g. an invalid URL). Used to emit
     * API request latency analytics.
     */
    abstract val timing: HttpRequestTiming?

    @OptIn(InternalSerializationApi::class)
    data class Success<out T>(
        val response: GraphQLResponse<T>,
        val correlationId: String? = null,
        override val timing: HttpRequestTiming? = null
    ) : GraphQLResult<T>()

    data class Failure(
        val error: PayPalSDKError,
        override val timing: HttpRequestTiming? = null
    ) : GraphQLResult<Nothing>()
}
