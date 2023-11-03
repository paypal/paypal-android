package com.paypal.android.ui.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.api.model.Order
import com.paypal.android.ui.card.validation.CardViewUiState
import com.paypal.android.uishared.components.CompleteOrderForm
import com.paypal.android.uishared.components.CreateOrderWithVaultOptionForm
import com.paypal.android.uishared.components.MessageView
import com.paypal.android.uishared.components.OrderView

// TODO: Investigate the best way to break this composable up into smaller individual units
@Suppress("LongMethod")
@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalMaterial3Api
@Composable
fun CardView(
    viewModel: CardViewModel = viewModel(),
    onCreateOrderSubmit: () -> Unit = {},
    onApproveOrderSubmit: () -> Unit = {},
    onCompleteOrderSubmit: () -> Unit = {},
    onUseTestCardClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        // continuously scroll to bottom of the list when event state is updated
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState)
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        CreateOrderWithVaultOptionForm(
            title = "Create an Order to proceed:",
            orderIntent = uiState.intentOption,
            shouldVault = uiState.shouldVault,
            vaultCustomerId = uiState.customerId,
            isLoading = uiState.isCreateOrderLoading,
            onIntentOptionSelected = { value -> viewModel.intentOption = value },
            onShouldVaultChanged = { value -> viewModel.shouldVault = value },
            onVaultCustomerIdChanged = { value -> viewModel.customerId = value },
            onSubmit = { onCreateOrderSubmit() }
        )
        uiState.createdOrder?.let { createdOrder ->
            Spacer(modifier = Modifier.size(24.dp))
            OrderView(order = createdOrder, title = "Order Created")
            Spacer(modifier = Modifier.size(24.dp))
            ApproveOrderForm(
                uiState = uiState,
                onCardNumberChange = { value -> viewModel.cardNumber = value },
                onExpirationDateChange = { value -> viewModel.cardExpirationDate = value },
                onSecurityCodeChange = { value -> viewModel.cardSecurityCode = value },
                onSCAOptionSelected = { value -> viewModel.scaOption = value },
                onUseTestCardClick = { onUseTestCardClick() },
                onSubmit = { onApproveOrderSubmit() }
            )
        }
        uiState.approveOrderResult?.let { cardResult ->
            Spacer(modifier = Modifier.size(24.dp))
            ApproveOrderSuccessView(cardResult = cardResult)
            Spacer(modifier = Modifier.size(24.dp))
            CompleteOrderForm(
                isLoading = uiState.isCompleteOrderLoading,
                orderIntent = uiState.intentOption,
                onSubmit = { onCompleteOrderSubmit() }
            )
        }
        uiState.approveOrderErrorMessage?.let { errorMessage ->
            Spacer(modifier = Modifier.size(24.dp))
            MessageView(message = errorMessage)
        }
        uiState.completedOrder?.let { completedOrder ->
            Spacer(modifier = Modifier.size(24.dp))
            OrderView(order = completedOrder, title = "Order Complete")
        }
        Spacer(modifier = Modifier.size(24.dp))
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun CardViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CardView()
        }
    }
}
