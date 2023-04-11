package com.paypal.android.paypalnativepayments

import com.paypal.android.corepayments.API

internal class PayPalNativeAPI(
    private val api: API
) {

    internal suspend fun fetchCachedOrRemoteClientID() = api.fetchCachedOrRemoteClientID()

    fun sendAnalyticsEvent(name: String) {
        api.sendAnalyticsEvent(name)
    }
}
