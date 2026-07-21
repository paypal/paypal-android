package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService

internal class PayPalWebAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(event: CheckoutEvent, params: AppSwitchAnalyticsEventParams, errorDescription: String? = null) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            orderId = params.checkoutOrderId,
            appSwitchEnabled = params.appSwitchEnabled,
            shopperSessionId = params.shopperSessionId,
            shopperSessionExpiration = params.shopperSessionExpiration,
            matchedAuthenticationMethods = params.matchedAuthenticationMethods,
            appSwitchUrl = params.appSwitchUrl,
            fallbackUrl = params.fallbackUrl,
            errorDescription = errorDescription,
            isCachedSession = params.isCachedSession,
            isVault = params.isVault,
            appSwitchEligible = params.appSwitchEligible,
            ineligibleReason = params.ineligibleReason,
            merchantId = params.merchantId,
            bnCode = params.bnCode,
            clientId = params.clientId,
            userAction = params.userActionValue,
            paypalInstalled = params.paypalInstalled,
            returnAppUrl = params.returnAppUrl,
            cancelAppUrl = params.cancelAppUrl,
            fallbackSchemeUrl = params.fallbackSchemeUrl,
        )
    }

    fun notify(event: VaultEvent, params: AppSwitchAnalyticsEventParams, errorDescription: String? = null) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            orderId = params.vaultSetupTokenId,
            appSwitchEnabled = params.appSwitchEnabled,
            shopperSessionId = params.shopperSessionId,
            shopperSessionExpiration = params.shopperSessionExpiration,
            matchedAuthenticationMethods = params.matchedAuthenticationMethods,
            appSwitchUrl = params.appSwitchUrl,
            fallbackUrl = params.fallbackUrl,
            errorDescription = errorDescription,
            isCachedSession = params.isCachedSession,
            isVault = params.isVault,
            appSwitchEligible = params.appSwitchEligible,
            ineligibleReason = params.ineligibleReason,
            merchantId = params.merchantId,
            bnCode = params.bnCode,
            clientId = params.clientId,
            userAction = params.userActionValue,
            paypalInstalled = params.paypalInstalled,
            returnAppUrl = params.returnAppUrl,
            cancelAppUrl = params.cancelAppUrl,
            fallbackSchemeUrl = params.fallbackSchemeUrl,
        )
    }

    fun notify(
        event: CreatePayPalSessionEvent,
        params: AppSwitchAnalyticsEventParams,
        errorDescription: String? = null,
    ) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            shopperSessionId = params.shopperSessionId,
            shopperSessionExpiration = params.shopperSessionExpiration,
            matchedAuthenticationMethods = params.matchedAuthenticationMethods,
            fallbackUrl = params.fallbackUrl,
            isCachedSession = params.isCachedSession,
            isVault = params.isVault,
            errorDescription = errorDescription,
            appSwitchEligible = params.appSwitchEligible,
            ineligibleReason = params.ineligibleReason,
            merchantId = params.merchantId,
            bnCode = params.bnCode,
            clientId = params.clientId,
            userAction = params.userActionValue,
            paypalInstalled = params.paypalInstalled,
            returnAppUrl = params.returnAppUrl,
            cancelAppUrl = params.cancelAppUrl,
            fallbackSchemeUrl = params.fallbackSchemeUrl,
        )
    }

    fun notifyApiRequestLatency(endpoint: String, startTime: Long, endTime: Long) {
        analyticsService.sendAnalyticsEvent(
            name = LatencyEvent.API_REQUEST_LATENCY.value,
            endpoint = endpoint,
//            startTime = startTime,
            endTime = endTime
        )
    }

    fun notifyUserPerceivedLatency(
        flow: String,
        presentationType: String,
        startTime: Long,
        endTime: Long
    ) {
        analyticsService.sendAnalyticsEvent(
            name = LatencyEvent.USER_PERCEIVED_LATENCY.value,
            flow = flow,
            presentationType = presentationType,
//            startTime = startTime,
            endTime = endTime
        )
    }
}
