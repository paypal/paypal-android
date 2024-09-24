package com.paypal.android.cardpayments

import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService

class CardAnalyticsContext(
    private val config: CoreConfig,
    private val analyticsService: AnalyticsService,
    private val orderId: String? = null,
    private val setupTokenId: String? = null
) {
    fun notifyApproveOrderStarted() {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:started", config, orderId)
    }

    fun notifyConfirmPaymentSourceSucceeded() {
        analyticsService.sendAnalyticsEvent(
            "card-payments:3ds:confirm-payment-source:succeeded",
            config,
            orderId
        )
    }

    fun notify3DSSucceeded() {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:succeeded", config, orderId)
    }

    fun notify3DSChallengeRequired() {
        analyticsService.sendAnalyticsEvent(
            "card-payments:3ds:confirm-payment-source:challenge-required",
            config,
            orderId
        )
    }

    fun notifyConfirmPaymentSourceFailed() {
        analyticsService.sendAnalyticsEvent(
            "card-payments:3ds:confirm-payment-source:failed",
            config,
            orderId
        )
    }

    fun notify3DSFailed() {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:failed", config, orderId)
    }

    fun notifyCardVault3DSSuccess() {
        analyticsService.sendAnalyticsEvent("card:browser-login:canceled", config, setupTokenId)
    }

    fun notifyCardVault3DSFailure() {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:failed", config, orderId)
    }
}