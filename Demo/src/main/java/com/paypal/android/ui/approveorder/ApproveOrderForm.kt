package com.paypal.android.ui.approveorder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paypal.android.R
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.uishared.components.ActionButton
import com.paypal.android.uishared.components.CardForm
import com.paypal.android.uishared.components.EnumOptionList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveOrderForm(
    uiState: ApproveOrderUiState,
    onUseTestCardClick: () -> Unit,
    onCardNumberChange: (String) -> Unit,
    onExpirationDateChange: (String) -> Unit,
    onSecurityCodeChange: (String) -> Unit,
    onSCAOptionSelected: (SCA) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.size(8.dp))
        CardForm(
            cardNumber = uiState.cardNumber,
            expirationDate = uiState.cardExpirationDate,
            securityCode = uiState.cardSecurityCode,
            onCardNumberChange = { onCardNumberChange(it) },
            onExpirationDateChange = { onExpirationDateChange(it) },
            onSecurityCodeChange = { onSecurityCodeChange(it) },
            onUseTestCardClick = { onUseTestCardClick() }
        )
        Spacer(modifier = Modifier.size(16.dp))
        EnumOptionList(
            title = stringResource(id = R.string.sca_title),
            stringArrayResId = R.array.sca_options,
            onOptionSelected = { onSCAOptionSelected(it) },
            selectedOption = uiState.scaOption
        )
        Spacer(modifier = Modifier.size(16.dp))
        ActionButton(
            text = "APPROVE ORDER",
            isLoading = uiState.isApproveOrderLoading,
            onClick = { onSubmit() },
            modifier = Modifier.fillMaxWidth()
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
                onSCAOptionSelected = {},
                onUseTestCardClick = {},
                onSubmit = {}
            )
        }
    }
}
