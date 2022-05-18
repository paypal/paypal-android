package com.paypal.android.core

import java.net.URL

data class HttpRequest(
    val url: URL,
    val method: HttpMethod,
    val body: String? = null,
    val headers: MutableMap<String, String> = mutableMapOf(),
)
