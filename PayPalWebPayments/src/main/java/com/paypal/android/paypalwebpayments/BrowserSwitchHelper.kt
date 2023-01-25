package com.paypal.android.paypalwebpayments

import android.net.Uri
import androidx.annotation.NonNull
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.braintreepayments.api.BrowserSwitchOptions
import org.json.JSONObject

internal class BrowserSwitchHelper(private val urlScheme: String) {

    private val redirectUriPayPalCheckout = "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout"

    private fun buildPayPalCheckoutUri(
        @NonNull orderId: String?,
        @NonNull config: CoreConfig,
        @NonNull funding: PayPalWebCheckoutFundingSource
    ): Uri {
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
            .appendQueryParameter("fundingSource", funding.value)
            .build()
    }

    fun configurePayPalBrowserSwitchOptions(
        @NonNull orderId: String?,
        @NonNull config: CoreConfig,
        @NonNull funding: PayPalWebCheckoutFundingSource
    ): BrowserSwitchOptions {
        val metadata = JSONObject().put("order_id", orderId)
        return BrowserSwitchOptions()
            .url(buildPayPalCheckoutUri(orderId, config, funding))
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
    }
}
