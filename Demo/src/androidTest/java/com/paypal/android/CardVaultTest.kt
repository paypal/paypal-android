package com.paypal.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.robots.DemoRobot
import com.paypal.android.robots.DeviceSettingsRobot
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
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
    }

    @After
    fun tearDown() {
        deviceSettingsRobot.resetAppLinksToDefaults()
    }

    @Test
    fun shouldVaultCardWith3DS(
        @TestParameter sca: SCA,
        /*
          * Want to run CUSTOM_URL_SCHEME tests first to ensure app links are configured before they are executed
          * since app links configuration takes time, running CUSTOM_URL_SCHEME gives extra time
         */
        @TestParameter(
            "CUSTOM_URL_SCHEME",
            "APP_LINKS"
        ) returnToAppStrategy: ReturnToAppStrategyOption,
        @TestParameter("VISA_3DS_SUCCESSFUL_AUTH") testCard: TestCard
    ) {
        if (returnToAppStrategy == ReturnToAppStrategyOption.APP_LINKS) {
            deviceSettingsRobot.setupAppLinksForCurrentApp()
        }

        robot.navigateToCardVault()
            .createSetupToken(
                returnToAppStrategy = returnToAppStrategy,
                sca = sca
            )
            .pickTestCard(testCard.displayName)
            .vaultCard()
            .verify3DSChallenge()
            .verifyCardVaulted()
            .createPaymentToken()
    }

    @Test
    fun shouldVaultCardWithout3DS(
        /*
        * Want to run CUSTOM_URL_SCHEME tests first to ensure app links are configured before they are executed
        * since app links configuration takes time, running CUSTOM_URL_SCHEME gives extra time
        */
        @TestParameter(
            "CUSTOM_URL_SCHEME",
            "APP_LINKS"
        ) returnToAppStrategy: ReturnToAppStrategyOption,
        @TestParameter(
            "VISA_VAULT_WITH_PURCHASE_NO_3DS",
            "VISA_NO_3DS",
        ) testCard: TestCard
    ) {
        if (returnToAppStrategy == ReturnToAppStrategyOption.APP_LINKS) {
            deviceSettingsRobot.setupAppLinksForCurrentApp()
        }
        robot.navigateToCardVault()
            .createSetupToken(
                returnToAppStrategy = ReturnToAppStrategyOption.CUSTOM_URL_SCHEME,
                sca = SCA.SCA_WHEN_REQUIRED
            )
            .pickTestCard(testCard.displayName)
            .vaultCard()
            .verifyCardVaulted()
            .createPaymentToken()
    }
}
