package com.paypal.android.paypalwebpayments

/**
 * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
 *
 * @property [setupTokenId] ID for the setup token associated with the vault approval
 * @property [approveVaultHref] URL for the approval web page
 * @property [appSwitchWhenEligible] whether to switch to the PayPal app when eligible
 */
data class PayPalWebVaultRequest @Deprecated("Use PayPalWebVaultRequest(setupTokenId) instead.")
constructor(
    val setupTokenId: String,
    val appSwitchWhenEligible: Boolean = false,
    @Deprecated("The approveVaultHref property is no longer required and will be ignored.")
    val approveVaultHref: String? = null // NEXT_MAJOR_VERSION: - Remove this property
) {

    /**
     * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
     *
     * @property [setupTokenId] ID for the setup token associated with the vault approval
     */
    constructor(setupTokenId: String) : this(setupTokenId, false)
}
