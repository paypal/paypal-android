package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * Captures the wall-clock duration of a single HTTP round trip.
 *
 * [startTime] is recorded immediately before the request is sent and [endTime]
 * immediately after the response (or error) is received. Both are epoch
 * milliseconds ([System.currentTimeMillis]). Used to emit API request latency
 * analytics without any downstream calculation — the raw epochs are reported.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HttpRequestTiming(
    val startTime: Long,
    val endTime: Long
)
