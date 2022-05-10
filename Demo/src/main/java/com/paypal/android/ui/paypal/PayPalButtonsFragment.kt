package com.paypal.android.ui.paypal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import com.paypal.android.databinding.FragmentPaypalButtonsStylingBinding
import com.paypal.android.ui.paymentbutton.PayLaterButton
import com.paypal.android.ui.paymentbutton.PayPalButton
import com.paypal.android.ui.paymentbutton.PayPalButtonColor
import com.paypal.android.ui.paymentbutton.PayPalButtonLabel
import com.paypal.android.ui.paymentbutton.PayPalCreditButton
import com.paypal.android.ui.paymentbutton.PayPalCreditButtonColor
import com.paypal.android.ui.paymentbutton.PaymentButtonColor
import com.paypal.android.ui.paymentbutton.PaymentButtonShape
import com.paypal.android.ui.paymentbutton.PaymentButtonSize
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PayPalButtonsFragment : Fragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
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
        binding.paymentButtonSizeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            PaymentButtonSize.values()
        )
        binding.paymentButtonSizeSpinner.onItemSelectedListener = itemSelectionListener
    }

    private fun initPaymentButtonShapeSpinner() {
        binding.paymentButtonShapeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            PaymentButtonShape.values()
        )
        binding.paymentButtonShapeSpinner.onItemSelectedListener = itemSelectionListener
    }

    private fun initPaymentButtonLabelSpinner() {
        binding.paymentButtonLabelSpinner.adapter = when (selectedButtonFundingType) {
            ButtonFundingType.PAYPAL -> ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                PayPalButtonLabel.values()
            )
            else -> ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                listOf("N/A")
            )
        }
        binding.paymentButtonLabelSpinner.onItemSelectedListener = itemSelectionListener
    }

    private fun initFundingTypesSpinner() {
        binding.paymentButtonFundingSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            ButtonFundingType.values()
        )
        binding.paymentButtonFundingSpinner.onItemSelectedListener = itemSelectionListener
    }

    private fun initPaymentButtonColorSpinner() {
        binding.paymentButtonColorSpinner.adapter = when (selectedButtonFundingType) {
            ButtonFundingType.PAYPAL, ButtonFundingType.PAY_LATER -> ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                PayPalButtonColor.values()
            )
            ButtonFundingType.PAYPAL_CREDIT -> ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                PayPalCreditButtonColor.values()
            )
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
