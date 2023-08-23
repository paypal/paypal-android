package com.paypal.android

import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.VaultListener
import com.paypal.android.cardpayments.VaultRequest
import com.paypal.android.cardpayments.VaultResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.models.TestCard
import com.paypal.android.ui.selectcard.SelectCardViewModel.Companion.validExpirationYear
import com.paypal.android.usecase.CreateSetupTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class ValidVaultTestCards {

    val clientId =
        "AcXwOk3dof7NCNcriyS8RVh5q39ozvdWUF9oHPrWqfyrDS4AwVdKe32Axuk2ADo6rI_31Vv6MGgOyzRt"
    val config = CoreConfig(clientId, Environment.SANDBOX)

    val sdkSampleServerAPI = SDKSampleServerAPI()
    val createSetupToken = CreateSetupTokenUseCase(sdkSampleServerAPI)

    @Test
    fun testAllCards() {
        val latch = CountDownLatch(1)

        launchActivity<DemoActivity>().use { scenario ->
            scenario.onActivity { activity ->
                CoroutineScope(Dispatchers.IO).launch {
                    for (testCard in vaultVisaCards) {
                        vaultTestCard(activity, testCard)
                    }

                    for (testCard in vaultMasterCardCards) {
                        vaultTestCard(activity, testCard)
                    }

//                    for (testCard in vaultAmexCards) {
//                        vaultTestCard(activity, testCard)
//                    }
                    latch.countDown()
                }
            }
        }
        latch.await()
    }

    private suspend fun vaultTestCard(activity: FragmentActivity, testCard: TestCard) {
        val setupToken = createSetupToken(null)
        val cardClient = CardClient(activity, config)
        cardClient.vaultListener = object : VaultListener {
            override fun onVaultSuccess(result: VaultResult) {
            }

            override fun onVaultFailure(error: PayPalSDKError) {
            }
        }
        val vaultRequest = VaultRequest(setupToken.id, testCard.card)
        cardClient.vault(activity, vaultRequest)
    }

    val vaultVisaCards = listOf(
        "4344511979394247",
        "4344511947924612",
        "4344511935682511",
        "4344511969824302",
        "4208455603499482",
        "4208454507119734",
        "4138828323480633",
        "4797921506311444",
        "4866322104881860",
        "4866322162904752",
        "4866322140564272",
        "4866322105602042",
        "4081502429825762",
        "4652152830551943",
        "4023558309448191",
        "4233876801545751",
        "4371495452282705",
        "4371495484916288",
        "4371495496224200",
        "4371495487011004",
        "4528047081677730",
        "4558140604854564",
        "4528047034214383",
        "4558140646661399",
        "4444192069793553",
        "4553375593479693",
        "4563530529800747",
        "4563530411232173",
        "4928527498331621",
        "5458406957971448",
        "4928527489073901",
        "4928527452394656",
        "4507343298206297",
        "4987764315005496",
        "4556278037515956",
        "4507371021526764"
    ).mapIndexed { index, cardNumber ->
        TestCard(
            name = "Visa Vault Card $index",
            card = Card(
                number = cardNumber,
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )
    }

    val vaultMasterCardCards = listOf(
        "5301896424684575",
        "5301896469316893",
        "5301896469776575",
        "5301896494977248",
        "5570215453076559",
        "5426323025348229",
        "5578162032891789",
        "5543992434865227",
        "5319790130653497",
        "5319790186388451",
        "5319790100328138",
        "5319790195375879",
        "5570127123886251",
        "5420909238670205",
        "5100910000993004",
        "5570125875902235",
        "5335443101282678",
        "5335443134215034",
        "5335443150509955",
        "5335443104180127",
        "5349271586455244",
        "5349271522590492",
        "5349271594086999",
        "5349271545864528",
        "5308343503800775",
        "5308343533616225",
        "5308343597922949",
        "5308343571454174",
        "5136324884396896",
        "5314609070556962",
        "5136324855682399",
        "5314609032106849",
        "5232720217457490",
        "5206181449118354",
        "5206181900115543",
        "5470432115202579"
    ).mapIndexed { index, cardNumber ->
        TestCard(
            name = "MasterCard Vault Card $index",
            card = Card(
                number = cardNumber,
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )
    }

    val vaultAmexCards = listOf(
        "374180335868954",
        "374180933445635",
        "374180574644017",
        "374180923372658",
        "379275014952163",
        "379275217567669",
        "379271493783054",
        "379270034832776",
        "372290892264943",
        "372290836899234",
        "372290836897899",
        "372290452582494",
        "347810822248120",
        "347810191760135",
        "347810688750250",
        "347810084589484",
        "377758638678225",
        "377758798521710",
        "377758609151848",
        "377758945123402",
        "375670725130246",
        "375670253469776",
        "375670576048034",
        "375670485252503",
        "375380980486099",
        "375380178022425",
        "375380274391617",
        "375380214196027",
        "374905828528339",
        "374905961816467",
        "374905762611778",
        "374905294851462",
    ).mapIndexed { index, cardNumber ->
        TestCard(
            name = "AMEX Vault Card $index",
            card = Card(
                number = cardNumber,
                expirationMonth = "01",
                expirationYear = validExpirationYear,
                securityCode = "123"
            ),
        )
    }
}