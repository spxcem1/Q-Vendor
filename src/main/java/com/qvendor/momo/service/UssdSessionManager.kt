package com.qvendor.momo.service

import com.qvendor.momo.data.ProfileAction
import com.qvendor.momo.data.UssdStepScript

object UssdSessionManager {

    @Volatile
    var isActive: Boolean = false
        private set

    @Volatile
    var currentActionName: String? = null
        private set

    @Volatile
    var hasParams: Boolean = false
        private set

    @Volatile
    var stepIndex: Int = 0
        private set

    @Volatile
    var stepScript: UssdStepScript = UssdStepScript()
        private set

    @Volatile
    var currentAction: ProfileAction? = null
        private set

    @Volatile
    var dynamicInputs: Map<String, String> = emptyMap()
        private set

    @Volatile
    var combinedDigits: String = ""
        private set

    @Volatile
    var phonePastedCount: Int = 0
        private set

    val preferredPhoneNumber: String?
        get() {
            val digits = combinedDigits.filter { it.isDigit() }
            return if (digits.length >= 10) digits.substring(0, 10) else null
        }

    val preferredAmount: String?
        get() {
            val digits = combinedDigits.filter { it.isDigit() }
            return if (digits.length > 10) digits.substring(10) else null
        }

    fun submit(action: ProfileAction, inputs: Map<String, String> = emptyMap(), combinedClipboard: String? = null) {
        this.currentAction = action
        this.currentActionName = action.name
        this.hasParams = action.isDynamic()
        this.stepIndex = 0
        this.stepScript = action.steps
        this.dynamicInputs = inputs
        val explicitDigits = buildString {
            val explicitPhone = inputs["phone"]?.filter { it.isDigit() }?.takeIf { it.length in 7..15 }
            val explicitAmount = inputs["amount"]?.let { normaliseAmount(it) }
            if (explicitPhone != null) append(explicitPhone)
            if (explicitAmount != null) append(explicitAmount.filter { it.isDigit() })
        }
        val clippedDigits = combinedClipboard?.filter { it.isDigit() }.orEmpty()
        this.combinedDigits = when {
            explicitDigits.isNotEmpty() -> explicitDigits
            else -> clippedDigits
        }
        this.phonePastedCount = 0
        this.isActive = true
    }

    fun registerPhonePast() {
        phonePastedCount++
    }

    fun normaliseAmount(raw: String): String? {
        val cleaned = raw.replace(Regex("[^0-9.]"), "")
        val dotCount = cleaned.count { it == '.' }
        val core = if (dotCount > 1) cleaned.replace(Regex("\\.(?=.*\\.)"), "")
                   else cleaned
        return core.takeIf { it.isNotEmpty() && core.toDoubleOrNull() != null }
    }

    fun advanceStep(): Int {
        stepIndex++
        return stepIndex
    }

    fun currentStep(): com.qvendor.momo.data.UssdStep? {
        val steps = stepScript.steps
        return if (stepIndex < steps.size) steps[stepIndex] else null
    }

    fun isLastStep(): Boolean {
        return stepIndex >= stepScript.steps.size - 1
    }

    fun getDelay(): Long {
        return if (stepIndex < stepScript.steps.size) {
            stepScript.steps[stepIndex].delayMs.coerceAtLeast(100)
        } else 100L
    }

    fun resolveInput(step: com.qvendor.momo.data.UssdStep): String {
        if (step.input.isNotEmpty()) return step.input

        val action = currentAction ?: return ""
        return when (stepIndex) {
            action.pinIndex -> dynamicInputs["pin"]?.replace(Regex("\\D"), "") ?: ""
            action.amountIndex -> dynamicInputs["amount"]?.replace(Regex("[^0-9.]"), "") ?: ""
            action.phoneIndex -> dynamicInputs["phone"]?.replace(Regex("\\D"), "") ?: ""
            action.referenceIndex -> dynamicInputs["reference"]?.replace(Regex("[^A-Za-z0-9]"), "") ?: ""
            else -> ""
        }
    }

    fun endSession() {
        isActive = false
        currentActionName = null
        currentAction = null
        stepIndex = 0
        stepScript = UssdStepScript()
        dynamicInputs = emptyMap()
        combinedDigits = ""
        phonePastedCount = 0
    }
}
