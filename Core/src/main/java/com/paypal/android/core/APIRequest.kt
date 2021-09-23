package com.paypal.android.core

data class APIRequest(val path: String, val method: HttpMethod, val body: String?) {

    var contentType: HttpContentType = HttpContentType.JSON

    private val _headers: MutableMap<String, String> = mutableMapOf()
    val headers: Map<String, String> = _headers
}
