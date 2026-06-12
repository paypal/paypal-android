package com.paypal.android.ui.paypalbuttons

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
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
import com.paypal.android.uishared.components.IntSlider
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
            PaymentButtonShapeOptionList(
                selectedOption = uiState.paymentButtonShape,
                onSelection = { value -> viewModel.paymentButtonShape = value }
            )
            CustomCornerRadiusSlider(
                cornerRadius = uiState.customCornerRadius,
                onCornerRadiusChange = { value -> viewModel.customCornerRadius = value }
            )
            PaymentButtonSizeOptionList(
                selectedOption = uiState.paymentButtonSize,
                onSelection = { value -> viewModel.paymentButtonSize = value }
            )
            Spacer(modifier = Modifier.size(contentPadding))
        }
    }
}

@Composable
fun CustomCornerRadiusSlider(cornerRadius: Int?, onCornerRadiusChange: (Int) -> Unit) {
    Card {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.inverseSurface)
        ) {
            Text(
                text = "Custom Corner Radius",
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(UIConstants.paddingMedium)
                    .fillMaxWidth()
            )
        }
        Column(
            modifier = Modifier.padding(
                horizontal = UIConstants.paddingLarge,
                vertical = UIConstants.paddingMedium
            )
        ) {
            Text(
                text = cornerRadius?.let { "${cornerRadius}px" } ?: "UNSET",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(bottom = UIConstants.paddingSmall)
                    .fillMaxWidth()
            )
            IntSlider(
                value = cornerRadius ?: 0,
                valueRange = 0..CORNER_RADIUS_SLIDER_MAX,
                steps = CORNER_RADIUS_SLIDER_MAX,
                colors = SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                onValueChange = onCornerRadiusChange,
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
                    PayPalButton(context).apply {
                        setOnClickListener {
                            showToast(context, "PayPalButton clicked!")
                        }
                    }
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
                    PayLaterButton(context).apply {
                        setOnClickListener {
                            showToast(context, "PayLaterButton clicked!")
                        }
                    }
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
                    PayPalCreditButton(context).apply {
                        setOnClickListener {
                            showToast(context, "PayPalCreditButton clicked!")
                        }
                    }
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
        button.customCornerRadius = customCornerRadius.toFloat()
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
