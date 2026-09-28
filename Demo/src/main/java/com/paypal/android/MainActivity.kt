package com.paypal.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import com.paypal.android.ui.DemoApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @ExperimentalComposeUiApi
    @ExperimentalMaterial3Api
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectCheckoutReturnIfNeeded(DemoActivityType.COMPONENT_ACTIVITY)) {
            return
        }
        setContent {
            DemoApp(
                activityType = DemoActivityType.COMPONENT_ACTIVITY,
                onSwitchActivityType = {
                    switchDemoActivityType(DemoActivityType.COMPONENT_ACTIVITY)
                }
            )
        }
    }
}
