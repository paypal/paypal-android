package com.paypal.android.ui.paypalbuttons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.paymentbuttons.PayLaterButton
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paymentbuttons.PayPalCreditButton
import com.paypal.android.paymentbuttons.PaymentButton
import com.paypal.android.paymentbuttons.PaymentButtonColor
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PayPalButtonsFragment : Fragment() {

    private val viewModel by viewModels<PayPalButtonsViewModel>()

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        PayPalButtonsView(uiState)
                    }
                }
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun PayPalButtonsView(
        uiState: PayPalButtonsUiState
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Button Preview",
                color = Color.Black,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.size(16.dp))
            PayPalButtonFactory(uiState = uiState)
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = "Button Options",
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
                    onSelection = { value ->
                        viewModel.selectedFundingType = value
                    }
                )
                Spacer(modifier = Modifier.size(8.dp))
                PayPalButtonColorOptionListFactory(uiState = uiState)
                if (uiState.fundingType == ButtonFundingType.PAYPAL) {
                    Spacer(modifier = Modifier.size(8.dp))
                    PayPalButtonLabelOptionList(
                        selectedOption = uiState.payPalButtonLabel,
                        onSelection = { value ->
                            viewModel.payPalButtonLabel = value
                        }
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                PaymentButtonShapeOptionList(
                    selectedOption = uiState.paymentButtonShape,
                    onSelection = { value ->
                        viewModel.paymentButtonShape = value
                    }
                )
                Spacer(modifier = Modifier.size(8.dp))
                PaymentButtonSizeOptionList(
                    selectedOption = uiState.paymentButtonSize,
                    onSelection = { value ->
                        viewModel.paymentButtonSize = value
                    }
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
    }

    @Composable
    fun PayPalButtonColorOptionListFactory(uiState: PayPalButtonsUiState) {
        when (uiState.fundingType) {
            ButtonFundingType.PAYPAL,
            ButtonFundingType.PAY_LATER -> {
                PayPalButtonColorOptionList(
                    selectedOption = uiState.payPalButtonColor,
                    onSelection = { value ->
                        viewModel.payPalButtonColor = value
                    }
                )
            }

            ButtonFundingType.PAYPAL_CREDIT -> {
                PayPalCreditButtonColorOptionList(
                    selectedOption = uiState.payPalCreditButtonColor,
                    onSelection = { value ->
                        viewModel.payPalCreditButtonColor = value
                    }
                )
            }
        }
    }
}
