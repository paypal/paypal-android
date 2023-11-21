package com.paypal.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paypal.android.models.TestCard
import com.paypal.android.ui.approveorder.ApproveOrderView
import com.paypal.android.ui.approveorder.ApproveOrderViewModel
import com.paypal.android.ui.features.FeaturesView
import com.paypal.android.ui.paypalbuttons.PayPalButtonsView
import com.paypal.android.ui.paypalweb.PayPalWebView
import com.paypal.android.ui.selectcard.SelectCardView
import com.paypal.android.ui.vaultcard.VaultCardView
import com.paypal.android.ui.vaultcard.VaultCardViewModel

// Ref: https://youtu.be/goFpG25uoc8?si=hqYGEaA95We6qUiE&t=76
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun DemoApp() {
    val navController = rememberNavController()
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "features") {
                composable(DemoAppDestinations.FEATURES_ROUTE) {
                    FeaturesView(onFeatureSelected = { feature ->
                        navController.navigate(feature.routeName)
                    })
                }
                composable(DemoAppDestinations.CARD_APPROVE_ORDER) { entry ->
                    val viewModel: ApproveOrderViewModel = hiltViewModel()

                    // prefill test card (if necessary)
                    entry.savedStateHandle.get<String>("test_card_id")?.let { testCardId ->
                        TestCard.byId(testCardId)?.let { testCard ->
                            viewModel.prefillCard(testCard)
                        }
                    }

                    // Ref: https://youtu.be/NhoV78E6yWo?si=zZR2kFKHtthJ93tG
                    ApproveOrderView(
                        viewModel = viewModel,
                        onUseTestCardClick = { navController.navigate(DemoAppDestinations.SELECT_TEST_CARD) }
                    )
                }
                composable(DemoAppDestinations.CARD_VAULT) { entry ->
                    val viewModel: VaultCardViewModel = hiltViewModel()

                    // prefill test card (if necessary)
                    entry.savedStateHandle.get<String>("test_card_id")?.let { testCardId ->
                        TestCard.byId(testCardId)?.let { testCard ->
                            viewModel.prefillCard(testCard)
                        }
                    }
                    VaultCardView(
                        viewModel = viewModel,
                        onUseTestCardClick = { navController.navigate(DemoAppDestinations.SELECT_TEST_CARD) }
                    )
                }
                composable(DemoAppDestinations.PAYPAL_WEB) {
                    PayPalWebView()
                }
                composable(DemoAppDestinations.PAYPAL_BUTTONS) {
                    PayPalButtonsView()
                }
                composable(DemoAppDestinations.SELECT_TEST_CARD) {
                    SelectCardView(onTestCardSelected = { testCardId ->
                        val savedStateHandle =
                            navController.previousBackStackEntry?.savedStateHandle
                        savedStateHandle?.set("test_card_id", testCardId)
                        navController.popBackStack()
                    })
                }
            }
        }
    }
}
