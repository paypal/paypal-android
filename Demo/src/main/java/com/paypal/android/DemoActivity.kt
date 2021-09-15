package com.paypal.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.paypal.android.core.Http
import com.paypal.android.core.HttpRequest
import kotlinx.coroutines.launch

class DemoActivity : AppCompatActivity() {

    val http = Http()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        lifecycleScope.launch {
            val request = HttpRequest("https://www.google.com")
            val result = http.send(request)
            print(result.responseCode)
        }
    }
}
