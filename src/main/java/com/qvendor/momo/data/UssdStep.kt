package com.qvendor.momo.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class UssdStep(
    val input: String = "",
    val delayMs: Long = 100L,
    val description: String = ""
) {
    val display: String
        get() = if (input.isNotEmpty()) "Enter: $input" else description.ifBlank { "Dynamic input" }
}

data class UssdStepScript(
    val steps: List<UssdStep> = emptyList()
)

data class ProfileAction(
    val name: String,
    val ussdCode: String,
    val steps: UssdStepScript = UssdStepScript(),
    val color: String = "#FF00C853",
    val requiresParams: Boolean = false,
    val pinIndex: Int = -1,
    val amountIndex: Int = -1,
    val phoneIndex: Int = -1,
    val referenceIndex: Int = -1
) {
    fun isDynamic(): Boolean = pinIndex >= 0 || amountIndex >= 0 || phoneIndex >= 0 || referenceIndex >= 0
}

data class ProfileActions(
    val actions: List<ProfileAction> = emptyList()
) {
    fun findByName(name: String): ProfileAction? =
        actions.find { it.name == name }

    fun withAction(action: ProfileAction): ProfileActions =
        copy(actions = actions + action)

    fun removeAction(name: String): ProfileActions =
        copy(actions = actions.filter { it.name != name })

    fun replaceAction(name: String, action: ProfileAction): ProfileActions =
        copy(actions = actions.map { if (it.name == name) action else it })

    companion object {
        val EMPTY = ProfileActions()

        private val gson = Gson()
        private val type = object : TypeToken<ProfileActions>() {}.type

        fun fromJson(json: String?): ProfileActions {
            if (json.isNullOrBlank()) return EMPTY
            return try {
                gson.fromJson<ProfileActions>(json, type) ?: EMPTY
            } catch (_: Exception) {
                EMPTY
            }
        }

        fun toJson(actions: ProfileActions): String = gson.toJson(actions)
    }
}
