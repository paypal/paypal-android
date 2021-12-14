package com.paypal.android.ui.payment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.paypal.android.BuildConfig
import com.paypal.android.R
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.paypal.android.ui.payment.paymentbutton.PayPalButton
import com.paypal.android.ui.payment.paymentbutton.PayPalCreditButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentButtonFragment : Fragment() {

    private val paymentButtonViewModel: PaymentFragmentViewModel by viewModels()

    private lateinit var paypalButton: PayPalButton
    private lateinit var paypalCreditButton: PayPalCreditButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_payment_button, container, false)

        paypalButton = view.findViewById(R.id.payPalButton)
        paypalCreditButton = view.findViewById(R.id.payPalCreditButton)

        val coreConfig = CoreConfig(BuildConfig.CLIENT_ID, environment = Environment.SANDBOX)
        val application = requireActivity().application
        val returnUrl = BuildConfig.APPLICATION_ID + "://paypalpay"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            paymentButtonViewModel.setPayPalClient(PayPalClient(application, coreConfig, returnUrl))
        }

        paypalButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                launchPayPalButtonPayment()
            }
        }

        paypalCreditButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                launchPayPalButtonPayment()
            }
        }

        return view
    }

    private fun launchPayPalButtonPayment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            paymentButtonViewModel.startPayPalCheckout()
        }
    }
}
