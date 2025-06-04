package com.paypal.android.ui.paypalbuttons

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
import com.paypal.android.utils.UIConstants

const val CORNER_RADIUS_SLIDER_MAX = 100

@Suppress("LongMethod")
@ExperimentalMaterial3Api
@Composable
fun PayPalButtonsView(viewModel: PayPalButtonsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
    ) {
        Text(
            text = stringResource(id = R.string.pay_pal_button_preview),
            color = Color.Black,
            style = MaterialTheme.typography.titleLarge,
        )
        PayPalButtonFactory(uiState = uiState)
        Text(
            text = stringResource(id = R.string.pay_pal_button_options),
            color = Color.Black,
            style = MaterialTheme.typography.titleLarge,
        )
        Column(
            verticalArrangement = UIConstants.spacingMedium,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .verticalScroll(scrollState)
        ) {
            PayPalButtonFundingTypeOptionList(
                selectedOption = uiState.fundingType,
                onSelection = { value -> viewModel.selectedFundingType = value }
            )
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
                PayPalButtonLabelOptionList(
                    selectedOption = uiState.payPalButtonLabel,
                    onSelection = { value -> viewModel.payPalButtonLabel = value }
                )
            }
            PaymentButtonEdgesOptionList(
                edges = uiState.paymentButtonEdges,
                onSelection = { value -> viewModel.paymentButtonEdges = value }
            )
            Spacer(modifier = Modifier.size(contentPadding))
        }
    }
}

@Composable
fun PayPalButtonFactory(uiState: PayPalButtonsUiState) {
//    val buttonModifier: Modifier = Modifier.wrapContentSize()
    val buttonModifier: Modifier = Modifier.fillMaxWidth()

    when (uiState.fundingType) {
        ButtonFundingType.PAYPAL -> {
            AndroidView(
                factory = { context ->
                    PayPalButton(context).apply {
                        setOnClickListener { showToast(context, "PayPalButton clicked!") }
                    }
                },
                update = { button -> configureButton(button, uiState) },
                modifier = buttonModifier
            )
        }

        ButtonFundingType.PAY_LATER -> {
            AndroidView(
                factory = { context ->
                    PayLaterButton(context).apply {
                        setOnClickListener { showToast(context, "PayLaterButton clicked!") }
                    }
                },
                update = { button -> configureButton(button, uiState) },
                modifier = buttonModifier
            )
        }

        ButtonFundingType.PAYPAL_CREDIT -> {
            AndroidView(
                factory = { context ->
                    PayPalCreditButton(context).apply {
                        setOnClickListener { showToast(context, "PayPalCreditButton clicked!") }
                    }
                },
                update = { button -> configureButton(button, uiState) },
                modifier = buttonModifier
            )
        }
    }
}

private fun configureButton(
    button: PaymentButton<out PaymentButtonColor>,
    uiState: PayPalButtonsUiState
) {
    button.edges = uiState.paymentButtonEdges

    if (button is PayPalButton) {
        button.label = uiState.payPalButtonLabel
    }

    if (button is PayPalCreditButton) {
        button.color = uiState.payPalCreditButtonColor
    } else {
        button.color = uiState.payPalButtonColor
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

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun PayPalButtonsViewPreview() {
    MaterialTheme {
        PayPalButtonsView()
    }
}
