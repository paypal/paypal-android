package com.paypal.android.ui.paypalwebvault

import com.paypal.android.api.model.SetupToken

data class PayPalWebVaultUiState(
    val isCreateSetupTokenLoading: Boolean = false,
    val vaultCustomerId: String = "",
    val setupToken: SetupToken? = null,
    val isUpdateSetupTokenLoading: Boolean = false
)
