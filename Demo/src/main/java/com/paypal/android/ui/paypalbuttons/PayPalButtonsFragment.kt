package com.paypal.android.ui.paypalbuttons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.databinding.FragmentPaypalButtonsStylingBinding
import com.paypal.android.paymentbuttons.PayLaterButton
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.paymentbuttons.PayPalCreditButton
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor
import com.paypal.android.paymentbuttons.PaymentButtonColor
import com.paypal.android.paymentbuttons.PaymentButtonShape
import com.paypal.android.paymentbuttons.PaymentButtonSize
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PayPalButtonsFragment : Fragment() {

    private val viewModel by viewModels<PayPalButtonsViewModel>()

    private val selectedButtonFundingType: ButtonFundingType
        get() {
            return ButtonFundingType.values()[
                    binding.paymentButtonFundingSpinner.selectedItemPosition
            ]
        }

    private val selectedColor: PaymentButtonColor?
        get() = when (selectedButtonFundingType) {
            ButtonFundingType.PAYPAL_CREDIT ->
                binding.paymentButtonColorSpinner.selectedItem as? PayPalCreditButtonColor

            else ->
                binding.paymentButtonColorSpinner.selectedItem as? PayPalButtonColor
        }

    private val selectedLabel: PayPalButtonLabel?
        get() = when (selectedButtonFundingType) {
            ButtonFundingType.PAYPAL -> PayPalButtonLabel.valueOf(
                binding.paymentButtonLabelSpinner.selectedItem.toString()
            )

            else -> null
        }

    private val selectedShape: PaymentButtonShape
        get() = PaymentButtonShape.valueOf(
            binding.paymentButtonShapeSpinner.selectedItem.toString()
        )

    private val selectedSize: PaymentButtonSize
        get() = PaymentButtonSize.valueOf(
            binding.paymentButtonSizeSpinner.selectedItem.toString()
        )

    private val binding: FragmentPaypalButtonsStylingBinding by lazy {
        FragmentPaypalButtonsStylingBinding.inflate(layoutInflater)
    }

    private val itemSelectionListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (parent?.id == binding.paymentButtonFundingSpinner.id) {
                initPaymentButtonColorSpinner()
                initPaymentButtonLabelSpinner()
            }
            showStyledButton()
        }

        override fun onNothingSelected(p0: AdapterView<*>?) = Unit
    }

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

        Column(modifier = Modifier.fillMaxSize()) {
            Text("Preview: ")
            PayPalButtonFactory(uiState = uiState)
            Text("Options: ")
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
                PayPalButtonColorOptionListFactory(uiState = uiState)
                if (uiState.fundingType == ButtonFundingType.PAYPAL) {
                    PayPalButtonLabelOptionList(
                        selectedOption = uiState.payPalButtonLabel,
                        onSelection = { value ->
                            viewModel.payPalButtonLabel = value
                        }
                    )
                }
                PaymentButtonShapeOptionList(
                    selectedOption = uiState.paymentButtonShape,
                    onSelection = { value ->
                        viewModel.paymentButtonShape = value
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
                        val button = PayPalButton(context)
                        button.color = uiState.payPalButtonColor
                        button.label = uiState.payPalButtonLabel
                        button.shape = uiState.paymentButtonShape
                        button
                    },
                    update = { button ->
                        button.color = uiState.payPalButtonColor
                        button.label = uiState.payPalButtonLabel
                        button.shape = uiState.paymentButtonShape
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ButtonFundingType.PAY_LATER -> {
                AndroidView(
                    factory = { context ->
                        val button = PayLaterButton(context)
                        button.color = uiState.payPalButtonColor
                        button.shape = uiState.paymentButtonShape
                        button
                    },
                    update = { button ->
                        button.color = uiState.payPalButtonColor
                        button.shape = uiState.paymentButtonShape
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ButtonFundingType.PAYPAL_CREDIT -> {
                AndroidView(
                    factory = { context ->
                        val button = PayPalCreditButton(context)
                        button.color = uiState.payPalCreditButtonColor
                        button.shape = uiState.paymentButtonShape
                        button
                    },
                    update = { button ->
                        button.color = uiState.payPalCreditButtonColor
                        button.shape = uiState.paymentButtonShape
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFundingTypesSpinner()
        initPaymentButtonColorSpinner()
        initPaymentButtonLabelSpinner()
        initPaymentButtonShapeSpinner()
        initPaymentButtonSizeSpinner()
    }

    private fun initPaymentButtonSizeSpinner() {
        binding.paymentButtonSizeSpinner.adapter = simpleArrayAdapter(PaymentButtonSize.values())
        binding.paymentButtonSizeSpinner.onItemSelectedListener = itemSelectionListener
    }

    private fun <T> simpleArrayAdapter(items: Array<T>) = ArrayAdapter(
        requireContext(),
        android.R.layout.simple_list_item_1,
        items
    )

    private fun initPaymentButtonShapeSpinner() {
        binding.paymentButtonShapeSpinner.adapter = simpleArrayAdapter(PaymentButtonShape.values())
        binding.paymentButtonShapeSpinner.onItemSelectedListener = itemSelectionListener
    }

    private fun initPaymentButtonLabelSpinner() {
        binding.paymentButtonLabelSpinner.adapter = when (selectedButtonFundingType) {
            ButtonFundingType.PAYPAL -> simpleArrayAdapter(PayPalButtonLabel.values())
            else -> simpleArrayAdapter(arrayOf("N/A"))
        }
        binding.paymentButtonLabelSpinner.onItemSelectedListener = itemSelectionListener
    }

    private fun initFundingTypesSpinner() {
        binding.paymentButtonFundingSpinner.adapter = simpleArrayAdapter(ButtonFundingType.values())
        binding.paymentButtonFundingSpinner.onItemSelectedListener = itemSelectionListener
    }

    private fun initPaymentButtonColorSpinner() {
        binding.paymentButtonColorSpinner.adapter = when (selectedButtonFundingType) {
            ButtonFundingType.PAYPAL, ButtonFundingType.PAY_LATER -> simpleArrayAdapter(
                PayPalButtonColor.values()
            )

            ButtonFundingType.PAYPAL_CREDIT -> simpleArrayAdapter(PayPalCreditButtonColor.values())
        }
        binding.paymentButtonColorSpinner.onItemSelectedListener = itemSelectionListener
    }

    private fun showStyledButton() {
        val styledButton = buildStyledButton()
        styledButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT
        )
        binding.placeHolderLinearLayout.removeAllViews()
        binding.placeHolderLinearLayout.addView(styledButton)
    }

    private fun buildStyledButton(): View =
        when (selectedButtonFundingType) {
            ButtonFundingType.PAYPAL -> PayPalButton(requireContext(), null).apply {
                selectedColor?.let {
                    color = it
                }
                shape = selectedShape
                size = selectedSize
                selectedLabel?.let {
                    label = it
                }
            }

            ButtonFundingType.PAY_LATER -> PayLaterButton(requireContext(), null).apply {
                selectedColor?.let {
                    color = it
                }
                shape = selectedShape
                size = selectedSize
            }

            ButtonFundingType.PAYPAL_CREDIT -> PayPalCreditButton(requireContext(), null).apply {
                selectedColor?.let {
                    color = it
                }
                shape = selectedShape
                size = selectedSize
            }
        }
}
