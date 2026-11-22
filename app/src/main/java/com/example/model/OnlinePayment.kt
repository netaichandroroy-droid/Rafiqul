package com.example.model

enum class PaymentChannel(val title: String, val bnTitle: String) {
    BKASH("bKash", "বিকাশ"),
    NAGAD("Nagad", "নগদ"),
    ROCKET("Rocket", "রকেট"),
    BANK("Bank", "ব্যাংক")
}

data class OnlinePaymentAccount(
    val id: String,
    val channel: PaymentChannel,
    val accountNumber: String = "",
    val accountType: String = "Personal",
    val isEnabled: Boolean = false
)

data class OnlinePaymentConfig(
    val isOnlinePaymentEnabled: Boolean = true,
    val accounts: List<OnlinePaymentAccount> = defaultAccounts()
) {
    companion object {
        fun defaultAccounts(): List<OnlinePaymentAccount> = listOf(
            OnlinePaymentAccount(id = "bkash", channel = PaymentChannel.BKASH, accountType = "Personal", isEnabled = false),
            OnlinePaymentAccount(id = "nagad", channel = PaymentChannel.NAGAD, accountType = "Personal", isEnabled = false),
            OnlinePaymentAccount(id = "rocket", channel = PaymentChannel.ROCKET, accountType = "Personal", isEnabled = false),
            OnlinePaymentAccount(id = "bank", channel = PaymentChannel.BANK, accountType = "Bank Account", isEnabled = false)
        )
    }
}
