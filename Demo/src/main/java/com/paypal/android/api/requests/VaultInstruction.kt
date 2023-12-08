package com.paypal.android.api.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class VaultInstruction {
    @SerialName("ON_PAYER_APPROVAL")
    OnPayerApproval
}
