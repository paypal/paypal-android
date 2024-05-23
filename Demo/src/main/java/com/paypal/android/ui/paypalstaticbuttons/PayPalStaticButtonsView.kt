package com.paypal.android.ui.paypalstaticbuttons

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.paypal.android.R

@SuppressLint("InflateParams")
@Composable
fun PayPalStaticButtonsView() {
    AndroidView(
        factory = { context ->
            val view = LayoutInflater.from(context)
                .inflate(R.layout.pay_later_button_test_layout, null, false)

            view
        },
        modifier = Modifier.fillMaxSize()
    )
}
