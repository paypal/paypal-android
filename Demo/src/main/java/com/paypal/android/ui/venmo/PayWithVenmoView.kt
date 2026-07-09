package com.paypal.android.ui.venmo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.R
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.OnLifecycleOwnerResumeEffect
import com.paypal.android.utils.OnNewIntentEffect
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivityOrNull
import com.paypal.android.venmo.VenmoEligibilityResult
import com.paypal.android.venmo.VenmoFinishStartResult

@Composable
fun PayWithVenmoView(
    viewModel: PayWithVenmoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    OnLifecycleOwnerResumeEffect {
        val intent = context.getActivityOrNull()?.intent
        intent?.let { viewModel.finishVenmo(it) }
    }

    OnNewIntentEffect { newIntent ->
        viewModel.finishVenmo(newIntent)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) {
        // continuously scroll to bottom of the list when event state is updated
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
            .verticalScroll(scrollState)
    ) {
        Step1_CheckEligibility(uiState, viewModel)
        if (uiState.isEligibilityCheckSuccessful) {
            Step2_CreateOrder(uiState, viewModel)
            if (uiState.isCreateOrderSuccessful) {
                Step3_StartPayWithVenmo(uiState, viewModel)
                if (uiState.isVenmoSuccessful) {
                    Step4_CompleteOrder(uiState, viewModel)
                }
            }
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

@Composable
private fun Step1_CheckEligibility(uiState: PayWithVenmoUiState, viewModel: PayWithVenmoViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Check Eligibility")

        // Map eligibility result to failure state if not eligible
        val displayState = when (val checkState = uiState.checkEligibilityState) {
            is ActionState.Success -> {
                val result = checkState.value
                if (result is VenmoEligibilityResult.Eligible) {
                    checkState
                } else {
                    // Show Ineligible or Error as failure
                    val errorMessage = when (result) {
                        is VenmoEligibilityResult.Ineligible -> result.reason
                        is VenmoEligibilityResult.Error -> result.error.errorDescription
                        else -> "Venmo is not eligible"
                    }
                    ActionState.Failure(Exception(errorMessage))
                }
            }
            else -> checkState
        }

        ActionButtonColumn(
            defaultTitle = "CHECK ELIGIBILITY",
            successTitle = "ELIGIBILITY CHECKED",
            state = displayState,
            onClick = { viewModel.checkEligibility() },
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> EligibilityResultView(result = state.value)
            }
        }
    }
}

@Composable
private fun Step2_CreateOrder(uiState: PayWithVenmoUiState, viewModel: PayWithVenmoViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Create an Order")
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
private fun Step3_StartPayWithVenmo(
    uiState: PayWithVenmoUiState,
    viewModel: PayWithVenmoViewModel
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 3, title = stringResource(R.string.launch_venmo))
        ActionButtonColumn(
            defaultTitle = "START CHECKOUT",
            successTitle = "CHECKOUT COMPLETE",
            state = uiState.payWithVenmoState,
            onClick = { context.getActivityOrNull()?.let { viewModel.startVenmo(it) } },
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> VenmoResultView(result = state.value)
            }
        }
    }
}

@Composable
private fun Step4_CompleteOrder(uiState: PayWithVenmoUiState, viewModel: PayWithVenmoViewModel) {
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
fun EligibilityResultView(result: VenmoEligibilityResult) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        when (result) {
            VenmoEligibilityResult.Eligible -> {
                PropertyView(name = "Status", value = "Venmo is eligible")
            }
            is VenmoEligibilityResult.Ineligible -> {
                PropertyView(name = "Status", value = "Not Eligible")
                PropertyView(name = "Reason", value = result.reason)
            }
            is VenmoEligibilityResult.Error -> {
                PropertyView(name = "Status", value = "Error")
                PropertyView(name = "Error", value = result.error.errorDescription)
            }
        }
    }
}

@Composable
fun VenmoFinishStartSuccessView(result: VenmoFinishStartResult.Success) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Token", value = result.token)
        PropertyView(name = "Payer ID", value = result.payerId)
        PropertyView(name = "Approved", value = result.approved.toString())
    }
}

@Composable
fun VenmoResultView(result: VenmoFinishStartResult) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        when (result) {
            is VenmoFinishStartResult.Success -> {
                PropertyView(name = "Token", value = result.token)
                PropertyView(name = "Payer ID", value = result.payerId)
                PropertyView(name = "Approved", value = result.approved.toString())
            }
            is VenmoFinishStartResult.Canceled -> {
                PropertyView(name = "Status", value = "Canceled by user")
                result.orderId?.let { PropertyView(name = "Order ID", value = it) }
            }
            is VenmoFinishStartResult.NoResult -> {
                PropertyView(name = "Status", value = "No Venmo result received")
            }
            is VenmoFinishStartResult.Failure -> {
                PropertyView(name = "Status", value = "Error")
                PropertyView(name = "Error", value = result.error.errorDescription)
            }
        }
    }
}
