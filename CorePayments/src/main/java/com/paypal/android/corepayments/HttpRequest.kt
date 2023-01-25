package com.paypal.android.corepayments

import java.net.URL

internal data class HttpRequest(
    val url: URL,
    val method: HttpMethod,
    val body: String? = null,
    val headers: MutableMap<String, String> = mutableMapOf(),
)
