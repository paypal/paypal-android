package com.paypal.android.corepayments.api

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.PayPalSDKError

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CreateLowScopedAccessTokenResult {
    /**
     * The request to get client token was successful.
     *
     * @property token the access token from the response
     */
    data class Success(val token: String) : CreateLowScopedAccessTokenResult()

    /**
     * There was an error with the request to get client token.
     *
     * @property error the error that occurred
     */
    data class Failure(val error: PayPalSDKError) : CreateLowScopedAccessTokenResult()
}
