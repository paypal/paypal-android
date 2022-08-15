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
            tryAgainButton.setOnClickListener { viewModel.reset() }
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
            contentGroup.visibility = View.GONE
            tryAgainButton.visibility = View.GONE
            hideProgress()
        }
    }

    private fun generatingToken() {
        showProgress(getString(R.string.fetching_access_token))
        with(binding) {
            fetchAccessTokenButton.isEnabled = false
            startNativeCheckout.visibility = View.GONE
        }
    }

    private fun tokenGenerated(viewState: PayPalNativeViewModel.ViewState.TokenGenerated) {
        hideProgress()
        setContent(getString(R.string.token_generated), viewState.token)
        with(binding) {
            startNativeCheckout.visibility = View.VISIBLE
            startNativeCheckout.isEnabled = true
            checkoutOptionsRadioGroup.visibility = View.VISIBLE
            selectedOptionId = R.id.order_checkout
            orderCheckout.isChecked = true
            fetchAccessTokenButton.visibility = View.GONE
        }
    }

    private fun setContent(titleText: String, contentText: String) {
        with(binding) {
            contentGroup.visibility = View.VISIBLE
            title.text = titleText
            content.text = contentText
        }
    }

    private fun checkoutInit() {
        showProgress(getString(R.string.init_checkout))
        with(binding) {
            startNativeCheckout.isEnabled = false
            checkoutOptionsRadioGroup.visibility = View.GONE
        }
    }

    private fun checkoutStart() {
        showProgress(getString(R.string.start_checkout))
    }

    private fun checkoutError(viewState: PayPalNativeViewModel.ViewState.CheckoutError) {
        val message = viewState.message ?: viewState.error?.reason ?: getString(R.string.something_went_wrong)
        setContent(getString(R.string.error), message)
        hideProgress()
        with(binding) {
            startNativeCheckout.visibility = View.GONE
            tryAgainButton.visibility = View.VISIBLE
        }
    }

    private fun orderCreated(viewState: PayPalNativeViewModel.ViewState.OrderCreated) {
        setContent(getString(R.string.order_created), "OrderId: ${viewState.orderId}")
        hideProgress()
    }

    private fun checkoutCancelled() {
        setContent(getString(R.string.cancelled), getString(R.string.checkout_cancelled_by_user))
        hideProgress()
        with(binding) {
            startNativeCheckout.visibility = View.GONE
            tryAgainButton.visibility = View.VISIBLE
        }
    }

    private fun checkoutComplete(viewState: PayPalNativeViewModel.ViewState.CheckoutComplete) {
        val content = "Order Id: ${viewState.orderId} \n" +
                "Payer Id: ${viewState.payerId} \n" +
                "Payment Id: ${viewState.paymentId} \n" +
                "Billing Token: ${viewState.billingToken}"
        setContent(getString(R.string.approved), content)
        hideProgress()
        with(binding) {
            tryAgainButton.visibility = View.VISIBLE
            startNativeCheckout.visibility = View.GONE
        }
    }

    private fun showProgress(text: String) {
        with(binding) {
            progressGroup.visibility = View.VISIBLE
            progressText.text = text
            contentGroup.visibility = View.GONE
        }
    }

    private fun hideProgress() {
        binding.progressGroup.visibility = View.GONE
    }
}
