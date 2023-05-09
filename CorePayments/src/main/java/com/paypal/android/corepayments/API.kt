package com.paypal.android.corepayments

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.analytics.DeviceInspector
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * This class is exposed for internal PayPal use only. Do not use.
 * It is not covered by Semantic Versioning and may change or be removed at any time.
 */
class API internal constructor(
    private val configuration: CoreConfig,
    private val http: Http,
    private val httpRequestFactory: HttpRequestFactory,
) {

    constructor(configuration: CoreConfig) :
            this(
                configuration,
                Http(),
                HttpRequestFactory()
            )

    suspend fun send(apiRequest: APIRequest): HttpResponse {
        val httpRequest =
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        return http.send(httpRequest)
    }
}
