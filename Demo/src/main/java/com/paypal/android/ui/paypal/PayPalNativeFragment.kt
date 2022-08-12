package com.paypal.android.ui.paypal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.paypal.android.api.services.SDKSampleServerApi
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalCheckoutListener
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.databinding.FragmentPayPalNativeBinding
import com.paypal.android.viewmodels.PayPalNativeViewModel
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeData
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PayPalNativeFragment : Fragment(), PayPalCheckoutListener {

    private lateinit var binding: FragmentPayPalNativeBinding

    @Inject
    lateinit var sdkSampleServerApi: SDKSampleServerApi

    private val viewModel: PayPalNativeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentPayPalNativeBinding.inflate(inflater, container, false)

        binding.startNativeCheckout.setOnClickListener { viewModel.billingAgreementCheckout() }
        return binding.root
    }

    override fun onPayPalCheckoutStart() {
        Toast.makeText(requireContext(), "PayPal START", Toast.LENGTH_LONG).show()
    }

    override fun onPayPalCheckoutSuccess(result: PayPalCheckoutResult) {
        Toast.makeText(requireContext(), "PayPal SUCCESS", Toast.LENGTH_LONG).show()
    }

    override fun onPayPalCheckoutFailure(error: PayPalSDKError) {
        Toast.makeText(requireContext(), "PayPal FAILURE", Toast.LENGTH_LONG).show()
    }

    override fun onPayPalCheckoutCanceled() {
        Toast.makeText(requireContext(), "PayPal CANCELED", Toast.LENGTH_LONG).show()
    }

    override fun onPayPalCheckoutShippingChange(
        shippingChangeData: ShippingChangeData,
        shippingChangeActions: ShippingChangeActions
    ) {
        Toast.makeText(requireContext(), "PayPal SHIPPING CHANGE", Toast.LENGTH_LONG).show()
    }
}
