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
import com.paypal.android.paypalwebpayments.PayPalUserIdentity
import com.paypal.android.utils.UIConstants

@Composable
fun PayPalUserIdentityForm(
    userIdentity: PayPalUserIdentity?,
    onUserIdentityChange: (PayPalUserIdentity?) -> Unit,
    modifier: Modifier = Modifier
) {
    var existingPayPalSessionIdValue by remember { mutableStateOf(userIdentity?.existingPayPalSessionId ?: "") }
    var emailValue by remember { mutableStateOf(userIdentity?.email ?: "") }
    var phoneValue by remember { mutableStateOf(userIdentity?.phone ?: "") }

    fun notifyChange(existingId: String, email: String, phone: String) {
        val newExistingId = existingId.ifBlank { null }
        val newEmail = email.ifBlank { null }
        val newPhone = phone.ifBlank { null }
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
                    notifyChange(input, emailValue, phoneValue)
                }
            )
            IdentityTextField(
                value = emailValue,
                label = "Email",
                onValueChange = { input ->
                    emailValue = input
                    notifyChange(existingPayPalSessionIdValue, input, phoneValue)
                }
            )
            IdentityTextField(
                value = phoneValue,
                label = "Phone",
                onValueChange = { input ->
                    phoneValue = input
                    notifyChange(existingPayPalSessionIdValue, emailValue, input)
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
