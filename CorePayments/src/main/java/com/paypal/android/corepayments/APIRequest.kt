package com.paypal.android.corepayments

data class APIRequest(val path: String, val method: HttpMethod = HttpMethod.GET, val body: String? = null)
