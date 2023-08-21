package com.paypal.android.ui.approveorderprogress

import android.os.Bundle
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.ui.approveorderprogress.events.ApproveOrderEvent
import com.paypal.android.uishared.events.ComposableEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ApproveOrderProgressFragment : Fragment() {

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    private lateinit var cardClient: CardClient
    private lateinit var payPalDataCollector: PayPalDataCollector

    private val viewModel by viewModels<ApproveOrderProgressViewModel>()

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.appendEventToLog(ApproveOrderEvent.Message("Fetching Client ID..."))
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
}
