package com.paypal.android.ui.vault

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.paypal.android.models.TestCard
import com.paypal.android.ui.selectcard.SelectCardFragment
import com.paypal.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VaultCardFragment : Fragment() {

    private val viewModel by viewModels<VaultCardViewModel>()

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        registerPrefillCardListener()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        VaultCardView(
                            viewModel = viewModel,
                            onUseTestCardClick = { showTestCards() }
                        )
                    }
                }
            }
        }
    }

    private fun registerPrefillCardListener() {
        setFragmentResultListener(SelectCardFragment.REQUEST_KEY_TEST_CARD) { _, bundle ->
            bundle.parcelable<TestCard>(SelectCardFragment.DATA_KEY_TEST_CARD)?.let { testCard ->
                viewModel.prefillCard(testCard)
            }
        }
    }

    private fun showTestCards() {
        val action = VaultFragmentDirections.actionVaultFragmentToSelectCardFragment()
        findNavController().navigate(action)
    }
}
