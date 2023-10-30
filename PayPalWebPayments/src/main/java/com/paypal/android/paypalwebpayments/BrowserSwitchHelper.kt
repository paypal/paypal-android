package com.paypal.android.paypalwebpayments

import android.net.Uri
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import org.json.JSONObject
import java.net.URL

internal class BrowserSwitchHelper(private val urlScheme: String) {

    private val redirectUriPayPalCheckout = "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout"

    private fun buildPayPalCheckoutUri(
        orderId: String?,
        config: CoreConfig,
        funding: PayPalWebCheckoutFundingSource
    ): Uri {
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://www.paypal.com"
            Environment.SANDBOX -> "https://www.sandbox.paypal.com"
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
        href: String,
        config: CoreConfig,
    ): BrowserSwitchOptions {
//        val metadata = JSONObject().put("order_id", orderId)
        return BrowserSwitchOptions()
            .url(Uri.parse(href))
            .returnUrlScheme(urlScheme)
//            .metadata(metadata)
    }

    fun configurePayPalBrowserSwitchOptions(
        orderId: String?,
        config: CoreConfig,
        funding: PayPalWebCheckoutFundingSource
    ): BrowserSwitchOptions {
        val metadata = JSONObject().put("order_id", orderId)
        return BrowserSwitchOptions()
            .url(buildPayPalCheckoutUri(orderId, config, funding))
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
    }
}
