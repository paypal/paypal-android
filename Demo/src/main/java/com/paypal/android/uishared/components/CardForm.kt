package com.paypal.android.uishared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.ui.approveorder.CardNumberVisualTransformation
import com.paypal.android.ui.approveorder.DateVisualTransformation

@ExperimentalMaterial3Api
@Composable
fun CardForm(
    cardNumber: String,
    expirationDate: String,
    securityCode: String,
    onCardNumberChange: (String) -> Unit,
    onExpirationDateChange: (String) -> Unit,
    onSecurityCodeChange: (String) -> Unit,
    onUseTestCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.inverseSurface)
        ) {
            Text(
                text = "CARD",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .weight(1.0f)
                    .padding(start = 16.dp)
            )
            Button(
                shape = RectangleShape,
                onClick = onUseTestCardClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 50.dp)
                    .padding(0.dp)
            ) {
                Text(
                    text = "✨ Use a Test Card",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
        ) {
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = cardNumber,
                label = { Text(stringResource(id = R.string.card_field_card_number)) },
                onValueChange = { value -> onCardNumberChange(value) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                visualTransformation = CardNumberVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = expirationDate,
                    label = { Text(stringResource(id = R.string.card_field_expiration)) },
                    onValueChange = { value -> onExpirationDateChange(value) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    visualTransformation = DateVisualTransformation(),
                    modifier = Modifier.weight(weight = 1.5f)
                )
                OutlinedTextField(
                    value = securityCode,
                    label = { Text(stringResource(id = R.string.card_field_security_code)) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { value -> onSecurityCodeChange(value) },
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun CardFormPreview() {
    MaterialTheme {
        Surface() {
            CardForm(
                cardNumber = "",
                expirationDate = "",
                securityCode = "",
                onCardNumberChange = {},
                onExpirationDateChange = {},
                onSecurityCodeChange = {},
                onUseTestCardClick = {},
            )
        }
    }
}
