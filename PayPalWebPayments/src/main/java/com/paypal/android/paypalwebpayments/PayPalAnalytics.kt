package com.paypal.android.paypalwebpayments

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService

internal class PayPalAnalytics(
    private val analyticsService: AnalyticsService
) {
    constructor(context: Context) : this(AnalyticsService(context.applicationContext))

    fun createAnalyticsContext(
        config: CoreConfig,
        orderId: String? = null,
        setupTokenId: String? = null
    ): PayPalAnalyticsContext {
        return PayPalAnalyticsContext(config, analyticsService)
    }

    fun createAnalyticsContext(request: PayPalWebCheckoutRequest): PayPalAnalyticsContext {
        return PayPalAnalyticsContext(request.config, analyticsService, orderId = request.orderId)
    }

    fun createAnalyticsContext(vaultRequest: PayPalWebVaultRequest): PayPalAnalyticsContext {
        return PayPalAnalyticsContext(vaultRequest.config, analyticsService)
    }
}
