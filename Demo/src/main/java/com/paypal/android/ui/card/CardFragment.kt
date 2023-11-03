package com.paypal.android.ui.card

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.paypal.android.api.model.Order
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.CardResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.models.OrderRequest
import com.paypal.android.models.TestCard
import com.paypal.android.ui.card.validation.CardViewUiState
import com.paypal.android.ui.selectcard.SelectCardFragment
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateOrderUseCase
import com.paypal.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    companion object {
        const val TAG = "CardFragment"
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    @Inject
    lateinit var createOrderUseCase: CreateOrderUseCase

    @Inject
    lateinit var completeOrderUseCase: CompleteOrderUseCase

    private lateinit var cardClient: CardClient
    private lateinit var payPalDataCollector: PayPalDataCollector

    private val viewModel by viewModels<CardViewModel>()

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        registerPrefillCardListener()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        CardView(
                            onCreateOrderSubmit = { createOrder() },
                            onApproveOrderSubmit = { approveOrder() },
                            onCompleteOrderSubmit = { completeOrder() },
                            onUseTestCardClick = { showTestCards() }
                        )
                    }
                }
            }
        }
    }

    private fun registerPrefillCardListener() {
        setFragmentResultListener(SelectCardFragment.REQUEST_KEY_TEST_CARD) { _, bundle ->
            bundle.parcelable<TestCard>(SelectCardFragment.DATA_KEY_TEST_CARD)?.let { testCard ->
                viewModel.prefillCard(testCard)
            }
        }
    }

    private fun createOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCreateOrderLoading = true

            val uiState = viewModel.uiState.value
            val orderRequest = uiState.run { OrderRequest(intentOption, shouldVault, customerId) }
            viewModel.createdOrder = createOrderUseCase(orderRequest)
            viewModel.isCreateOrderLoading = false
        }
    }

    private fun approveOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isApproveOrderLoading = true

            val clientId = sdkSampleServerAPI.fetchClientId()
            val coreConfig = CoreConfig(clientId = clientId)
            payPalDataCollector = PayPalDataCollector(coreConfig)

            cardClient = CardClient(requireActivity(), coreConfig)
            cardClient.approveOrderListener = object : ApproveOrderListener {
                override fun onApproveOrderSuccess(result: CardResult) {
                    viewModel.approveOrderResult = result
                    viewModel.isApproveOrderLoading = false
                }

                override fun onApproveOrderFailure(error: PayPalSDKError) {
                    viewModel.approveOrderErrorMessage = "CAPTURE fail: ${error.errorDescription}"
                    viewModel.isApproveOrderLoading = false
                }

                override fun onApproveOrderCanceled() {
                    viewModel.approveOrderErrorMessage = "USER CANCELED"
                    viewModel.isApproveOrderLoading = false
                }

                override fun onApproveOrderThreeDSecureWillLaunch() {
                    Log.d(TAG, "3DS Auth Requested")
                }

                override fun onApproveOrderThreeDSecureDidFinish() {
                    Log.d(TAG, "3DS Success")
                    viewModel.isApproveOrderLoading = false
                }
            }

            val uiState = viewModel.uiState.value
            val order = viewModel.createdOrder
            val cardRequest = createCardRequest(uiState, order!!)

            cardClient.approveOrder(requireActivity(), cardRequest)
        }
    }

    private fun completeOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCompleteOrderLoading = true

            val cmid = payPalDataCollector.collectDeviceData(requireContext())
            val orderId = viewModel.createdOrder!!.id!!
            val orderIntent = viewModel.intentOption

            viewModel.completedOrder = completeOrderUseCase(orderId, orderIntent, cmid)
            viewModel.isCompleteOrderLoading = false
        }
    }

    private fun showTestCards() {
        val action = CardFragmentDirections.actionCardFragmentToSelectCardFragment()
        findNavController().navigate(action)
    }

    private fun createCardRequest(uiState: CardViewUiState, order: Order): CardRequest {
        val card = parseCard(uiState)
        val sca = when (uiState.scaOption) {
            "ALWAYS" -> SCA.SCA_ALWAYS
            else -> SCA.SCA_WHEN_REQUIRED
        }
        return CardRequest(order.id!!, card, APP_RETURN_URL, sca)
    }

    private fun parseCard(uiState: CardViewUiState): Card {
        // TODO: handle invalid date string
        var expirationMonth = ""
        var expirationYear = ""

        // expiration date in UI State needs to be formatted because it uses a visual transformation
        val dateString = DateString(uiState.cardExpirationDate)
        val dateStringComponents = dateString.formatted.split("/")
        if (dateStringComponents.isNotEmpty()) {
            expirationMonth = dateStringComponents[0]
            if (dateStringComponents.size > 1) {
                val rawYear = dateStringComponents[1]
                expirationYear = if (rawYear.length == 2) {
                    // pad with 20 to assume 2000's
                    "20$rawYear"
                } else {
                    rawYear
                }
            }
        }

        return Card(
            number = uiState.cardNumber,
            expirationMonth = expirationMonth,
            expirationYear = expirationYear,
            securityCode = uiState.cardSecurityCode
        )
    }
}
