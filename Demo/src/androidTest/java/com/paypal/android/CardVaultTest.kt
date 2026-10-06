package com.paypal.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.robots.DemoRobot
import com.paypal.android.robots.DeviceSettingsRobot
import com.paypal.android.utils.TestCard
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CardVaultTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { DemoRobot(composeTestRule) }

    private val deviceSettingsRobot = DeviceSettingsRobot()

    @Before
    fun setUp() {
        deviceSettingsRobot.disablePasswordManagers()
        deviceSettingsRobot.resetAppLinksToDefaults()
        deviceSettingsRobot.setupAppLinksForCurrentApp()
    }

    @After
    fun tearDown() {
        deviceSettingsRobot.resetAppLinksToDefaults()
    }

    @Test
    fun shouldVaultCardWith3DS(
        @TestParameter sca: SCA,
        @TestParameter("VISA_3DS_SUCCESSFUL_AUTH") testCard: TestCard
    ) {
        robot.navigateToCardVault()
            .createSetupToken(sca)
            .pickTestCard(testCard.displayName)
            .vaultCard()
            .verify3DSChallenge()
            .verifyCardVaulted()
            .createPaymentToken()
    }

    @Test
    fun shouldVaultCardWithout3DS(
        @TestParameter(
            "VISA_VAULT_WITH_PURCHASE_NO_3DS",
            "VISA_NO_3DS",
        ) testCard: TestCard
    ) {
        robot.navigateToCardVault()
            .createSetupToken(SCA.SCA_WHEN_REQUIRED)
            .pickTestCard(testCard.displayName)
            .vaultCard()
            .verifyCardVaulted()
            .createPaymentToken()
    }
}
