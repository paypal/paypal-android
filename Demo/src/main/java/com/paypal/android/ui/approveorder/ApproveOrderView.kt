package com.paypal.android.ui.approveorder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.CardResultView
import com.paypal.android.uishared.components.CreateOrderWithVaultOptionForm
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.OnLifecycleOwnerResumeEffect
import com.paypal.android.utils.OnNewIntentEffect
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivityOrNull

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun ApproveOrderView(
    viewModel: ApproveOrderViewModel,
    onUseTestCardClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.maxValue) {
        // continuously scroll to bottom of the list when scroll bounds change
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val context = LocalContext.current
    OnLifecycleOwnerResumeEffect {
        val intent = context.getActivityOrNull()?.intent
        intent?.let { viewModel.completeAuthChallenge(it) }
    }

    OnNewIntentEffect { newIntent ->
        viewModel.completeAuthChallenge(newIntent)
    }

    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
            .verticalScroll(scrollState)
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Step1_CreateOrder(uiState, viewModel)
        if (uiState.isCreateOrderSuccessful) {
            Step2_ApproveOrder(uiState, viewModel, onUseTestCardClick)
        }
        if (uiState.isApproveOrderSuccessful) {
            Step3_CompleteOrder(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

@Composable
private fun Step1_CreateOrder(uiState: ApproveOrderUiState, viewModel: ApproveOrderViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create Order")
        CreateOrderWithVaultOptionForm(
            orderIntent = uiState.intentOption,
            shouldVault = uiState.shouldVault,
            onIntentOptionChanged = { value -> viewModel.intentOption = value },
            onShouldVaultChanged = { value -> viewModel.shouldVault = value },
        )
        ActionButtonColumn(
            defaultTitle = "CREATE ORDER",
            successTitle = "ORDER CREATED",
            state = uiState.createOrderState,
            onClick = { viewModel.createOrder() },
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> OrderView(order = state.value)
            }
        }
    }
}

@Composable
private fun Step2_ApproveOrder(
    uiState: ApproveOrderUiState,
    viewModel: ApproveOrderViewModel,
    onUseTestCardClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium
    ) {
        StepHeader(stepNumber = 2, title = "Approve Order")
        ApproveOrderForm(
            uiState = uiState,
            onCardNumberChange = { value -> viewModel.cardNumber = value },
            onExpirationDateChange = { value -> viewModel.cardExpirationDate = value },
            onSecurityCodeChange = { value -> viewModel.cardSecurityCode = value },
            onSCAChange = { value -> viewModel.scaOption = value },
            onUseTestCardClick = { onUseTestCardClick() },
        )
        ActionButtonColumn(
            defaultTitle = "APPROVE ORDER",
            successTitle = "ORDER APPROVED",
            state = uiState.approveOrderState,
            onClick = {
                context.getActivityOrNull()?.let { viewModel.approveOrder(it) }
            },
            modifier = Modifier.fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> CardResultView(result = state.value)
            }
        }
    }
}

@Composable
private fun Step3_CompleteOrder(uiState: ApproveOrderUiState, viewModel: ApproveOrderViewModel) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium
    ) {
        StepHeader(stepNumber = 3, title = "Complete Order")
        val successTitle = when (uiState.intentOption) {
            OrderIntent.CAPTURE -> "ORDER CAPTURED"
            OrderIntent.AUTHORIZE -> "ORDER AUTHORIZED"
        }
        ActionButtonColumn(
            defaultTitle = "${uiState.intentOption.name} ORDER",
            successTitle = successTitle,
            state = uiState.completeOrderState,
            onClick = { viewModel.completeOrder(context) },
            modifier = Modifier.fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Success -> OrderView(order = state.value)
                is CompletedActionState.Failure -> ErrorView(error = state.value)
            }
        }
    }
}
