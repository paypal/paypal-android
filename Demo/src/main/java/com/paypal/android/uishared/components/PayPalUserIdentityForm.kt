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
import com.paypal.android.paypalwebpayments.PayPalUserIdentity
import com.paypal.android.uishared.components.IdentityOption.EMAIL
import com.paypal.android.uishared.components.IdentityOption.NONE
import com.paypal.android.uishared.components.IdentityOption.SERVER_SIDE_SHOPPER_SESSION
import com.paypal.android.utils.UIConstants

private enum class IdentityOption {
    NONE,
    EMAIL,
    SERVER_SIDE_SHOPPER_SESSION;
}

private fun PayPalUserIdentity.toIdentityOption(): IdentityOption = when (this) {
    is PayPalUserIdentity.None -> NONE
    is PayPalUserIdentity.Email -> EMAIL
    is PayPalUserIdentity.ServerSideShopperSession -> SERVER_SIDE_SHOPPER_SESSION
}

@Suppress("LongMethod")
@Composable
fun PayPalUserIdentityForm(
    userIdentity: PayPalUserIdentity,
    onUserIdentityChange: (PayPalUserIdentity) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedOption = userIdentity.toIdentityOption()

    // Email option tracks two independent fields
    var emailValue by remember(selectedOption) {
        mutableStateOf((userIdentity as? PayPalUserIdentity.Email)?.email ?: "")
    }
    var phoneValue by remember(selectedOption) {
        mutableStateOf((userIdentity as? PayPalUserIdentity.Email)?.phone ?: "")
    }

    // ServerSideShopperSession option tracks one field
    var sessionIdValue by remember(selectedOption) {
        mutableStateOf(
            (userIdentity as? PayPalUserIdentity.ServerSideShopperSession)
                ?.serverSideShopperSessionId ?: ""
        )
    }

    Card(modifier = modifier) {
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.inverseSurface)
        ) {
            Text(
                text = "USER IDENTITY",
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(UIConstants.paddingMedium)
                    .fillMaxWidth()
            )
        }
        Column(modifier = Modifier.selectableGroup()) {
            IdentityOption.entries.forEachIndexed { index, option ->
                val isSelected = option == selectedOption
                val isLast = index == IdentityOption.entries.lastIndex

                Column {
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = UIConstants.minimumTouchSize)
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    onUserIdentityChange(
                                        when (option) {
                                            NONE -> PayPalUserIdentity.None
                                            EMAIL -> PayPalUserIdentity.Email(
                                                email = emailValue.ifBlank { null },
                                                phone = phoneValue.ifBlank { null }
                                            )
                                            SERVER_SIDE_SHOPPER_SESSION ->
                                                PayPalUserIdentity.ServerSideShopperSession(
                                                    serverSideShopperSessionId = sessionIdValue
                                                )
                                        }
                                    )
                                },
                                role = Role.RadioButton
                            )
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

                    if (isSelected) {
                        when (option) {
                            NONE -> { /* no input fields */ }
                            EMAIL -> {
                                OutlinedTextField(
                                    value = emailValue,
                                    label = { Text("Email") },
                                    onValueChange = { input ->
                                        emailValue = input
                                        onUserIdentityChange(
                                            PayPalUserIdentity.Email(
                                                email = input.ifBlank { null },
                                                phone = phoneValue.ifBlank { null }
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = UIConstants.paddingMedium,
                                            end = UIConstants.paddingMedium,
                                            bottom = UIConstants.paddingSmall
                                        )
                                )
                                OutlinedTextField(
                                    value = phoneValue,
                                    label = { Text("Phone") },
                                    onValueChange = { input ->
                                        phoneValue = input
                                        onUserIdentityChange(
                                            PayPalUserIdentity.Email(
                                                email = emailValue.ifBlank { null },
                                                phone = input.ifBlank { null }
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = UIConstants.paddingMedium,
                                            end = UIConstants.paddingMedium,
                                            bottom = UIConstants.paddingMedium
                                        )
                                )
                            }
                            SERVER_SIDE_SHOPPER_SESSION -> {
                                OutlinedTextField(
                                    value = sessionIdValue,
                                    label = { Text("Server-Side Shopper Session ID") },
                                    onValueChange = { input ->
                                        sessionIdValue = input
                                        onUserIdentityChange(
                                            PayPalUserIdentity.ServerSideShopperSession(
                                                serverSideShopperSessionId = input
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = UIConstants.paddingMedium,
                                            end = UIConstants.paddingMedium,
                                            bottom = UIConstants.paddingMedium
                                        )
                                )
                            }
                        }
                    }
                }

                if (!isLast) {
                    Divider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
                }
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
