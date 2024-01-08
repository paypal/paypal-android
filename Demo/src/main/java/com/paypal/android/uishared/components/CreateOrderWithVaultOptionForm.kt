package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.R
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.utils.UIConstants

@Composable
fun CreateOrderWithVaultOptionForm(
    orderIntent: OrderIntent = OrderIntent.AUTHORIZE,
    shouldVault: StoreInVaultOption = StoreInVaultOption.NO,
    onShouldVaultChanged: (StoreInVaultOption) -> Unit = {},
    onIntentOptionChanged: (OrderIntent) -> Unit = {},
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium
    ) {
        EnumOptionList(
            title = stringResource(id = R.string.intent_title),
            stringArrayResId = R.array.intent_options,
            onSelectedOptionChange = { onIntentOptionChanged(it) },
            selectedOption = orderIntent
        )
        EnumOptionList(
            title = stringResource(id = R.string.store_in_vault),
            stringArrayResId = R.array.store_in_vault_options,
            onSelectedOptionChange = { onShouldVaultChanged(it) },
            selectedOption = shouldVault
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
