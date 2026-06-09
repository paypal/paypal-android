package com.paypal.android.corepayments.analytics

import androidx.annotation.RestrictTo

/**
 * Optional parameters for an analytics event. Only set the values relevant to a given event —
 * any param left unset will be omitted (null) from the outgoing payload.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventParams(
    val orderId: String? = null,
    val vaultSetupToken: String? = null,
    val buttonType: String? = null,
    val appSwitchEnabled: Boolean = false
)
