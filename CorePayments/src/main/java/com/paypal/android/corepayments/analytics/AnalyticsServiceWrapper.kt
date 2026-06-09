package com.paypal.android.corepayments.analytics

import androidx.annotation.RestrictTo

/**
 * Marker interface for analytics wrapper classes. Enforces that every wrapper
 * holds an [AnalyticsService] instance, making the pattern consistent across
 * [com.paypal.android.cardpayments.analytics.CardAnalytics],
 * [com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics], and
 * [com.paypal.android.paymentbuttons.analytics.PaymentButtonAnalytics].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AnalyticsServiceWrapper {
    val analyticsService: AnalyticsService
}
