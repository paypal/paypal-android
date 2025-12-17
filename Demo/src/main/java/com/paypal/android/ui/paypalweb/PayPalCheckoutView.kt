package com.paypal.android.ui.paypalweb

import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.BuildConfig
import com.paypal.android.DemoConstants
import com.paypal.android.R
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.compose.rememberPayPalCheckoutLauncher
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.BooleanOptionList
import com.paypal.android.uishared.components.CreateOrderForm
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.ActionState
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
        // continuously scroll to bottom of the list when event state is updated
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val context = LocalContext.current

    // State for Composable API checkout
    var composableApiCheckoutState: ActionState<PayPalCheckoutResult, Exception> by remember {
        mutableStateOf(ActionState.Idle)
    }

    // Setup Composable API launcher with logging
    val coreConfig = remember {
        CoreConfig(BuildConfig.CLIENT_ID, Environment.SANDBOX)
    }

    val payPalLauncher = rememberPayPalCheckoutLauncher(
        configuration = coreConfig
    )

    // Only setup lifecycle hooks for standard API
    if (!uiState.useComposableApi) {
        OnLifecycleOwnerResumeEffect {
            println("Karthik OnLifecycleOwnerResumeEffect called")
            val intent = context.getActivityOrNull()?.intent
            intent?.let { viewModel.completeAuthChallenge(it) }
        }

        OnNewIntentEffect { newIntent ->
            viewModel.completeAuthChallenge(newIntent)
        }
    }

    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
            .verticalScroll(scrollState)
    ) {
        Step1_CreateOrder(uiState, viewModel)
        if (uiState.isCreateOrderSuccessful) {
            Step2_StartPayPalCheckout(
                uiState = uiState,
                viewModel = viewModel,
                payPalLauncher = payPalLauncher,
                composableApiCheckoutState = composableApiCheckoutState,
                onComposableApiCheckoutStateChange = { composableApiCheckoutState = it }
            )
        }
        val isCheckoutSuccessful = if (uiState.useComposableApi) {
            composableApiCheckoutState is ActionState.Success
        } else {
            uiState.isPayPalWebCheckoutSuccessful
        }
        if (isCheckoutSuccessful) {
            Step3_CompleteOrder(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

// Data class to hold checkout result for Composable API
data class PayPalCheckoutResult(
    val orderId: String?,
    val payerId: String?
)

private const val TAG = "PayPalCheckoutView"

@Composable
private fun Step1_CreateOrder(uiState: PayPalUiState, viewModel: PayPalCheckoutViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create an Order")
        BooleanOptionList(
            title = stringResource(id = R.string.use_composable_api),
            selectedOption = uiState.useComposableApi,
            onSelectedOptionChange = { value -> viewModel.useComposableApi = value },
            modifier = Modifier.fillMaxWidth()
        )
        BooleanOptionList(
            title = stringResource(id = R.string.app_switch_when_available),
            selectedOption = uiState.appSwitchWhenEligible,
            onSelectedOptionChange = { value -> viewModel.appSwitchWhenEligible = value },
            modifier = Modifier.fillMaxWidth()
        )
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
private fun Step2_StartPayPalCheckout(
    uiState: PayPalUiState,
    viewModel: PayPalCheckoutViewModel,
    payPalLauncher: com.paypal.android.paypalwebpayments.compose.PayPalCheckoutLauncher,
    composableApiCheckoutState: ActionState<PayPalCheckoutResult, Exception>,
    onComposableApiCheckoutStateChange: (ActionState<PayPalCheckoutResult, Exception>) -> Unit
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = stringResource(R.string.launch_paypal))
        StartPayPalWebCheckoutForm(
            fundingSource = uiState.fundingSource,
            onFundingSourceChange = { value -> viewModel.fundingSource = value },
        )

        if (uiState.useComposableApi) {
            // Use Composable API launcher
            ActionButtonColumn(
                defaultTitle = "START CHECKOUT (Composable API)",
                successTitle = "CHECKOUT COMPLETE",
                state = composableApiCheckoutState,
                onClick = {
                    val orderId = viewModel.createdOrder?.id
                    if (orderId == null) {
                        onComposableApiCheckoutStateChange(
                            ActionState.Failure(Exception("Create an order to continue."))
                        )
                    } else {
                        Log.d(TAG, "Composable API: Launching checkout for orderId: $orderId")
                        onComposableApiCheckoutStateChange(ActionState.Loading)
                        val request = PayPalWebCheckoutRequest(
                            orderId = orderId,
                            fundingSource = uiState.fundingSource,
                            appSwitchWhenEligible = uiState.appSwitchWhenEligible,
                            appLinkUrl = DemoConstants.APP_URL,
                            fallbackUrlScheme = DemoConstants.APP_FALLBACK_URL_SCHEME
                        )
                        payPalLauncher.start(
                            request = request,
                            onResult = { result ->
                                when (result) {
                                    is PayPalWebCheckoutFinishStartResult.Success -> {
                                        println("Karthik onCheckoutSuccess called with result: $result")
                                        onComposableApiCheckoutStateChange(
                                            ActionState.Success(
                                                PayPalCheckoutResult(
                                                    orderId = result.orderId,
                                                    payerId = result.payerId
                                                )
                                            )
                                        )
                                    }

                                    is PayPalWebCheckoutFinishStartResult.Canceled -> {
                                        println("Karthik onCheckoutCanceled called")
                                        onComposableApiCheckoutStateChange(
                                            ActionState.Failure(
                                                Exception("USER CANCELED")
                                            )
                                        )
                                    }

                                    is PayPalWebCheckoutFinishStartResult.Failure -> {
                                        println("Karthik onCheckoutError called with error: ${result.error}")
                                        onComposableApiCheckoutStateChange(
                                            ActionState.Failure(
                                                if (result.error is Exception) result.error else Exception(
                                                    result.error.message,
                                                    result.error
                                                )
                                            )
                                        )
                                    }

                                    PayPalWebCheckoutFinishStartResult.NoResult -> { /* No-op */
                                    }
                                }
                            },
                            onPresentationResult = { result ->
                                println("Karthik launch checkout callback result: $result")
                                Log.d(TAG, "Composable API: Presentation result: $result")
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { state ->
                when (state) {
                    is CompletedActionState.Failure -> ErrorView(error = state.value)
                    is CompletedActionState.Success -> state.value.run {
                        PayPalWebCheckoutResultView(orderId, payerId)
                    }
                }
            }
        } else {
            // Use standard API
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
}

@Composable
private fun Step3_CompleteOrder(uiState: PayPalUiState, viewModel: PayPalCheckoutViewModel) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 3, title = "Complete Order")
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

@Preview
@Composable
fun PayPalWebViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            PayPalCheckoutView()
        }
    }
}
