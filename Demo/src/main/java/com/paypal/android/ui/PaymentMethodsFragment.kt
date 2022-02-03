package com.paypal.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.paypal.android.R
import com.paypal.android.ui.theme.DemoTheme

class PaymentMethodsFragment : Fragment(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DemoTheme {
                    Column {
                        Button(
                            onClick = { launchCardFragment() },
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp)
                                .fillMaxWidth()
                        ) { Text(stringResource(R.string.payment_methods_card)) }
                        Button(
                            onClick = { launchPayPalFragment() },
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .fillMaxWidth()
                        ) { Text(stringResource(R.string.payment_methods_paypal)) }
                    }
                }
            }
        }
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

    override fun onClick(p0: View?) {
        TODO("Not yet implemented")
    }
}
