package com.paypal.android.ui.paypalbuttons

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.R
import com.paypal.android.paymentbuttons.PayLaterButton
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paymentbuttons.PayPalCreditButton
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor
import com.paypal.android.paymentbuttons.PaymentButton
import com.paypal.android.paymentbuttons.PaymentButtonColor

@Suppress("LongMethod")
@ExperimentalMaterial3Api
@Composable
fun PayPalButtonsView(viewModel: PayPalButtonsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.pay_pal_button_preview),
            color = Color.Black,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.size(16.dp))
        PayPalButtonFactory(uiState = uiState)
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(id = R.string.pay_pal_button_options),
            color = Color.Black,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .verticalScroll(scrollState)
        ) {
            PayPalButtonFundingTypeOptionList(
                selectedOption = uiState.fundingType,
                onSelection = { value -> viewModel.selectedFundingType = value }
            )
            Spacer(modifier = Modifier.size(8.dp))
            PayPalButtonColorOptionListFactory(
                fundingType = uiState.fundingType,
                payPalButtonColor = uiState.payPalButtonColor,
                payPalCreditButtonColor = uiState.payPalCreditButtonColor,
                onPayPalButtonColorChange = { value -> viewModel.payPalButtonColor = value },
                onPayPalCreditButtonColorChange = { value ->
                    viewModel.payPalCreditButtonColor = value
                }
            )
            if (uiState.fundingType == ButtonFundingType.PAYPAL) {
                Spacer(modifier = Modifier.size(8.dp))
                PayPalButtonLabelOptionList(
                    selectedOption = uiState.payPalButtonLabel,
                    onSelection = { value -> viewModel.payPalButtonLabel = value }
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            PaymentButtonShapeOptionList(
                selectedOption = uiState.paymentButtonShape,
                onSelection = { value -> viewModel.paymentButtonShape = value }
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "Custom Corner Radius",
                color = Color.Black,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Slider(
                value = uiState.customCornerRadius ?: 0.0f,
                valueRange = 0f..100.0f,
                onValueChange = { value -> viewModel.customCornerRadius = value }
            )
            Spacer(modifier = Modifier.size(8.dp))
            PaymentButtonSizeOptionList(
                selectedOption = uiState.paymentButtonSize,
                onSelection = { value -> viewModel.paymentButtonSize = value }
            )
        }
    }
}

@Composable
fun PayPalButtonFactory(uiState: PayPalButtonsUiState) {
    when (uiState.fundingType) {
        ButtonFundingType.PAYPAL -> {
            AndroidView(
                factory = { context ->
                    PayPalButton(context)
                },
                update = { button ->
                    configureButton(button, uiState)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        ButtonFundingType.PAY_LATER -> {
            AndroidView(
                factory = { context ->
                    PayLaterButton(context)
                },
                update = { button ->
                    configureButton(button, uiState)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        ButtonFundingType.PAYPAL_CREDIT -> {
            AndroidView(
                factory = { context ->
                    PayPalCreditButton(context)
                },
                update = { button ->
                    configureButton(button, uiState)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun configureButton(
    button: PaymentButton<out PaymentButtonColor>,
    uiState: PayPalButtonsUiState
) {
    button.shape = uiState.paymentButtonShape
    button.size = uiState.paymentButtonSize

    if (button is PayPalButton) {
        button.label = uiState.payPalButtonLabel
    }

    if (button is PayPalCreditButton) {
        button.color = uiState.payPalCreditButtonColor
    } else {
        button.color = uiState.payPalButtonColor
    }

    uiState.customCornerRadius?.let { customCornerRadius ->
        button.customCornerRadius = customCornerRadius
    }
}

@Composable
fun PayPalButtonColorOptionListFactory(
    fundingType: ButtonFundingType,
    payPalButtonColor: PayPalButtonColor,
    payPalCreditButtonColor: PayPalCreditButtonColor,
    onPayPalButtonColorChange: (PayPalButtonColor) -> Unit,
    onPayPalCreditButtonColorChange: (PayPalCreditButtonColor) -> Unit,
) {
    when (fundingType) {
        ButtonFundingType.PAYPAL,
        ButtonFundingType.PAY_LATER -> {
            PayPalButtonColorOptionList(
                selectedOption = payPalButtonColor,
                onSelection = onPayPalButtonColorChange,
            )
        }

        ButtonFundingType.PAYPAL_CREDIT -> {
            PayPalCreditButtonColorOptionList(
                selectedOption = payPalCreditButtonColor,
                onSelection = onPayPalCreditButtonColorChange,
            )
        }
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun FeaturesViewPreview() {
    MaterialTheme {
        PayPalButtonsView()
    }
}
