package com.paypal.android.uishared.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.paypal.android.R
import com.paypal.android.ui.approveorder.DateVisualTransformation

@Composable
fun ExpirationDateTextField(
    expirationDate: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = expirationDate,
        label = { Text(stringResource(id = R.string.card_field_expiration)) },
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        visualTransformation = DateVisualTransformation(),
        modifier = modifier
    )
}