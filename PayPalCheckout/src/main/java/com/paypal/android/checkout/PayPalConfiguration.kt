package com.paypal.android.checkout

import android.app.Application
import com.paypal.android.core.Environment
import com.paypal.android.core.CoreConfig

class PayPalConfiguration(
    /**
     * Client ID is used to connect the Checkout SDK to your PayPal app. Your app's Client ID can be
     * found in the PayPal Developer Portal.
     *
     * @see [Developer Portal](https://developer.paypal.com/developer/applications/)
     * @see [Managing Sandbox Apps]
     * (https://developer.paypal.com/docs/api-basics/manage-apps/#create-or-edit-sandbox-and-live-apps)
     */
    clientId: String,

    /**
     * Sets the [Environment] that the SDK will run in.
     *
     * Use [Environment.LIVE] for production.
     * Use [Environment.SANDBOX] for development and testing.
     */
    environment: Environment,

    /**
     * [Application] instance that is required to use the SDK.
     */
    val application: Application,

    /**
     * This is the Return URL value that was added to your app in the PayPal Developer Portal.
     *
     * Please ensure that this value is set in the PayPal Developer Portal, as it is required for a
     * successful checkout flow.
     *
     * The Return URL should contain your app's package name appended with "://paypalpay".
     * Example: "com.sample.example://paypalpay"
     *
     * @see [Developer Portal](https://developer.paypal.com/developer/applications/)
     */
    val returnUrl: String,

    /**
     * This is the currencyCode that is used when determining funding eligibility, it is recommended
     * that this value be set to the same currency used for orders. While this value is not required
     * its usage is recommended.
     */
    val currencyCode: CurrencyCode? = null,

    /**
     * Sets the [UserAction] that is used to help with funding eligibility which can be used to help
     * determine if a specific [PaymentButton] can be rendered (among other things such as falling
     * back to a web experience when a native one cannot be provided). While this field is optional
     * it is recommended that this value be set to provide a better buyer experience.
     */
    val userAction: UserAction? = null,

    /**
     * Sets the [PaymentButtonIntent] that is used to help with funding eligibility which can be
     * used to help determine if a specific [PaymentButton] can be rendered.
     */
    val paymentButtonIntent: PaymentButtonIntent? = null,

    /**
     * SettingsConfig sets additional flags for debugging and testing.
     */
    val settingsConfig: SettingsConfig = SettingsConfig(),

) {
    val paymentsConfiguration =
        CoreConfig(clientId = clientId, environment = environment)
}
