package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.ui.OptionList
import com.paypal.android.ui.WireframeButton
import com.paypal.android.uishared.enums.BooleanOption

@Composable
fun CreateOrderWithVaultOptionForm(
    orderIntent: OrderIntent = OrderIntent.AUTHORIZE,
    shouldVault: BooleanOption = BooleanOption.NO,
    vaultCustomerId: String = "",
    isLoading: Boolean = false,
    onShouldVaultChanged: (BooleanOption) -> Unit = {},
    onVaultCustomerIdChanged: (String) -> Unit = {},
    onIntentOptionSelected: (OrderIntent) -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    val localFocusManager = LocalFocusManager.current

    val captureValue = stringResource(id = R.string.intent_capture)
    val authorizeValue = stringResource(id = R.string.intent_authorize)
    val selectedOrderIntent = when (orderIntent) {
        OrderIntent.CAPTURE -> captureValue
        OrderIntent.AUTHORIZE -> authorizeValue
    }
    Column {
        Spacer(modifier = Modifier.size(8.dp))
        OptionList(
            title = stringResource(id = R.string.intent_title),
            options = listOf(authorizeValue, captureValue),
            selectedOption = selectedOrderIntent,
            onOptionSelected = { option ->
                val newOrderIntent = when (option) {
                    captureValue -> OrderIntent.CAPTURE
                    authorizeValue -> OrderIntent.AUTHORIZE
                    else -> null
                }
                newOrderIntent?.let { onIntentOptionSelected(it) }
            }
        )
        Spacer(modifier = Modifier.size(16.dp))
        EnumOptionList(
            title = stringResource(id = R.string.store_in_vault),
            stringArrayResId = R.array.boolean_options,
            onOptionSelected = { onShouldVaultChanged(it) },
            selectedOption = shouldVault
        )

        OutlinedTextField(
            value = vaultCustomerId,
            label = { Text("VAULT CUSTOMER ID (OPTIONAL)") },
            onValueChange = { onVaultCustomerIdChanged(it) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { localFocusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.size(8.dp))
        WireframeButton(
            text = "Create Order",
            isLoading = isLoading,
            onClick = { onSubmit() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
    }
}

@Preview
@Composable
fun CreateOrderWithVaultOptionFormPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CreateOrderWithVaultOptionForm()
        }
    }
}
