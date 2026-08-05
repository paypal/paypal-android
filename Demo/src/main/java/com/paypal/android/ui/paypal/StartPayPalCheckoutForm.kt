package com.paypal.android.ui.paypal

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.paypalpayments.PayPalCheckoutFundingSource
import com.paypal.android.uishared.components.EnumOptionList
import com.paypal.android.utils.UIConstants

@Composable
fun StartPayPalCheckoutForm(
    fundingSource: PayPalCheckoutFundingSource,
    onFundingSourceChange: (PayPalCheckoutFundingSource) -> Unit,
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium
    ) {
        EnumOptionList(
            title = stringResource(id = R.string.pay_pal_funding_source_title),
            stringArrayResId = R.array.pay_pal_funding_source_options,
            onSelectedOptionChange = onFundingSourceChange,
            selectedOption = fundingSource
        )
    }
}
