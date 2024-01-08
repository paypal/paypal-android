package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.utils.UIConstants

@Composable
fun CardPaymentTokenView(paymentToken: CardPaymentToken) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "ID", value = paymentToken.id)
        PropertyView(name = "Customer ID", value = paymentToken.customerId)
        PropertyView(name = "Card Brand", value = paymentToken.cardBrand)
        PropertyView(name = "Card Last 4", value = paymentToken.cardLast4)
    }
}
