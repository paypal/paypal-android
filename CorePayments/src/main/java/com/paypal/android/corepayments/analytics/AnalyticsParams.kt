package com.paypal.android.corepayments.analytics

import androidx.annotation.RestrictTo

/**
 * Well-known FPTI parameter keys used when building analytics event param maps.
 * Each analytics component references these constants to ensure consistent key naming.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AnalyticsParams {
    const val ORDER_ID = "order_id"
    const val SETUP_TOKEN_ID = "setup_token_id"
    const val BUTTON_TYPE = "button_type"
}
