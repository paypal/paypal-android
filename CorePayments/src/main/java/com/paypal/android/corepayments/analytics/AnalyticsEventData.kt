package com.paypal.android.corepayments.analytics

// Ref: https://blog.klipse.tech/databook/2022/06/22/separate-code-from-data.html
internal data class AnalyticsEventData(
    val environment: String,
    val eventName: String,
    val timestamp: Long,
    val orderId: String?,
    val buttonType: String? = null
)
