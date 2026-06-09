package com.paypal.android.corepayments.analytics

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventParams(
    val orderId: String? = null,
    val setupTokenId: String? = null,
    val buttonType: String? = null,
    val appSwitchEnabled: Boolean = false
)
