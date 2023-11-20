package com.paypal.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paypal.android.ui.approveorder.ApproveOrderView
import com.paypal.android.ui.features.FeaturesView
import com.paypal.android.ui.selectcard.SelectCardView

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
                composable(DemoAppDestinations.CARD_APPROVE_ORDER) {
                    ApproveOrderView(onUseTestCardClick = {
                        navController.navigate(DemoAppDestinations.SELECT_TEST_CARD)
                    })
                }
                composable(DemoAppDestinations.SELECT_TEST_CARD) {
                    SelectCardView(onTestCardSelected = { testCard ->

                    })
                }
            }
        }
    }
}
