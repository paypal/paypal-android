package com.paypal.android.ui.approveorderprogress

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.paypal.android.DemoViewModel
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardAuthChallengeResult
import com.paypal.android.cardpayments.CardAuthLauncher
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.ui.approveorderprogress.events.ApproveOrderEvent
import com.paypal.android.ui.card.DataCollectorHandler
import com.paypal.android.uishared.events.ComposableEvent
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

    @Inject
    lateinit var dataCollectorHandler: DataCollectorHandler

    private val cardAuthLauncher = CardAuthLauncher()

    private val args: ApproveOrderProgressFragmentArgs by navArgs()
    private val viewModel by viewModels<ApproveOrderProgressViewModel>()

    private val activityViewModel by activityViewModels<DemoViewModel>()

    lateinit private var cardClient: CardClient

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // setup card client
        val configuration = CoreConfig(clientId = sdkSampleServerAPI.clientId)
        cardClient = CardClient(requireActivity(), configuration)
        cardClient.approveOrderListener = object : ApproveOrderListener {
            override fun onApproveOrderSuccess(result: CardResult) {
                viewModel.appendEventToLog(ApproveOrderEvent.Message("Order Approved"))
                viewModel.appendEventToLog(ApproveOrderEvent.ApproveSuccess(result))
                viewLifecycleOwner.lifecycleScope.launch {
                    finishOrder(result)
                }
            }

            override fun onApproveOrderFailure(error: PayPalSDKError) {
                viewModel.appendEventToLog(ApproveOrderEvent.Message("CAPTURE fail: ${error.errorDescription}"))
            }

            override fun onApproveOrderCanceled() {
                viewModel.appendEventToLog(ApproveOrderEvent.Message("USER CANCELED"))
            }

            override fun didReceiveAuthChallenge(authChallenge: CardAuthChallenge) {
                viewModel.appendEventToLog(ApproveOrderEvent.Message("3DS Auth Requested"))
                cardAuthLauncher.launch(requireActivity(), authChallenge)
            }

            override fun onApproveOrderThreeDSecureDidFinish() {
                viewModel.appendEventToLog(ApproveOrderEvent.Message("3DS Success"))
            }
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val events by viewModel.eventLog.collectAsStateWithLifecycle()
                        ApproveOrderProgressView(events)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val cardAuthResult = checkForCardAuthResult()
        if (cardAuthResult != null) {
            cardAuthLauncher.clearPendingRequests(requireContext())
            cardClient.continueApproveOrder(cardAuthResult)
        } else {
            executeCardRequestFromArgs()
        }
    }

    private fun checkForCardAuthResult(): CardAuthChallengeResult? {
        val context = requireContext()
        return cardAuthLauncher.parseResult(context, activity?.intent)
            ?: cardAuthLauncher.parseResult(context, activityViewModel.newIntent.value)
    }

    private fun executeCardRequestFromArgs() {
        val cardRequest = args.cardRequest!!
        viewModel.appendEventToLog(ApproveOrderEvent.Message("Fetching Client ID..."))

        dataCollectorHandler.setLogging(true)
        val clientMetadataId = dataCollectorHandler.getClientMetadataId(cardRequest.orderId)
        Log.i(TAG, "MetadataId: $clientMetadataId")

        viewModel.appendEventToLog(ApproveOrderEvent.Message("Authorizing Order..."))

        // approve order using card request
        cardClient.approveOrder(cardRequest)
    }

    @ExperimentalMaterial3Api
    @Composable
    fun ApproveOrderProgressView(events: List<ComposableEvent>) {
        val listState = rememberLazyListState()
        LaunchedEffect(events) {
            // continuously scroll to bottom of the list when event state is updated
            listState.animateScrollToItem(events.size)
        }
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(16.dp)
        ) {
            items(events) {
                it.AsComposable()
            }
        }
    }

    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun ApproveOrderProgressPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                ApproveOrderProgressView(emptyList())
            }
        }
    }

    private suspend fun finishOrder(cardResult: CardResult) {
        viewModel.appendEventToLog(ApproveOrderEvent.Message("Fetching Order Info..."))

        val orderId = cardResult.orderId
        val order = sdkSampleServerAPI.getOrder(orderId)
        viewModel.appendEventToLog(ApproveOrderEvent.GetOrder(order))

        val finishResult = when (order.intent) {
            "CAPTURE" -> {
                viewModel.appendEventToLog(ApproveOrderEvent.Message("Capturing Order..."))
                sdkSampleServerAPI.captureOrder(orderId)
            }

            "AUTHORIZE" -> {
                viewModel.appendEventToLog(ApproveOrderEvent.Message("Authorizing Order..."))
                sdkSampleServerAPI.authorizeOrder(orderId)
            }

            else -> {
                null
            }
        }

        if (finishResult == null) {
            viewModel.appendEventToLog(ApproveOrderEvent.Message("Order Intent Could Not Be Determined"))
        } else {
            viewModel.appendEventToLog(ApproveOrderEvent.OrderComplete(finishResult))
        }
    }
}
