package com.paypal.android.uishared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.paypal.android.paypalpayments.PayPalPhoneNumber
import com.paypal.android.paypalpayments.PayPalUserIdentity
import com.paypal.android.utils.UIConstants

@Composable
fun PayPalUserIdentityForm(
    userIdentity: PayPalUserIdentity?,
    onUserIdentityChange: (PayPalUserIdentity?) -> Unit,
    modifier: Modifier = Modifier
) {
    var existingPayPalSessionIdValue by remember { mutableStateOf(userIdentity?.existingPayPalSessionId ?: "") }
    var emailValue by remember { mutableStateOf(userIdentity?.email ?: "") }
    var countryCodeValue by remember { mutableStateOf(userIdentity?.phone?.countryCode ?: "") }
    var nationalNumberValue by remember { mutableStateOf(userIdentity?.phone?.nationalNumber ?: "") }

    fun notifyChange(existingId: String, email: String, countryCode: String, nationalNumber: String) {
        val newExistingId = existingId.ifBlank { null }
        val newEmail = email.ifBlank { null }
        val newCountryCode = countryCode.ifBlank { null }
        val newNationalNumber = nationalNumber.ifBlank { null }
        val newPhone = if (newCountryCode != null && newNationalNumber != null) {
            PayPalPhoneNumber(countryCode = newCountryCode, nationalNumber = newNationalNumber)
        } else {
            null
        }
        onUserIdentityChange(
            if (newExistingId == null && newEmail == null && newPhone == null) {
                null
            } else {
                PayPalUserIdentity(
                existingPayPalSessionId = newExistingId,
                email = newEmail,
                phone = newPhone
            )
            }
        )
    }

    Card(modifier = modifier) {
        IdentityFormHeader()
        Column {
            IdentityTextField(
                value = existingPayPalSessionIdValue,
                label = "Existing PayPal Session ID",
                onValueChange = { input ->
                    existingPayPalSessionIdValue = input
                    notifyChange(input, emailValue, countryCodeValue, nationalNumberValue)
                }
            )
            IdentityTextField(
                value = emailValue,
                label = "Email",
                onValueChange = { input ->
                    emailValue = input
                    notifyChange(existingPayPalSessionIdValue, input, countryCodeValue, nationalNumberValue)
                }
            )
            IdentityTextField(
                value = countryCodeValue,
                label = "Phone Country Code",
                onValueChange = { input ->
                    countryCodeValue = input
                    notifyChange(existingPayPalSessionIdValue, emailValue, input, nationalNumberValue)
                }
            )
            IdentityTextField(
                value = nationalNumberValue,
                label = "Phone National Number",
                onValueChange = { input ->
                    nationalNumberValue = input
                    notifyChange(existingPayPalSessionIdValue, emailValue, countryCodeValue, input)
                }
            )
        }
    }
}

@Composable
private fun IdentityTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    bottomPadding: Dp = UIConstants.paddingMedium,
) {
    OutlinedTextField(
        value = value,
        label = { Text(label) },
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = UIConstants.paddingMedium,
                end = UIConstants.paddingMedium,
                bottom = bottomPadding
            )
    )
}

@Composable
private fun IdentityFormHeader() {
    Row(modifier = Modifier.background(MaterialTheme.colorScheme.inverseSurface)) {
        Text(
            text = "USER IDENTITY",
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(UIConstants.paddingMedium)
                .fillMaxWidth()
        )
    }
}

@Preview
@Composable
fun PayPalUserIdentityFormPreview() {
    MaterialTheme {
        Surface {
            PayPalUserIdentityForm(
                userIdentity = null,
                onUserIdentityChange = {}
            )
        }
    }
}
