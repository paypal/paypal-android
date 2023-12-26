package com.paypal.android.ui.paypalweb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.uishared.components.ActionButton
import com.paypal.android.uishared.components.EnumOptionList
import com.paypal.android.utils.UIConstants

@Composable
fun StartPayPalWebCheckoutForm(
    fundingSource: PayPalWebCheckoutFundingSource,
    onFundingSourceSelected: (PayPalWebCheckoutFundingSource) -> Unit,
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium
    ) {
        EnumOptionList(
            title = stringResource(id = R.string.pay_pal_funding_source_title),
            stringArrayResId = R.array.pay_pal_funding_source_options,
            onOptionSelected = onFundingSourceSelected,
            selectedOption = fundingSource
        )
    }
}
