package com.paypal.android.core

data class HttpResponse(val status: Int, val body: String? = null, val error: Throwable? = null) {
    companion object {
        const val STATUS_UNDETERMINED = -1
        const val STATUS_UNKNOWN_HOST = -2
    }
}
