package com.paypal.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.robots.DemoRobot
import com.paypal.android.robots.DeviceSettingsRobot
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.utils.TestCard
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CardApproveOrderTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val deviceSettingsRobot = DeviceSettingsRobot()
    private val demoRobot by lazy { DemoRobot(composeTestRule) }

    @Before
    fun setUp() {
        deviceSettingsRobot.resetAppLinksToDefaults()
        deviceSettingsRobot.disablePasswordManagers()
    }

    @After
    fun tearDown() {
        deviceSettingsRobot.resetAppLinksToDefaults()
    }

    @Test
    fun shouldApproveOrderWithCardWith3DS(
        @TestParameter("AUTHORIZE", "CAPTURE") intent: String,
        @TestParameter("VISA_3DS_SUCCESSFUL_AUTH") testCard: TestCard,
        @TestParameter sca: SCA,
        @TestParameter storeInVaultOption: StoreInVaultOption,
        @TestParameter(
            "CUSTOM_URL_SCHEME",
            "APP_LINKS"
        ) returnToAppStrategy: ReturnToAppStrategyOption,
    ) {
        if (returnToAppStrategy == ReturnToAppStrategyOption.APP_LINKS) {
            deviceSettingsRobot.setupAppLinksForCurrentApp()
        }

        demoRobot.navigateToApproveOrder()
            .createOrder(intent, returnToAppStrategy)
            .setStoreInVaultOption(storeInVaultOption)
            .pickTestCard(testCard.displayName)
            .setSCA(sca)
            .clickApproveOrder()
            .verify3DSChallenge()
            .completeOrder(intent)
    }

    @Test
    fun shouldApproveOrderWithCardWithout3DS(
        @TestParameter("AUTHORIZE", "CAPTURE") intent: String,
        @TestParameter(
            "VISA_VAULT_WITH_PURCHASE_NO_3DS",
            "VISA_NO_3DS",
        ) testCard: TestCard,
        @TestParameter storeInVaultOption: StoreInVaultOption,
        @TestParameter(
            "CUSTOM_URL_SCHEME",
            "APP_LINKS"
        ) returnToAppStrategy: ReturnToAppStrategyOption,
    ) {
        if (returnToAppStrategy == ReturnToAppStrategyOption.APP_LINKS) {
            deviceSettingsRobot.setupAppLinksForCurrentApp()
        }

        demoRobot.navigateToApproveOrder()
            .createOrder(intent, returnToAppStrategy)
            .setStoreInVaultOption(storeInVaultOption)
            .pickTestCard(testCard.displayName)
            .setSCA(SCA.SCA_WHEN_REQUIRED)
            .clickApproveOrder()
            .completeOrder(intent)
    }
}
