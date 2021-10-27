package com.paypal.android.ui.card

import android.os.Bundle
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.paypal.android.R
import com.paypal.android.ui.theme.DemoTheme
import com.paypal.android.utils.SharedPreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    @Inject
    lateinit var preferenceUtil: SharedPreferenceUtil
    private val cardViewModel: CardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cardViewModel.environment = preferenceUtil.getEnvironment()
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
                        DropDown(
                            cardViewModel.autoFillCards.map { it.first },
                            stringResource(R.string.card_field_prefill_card_fields),
                            { selectedCard -> cardViewModel.onPrefillCardSelected(selectedCard) },
                            Modifier.padding(16.dp)
                        )
                        CardFields(
                            cardViewModel,
                            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        )
                        Button(
                            onClick = { cardViewModel.onCardFieldSubmit() },
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .fillMaxWidth()
                        ) { Text(stringResource(R.string.card_field_submit)) }
                    }
                }
            }
        }
    }
}
