package com.paypal.android.ui.approveorderprogress

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.ui.card.DataCollectorHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ApproveOrderProgressFragment : Fragment() {

    companion object {
        const val TAG = "CardFragment"
    }

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    private lateinit var cardClient: CardClient

    @Inject
    lateinit var dataCollectorHandler: DataCollectorHandler

    private val args: ApproveOrderProgressFragmentArgs by navArgs()
    private val viewModel by viewModels<ApproveOrderProgressViewModel>()

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launch {
            executeCardRequestFromArgs()
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ApproveOrderProgressView()
                    }
                }
            }
        }
    }

    private suspend fun executeCardRequestFromArgs() {
        val cardRequest = args.cardRequest

//        val orderIntent = when (uiState.intentOption) {
//            "AUTHORIZE" -> OrderIntent.AUTHORIZE
//            else -> OrderIntent.CAPTURE
//        }

        val clientId = sdkSampleServerAPI.fetchClientId()
        val configuration = CoreConfig(clientId = clientId)
        cardClient = CardClient(requireActivity(), configuration)

        cardClient.approveOrderListener = object : ApproveOrderListener {
            override fun onApproveOrderSuccess(result: CardResult) {
                viewLifecycleOwner.lifecycleScope.launch {
//                    finishOrder(result, orderIntent)
                }
            }

            override fun onApproveOrderFailure(error: PayPalSDKError) {
                viewModel.updateStatusText("CAPTURE fail: ${error.errorDescription}")
            }

            override fun onApproveOrderCanceled() {
                viewModel.updateStatusText("USER CANCELED")
            }

            override fun onApproveOrderThreeDSecureWillLaunch() {
                viewModel.updateStatusText("3DS launched")
            }

            override fun onApproveOrderThreeDSecureDidFinish() {
                viewModel.updateStatusText("3DS finished")
            }
        }

        dataCollectorHandler.setLogging(true)
        val clientMetadataId = dataCollectorHandler.getClientMetadataId(cardRequest.orderId)
        Log.i(TAG, "MetadataId: $clientMetadataId")

        viewModel.updateStatusText("Authorizing order...")

        // approve order using card request
        cardClient.approveOrder(requireActivity(), cardRequest)
    }

    @ExperimentalMaterial3Api
    @Composable
    fun ApproveOrderProgressView() {

    }

    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun ApproveOrderProgressPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                ApproveOrderProgressView()
            }
        }
    }

//    private suspend fun finishOrder(cardResult: CardResult, orderIntent: OrderIntent) {
//        val orderId = cardResult.orderId
//        val finishResult = when (orderIntent) {
//            OrderIntent.CAPTURE -> {
//                viewModel.updateStatusText("Capturing order with ID: ${cardResult.orderId}...")
//                sdkSampleServerAPI.captureOrder(orderId)
//            }
//
//            OrderIntent.AUTHORIZE -> {
//                viewModel.updateStatusText("Authorizing order with ID: ${cardResult.orderId}...")
//                sdkSampleServerAPI.authorizeOrder(orderId)
//            }
//        }
//
//        viewModel.updateStatusText("Status: ${finishResult.status}")
//        val orderDetailsText = "Confirmed Order: $orderId"
//        val deepLink = cardResult.deepLinkUrl?.toString().orEmpty()
//        val joinedText = listOf(orderDetailsText, deepLink).joinToString("\n")
//        viewModel.updateOrderDetailsText(joinedText)
//    }
}