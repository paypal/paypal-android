package com.paypal.android.ui.paypal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.paypal.android.R
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.databinding.FragmentPayPalNativeBinding
import com.paypal.android.uishared.components.CreateOrderWithShippingPreferenceForm
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.usecase.GetOrderUseCase
import com.paypal.android.viewmodels.NativeCheckoutViewState
import com.paypal.android.viewmodels.PayPalNativeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class PayPalNativeFragment : Fragment() {

    private lateinit var binding: FragmentPayPalNativeBinding

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    @Inject
    lateinit var getOrderUseCase: GetOrderUseCase

    private var selectedShippingPreference: ShippingPreferenceType? = null

    private val viewModel: PayPalNativeViewModel by viewModels()

    private val orderIntent: OrderIntent
        get() = when (binding.radioGroupIntent.checkedRadioButtonId) {
            R.id.intent_authorize -> OrderIntent.AUTHORIZE
            else -> OrderIntent.CAPTURE
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PayPalNativeView(
                        uiState,
                        onCreateOrderClick = { createOrder() }
                    )
                }
            }
        }
    }

    private fun createOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCreateOrderLoading = true

            val shippingPreference = viewModel.shippingPreference
            val orderIntent = viewModel.intentOption
            viewModel.createdOrder = getOrderUseCase(shippingPreference, orderIntent)

            viewModel.isCreateOrderLoading = false
        }
    }

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View {
//        binding = FragmentPayPalNativeBinding.inflate(inflater, container, false)
//        viewModel.state.observe(viewLifecycleOwner) { viewState ->
//            checkViewState(viewState)
//        }
//        with(binding) {
//            startNativeCheckout.setOnClickListener { startCheckout() }
//            fetchClientIdButton.setOnClickListener { viewModel.fetchClientId() }
//            tryAgainButton.setOnClickListener { viewModel.reset() }
//        }
//        initShippingOptions()
//        return binding.root
//    }

    private fun startCheckout() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.startNativeCheckout()
        }
//        binding.checkoutOptionsRadioGroup.isVisible = false
//        binding.radioGroupIntent.isVisible = false
    }

    private fun initShippingOptions() {
        ShippingPreferenceType.values().forEach { shippingPreferenceType ->
            val radioButton = RadioButton(requireContext())
            radioButton.text = shippingPreferenceType.description
            radioButton.setOnClickListener {
                selectedShippingPreference = shippingPreferenceType
                binding.startNativeCheckout.isEnabled = true
            }
            binding.checkoutOptionsRadioGroup.addView(radioButton)
        }
    }

    private fun checkViewState(viewState: NativeCheckoutViewState) {
        when (viewState) {
            NativeCheckoutViewState.Initial -> setInitialState()
            NativeCheckoutViewState.CheckoutInit -> checkoutInit()
            NativeCheckoutViewState.CheckoutCancelled -> checkoutCancelled()
            is NativeCheckoutViewState.CheckoutComplete -> checkoutComplete(viewState)
            is NativeCheckoutViewState.CheckoutError -> checkoutError(viewState)
            NativeCheckoutViewState.CheckoutStart -> checkoutStart()
            NativeCheckoutViewState.FetchingClientId -> generatingToken()
            is NativeCheckoutViewState.OrderCreated -> orderCreated(viewState)
            is NativeCheckoutViewState.ClientIdFetched -> clientIdFetched(viewState)
            NativeCheckoutViewState.OrderPatched -> orderPatched()
            is NativeCheckoutViewState.OrderCaptured -> orderCaptured(viewState)
            is NativeCheckoutViewState.OrderAuthorized -> orderAuthorized(viewState)
        }
    }

    private fun setInitialState() {
        with(binding) {
            fetchClientIdButton.visibility = View.VISIBLE
            fetchClientIdButton.isEnabled = true
            startNativeCheckout.visibility = View.GONE
            contentGroup.visibility = View.GONE
            tryAgainButton.visibility = View.GONE
            hideProgress()
        }
    }

    private fun generatingToken() {
        showProgress(getString(R.string.fetching_client_id))
        with(binding) {
            fetchClientIdButton.isEnabled = false
            startNativeCheckout.visibility = View.GONE
        }
    }

    private fun clientIdFetched(viewState: NativeCheckoutViewState.ClientIdFetched) {
        hideProgress()
        setContent(getString(R.string.client_id_fetched), viewState.token)
        with(binding) {
            startNativeCheckout.visibility = View.VISIBLE
            fetchClientIdButton.visibility = View.GONE
            checkoutOptionsRadioGroup.clearCheck()
            checkoutOptionsRadioGroup.isVisible = true
            radioGroupIntent.isVisible = true
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
        }
    }

    private fun checkoutStart() {
        showProgress(getString(R.string.starting_paypal))
    }

    private fun checkoutError(viewState: NativeCheckoutViewState.CheckoutError) {
        val message =
            viewState.message ?: viewState.error?.reason ?: getString(R.string.something_went_wrong)
        setContent(getString(R.string.error), message)
        hideProgress()
        with(binding) {
            startNativeCheckout.visibility = View.GONE
            tryAgainButton.visibility = View.VISIBLE
        }
    }

    private fun orderCreated(viewState: NativeCheckoutViewState.OrderCreated) {
        setContent(getString(R.string.order_created), "OrderId: ${viewState.orderId}")
        hideProgress()
    }

    private fun orderCaptured(viewState: NativeCheckoutViewState.OrderCaptured) {
        val contentText = viewState.order.run { "OrderId: $id Status: $status Intent: CAPTURE" }
        setContent(getString(R.string.order_created), contentText)
        hideProgress()
    }

    private fun orderAuthorized(viewState: NativeCheckoutViewState.OrderAuthorized) {
        val contentText = viewState.order.run { "OrderId: $id Status: $status Intent: AUTHORIZE" }
        setContent(getString(R.string.order_created), contentText)
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

    private fun checkoutComplete(viewState: NativeCheckoutViewState.CheckoutComplete) {
        val content = "Order Id: ${viewState.orderId} \n" + "Payer Id: ${viewState.payerId} \n"
        setContent(getString(R.string.approved), content)
        hideProgress()
        with(binding) {
            tryAgainButton.visibility = View.VISIBLE
            startNativeCheckout.visibility = View.GONE
        }
        viewState.orderId?.let { orderId ->
            when (orderIntent) {
                OrderIntent.CAPTURE -> {
                    showProgress("Capturing Order...")
                    viewModel.captureOrder(orderId)
                }

                OrderIntent.AUTHORIZE -> {
                    showProgress("Authorizing Order...")
                    viewModel.authorizeOrder(orderId)
                }
            }
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

    private fun orderPatched() {
        Toast.makeText(requireContext(), "Order Patched", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Patch Order after shipping change was successful")
    }

    companion object {
        private val TAG = PayPalNativeFragment::class.java.simpleName
    }

    @Composable
    fun PayPalNativeView(
        uiState: PayPalNativeUiState,
        onCreateOrderClick: () -> Unit
    ) {
        val scrollState = rememberScrollState()
        LaunchedEffect(uiState) {
            // continuously scroll to bottom of the list when event state is updated
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            CreateOrderWithShippingPreferenceForm(
                title = "Create an order to proceed with ${stringResource(R.string.feature_paypal_native)}:",
                orderIntent = uiState.intentOption,
                shippingPreference = uiState.shippingPreference,
                isLoading = uiState.isCreateOrderLoading,
                onIntentOptionSelected = { value -> viewModel.intentOption = value },
                onShippingPreferenceSelected = { value -> viewModel.shippingPreference = value },
                onSubmit = { onCreateOrderClick() }
            )
            uiState.createdOrder?.let { createdOrder ->
                Spacer(modifier = Modifier.size(24.dp))
                OrderView(order = createdOrder, title = "Order Created")
                Spacer(modifier = Modifier.size(24.dp))
                StartPayPalNativeCheckoutForm(
                    isLoading = uiState.isStartCheckoutLoading,
                    onSubmit = { startCheckout() }
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}
