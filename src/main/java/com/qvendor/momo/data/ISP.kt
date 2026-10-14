package com.qvendor.momo.data

enum class ISP(
    val displayName: String,
    val brandColor: String,
    val defaultActions: ProfileActions
) {
    MTN("MTN Ghana", "#FFC107",
        ProfileActions(
            actions = listOf(
                ProfileAction(
                    name = "Check Balance",
                    ussdCode = "*170#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("6", 100L, "Check Balance")
                    )),
                    color = "#FF00C853",
                    requiresParams = false,
                    pinIndex = -1,
                    amountIndex = -1,
                    phoneIndex = -1
                ),
                ProfileAction(
                    name = "Cash In",
                    ussdCode = "*170#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("1", 100L, "Cash In"),
                        UssdStep("1", 100L, "From Agent"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF2E7D32",
                    requiresParams = true,
                    pinIndex = 5,
                    amountIndex = 3,
                    phoneIndex = -1,
                    referenceIndex = 4
                ),
                ProfileAction(
                    name = "Cash Out",
                    ussdCode = "*170#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("3", 100L, "Cash Out"),
                        UssdStep("1", 100L, "From Agent"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF1B5E20",
                    requiresParams = true,
                    pinIndex = 5,
                    amountIndex = 3,
                    phoneIndex = -1,
                    referenceIndex = 4
                ),
                ProfileAction(
                    name = "Transfer Airtime",
                    ussdCode = "*170#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("2", 100L, "Transfer Money"),
                        UssdStep("1", 100L, "MoMo User"),
                        UssdStep("", 100L, "Enter phone"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF388E3C",
                    requiresParams = true,
                    pinIndex = 6,
                    amountIndex = 4,
                    phoneIndex = 3,
                    referenceIndex = 5
                ),
                ProfileAction(
                    name = "Send Money",
                    ussdCode = "*170#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("2", 100L, "Transfer Money"),
                        UssdStep("1", 100L, "MoMo User"),
                        UssdStep("", 100L, "Enter phone"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF1565C0",
                    requiresParams = true,
                    pinIndex = 6,
                    amountIndex = 4,
                    phoneIndex = 3,
                    referenceIndex = 5
                )
            )
        )
    ),
    TELECEL("Telecel Cash", "#E60000",
        ProfileActions(
            actions = listOf(
                ProfileAction(
                    name = "Check Balance",
                    ussdCode = "*110#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("6", 100L, "Check Balance")
                    )),
                    color = "#FF00C853",
                    requiresParams = false,
                    pinIndex = -1,
                    amountIndex = -1,
                    phoneIndex = -1
                ),
                ProfileAction(
                    name = "Cash In",
                    ussdCode = "*110#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("1", 100L, "Cash In"),
                        UssdStep("1", 100L, "From Agent"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF2E7D32",
                    requiresParams = true,
                    pinIndex = 5,
                    amountIndex = 3,
                    phoneIndex = -1,
                    referenceIndex = 4
                ),
                ProfileAction(
                    name = "Cash Out",
                    ussdCode = "*110#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("3", 100L, "Cash Out"),
                        UssdStep("1", 100L, "From Agent"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF1B5E20",
                    requiresParams = true,
                    pinIndex = 5,
                    amountIndex = 3,
                    phoneIndex = -1,
                    referenceIndex = 4
                ),
                ProfileAction(
                    name = "Transfer Airtime",
                    ussdCode = "*110#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("2", 100L, "Transfer Money"),
                        UssdStep("1", 100L, "MoMo User"),
                        UssdStep("", 100L, "Enter phone"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF388E3C",
                    requiresParams = true,
                    pinIndex = 6,
                    amountIndex = 4,
                    phoneIndex = 3,
                    referenceIndex = 5
                ),
                ProfileAction(
                    name = "Send Money",
                    ussdCode = "*110#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("2", 100L, "Transfer Money"),
                        UssdStep("1", 100L, "MoMo User"),
                        UssdStep("", 100L, "Enter phone"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF1565C0",
                    requiresParams = true,
                    pinIndex = 6,
                    amountIndex = 4,
                    phoneIndex = 3,
                    referenceIndex = 5
                )
            )
        )
    ),
    AIRTELTIGO("AirtelTigo Money", "#0082CA",
        ProfileActions(
            actions = listOf(
                ProfileAction(
                    name = "Check Balance",
                    ussdCode = "*500#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("6", 100L, "Check Balance")
                    )),
                    color = "#FF00C853",
                    requiresParams = false,
                    pinIndex = -1,
                    amountIndex = -1,
                    phoneIndex = -1
                ),
                ProfileAction(
                    name = "Cash In",
                    ussdCode = "*500#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("1", 100L, "Cash In"),
                        UssdStep("1", 100L, "From Agent"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF2E7D32",
                    requiresParams = true,
                    pinIndex = 5,
                    amountIndex = 3,
                    phoneIndex = -1,
                    referenceIndex = 4
                ),
                ProfileAction(
                    name = "Cash Out",
                    ussdCode = "*500#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("3", 100L, "Cash Out"),
                        UssdStep("1", 100L, "From Agent"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF1B5E20",
                    requiresParams = true,
                    pinIndex = 5,
                    amountIndex = 3,
                    phoneIndex = -1,
                    referenceIndex = 4
                ),
                ProfileAction(
                    name = "Transfer Airtime",
                    ussdCode = "*500#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("2", 100L, "Transfer Money"),
                        UssdStep("1", 100L, "MoMo User"),
                        UssdStep("", 100L, "Enter phone"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF388E3C",
                    requiresParams = true,
                    pinIndex = 6,
                    amountIndex = 4,
                    phoneIndex = 3,
                    referenceIndex = 5
                ),
                ProfileAction(
                    name = "Send Money",
                    ussdCode = "*500#",
                    steps = UssdStepScript(listOf(
                        UssdStep("1", 100L, "Mobile Money"),
                        UssdStep("2", 100L, "Transfer Money"),
                        UssdStep("1", 100L, "MoMo User"),
                        UssdStep("", 100L, "Enter phone"),
                        UssdStep("", 100L, "Enter amount"),
                        UssdStep("", 100L, "Enter reference"),
                        UssdStep("", 100L, "Enter PIN")
                    )),
                    color = "#FF1565C0",
                    requiresParams = true,
                    pinIndex = 6,
                    amountIndex = 4,
                    phoneIndex = 3,
                    referenceIndex = 5
                )
            )
        )
    ),
    CUSTOM("Custom", "#455A64", ProfileActions.EMPTY);

    companion object {
        fun fromName(name: String): ISP = values().find { it.name == name } ?: CUSTOM
    }
}
