package com.paypal.android.corepayments

import androidx.annotation.RestrictTo
import java.net.HttpURLConnection

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HttpResponse(
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val error: Throwable? = null
) {
    companion object {
        const val STATUS_UNDETERMINED = -1
        const val STATUS_UNKNOWN_HOST = -2
        const val SERVER_ERROR = -3

        val SUCCESSFUL_STATUS_CODES = HttpURLConnection.HTTP_OK..299
    }

    var isSuccessful: Boolean = status in SUCCESSFUL_STATUS_CODES
}
