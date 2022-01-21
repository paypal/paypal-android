package com.paypal.android.checkout

import android.net.Uri
import androidx.annotation.NonNull
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.paypal.authcore.BuildConfig
import com.braintreepayments.api.BrowserSwitchOptions
import org.json.JSONObject


internal class BrowserSwitchHelper {

    fun buildPayPalCheckoutUri(@NonNull orderId: String?, @NonNull config: CoreConfig): Uri {
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://www.paypal.com"
            Environment.SANDBOX -> "https://www.sandbox.paypal.com"
            Environment.STAGING -> "https://www.msmaster.qa.paypal.com"
        }
        return Uri.parse(baseURL)
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI_PAYPAL_CHECKOUT)
            .appendQueryParameter("native_xo", "1")
            .build()
    }

    fun configurePayPalBrowserSwitchOptions(
        @NonNull orderId: String?,
        @NonNull config: CoreConfig
    ): BrowserSwitchOptions {
        val metadata = JSONObject().put("order_id", orderId)
        return BrowserSwitchOptions()
            .requestCode(123)
            .url(buildPayPalCheckoutUri(orderId, config))
            .returnUrlScheme(REDIRECT_URI_PAYPAL_CHECKOUT)
            .metadata(metadata)
    }

    companion object {
        private const val URL_SCHEME = "com.paypal.android.demo"

        private val REDIRECT_URI_PAYPAL_CHECKOUT =
            String.format("%s://x-callback-url/paypal-sdk/paypal-checkout", URL_SCHEME)
    }
}