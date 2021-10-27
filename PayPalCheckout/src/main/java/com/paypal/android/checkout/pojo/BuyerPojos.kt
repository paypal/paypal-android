package com.paypal.android.checkout.pojo

data class Buyer(
    val userId: String?,
    val email: Email?,
    val name: Name?,
    val addresses: List<Address>?,
    val phones: List<Phone>?
) {
    internal constructor(buyer: com.paypal.pyplcheckout.pojo.Buyer?) : this(
        userId = buyer?.userId,
        email = Email(buyer?.email),
        name = Name(buyer?.name),
        addresses = buyer?.addresses?.map
        { address -> Address(address) }?.toList(),
        phones = buyer?.phones?.map
        { phone -> Phone(phone) }?.toList()
    )
}

data class Address(
    val isPrimary: Boolean?,
    val fullAddress: String?,
    val addressId: String?
) {
    internal constructor(address: com.paypal.pyplcheckout.pojo.Address) : this(
        isPrimary = address.isPrimary,
        fullAddress = address.fullAddress,
        addressId = address.addressId
    )
}

data class Phone(
    val number: String?
) {
    internal constructor(phone: com.paypal.pyplcheckout.pojo.Phone) : this(number = phone.number)
}

data class Email(
    val stringValue: String?,
    val additionalProperties: MutableMap<String, Any>? = HashMap()
) {
    internal constructor(email: com.paypal.pyplcheckout.pojo.Email?) : this(
        stringValue = email?.stringValue,
        additionalProperties = email?.additionalProperties
    )
}

data class Name(
    var fullName: String?,
    var givenName: String?,
    var familyName: String?,
    val additionalProperties: MutableMap<String, Any>? = HashMap()
) {
    internal constructor(name: com.paypal.pyplcheckout.pojo.Name?) : this(
        fullName = name?.fullName,
        givenName = name?.givenName,
        familyName = name?.familyName,
        additionalProperties = name?.additionalProperties
    )
}
