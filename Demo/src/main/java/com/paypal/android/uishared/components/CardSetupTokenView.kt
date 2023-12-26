package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paypal.android.api.model.SetupToken
import com.paypal.android.utils.UIConstants

@Composable
fun SetupTokenView(setupToken: SetupToken) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "ID", value = setupToken.id)
        PropertyView(name = "Customer ID", value = setupToken.customerId)
        PropertyView(name = "Status", value = setupToken.status)
    }
}
