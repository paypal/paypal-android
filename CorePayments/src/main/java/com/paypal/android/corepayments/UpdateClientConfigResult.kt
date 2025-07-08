package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class UpdateClientConfigResult {

    data class Success(val clientConfig: String) : UpdateClientConfigResult()
    data class Failure(val error: PayPalSDKError) : UpdateClientConfigResult()
}