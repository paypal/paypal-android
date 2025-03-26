package com.paypal.android.corepayments

import android.content.Context
import android.util.Log

class GooglePayClient(
    private val googlePayAPI: GooglePayAPI
) {
    constructor(context: Context, config: CoreConfig) : this(
        googlePayAPI = GooglePayAPI(context, config)
    )

    suspend fun start() {
        when (val result = googlePayAPI.getGooglePayConfig()) {
            is GetGooglePayConfigResult.Failure -> TODO("handle error")
            is GetGooglePayConfigResult.Success -> {
                val config = result.config
                Log.d("GooglePayClient", "Google Pay Eligible: ${config.isEligible}")
            }
        }
    }
}