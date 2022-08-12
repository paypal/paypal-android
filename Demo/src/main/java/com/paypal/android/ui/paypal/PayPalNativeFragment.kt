package com.paypal.android.ui.paypal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.radiobutton.MaterialRadioButton
import com.paypal.android.R
import com.paypal.android.api.services.SDKSampleServerApi
import com.paypal.android.databinding.FragmentPayPalNativeBinding
import com.paypal.android.viewmodels.PayPalNativeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PayPalNativeFragment : Fragment() {

    private lateinit var binding: FragmentPayPalNativeBinding

    @Inject
    lateinit var sdkSampleServerApi: SDKSampleServerApi

    private val viewModel: PayPalNativeViewModel by viewModels()

    private var selectedOptionId = R.id.order_checkout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentPayPalNativeBinding.inflate(inflater, container, false)
        viewModel.state.observe(viewLifecycleOwner) { viewState ->
            checkViewState(viewState)
        }
        with(binding) {
            fetchAccessTokenButton.setOnClickListener { viewModel.fetchAccessToken() }
            startNativeCheckout.setOnClickListener { onStartNativeCheckoutClicked() }
            orderCheckout.setOnClickListener { onRadioButtonClicked(it) }
            orderIdCheckout.setOnClickListener { onRadioButtonClicked(it) }
            billingAgreementCheckout.setOnClickListener { onRadioButtonClicked(it) }
            vaultCheckout.setOnClickListener { onRadioButtonClicked(it) }
            errorButton.setOnClickListener { viewModel.reset() }
        }
        return binding.root
    }

    private fun onRadioButtonClicked(view: View) {
        if (view is MaterialRadioButton && view.isChecked) {
            selectedOptionId = view.id
        }
    }

    private fun onStartNativeCheckoutClicked() {
        when (selectedOptionId) {
            R.id.order_checkout -> { viewModel.orderCheckout() }
            R.id.order_id_checkout -> { viewModel.orderIdCheckout() }
            R.id.billing_agreement_checkout -> { viewModel.billingAgreementCheckout() }
            R.id.vault_checkout -> { viewModel.vaultCheckout() }
            else -> {}
        }
    }

    private fun checkViewState(viewState: PayPalNativeViewModel.ViewState) {
        when (viewState) {
            PayPalNativeViewModel.ViewState.CheckoutCancelled -> checkoutCancelled()
            is PayPalNativeViewModel.ViewState.CheckoutComplete -> checkoutComplete(viewState)
            is PayPalNativeViewModel.ViewState.CheckoutError -> checkoutError(viewState)
            PayPalNativeViewModel.ViewState.CheckoutStart -> checkoutStart()
            PayPalNativeViewModel.ViewState.GeneratingToken -> generatingToken()
            PayPalNativeViewModel.ViewState.Initial -> setInitialState()
            is PayPalNativeViewModel.ViewState.OrderCreated -> orderCreated(viewState)
            is PayPalNativeViewModel.ViewState.TokenGenerated -> tokenGenerated(viewState)
            PayPalNativeViewModel.ViewState.CheckoutInit -> checkoutInit()
        }
    }

    private fun setInitialState() {
        with(binding) {
            fetchAccessTokenButton.visibility = View.VISIBLE
            fetchAccessTokenButton.isEnabled = true
            startNativeCheckout.visibility = View.GONE
            checkoutOptionsRadioGroup.visibility = View.GONE
            errorGroup.visibility = View.GONE
            hideProgress()
        }
    }

    private fun generatingToken() {
        showProgress("Fetching access token...")
        with(binding) {
            fetchAccessTokenButton.isEnabled = false
            startNativeCheckout.visibility = View.GONE
        }
    }

    private fun tokenGenerated(viewState: PayPalNativeViewModel.ViewState.TokenGenerated) {
        hideProgress()
        with(binding) {
            startNativeCheckout.visibility = View.VISIBLE
            startNativeCheckout.isEnabled = true
            checkoutOptionsRadioGroup.visibility = View.VISIBLE
            selectedOptionId = R.id.order_checkout
            orderCheckout.isChecked = true
            fetchAccessTokenButton.visibility = View.GONE
        }
    }

    private fun checkoutInit() {
        showProgress("Initializing Checkout...")
        with(binding) {
            startNativeCheckout.isEnabled = false
            checkoutOptionsRadioGroup.visibility = View.GONE
        }
    }

    private fun checkoutStart() {
        showProgress("Starting Paypal...")
    }

    private fun checkoutError(viewState: PayPalNativeViewModel.ViewState.CheckoutError) {
        val message = viewState.message ?: viewState.error?.reason ?: "Oops! Something went wrong"
        hideProgress()
        with(binding) {
            errorGroup.visibility = View.VISIBLE
            errorMessage.text = message
        }
    }

    private fun orderCreated(viewState: PayPalNativeViewModel.ViewState.OrderCreated) {
    }

    private fun checkoutCancelled() {
        val message = "Checkout cancelled by user"
        hideProgress()
        with(binding) {
            errorGroup.visibility = View.VISIBLE
            errorMessage.text = message
        }
    }

    private fun checkoutComplete(viewState: PayPalNativeViewModel.ViewState.CheckoutComplete) {
    }

    private fun showProgress(text: String) {
        with(binding) {
            progressGroup.visibility = View.VISIBLE
            progressText.text = text
        }
    }

    private fun hideProgress() {
        binding.progressGroup.visibility = View.GONE
    }
}
