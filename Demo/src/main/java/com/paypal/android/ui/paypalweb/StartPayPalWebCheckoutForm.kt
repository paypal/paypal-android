package com.paypal.android.ui.paypalweb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.ui.OptionList
import com.paypal.android.ui.WireframeButton

@Composable
fun StartPayPalWebCheckoutForm(
    fundingSource: PayPalWebCheckoutFundingSource,
    isLoading: Boolean,
    onFundingSourceSelected: (PayPalWebCheckoutFundingSource) -> Unit,
    onSubmit: () -> Unit
) {
    val oneTimeCheckoutValue = stringResource(R.string.funding_source_one_time_checkout)
    val payPalCreditValue = stringResource(R.string.funding_source_pay_pal_credit)
    val payPalPayLaterValue = stringResource(R.string.funding_source_pay_pal_pay_later)
    val selectedValue = when (fundingSource) {
        PayPalWebCheckoutFundingSource.PAYPAL -> oneTimeCheckoutValue
        PayPalWebCheckoutFundingSource.PAYPAL_CREDIT -> payPalCreditValue
        PayPalWebCheckoutFundingSource.PAY_LATER -> payPalPayLaterValue
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Launch PayPal Web Checkout",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(16.dp))
            OptionList(
                title = stringResource(id = R.string.select_funding),
                options = listOf(oneTimeCheckoutValue, payPalCreditValue, payPalPayLaterValue),
                selectedOption = selectedValue,
                onOptionSelected = { option ->
                    val newFundingValue = when (option) {
                        oneTimeCheckoutValue -> PayPalWebCheckoutFundingSource.PAYPAL
                        payPalCreditValue -> PayPalWebCheckoutFundingSource.PAYPAL_CREDIT
                        payPalPayLaterValue -> PayPalWebCheckoutFundingSource.PAY_LATER
                        else -> null
                    }
                    newFundingValue?.let { onFundingSourceSelected(it) }
                }
            )
            Spacer(modifier = Modifier.size(16.dp))
            WireframeButton(
                text = "Start Checkout",
                isLoading = isLoading,
                onClick = { onSubmit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}
