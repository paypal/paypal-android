package com.paypal.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.paypal.android.card.CardClient
import com.paypal.android.core.PaymentsClient

class DemoActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)
    }
}