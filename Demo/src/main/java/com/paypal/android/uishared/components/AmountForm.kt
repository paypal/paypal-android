package com.paypal.android.uishared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.R
import com.paypal.android.utils.UIConstants

@Composable
fun AmountForm(
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        AmountFormHeader()
        AmountTextField(
            amount = amount,
            onValueChange = onAmountChange
        )
    }
}

@Composable
private fun AmountFormHeader() {
    Row(modifier = Modifier.background(MaterialTheme.colorScheme.inverseSurface)) {
        Text(
            text = stringResource(id = R.string.amount_title),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(UIConstants.paddingMedium)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun AmountTextField(
    amount: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = amount,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = UIConstants.paddingMedium,
                end = UIConstants.paddingMedium,
                top = UIConstants.paddingMedium,
                bottom = UIConstants.paddingMedium
            )
    )
}

@Preview
@Composable
fun AmountFormPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AmountForm(
                amount = "10.99",
                onAmountChange = {}
            )
        }
    }
}
