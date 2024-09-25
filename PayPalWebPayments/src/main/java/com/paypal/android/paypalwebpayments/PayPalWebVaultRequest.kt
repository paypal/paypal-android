package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.CoreConfig

/**
 * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
 *
 * @property [setupTokenId] ID for the setup token associated with the vault approval
 * @property [approveVaultHref] URL for the approval web page
 */
data class PayPalWebVaultRequest @Deprecated("Use PayPalWebVaultRequest(setupTokenId) instead.")
constructor(
    val config: CoreConfig,
    val setupTokenId: String,
    @Deprecated("The approveVaultHref property is no longer required and will be ignored.")
    val approveVaultHref: String? // NEXT_MAJOR_VERSION: - Remove this property
) {

    /**
     * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
     *
     * @property [setupTokenId] ID for the setup token associated with the vault approval
     */
    constructor(config: CoreConfig, setupTokenId: String) : this(config, setupTokenId, null)
}
