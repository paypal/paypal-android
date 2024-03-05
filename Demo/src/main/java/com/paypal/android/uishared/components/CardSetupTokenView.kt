package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.utils.UIConstants

@Composable
fun CardSetupTokenView(setupToken: CardSetupToken) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "ID", value = setupToken.id)
        PropertyView(name = "Customer ID", value = setupToken.customerId)
        PropertyView(name = "Status", value = setupToken.status)
        setupToken.verificationStatus?.let { verificationStatus ->
            PropertyView(name = "Verification Status", value = verificationStatus)
        }
    }
}
