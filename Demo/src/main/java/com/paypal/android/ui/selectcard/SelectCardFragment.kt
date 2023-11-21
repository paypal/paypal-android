package com.paypal.android.ui.selectcard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.paypal.android.models.TestCard

class SelectCardFragment : Fragment() {

    companion object {
        const val REQUEST_KEY_TEST_CARD = "SELECT_CARD_REQUEST_KEY_TEST_CARD"
        const val DATA_KEY_TEST_CARD = "SELECT_CARD_DATA_KEY_TEST_CARD"
    }

    @ExperimentalFoundationApi
    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SelectCardView(onTestCardSelected = {})
                }
            }
        }
    }

    private fun onTestCardSelected(testCard: TestCard) {
        val bundle = bundleOf(DATA_KEY_TEST_CARD to testCard)
        setFragmentResult(REQUEST_KEY_TEST_CARD, bundle)

        // go back to previous fragment with test card as a result
        findNavController().navigateUp()
    }
}
