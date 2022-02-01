package com.paypal.android.checkout

import android.net.Uri
import androidx.annotation.NonNull
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.braintreepayments.api.BrowserSwitchOptions
import org.json.JSONObject

internal class BrowserSwitchHelper(private val urlScheme: String) {

    private val redirectUriPayPalCheckout = "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout"

    private fun buildPayPalCheckoutUri(@NonNull orderId: String?, @NonNull config: CoreConfig): Uri {
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://www.paypal.com"
            Environment.SANDBOX -> "https://www.sandbox.paypal.com"
            Environment.STAGING -> "https://www.msmaster.qa.paypal.com"
        }
        return Uri.parse(baseURL)
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("redirect_uri", redirectUriPayPalCheckout)
            .appendQueryParameter("native_xo", "1")
            .build()
    }

    fun configurePayPalBrowserSwitchOptions(
        @NonNull orderId: String?,
        @NonNull config: CoreConfig
    ): BrowserSwitchOptions {
        val metadata = JSONObject().put("order_id", orderId)
        return BrowserSwitchOptions()
            .url(buildPayPalCheckoutUri(orderId, config))
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
    }
}
