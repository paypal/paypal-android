package com.paypal.android.cardpayments

import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService

class CardAnalyticsContext(
    private val config: CoreConfig,
    private val analyticsService: AnalyticsService,
    val trackingId: String,
    private val orderId: String? = null,
    private val setupTokenId: String? = null
) {
    fun notifyConfirmPaymentSourceStarted() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:started",
            config = config,
            orderId = orderId,
            trackingId = trackingId
        )
    }

    fun notifyConfirmPaymentSourceSucceeded() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:succeeded",
            config = config,
            orderId = orderId,
            trackingId = trackingId
        )
    }

    fun notifyConfirmPaymentSourceFailed() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:failed",
            config = config,
            orderId = orderId,
            trackingId = trackingId
        )
    }

    fun notifyConfirmPaymentSourceSCARequired() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:sca-required",
            config = config,
            orderId = orderId,
            trackingId = trackingId
        )
    }

    fun notifyConfirmPaymentSourceSCADidLaunch() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:sca-did-launch",
            config = config,
            orderId = orderId,
            trackingId = trackingId
        )
    }

    fun notifyConfirmPaymentSourceSCALaunchFailed() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:sca-launch-failed",
            config = config,
            orderId = orderId,
            trackingId = trackingId
        )
    }

    fun notifyConfirmPaymentSourceSCASucceeded() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:sca-succeeded",
            config = config,
            orderId = orderId,
            trackingId = trackingId
        )
    }

    fun notifyConfirmPaymentSourceSCAResponseInvalid() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:sca-response-invalid",
            config = config,
            orderId = orderId,
            trackingId = trackingId
        )
    }

    fun notifyConfirmPaymentSourceSCAFailed() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:sca-failed",
            config = config,
            orderId = orderId,
            trackingId = trackingId
        )
    }

    fun notifyVaultStarted() {
        analyticsService.sendAnalyticsEvent(
            "card:vault:started",
            config = config,
            trackingId = trackingId,
        )
    }

    fun notifyVaultUpdateSetupTokenSucceeded() {
        analyticsService.sendAnalyticsEvent(
            "card:vault:update-setup-token-succeeded",
            config = config,
            trackingId = trackingId,
        )
    }

    fun notifyVaultSucceeded() {
        analyticsService.sendAnalyticsEvent(
            "card:vault:succeeded",
            config = config,
            trackingId = trackingId,
        )
    }

    fun notifyVaultSCARequired() {
        analyticsService.sendAnalyticsEvent(
            "card:confirm-payment-source:sca-required",
            config = config,
            trackingId = trackingId
        )
    }

    fun notifyVaultSCADidLaunch() {
        analyticsService.sendAnalyticsEvent(
            "card:vault:sca-did-launch",
            config = config,
            trackingId = trackingId
        )
    }

    fun notifyVaultSCALaunchFailed() {
        analyticsService.sendAnalyticsEvent(
            "card:vault:sca-launch-failed",
            config = config,
            trackingId = trackingId
        )
    }

    fun notifyVaultSCASucceeded() {
        analyticsService.sendAnalyticsEvent(
            "card:vault:sca-succeeded",
            config = config,
            trackingId = trackingId
        )
    }

    fun notifyVaultSCAUserCanceled() {
        analyticsService.sendAnalyticsEvent(
            "card:vault:sca-user-canceled",
            config = config,
            trackingId = trackingId
        )
    }

    fun notifyVaultSCAFailed() {
        analyticsService.sendAnalyticsEvent(
            "card:vault:sca-failed",
            config = config,
            trackingId = trackingId
        )
    }
}