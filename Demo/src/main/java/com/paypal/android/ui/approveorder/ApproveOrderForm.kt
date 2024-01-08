package com.paypal.android.ui.approveorder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.paypal.android.R
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.uishared.components.CardForm
import com.paypal.android.uishared.components.EnumOptionList
import com.paypal.android.utils.UIConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveOrderForm(
    uiState: ApproveOrderUiState,
    onUseTestCardClick: () -> Unit,
    onCardNumberChange: (String) -> Unit,
    onExpirationDateChange: (String) -> Unit,
    onSecurityCodeChange: (String) -> Unit,
    onSCAChange: (SCA) -> Unit,
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        CardForm(
            cardNumber = uiState.cardNumber,
            expirationDate = uiState.cardExpirationDate,
            securityCode = uiState.cardSecurityCode,
            onCardNumberChange = { onCardNumberChange(it) },
            onExpirationDateChange = { onExpirationDateChange(it) },
            onSecurityCodeChange = { onSecurityCodeChange(it) },
            onUseTestCardClick = { onUseTestCardClick() }
        )
        EnumOptionList(
            title = stringResource(id = R.string.sca_title),
            stringArrayResId = R.array.sca_options,
            onSelectedOptionChange = { onSCAChange(it) },
            selectedOption = uiState.scaOption
        )
    }
}

@Preview
@Composable
fun ApproveOrderFormPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ApproveOrderForm(
                uiState = ApproveOrderUiState(),
                onCardNumberChange = {},
                onExpirationDateChange = {},
                onSecurityCodeChange = {},
                onSCAChange = {},
                onUseTestCardClick = {},
            )
        }
    }
}
