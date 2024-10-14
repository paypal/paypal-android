package com.paypal.android.uishared.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.paypal.android.R

@Composable
fun SecurityCodeTextField(
    securityCode: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = securityCode,
        label = { Text(stringResource(id = R.string.card_field_security_code)) },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        visualTransformation = PasswordVisualTransformation(),
        onValueChange = onValueChange,
        modifier = modifier
    )
}