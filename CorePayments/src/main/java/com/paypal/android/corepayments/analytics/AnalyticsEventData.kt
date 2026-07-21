package com.paypal.android.corepayments.analytics

import androidx.annotation.RestrictTo

// Ref: https://blog.klipse.tech/databook/2022/06/22/separate-code-from-data.html

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventData(
    val environment: String = "",
    val eventName: String = "",
    val timestamp: Long = 0L,
    val orderId: String? = null,
    val buttonType: String? = null,
    val appSwitchEnabled: Boolean? = null,
    val shopperSessionId: String? = null,
    val shopperSessionExpiration: String? = null,
    val matchedAuthenticationMethods: List<String>? = null,
    val appSwitchUrl: String? = null,
    val checkoutFallbackUrl: String? = null,
    val errorDescription: String? = null,
    val isCachedSession: Boolean? = null,
    val isVault: Boolean? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val endpoint: String? = null,
    val presentationType: String? = null,
    val flow: String? = null,
    val appSwitchEligible: Boolean? = null,
    val ineligibleReason: String? = null,
    val merchantId: String? = null,
    val bnCode: String? = null,
    val clientId: String? = null,
    val userAction: String? = null,
    val paypalInstalled: String? = null,
    val returnAppUrl: String? = null,
    val cancelAppUrl: String? = null,
    val fallbackSchemeUrl: String? = null,
    val linkType: String? = null
)
