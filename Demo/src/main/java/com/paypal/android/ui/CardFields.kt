package com.paypal.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun CardFields(modifier: Modifier = Modifier) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var securityCode by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        CardField(
            cardNumber = cardNumber,
            onNumberChange = { cardNumber = it },
            modifier = Modifier.fillMaxWidth()
        )
        Row {
            TextField(
                value = expiry,
                onValueChange = { expiry = it },
                label = { Text("Expiration") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1F)
            )
            SecurityCodeField(
                securityCode = securityCode,
                onSecurityCodeChange = { securityCode = it },
                modifier = Modifier.weight(1F)
            )
        }
    }
}

@Composable
fun CardField(cardNumber: String, onNumberChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val maxCardLength = 16

    TextField(
        value = cardNumber,
        onValueChange = { if (it.length <= maxCardLength) onNumberChange(it) },
        label = { Text("Card Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun SecurityCodeField(
    securityCode: String,
    onSecurityCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxSecurityCodeLength = 3

    TextField(
        value = securityCode,
        onValueChange = { if (it.length <= maxSecurityCodeLength) onSecurityCodeChange(it) },
        label = { Text("Security Code") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}