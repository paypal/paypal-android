package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.ReturnToAppStrategy

/**
 * Request to vault a PayPal payment method.
 *
 * @deprecated Use [PayPalWebCheckoutClient.createPayPalSession] followed by
 * [PayPalWebCheckoutClient.vault] with only the setup token id instead.
 */
@Deprecated(
    message = "Use createPayPalSession() followed by vault(activity, setupTokenId, callback) instead.",
    level = DeprecationLevel.WARNING
)
data class PayPalWebVaultRequest @Deprecated("Use vault(activity, setupTokenId, callback) instead.")
constructor(
    val setupTokenId: String,
    val returnToAppStrategy: ReturnToAppStrategy? = null,
    @Deprecated("The approveVaultHref property is no longer required and will be ignored.")
    val approveVaultHref: String? = null // NEXT_MAJOR_VERSION: - Remove this property
) {

    /**
     * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
     *
     * @property [setupTokenId] id for the setup token associated with the vault approval
     */
    constructor(
        setupTokenId: String,
        returnToAppStrategy: ReturnToAppStrategy? = null
    ) : this(setupTokenId, returnToAppStrategy, null)
}
