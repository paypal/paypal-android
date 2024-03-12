package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.utils.UIConstants

@Composable
fun CardVaultResultView(result: CardVaultResult) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Setup Token ID", value = result.setupTokenId)
        PropertyView(name = "Status", value = result.status)
    }
}
