package com.paypal.android.uishared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.paypal.android.paypalwebpayments.PayPalUserIdentity
import com.paypal.android.uishared.components.IdentityOption.EMAIL
import com.paypal.android.uishared.components.IdentityOption.NONE
import com.paypal.android.uishared.components.IdentityOption.EXISTING_PAYPAL_SESSION
import com.paypal.android.utils.UIConstants

private enum class IdentityOption {
    NONE,
    EMAIL,
    EXISTING_PAYPAL_SESSION;
}

private fun PayPalUserIdentity?.toIdentityOption(): IdentityOption = when (this) {
    is PayPalUserIdentity.Email -> EMAIL
    is PayPalUserIdentity.ExistingPayPalSession -> EXISTING_PAYPAL_SESSION
    null -> NONE
}

@Composable
fun PayPalUserIdentityForm(
    userIdentity: PayPalUserIdentity?,
    onUserIdentityChange: (PayPalUserIdentity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedOption = userIdentity.toIdentityOption()
    Card(modifier = modifier) {
        IdentityFormHeader()
        Column(modifier = Modifier.selectableGroup()) {
            IdentityOption.entries.forEachIndexed { index, option ->
                IdentityOptionItem(
                    option = option,
                    isSelected = option == selectedOption,
                    onSelect = {
                        onUserIdentityChange(
                            when (option) {
                                NONE -> null
                                EMAIL -> PayPalUserIdentity.Email()
                                EXISTING_PAYPAL_SESSION ->
                                    PayPalUserIdentity.ExistingPayPalSession("")
                            }
                        )
                    },
                    onUserIdentityChange = onUserIdentityChange,
                )
                if (index != IdentityOption.entries.lastIndex) {
                    Divider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
                }
            }
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

@Composable
private fun IdentityOptionRow(
    option: IdentityOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = UIConstants.minimumTouchSize)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
    ) {
        Text(
            text = option.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(UIConstants.paddingMedium)
                .weight(1.0f)
                .align(Alignment.CenterVertically)
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = UIConstants.paddingMedium)
        )
    }
}

@Composable
private fun EmailIdentityFields(onUserIdentityChange: (PayPalUserIdentity?) -> Unit) {
    var emailValue by remember { mutableStateOf("") }
    var phoneValue by remember { mutableStateOf("") }
    IdentityTextField(
        value = emailValue,
        label = "Email",
        bottomPadding = UIConstants.paddingSmall,
        onValueChange = { input ->
            emailValue = input
            onUserIdentityChange(
                PayPalUserIdentity.Email(
                    email = input.ifBlank { null },
                    phone = phoneValue.ifBlank { null }
                )
            )
        }
    )
    IdentityTextField(
        value = phoneValue,
        label = "Phone",
        onValueChange = { input ->
            phoneValue = input
            onUserIdentityChange(
                PayPalUserIdentity.Email(
                    email = emailValue.ifBlank { null },
                    phone = input.ifBlank { null }
                )
            )
        }
    )
}

@Composable
private fun ExistingPayPalSessionIdentityFields(
    onUserIdentityChange: (PayPalUserIdentity?) -> Unit,
) {
    var sessionIdValue by remember { mutableStateOf("") }
    IdentityTextField(
        value = sessionIdValue,
        label = "Existing PayPal Session ID",
        onValueChange = { input ->
            sessionIdValue = input
            onUserIdentityChange(
                PayPalUserIdentity.ExistingPayPalSession(existingPayPalSessionId = input)
            )
        }
    )
}

@Composable
private fun IdentityOptionItem(
    option: IdentityOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUserIdentityChange: (PayPalUserIdentity?) -> Unit,
) {
    Column {
        IdentityOptionRow(option = option, isSelected = isSelected, onClick = onSelect)
        if (isSelected) {
            when (option) {
                NONE -> {}
                EMAIL -> EmailIdentityFields(onUserIdentityChange = onUserIdentityChange)
                EXISTING_PAYPAL_SESSION ->
                    ExistingPayPalSessionIdentityFields(onUserIdentityChange)
            }
        }
    }
}

@Preview
@Composable
fun PayPalUserIdentityFormPreview() {
    MaterialTheme {
        Surface {
            PayPalUserIdentityForm(
                userIdentity = PayPalUserIdentity.Email(
                    email = "test@example.com",
                    phone = null
                ),
                onUserIdentityChange = {}
            )
        }
    }
}
