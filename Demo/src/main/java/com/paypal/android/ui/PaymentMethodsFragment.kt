package com.paypal.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.paypal.android.databinding.FragmentPaymentMethodsBinding

class PaymentMethodsFragment : Fragment() {

    private lateinit var binding: FragmentPaymentMethodsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentMethodsBinding.inflate(inflater, container, false)

        binding.run {
            cardButton.setOnClickListener { launchCardFragment() }
            payPalButton.setOnClickListener { launchPayPalFragment() }
            payPalNativeButton.setOnClickListener { launchPayPalNativeFragment() }
        }
        return binding.root
    }

    private fun launchPayPalFragment() {
        navigate(PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToPayPalFragment())
    }

    private fun launchCardFragment() {
        navigate(PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToSelectCardFragment())
    }

    private fun launchPayPalNativeFragment() {
        navigate(PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToPayPalNativeFragment())
    }

    private fun navigate(action: NavDirections) {
        findNavController().navigate(action)
    }
}
