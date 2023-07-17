package com.paypal.android.ui.features

import androidx.annotation.StringRes
import com.paypal.android.R

enum class Feature(@StringRes val stringRes: Int) {
    CARD_APPROVE_ORDER(R.string.feature_approve_order),
    CARD_VAULT_WITH_PURCHASE(R.string.feature_vault_with_purchase),
    CARD_VAULT_WITHOUT_PURCHASE(R.string.feature_vault_without_purchase),
    PAYPAL_WEB(R.string.feature_paypal_web),
    PAYPAL_NATIVE(R.string.feature_paypal_native)
}
