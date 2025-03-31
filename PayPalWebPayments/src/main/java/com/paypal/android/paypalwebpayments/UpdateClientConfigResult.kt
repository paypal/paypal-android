package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class UpdateClientConfigResult {

    data class Success(val clientConfig: String): UpdateClientConfigResult()
    data class Failure(val error: PayPalSDKError): UpdateClientConfigResult()
}

