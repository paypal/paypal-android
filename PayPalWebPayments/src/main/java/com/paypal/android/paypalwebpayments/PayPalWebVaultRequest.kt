package com.paypal.android.paypalwebpayments

/**
 * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vaultAsync].
 *
 * @property [setupTokenId] ID for the setup token associated with the vault approval
 * @property [approveVaultHref] URL for the approval web page
 * @property [appSwitchWhenEligible] whether to switch to the PayPal app when eligible
 * @property [appLinkUrl] The app link URL to use for returning to app
 */
data class PayPalWebVaultRequest @Deprecated("Use PayPalWebVaultRequest(setupTokenId) instead.")
constructor(
    val setupTokenId: String,
    val appSwitchWhenEligible: Boolean = false,
    val appLinkUrl: String? = null,
    @Deprecated("The approveVaultHref property is no longer required and will be ignored.")
    val approveVaultHref: String? = null // NEXT_MAJOR_VERSION: - Remove this property
) {

    /**
     * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vaultAsync].
     *
     * @property [setupTokenId] ID for the setup token associated with the vault approval
     */
    constructor(
        setupTokenId: String,
        appSwitchWhenEligible: Boolean = false,
        appLinkUrl: String? = null
    ) : this(setupTokenId, appSwitchWhenEligible, appLinkUrl, null)
}
