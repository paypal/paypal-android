package com.paypal.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.paypal.android.R
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
        }
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.payment_methods_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.payment_methods_menu_settings -> {
                launchSettingsFragment()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun launchPayPalFragment() {
        navigate(PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToPayPalFragment())
    }

    private fun launchCardFragment() {
        navigate(PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToCardFragment())
    }

    private fun launchSettingsFragment() {
        navigate(PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToSettingsFragment())
    }

    private fun navigate(action: NavDirections) {
        findNavController().navigate(action)
    }
}
