package com.paypal.android.paypalwebpayments

/**
 * Creates an instance of a PayPalRequest.
 *
 * @param orderId The ID of the order to be approved.
 * @param fundingSource specify funding (credit, paylater or default)
 * @param appSwitchWhenEligible whether to switch to the PayPal app when eligible
 * @param appLinkUrl The app link URL to use for browser switch, or null to use default.
 *   Example values: "$myAppScheme://$myAppHost/$myPath", "https://$myDomain/$myPath"
 *
 *   **IMPORTANT:** When using auth tab launcher flow (passing `ActivityResultLauncher` to [PayPalWebCheckoutClient.start]),
 *   this URL must match the success URL specified in the `experience_context` when creating the order
 *   on your server. The auth tab uses this URL to determine when the authentication flow is complete
 *   and automatically close the browser tab. If the URLs don't match, the browser tab will not close
 *   automatically after successful authentication.
 *
 *   For example, if your order creation request includes:
 *   ```
 *   "payment_source": {
 *     "paypal": {
 *       "experience_context": {
 *         "return_url": "https://example.com/success",
 *         "cancel_url": "https://example.com/cancel"
 *       }
 *     }
 *   }
 *   ```
 *   Then `appLinkUrl` should be set to `"https://example.com/success"`.
 *
 * @param fallbackUrlScheme The fallback custom URL scheme to use when app link is not configured properly.
 */
data class PayPalWebCheckoutRequest @JvmOverloads constructor(
    val orderId: String,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL,
    val appSwitchWhenEligible: Boolean = false,
    val appLinkUrl: String? = null,
    val fallbackUrlScheme: String? = null
)
