package com.paypal.android.corepayments

data class APIRequest(val path: String, val method: HttpMethod, val body: String? = null)
