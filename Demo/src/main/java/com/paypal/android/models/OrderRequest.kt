package com.paypal.android.models

import com.paypal.android.api.model.OrderIntent
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    val intent: OrderIntent,
    val shouldVaultOnSuccess: Boolean,
    // Matches XOSphere's PPCP Direct integration, which always sends
    // payment_source.paypal.experience_context.payment_method_selected on order creation
    // (PAYPAL / PAYPAL_PAY_LATER / PAYPAL_CREDIT). Plain String here (rather than the SDK's
    // PayPalWebCheckoutFundingSource enum) so this model stays kotlinx-serializable.
    val paymentMethodSelected: String = "PAYPAL_CREDIT",
)
