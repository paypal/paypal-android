package com.paypal.android.plainclothes

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paypal.android.uishared.effects.NavDestinationChangeDisposableEffect
import com.paypal.android.utils.UIConstants

// The architecture of the Demo app is heavily influenced by Google sample apps written
// entirely in compose, most specifically the Jetsnack app
// Ref: https://github.com/android/compose-samples/tree/main

@Suppress("LongMethod")
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun PlainClothesApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var shouldDisplayBackButton by remember { mutableStateOf(false) }

    NavDestinationChangeDisposableEffect(navController) { controller ->
        shouldDisplayBackButton = controller.previousBackStackEntry != null
    }

    MaterialTheme {
        Scaffold { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = PlainClothesAppDestinations.HOME,
                enterTransition = {
                    fadeIn() + slideInVertically { UIConstants.getSlideInStartOffsetY(it) }
                },
                exitTransition = { fadeOut() },
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(PlainClothesAppDestinations.HOME) {
                    HomeView()
                }
            }
        }
    }
}
