package com.paypal.android.core

import java.net.URL
import java.util.Locale

internal data class HttpRequest(
    val url: URL,
    val method: HttpMethod,
    val body: String? = null,
    val headers: MutableMap<String, String> = mutableMapOf(),
    val language: String = Locale.getDefault().language
)
