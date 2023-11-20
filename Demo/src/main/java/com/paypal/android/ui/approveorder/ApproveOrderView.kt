package com.paypal.android.ui.approveorder

import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.uishared.components.CompleteOrderForm
import com.paypal.android.uishared.components.CreateOrderWithVaultOptionForm
import com.paypal.android.uishared.components.MessageView
import com.paypal.android.uishared.components.OrderView

// TODO: Investigate the best way to break this composable up into smaller individual units
@Suppress("LongMethod")
@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalMaterial3Api
@Composable
fun ApproveOrderView(
    viewModel: ApproveOrderViewModel = viewModel(),
    onUseTestCardClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
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
            onSubmit = { viewModel.createOrder() }
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
                onSubmit = {
                    context.getActivity()?.let { viewModel.approveOrder(it) }
                }
            )
        }
        uiState.approveOrderResult?.let { cardResult ->
            Spacer(modifier = Modifier.size(24.dp))
            ApproveOrderSuccessView(cardResult = cardResult)
            Spacer(modifier = Modifier.size(24.dp))
            CompleteOrderForm(
                isLoading = uiState.isCompleteOrderLoading,
                orderIntent = uiState.intentOption,
                onSubmit = { viewModel.completeOrder(context) }
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

// TODO: move to utility file
// Ref: https://stackoverflow.com/a/68423182
fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
