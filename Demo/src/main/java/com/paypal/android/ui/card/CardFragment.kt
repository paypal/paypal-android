package com.paypal.android.ui.card

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.paypal.android.R
import com.paypal.android.databinding.FragmentCardBinding
import com.paypal.android.text.SimpleTextWatcher
import com.paypal.android.ui.card.validation.CardFormatter
import com.paypal.android.utils.SharedPreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    private lateinit var binding: FragmentCardBinding

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
        binding = FragmentCardBinding.inflate(inflater, container, false)
        initializeDropDown()
        return binding.root
//        return ComposeView(requireContext()).apply {
//            setContent {
//                DemoTheme {
//                    Column {
//                        DropDown(
//                            cardViewModel.autoFillCards.map { it.first },
//                            stringResource(R.string.card_field_prefill_card_fields),
//                            { selectedCard -> cardViewModel.onPrefillCardSelected(selectedCard) },
//                            Modifier.padding(16.dp)
//                        )
//                        CardFields(
//                            cardViewModel,
//                            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
//                        )
//                        Button(
//                            onClick = { cardViewModel.onCardFieldSubmit() },
//                            modifier = Modifier
//                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
//                                .fillMaxWidth()
//                        ) { Text(stringResource(R.string.card_field_submit)) }
//                    }
//                }
//            }
//        }
    }

    private fun initializeDropDown() {
        // set drop down menu items
        val autoFillCardNames = cardViewModel.autoFillCards.map { it.first }
        val adapter = ArrayAdapter(requireActivity(), R.layout.dropdown_item, autoFillCardNames)

        binding.run {
            autoCompleteTextView.setAdapter(adapter)
            autoCompleteTextView.addTextChangedListener(object: SimpleTextWatcher() {
                override fun afterTextChanged(p0: Editable?) {
                    cardViewModel.selectedPrefillCard.value = autoCompleteTextView.text.toString()
                }
            })
        }

        cardViewModel.selectedPrefillCard.observe(viewLifecycleOwner) { cardName ->
            cardViewModel.run {
                autoFillCards.find { it.first == cardName }?.second?.apply {

//                    _cardNumber.value = CardFormatter.formatCardNumber(number)
//                    _expirationDate.value = "$expirationMonth/$expirationYear"
//                    _securityCode.value = securityCode
                }
            }
        }
    }
}
