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
            isVaultRequest = params.isVaultRequest,
            appSwitchEligible = params.appSwitchEligible,
            ineligibleReason = params.ineligibleReason,
            merchantId = params.merchantId,
            bnCode = params.bnCode,
            clientId = params.clientId,
            userAction = params.userActionValue,
            paypalNativeAppInstalled = params.paypalNativeAppInstalled,
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
            isVaultRequest = params.isVaultRequest,
            appSwitchEligible = params.appSwitchEligible,
            ineligibleReason = params.ineligibleReason,
            merchantId = params.merchantId,
            bnCode = params.bnCode,
            clientId = params.clientId,
            userAction = params.userActionValue,
            paypalNativeAppInstalled = params.paypalNativeAppInstalled,
            returnAppUrl = params.returnAppUrl,
            cancelAppUrl = params.cancelAppUrl,
            fallbackSchemeUrl = params.fallbackSchemeUrl,
        )
    }

    fun notify(event: CreatePayPalSessionEvent, params: AppSwitchAnalyticsEventParams, errorDescription: String? = null) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            shopperSessionId = params.shopperSessionId,
            shopperSessionExpiration = params.shopperSessionExpiration,
            matchedAuthenticationMethods = params.matchedAuthenticationMethods,
            fallbackUrl = params.fallbackUrl,
            isCachedSession = params.isCachedSession,
            isVaultRequest = params.isVaultRequest,
            errorDescription = errorDescription,
            appSwitchEligible = params.appSwitchEligible,
            ineligibleReason = params.ineligibleReason,
            merchantId = params.merchantId,
            bnCode = params.bnCode,
            clientId = params.clientId,
            userAction = params.userActionValue,
            paypalNativeAppInstalled = params.paypalNativeAppInstalled,
            returnAppUrl = params.returnAppUrl,
            cancelAppUrl = params.cancelAppUrl,
            fallbackSchemeUrl = params.fallbackSchemeUrl,
        )
    }
}
