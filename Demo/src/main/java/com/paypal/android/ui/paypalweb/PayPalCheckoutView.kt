package com.paypal.android.ui.paypalweb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.paypal.android.uishared.state.ActionState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.R
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.CreateOrderForm
import com.paypal.android.uishared.components.EnumOptionList
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.PayPalUserIdentityForm
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.OnLifecycleOwnerResumeEffect
import com.paypal.android.utils.OnNewIntentEffect
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivityOrNull

@Composable
fun PayPalCheckoutView(
    viewModel: PayPalCheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) {
        // Auto-scroll once the user has started the flow; avoids scrolling on initial render
        if (uiState.createOrderState !is ActionState.Idle) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
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
    ) {
        Step1_CreatePayPalSession(uiState, viewModel)
        Step2_CreateOrder(uiState, viewModel)
        if (uiState.isCreateOrderSuccessful) {
            Step3_StartPayPalCheckout(uiState, viewModel)
        }
        if (uiState.isPayPalWebCheckoutSuccessful) {
            Step4_CompleteOrder(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

@Composable
private fun Step1_CreatePayPalSession(uiState: PayPalUiState, viewModel: PayPalCheckoutViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create PayPal Session")
        PayPalUserIdentityForm(
            userIdentity = uiState.userIdentity,
            onUserIdentityChange = { value -> viewModel.userIdentity = value },
            modifier = Modifier.fillMaxWidth()
        )
        EnumOptionList(
            title = stringResource(R.string.user_action_title),
            stringArrayResId = R.array.user_action_options,
            onSelectedOptionChange = { value -> viewModel.userAction = value },
            selectedOption = uiState.userAction,
            modifier = Modifier.fillMaxWidth()
        )
        ActionButtonColumn(
            defaultTitle = "CREATE PAYPAL SESSION",
            successTitle = "SESSION CREATION INITIATED",
            state = if (uiState.isPayPalSessionCreated) ActionState.Success(Unit) else ActionState.Idle,
            onClick = { viewModel.createPayPalSession() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Step2_CreateOrder(uiState: PayPalUiState, viewModel: PayPalCheckoutViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Create an Order")
        CreateOrderForm(
            orderIntent = uiState.intentOption,
            onOrderIntentChange = { value -> viewModel.intentOption = value },
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
private fun Step3_StartPayPalCheckout(uiState: PayPalUiState, viewModel: PayPalCheckoutViewModel) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 3, title = stringResource(R.string.launch_paypal))
        StartPayPalWebCheckoutForm(
            fundingSource = uiState.fundingSource,
            onFundingSourceChange = { value -> viewModel.fundingSource = value },
        )
        ActionButtonColumn(
            defaultTitle = "START CHECKOUT",
            successTitle = "CHECKOUT COMPLETE",
            state = uiState.payPalWebCheckoutState,
            onClick = { context.getActivityOrNull()?.let { viewModel.startCheckout(it) } },
            modifier = Modifier.fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> state.value.run {
                    PayPalWebCheckoutResultView(orderId, payerId)
                }
            }
        }
    }
}

@Composable
private fun Step4_CompleteOrder(uiState: PayPalUiState, viewModel: PayPalCheckoutViewModel) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 4, title = "Complete Order")
        ActionButtonColumn(
            defaultTitle = "COMPLETE ORDER",
            successTitle = "ORDER COMPLETED",
            state = uiState.completeOrderState,
            onClick = { viewModel.completeOrder(context) },
            modifier = Modifier.fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> OrderView(order = state.value)
            }
        }
    }
}

@Preview
@Composable
fun PayPalWebViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            PayPalCheckoutView()
        }
    }
}
