package com.paypal.android.uishared.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.R
import com.paypal.android.uishared.enums.StoreInVaultOption

@Composable
fun StoreInVaultOptionForm(
    modifier: Modifier = Modifier,
    shouldVault: StoreInVaultOption = StoreInVaultOption.NO,
    onShouldVaultChanged: (StoreInVaultOption) -> Unit = {}
) {
    EnumOptionList(
        title = stringResource(id = R.string.store_in_vault),
        stringArrayResId = R.array.store_in_vault_options,
        onSelectedOptionChange = { onShouldVaultChanged(it) },
        selectedOption = shouldVault,
        modifier = modifier
    )
}

@Preview
@Composable
fun StoreInVaultOptionFormPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            StoreInVaultOptionForm()
        }
    }
}
