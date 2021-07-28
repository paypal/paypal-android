package com.paypal.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.paypal.android.card.payWithCard
import com.paypal.android.core.PaymentsClient

class DemoActivity : AppCompatActivity() {

    private val payments = PaymentsClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        payments.payWithCard()
    }
}