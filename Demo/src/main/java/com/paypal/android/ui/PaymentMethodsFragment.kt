package com.paypal.android.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.paypal.android.R
import com.paypal.android.ui.card.CardFields
import com.paypal.android.ui.card.CardViewModel
import com.paypal.android.ui.theme.DemoTheme

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PaymentMethodsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PaymentMethodsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

    fun launchPayPalFragment() {

    }

    fun launchCardFragment() {
        val action = PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToCardFragment()
        findNavController().navigate(action)
    }
}