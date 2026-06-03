package com.paypal.android.corepayments.analytics

import android.content.Context
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.CoreConfig

/**
 * SDK-wide analytics registry. Initialize once in [android.app.Application.onCreate] before
 * creating any SDK clients. All clients and UI components read from here rather than
 * each creating their own [AnalyticsService].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AnalyticsServiceRegistry {
    private lateinit var _service: AnalyticsService
    val service: AnalyticsService get() = _service

    fun initialize(context: Context, coreConfig: CoreConfig) {
        if (!::_service.isInitialized) {
            _service = AnalyticsService(context.applicationContext, coreConfig)
        }
    }
}
