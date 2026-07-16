package com.paypal.android.corepayments.analytics

// Ref: https://blog.klipse.tech/databook/2022/06/22/separate-code-from-data.html
internal data class AnalyticsEventData(
    val environment: String,
    val eventName: String,
    val timestamp: Long,
    val orderId: String?,
    val buttonType: String? = null,
    val appSwitchEnabled: Boolean,
    // fields below support the mobile SDK analytics events documented at
    // https://paypal.atlassian.net/wiki/spaces/~7120203361479131b645799d3eacdd2de5b990/pages/2982842759
    val shopperSessionId: String? = null,
    val appSwitchUrl: String? = null,
    val errorDescription: String? = null,
    // captured right before firing a `:started` event and passed forward to the matching
    // `:succeeded`/`:failed` event so FPTI can derive latency as (t - start_time)
    val startTime: Long? = null,
    val isCachedSession: Boolean? = null,
    val isVaultRequest: Boolean? = null
)
