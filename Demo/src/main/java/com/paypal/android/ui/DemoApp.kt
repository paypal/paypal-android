package com.paypal.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paypal.android.models.TestCard
import com.paypal.android.ui.approveorder.ApproveOrderView
import com.paypal.android.ui.approveorder.ApproveOrderViewModel
import com.paypal.android.ui.features.FeaturesView
import com.paypal.android.ui.paypalbuttons.PayPalButtonsView
import com.paypal.android.ui.paypalnative.PayPalNativeView
import com.paypal.android.ui.paypalweb.PayPalWebView
import com.paypal.android.ui.selectcard.SelectCardView
import com.paypal.android.ui.vaultcard.VaultCardView
import com.paypal.android.ui.vaultcard.VaultCardViewModel
import com.paypal.android.uishared.components.DemoAppTopBar
import com.paypal.android.uishared.effects.NavDestinationChangeDisposableEffect

// Ref: https://youtu.be/goFpG25uoc8?si=hqYGEaA95We6qUiE&t=76
@Suppress("LongMethod")
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun DemoApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var shouldDisplayBackButton by remember { mutableStateOf(false) }

    NavDestinationChangeDisposableEffect(navController) { controller ->
        shouldDisplayBackButton = controller.previousBackStackEntry != null
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                val route = navBackStackEntry?.destination?.route
                val titleText = DemoAppDestinations.titleForDestination(route)
                DemoAppTopBar(
                    title = titleText,
                    shouldDisplayBackButton = shouldDisplayBackButton,
                    onBackButtonClick = {
                        val destinationId = navController.graph.startDestinationId
                        navController.popBackStack(destinationId, false)
                    }
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "features",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(DemoAppDestinations.FEATURES_ROUTE) {
                    FeaturesView(onFeatureSelected = { feature ->
                        navController.navigate(feature.routeName)
                    })
                }
                composable(DemoAppDestinations.CARD_APPROVE_ORDER) { entry ->
                    // prefill test card (if necessary)
                    val viewModel: ApproveOrderViewModel = hiltViewModel()
                    parseTestCardFromSavedState(entry.savedStateHandle)?.let { testCard ->
                        viewModel.prefillCard(testCard)
                    }
                    ApproveOrderView(
                        viewModel = viewModel,
                        onUseTestCardClick = { navController.navigate(DemoAppDestinations.SELECT_TEST_CARD) }
                    )
                }
                composable(DemoAppDestinations.CARD_VAULT) { entry ->
                    // prefill test card (if necessary)
                    val viewModel: VaultCardViewModel = hiltViewModel()
                    parseTestCardFromSavedState(entry.savedStateHandle)?.let { testCard ->
                        viewModel.prefillCard(testCard)
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
                composable(DemoAppDestinations.PAYPAL_NATIVE) {
                    PayPalNativeView()
                }
                composable(DemoAppDestinations.SELECT_TEST_CARD) {
                    SelectCardView(onTestCardSelected = { testCardId ->
                        val prevBackStackEntry = navController.previousBackStackEntry
                        prevBackStackEntry?.savedStateHandle?.set("test_card_id", testCardId)
                        navController.popBackStack()
                    })
                }
            }
        }
    }
}

// Ref: https://youtu.be/NhoV78E6yWo?si=zZR2kFKHtthJ93tG
private fun parseTestCardFromSavedState(savedStateHandle: SavedStateHandle): TestCard? =
    savedStateHandle.get<String>("test_card_id")?.let { TestCard.byId(it) }
