package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.ReturnToAppStrategy

/**
 * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
 *
 * @property [setupTokenId] ID for the setup token associated with the vault approval
 * @property [appSwitchWhenEligible] whether to switch to the PayPal app when eligible
 * @property [returnToAppStrategy] Strategy for returning to the app after checkout flow
 * @property [approveVaultHref] URL for the approval web page
 */
data class PayPalWebVaultRequest @Deprecated("Use PayPalWebVaultRequest(setupTokenId) instead.")
constructor(
    val setupTokenId: String,
    val appSwitchWhenEligible: Boolean = false,
    val returnToAppStrategy: ReturnToAppStrategy? = null,
    @Deprecated("The approveVaultHref property is no longer required and will be ignored.")
    val approveVaultHref: String? = null // NEXT_MAJOR_VERSION: - Remove this property
) {

    /**
     * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
     *
     * @property [setupTokenId] ID for the setup token associated with the vault approval
     */
    constructor(
        setupTokenId: String,
        appSwitchWhenEligible: Boolean = false,
        returnToAppStrategy: ReturnToAppStrategy? = null
    ) : this(setupTokenId, appSwitchWhenEligible, returnToAppStrategy, null)
}
