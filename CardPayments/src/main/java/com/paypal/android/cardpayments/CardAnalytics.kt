package com.paypal.android.cardpayments

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService

class CardAnalytics(
    private val analyticsService: AnalyticsService
) {
    constructor(context: Context) : this(AnalyticsService(context.applicationContext))

    fun createAnalyticsContext(
        config: CoreConfig,
        orderId: String? = null,
        setupTokenId: String? = null
    ): CardAnalyticsContext {
        return CardAnalyticsContext(config, analyticsService, orderId, setupTokenId)
    }

    fun createAnalyticsContext(cardRequest: CardRequest.ApproveOrder): CardAnalyticsContext {
        return CardAnalyticsContext(
            cardRequest.config,
            analyticsService,
            orderId = cardRequest.orderId
        )
    }

    fun createAnalyticsContext(vaultRequest: CardRequest.Vault): CardAnalyticsContext {
        return CardAnalyticsContext(
            vaultRequest.config,
            analyticsService,
            setupTokenId = vaultRequest.setupTokenId
        )
    }
}