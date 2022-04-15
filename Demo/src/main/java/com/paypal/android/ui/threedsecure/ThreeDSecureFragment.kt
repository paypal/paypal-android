package com.paypal.android.ui.threedsecure

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.paypal.android.R
import com.paypal.android.threedsecure.ThreeDSecureClient

class ThreeDSecureFragment : Fragment() {

    private val threeDSecureClient = ThreeDSecureClient()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_three_d_secure, container, false)
    }
}