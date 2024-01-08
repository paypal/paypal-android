package com.paypal.android.uishared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.R
import com.paypal.android.ui.approveorder.CardNumberVisualTransformation
import com.paypal.android.ui.approveorder.DateVisualTransformation
import com.paypal.android.utils.UIConstants

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
) {
    Card {
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
                    .padding(start = UIConstants.paddingMedium)
                    .weight(1.0f)
            )
            Button(
                shape = RoundedCornerShape(
                    topStart = UIConstants.buttonCornerRadius,
                    bottomStart = UIConstants.buttonCornerRadius
                ),
                onClick = onUseTestCardClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.defaultMinSize(minHeight = UIConstants.minimumTouchSize)
            ) {
                Text(
                    text = "✨ Use a Test Card",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(UIConstants.paddingSmall)
                )
            }
        }
        Column(
            verticalArrangement = UIConstants.spacingSmall,
            modifier = Modifier.padding(UIConstants.paddingSmall)
        ) {
            CardNumberTextField(
                cardNumber = cardNumber,
                onValueChange = { value -> onCardNumberChange(value) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = UIConstants.spacingMedium) {
                ExpirationDateTextField(
                    expirationDate = expirationDate,
                    onValueChange = { value -> onExpirationDateChange(value) },
                    modifier = Modifier.weight(weight = 1.5f)
                )
                SecurityCodeTextField(
                    securityCode = securityCode,
                    onValueChange = { value -> onSecurityCodeChange(value) },
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
    }
}

@Composable
private fun CardNumberTextField(
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

@Composable
private fun ExpirationDateTextField(
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

@Composable
private fun SecurityCodeTextField(
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

@ExperimentalMaterial3Api
@Preview
@Composable
fun CardFormPreview() {
    MaterialTheme {
        Surface {
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
