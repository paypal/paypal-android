package com.paypal.android.api.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayPalPaymentSource(
    @SerialName("usage_type")
    var usageType: UsageType = UsageType.Merchant,
    @SerialName("experience_context")
    var experienceContext: ExperienceContext = ExperienceContext()
) : SerializablePaymentSource() {

    @Serializable
    data class ExperienceContext(
        @SerialName("vault_instruction")
        var vaultInstruction: VaultInstruction = VaultInstruction.OnPayerApproval,

        @SerialName("return_url")
        var returnUrl: String = "",

        @SerialName("cancel_url")
        var cancelUrl: String = "",
    )
}
