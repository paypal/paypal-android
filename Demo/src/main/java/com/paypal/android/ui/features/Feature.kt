package com.paypal.android.ui.features

import androidx.annotation.StringRes
import com.paypal.android.R

enum class Feature(@StringRes val stringRes: Int) {
    CARD_VAULT_WITH_PURCHASE(R.string.payment_methods_card),
    CARD_VAULT_WITHOUT_PURCHASE(R.string.feature_vault_without_purchase),
    PAYPAL_WEB(R.string.payment_methods_paypal),
    PAYPAL_NATIVE(R.string.payment_methods_paypal_native)
}
