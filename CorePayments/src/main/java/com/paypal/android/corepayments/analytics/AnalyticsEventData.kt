package com.paypal.android.corepayments.analytics

// Ref: https://blog.klipse.tech/databook/2022/06/22/separate-code-from-data.html
internal data class AnalyticsEventData(
    val environment: String,
    val eventName: String,
    val timestamp: Long,
    val orderId: String?,
    val buttonType: String? = null,
    val appSwitchEnabled: Boolean? = null,
    val shopperSessionId: String? = null,
    val appSwitchUrl: String? = null,
    val errorDescription: String? = null,
    val startTime: Long? = null,
    val isCachedSession: Boolean? = null,
    val isVaultRequest: Boolean? = null,
    val endTime: Long? = null,
    val endpoint: String? = null,
    val presentationType: String? = null,
    val flow: String? = null
)
