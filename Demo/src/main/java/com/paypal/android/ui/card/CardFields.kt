package com.paypal.android.ui.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun CardFields(
    cardViewModel: CardViewModel,
    modifier: Modifier = Modifier
) {
    val cardNumber: String by cardViewModel.cardNumber.observeAsState("")
    val expirationDate: String by cardViewModel.expirationDate.observeAsState("")
    val securityCode: String by cardViewModel.securityCode.observeAsState("")

    Column(modifier = modifier) {
        CardField(
            cardNumber = cardNumber,
            onNumberChange = { cardViewModel.onCardNumberChange(it) },
            modifier = Modifier.fillMaxWidth()
        )
        Row {
            TextField(
                value = TextFieldValue(expirationDate, TextRange(expirationDate.length)),
                onValueChange = {
                    if (it.text.length <= MAX_EXPIRATION_LENGTH) {
                        cardViewModel.onExpirationDateChange(it.text)
                    }
                },
                label = { Text("Expiration") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1F)
            )
            SecurityCodeField(
                securityCode = securityCode,
                onSecurityCodeChange = { cardViewModel.onSecurityCodeChange(it) },
                modifier = Modifier.weight(1F)
            )
        }
    }
}

@Composable
fun CardField(cardNumber: String, onNumberChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = cardNumber,
        onValueChange = { if (it.length <= MAX_CARD_LENGTH) onNumberChange(it) },
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
    TextField(
        value = securityCode,
        onValueChange = { if (it.length <= MAX_SECURITY_CODE_LENGTH) onSecurityCodeChange(it) },
        label = { Text("Security Code") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

const val MAX_CARD_LENGTH = 19
const val MAX_EXPIRATION_LENGTH = 5
const val MAX_SECURITY_CODE_LENGTH = 4
