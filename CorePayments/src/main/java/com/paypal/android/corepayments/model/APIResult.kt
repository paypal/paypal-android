package com.paypal.android.corepayments.model

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.PayPalSDKError

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class APIResult<T> {
    /**
     * The request was successful.
     *
     * @property data the data from the response
     */
    data class Success<T>(val data: T) : APIResult<T>()

    /**
     * There was an error with the request.
     *
     * @property error the error that occurred
     */
    data class Failure<T>(val error: PayPalSDKError) : APIResult<T>()
}
