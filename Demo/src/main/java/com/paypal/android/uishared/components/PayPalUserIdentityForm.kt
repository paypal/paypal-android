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
import com.paypal.android.uishared.components.IdentityOption.PHONE
import com.paypal.android.uishared.components.IdentityOption.SERVER_SIDE_SHOPPER_SESSION
import com.paypal.android.uishared.components.IdentityOption.UNKNOWN
import com.paypal.android.utils.UIConstants

private enum class IdentityOption(val inputHint: String?) {
    UNKNOWN(null),
    EMAIL("Email"),
    PHONE("Phone"),
    SERVER_SIDE_SHOPPER_SESSION("ServerSideShopperSession");

    fun toPayPalUserIdentity(input: String?): PayPalUserIdentity {
        return when (this) {
            UNKNOWN -> PayPalUserIdentity.Unknown
            EMAIL -> PayPalUserIdentity.Email(input ?: "")
            PHONE -> PayPalUserIdentity.Phone(input ?: "")
            SERVER_SIDE_SHOPPER_SESSION ->
                PayPalUserIdentity.ServerSideShopperSession(input ?: "")
        }
    }
}

private fun PayPalUserIdentity.toIdentityOption(): IdentityOption {
    return when (this) {
        is PayPalUserIdentity.Unknown -> UNKNOWN
        is PayPalUserIdentity.Email -> EMAIL
        is PayPalUserIdentity.Phone -> PHONE
        is PayPalUserIdentity.ServerSideShopperSession -> SERVER_SIDE_SHOPPER_SESSION
    }
}

@Composable
fun PayPalUserIdentityForm(
    userIdentity: PayPalUserIdentity,
    onUserIdentityChange: (PayPalUserIdentity) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedOption = userIdentity.toIdentityOption()
    var inputValue by remember(selectedOption) {
        mutableStateOf(userIdentity.identifier ?: "")
    }

    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.inverseSurface)
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
                                    onUserIdentityChange(option.toPayPalUserIdentity(inputValue))
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
                    if (isSelected && option != UNKNOWN) {
                        OutlinedTextField(
                            value = inputValue,
                            label = { Text(option.inputHint ?: "") },
                            onValueChange = { input ->
                                inputValue = input
                                onUserIdentityChange(option.toPayPalUserIdentity(input))
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
                userIdentity = PayPalUserIdentity.Email("test@example.com"),
                onUserIdentityChange = {}
            )
        }
    }
}
