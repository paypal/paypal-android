package com.paypal.android.corepayments

import android.content.Context

class GooglePayClient(
    private val googlePayAPI: GooglePayAPI
) {
    constructor(context: Context, config: CoreConfig) : this(
        googlePayAPI = GooglePayAPI(context, config)
    )

    suspend fun start() {
        val result = googlePayAPI.getGooglePayConfig()
        TODO("handle result")
    }
}