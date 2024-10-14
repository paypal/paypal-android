package com.paypal.android.uishared.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.paypal.android.R
import com.paypal.android.ui.approveorder.CardNumberVisualTransformation

@Composable
fun CardNumberTextField(
    cardNumber: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = cardNumber,
        label = { Text(stringResource(id = R.string.card_field_card_number)) },
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        visualTransformation = CardNumberVisualTransformation(),
        modifier = modifier
    )
}