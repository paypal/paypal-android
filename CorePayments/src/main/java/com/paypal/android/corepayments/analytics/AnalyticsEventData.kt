package com.paypal.android.corepayments.analytics

// Ref: https://blog.klipse.tech/databook/2022/06/22/separate-code-from-data.html
data class AnalyticsEventData(
    val environment: String,
    val eventName: String,
    val timestamp: Long,
    val orderId: String?
)
