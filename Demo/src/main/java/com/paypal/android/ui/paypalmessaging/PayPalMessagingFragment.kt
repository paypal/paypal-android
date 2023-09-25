package com.paypal.android.ui.paypalmessaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PayPalMessagingFragment : Fragment() {

    private val viewModel by viewModels<PayPalMessagingViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        viewModel.fetchClientId()
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val clientId by viewModel.clientId.collectAsStateWithLifecycle()
                    PayPalMessagingView(clientId)
                }
            }
        }
    }

    @Composable
    fun PayPalMessagingView(clientId: String) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (clientId.isNotEmpty()) {
//                    val config = PayPalMessageConfig()
//                    config.setGlobalAnalytics("", "")
//                    config.data = PayPalMessageData(clientId = clientId)

                    // Ref: https://developer.android.com/jetpack/compose/migrate/interoperability-apis/views-in-compose#androidview_in_lazy_lists
                    AndroidView(
                        factory = { context ->
                            val messageView = PayPalMessagingPlaceholderView(context)
//                            messageView.layoutParams = ViewGroup.LayoutParams(
//                                ViewGroup.LayoutParams.MATCH_PARENT,
//                                ViewGroup.LayoutParams.WRAP_CONTENT
//                            )

//                            // NOTE: Kotlin getters / setters would be preferable to Java style getters / setters
//                            messageView.setLogoType(PayPalMessageLogoType.ALTERNATIVE)
//                            messageView.setColor(PayPalMessageColor.MONOCHROME)
//                            messageView.setViewStates(
//                                PayPalMessageViewState(
//                                    onLoading = {
//                                        Log.d("TAG", "onLoading")
//                                    },
//                                    onError = {
//                                        Log.d("TAG", "onError")
//                                    },
//                                    onSuccess = {
//                                        Log.d("TAG", "onSuccess")
//                                    }
//                                )
//                            )
                            messageView
                        }
                    )
                }
            }
        }
    }

    @Preview
    @Composable
    fun PayPalMessagingViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                PayPalMessagingView("")
            }
        }
    }
}