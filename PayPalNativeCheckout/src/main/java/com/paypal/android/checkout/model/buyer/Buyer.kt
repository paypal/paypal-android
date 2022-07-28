package com.paypal.android.checkout.model.buyer

data class Buyer(
    val userId: String?,
    val email: Email?,
    val name: Name?,
    val addresses: List<BillingAddress>?,
    val phones: List<Phone>?
) {
    internal constructor(buyer: com.paypal.pyplcheckout.pojo.Buyer?) : this(
        userId = buyer?.userId,
        email = Email(buyer?.email),
        name = Name(buyer?.name),
        addresses = buyer?.addresses?.map
        { address -> BillingAddress(address) }?.toList(),
        phones = buyer?.phones?.map
        { phone -> Phone(phone) }?.toList()
    )
}