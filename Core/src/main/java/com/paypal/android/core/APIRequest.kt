package com.paypal.android.core

data class APIRequest(val path: String, val method: HttpMethod, val body: String?)
