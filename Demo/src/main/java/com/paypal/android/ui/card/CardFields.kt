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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.paypal.android.R

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
                label = { Text(stringResource(R.string.card_field_expiration)) },
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
        value = TextFieldValue(cardNumber, TextRange(cardNumber.length)),
        onValueChange = { onNumberChange(it.text) },
        label = { Text(stringResource(R.string.card_field_card_number)) },
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
        label = { Text(stringResource(R.string.card_field_security_code)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

const val MAX_EXPIRATION_LENGTH = 5
const val MAX_SECURITY_CODE_LENGTH = 4
