package com.qvendor.momo.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UssdAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "UssdA11y"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var stepJobRunning = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!UssdSessionManager.isActive) return

        val eventPkg = event?.packageName?.toString() ?: return
        if (!isUssdPackage(eventPkg)) return

        val rootNode = rootInActiveWindow ?: return

        if (!stepJobRunning) {
            stepJobRunning = true
            handler.postDelayed({ runCurrentStep(rootNode) }, 100)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun isUssdPackage(pkg: String): Boolean {
        return pkg.contains("phone", ignoreCase = true) ||
                pkg.contains("dialer", ignoreCase = true) ||
                pkg.contains("ussd", ignoreCase = true) ||
                pkg.contains("secphone", ignoreCase = true) ||
                pkg.contains("com.android.server.telecom", ignoreCase = true) ||
                pkg.contains("telecom", ignoreCase = true) ||
                pkg.contains("incallui", ignoreCase = true)
    }

    private fun runCurrentStep(root: AccessibilityNodeInfo) {
        if (!UssdSessionManager.isActive) {
            stepJobRunning = false
            return
        }

        val r = rootInActiveWindow ?: root

        val step = UssdSessionManager.currentStep()
        if (step == null) {
            trySmartFallbackForCurrentPrompt(r)
            stepJobRunning = false
            return
        }

        val input = UssdSessionManager.resolveInput(step)
        val actualInput = if (input.isEmpty() && step.input.isEmpty()) "" else if (input.isEmpty()) step.input else input
        val inputField = findUssdInputField(r)
        val okButton = findUssdButton(r)

        if (inputField == null && okButton == null) {
            handler.postDelayed({
                val retryRoot = rootInActiveWindow ?: return@postDelayed
                runCurrentStep(retryRoot)
            }, 100)
            return
        }

        if (inputField != null) {
            // Skip if the field already contains the desired input to avoid duplicate pastes
            val existingText = inputField.text?.toString() ?: ""
            if (existingText == actualInput) {
                Log.d(TAG, "Step ${UssdSessionManager.stepIndex}: input already present, skipping paste")
            } else {
                inputField.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                inputField.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val previousClip = clipboard.primaryClip
                clipboard.setPrimaryClip(ClipData.newPlainText("ussd", actualInput))
                val pasteSuccess = inputField.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                // Restore previous clipboard content
                if (previousClip != null) {
                    clipboard.setPrimaryClip(previousClip)
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                Log.d(TAG, "Step ${UssdSessionManager.stepIndex}: paste success=$pasteSuccess")

                inputField.refresh()
                val currentText = inputField.text?.toString() ?: ""
                if (currentText.isBlank() && actualInput.isNotEmpty()) {
                    Log.d(TAG, "Step ${UssdSessionManager.stepIndex}: paste left field blank, trying char-by-char")
                    typeCharByChar(inputField, actualInput)
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }

        // Click OK to submit the current input.
        handler.postDelayed({
            val freshRoot = rootInActiveWindow
            val btn = okButton ?: freshRoot?.let { findUssdButton(it) }
            btn?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Step ${UssdSessionManager.stepIndex}: clicked OK")
            // Treat a successful submit on the action's phone step as completion
            // of the phone session — which gates the amount-paste session.
            val action = UssdSessionManager.currentAction
            if (action != null && UssdSessionManager.stepIndex == action.phoneIndex) {
                UssdSessionManager.registerPhonePast()
            }
        }, 100)

        // Advance to the next USSD step if available; otherwise end this session cleanly.
        handler.postDelayed({
            UssdSessionManager.advanceStep()
            val nextStep = UssdSessionManager.currentStep()
            if (nextStep != null && UssdSessionManager.isActive) {
                stepJobRunning = true
                handler.postDelayed({
                    val nextRoot = rootInActiveWindow ?: return@postDelayed
                    runCurrentStep(nextRoot)
                }, nextStep.delayMs.coerceAtLeast(100))
            } else if (UssdSessionManager.isActive) {
                // All assigned steps consumed — keep the session active so the
                // smart-fallback path can respond to any further "Enter phone"
                // prompt that the dialer presents. The fallback only performs
                // an action when the prompt text matches AND a phone is known,
                // otherwise it leaves the dialog for the user.
                rootInActiveWindow?.let { finalizeOutcome(it, success = true) }
                stepJobRunning = false
            } else {
                stepJobRunning = false
            }
        }, 100)
    }

    private fun finalizeOutcome(root: AccessibilityNodeInfo, success: Boolean) {
        if (UssdSessionManager.currentAction == null) {
            UssdSessionManager.endSession()
            return
        }
        val ctx = applicationContext
        if (ctx is com.qvendor.momo.QVendorApp) {
            val pendingId = com.qvendor.momo.data.TransactionRecorder.pendingId
            if (pendingId > 0) {
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    com.qvendor.momo.data.TransactionRecorder.resolve(
                        ctx.database.transactionDao(),
                        pendingId,
                        success
                    )
                }
            }
        }
        UssdSessionManager.endSession()
    }

    private fun typeCharByChar(field: AccessibilityNodeInfo, text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val previousClip = clipboard.primaryClip
        var delay = 0L
        for (ch in text) {
            handler.postDelayed({
                clipboard.setPrimaryClip(ClipData.newPlainText("ussd", ch.toString()))
                field.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            }, delay)
            delay += 100
        }
        handler.postDelayed({
            // Restore original clipboard after typing
            if (previousClip != null) {
                clipboard.setPrimaryClip(previousClip)
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }, delay + 100)
    }

    private fun findUssdInputField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findUssdInputField(child)
            if (found != null) return found
        }
        return null
    }

    private fun findUssdButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val buttonsById = node.findAccessibilityNodeInfosByViewId("android:id/button1")
        if (buttonsById.isNotEmpty()) return buttonsById[0]

        val labels = listOf("ok", "send", "reply", "call", "envoyer", "enviar")
        return findButtonByText(node, labels)
    }

    private fun findButtonByText(node: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        if (node.isClickable && node.isEnabled) {
            val text = node.text?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            for (label in labels) {
                if (text == label || text.contains(label) || contentDesc.contains(label)) return node
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findButtonByText(child, labels)
            if (found != null) return found
        }
        return null
    }

    private fun completeSession(root: AccessibilityNodeInfo) {
        val okButton = findUssdButton(root)
        okButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ?: performGlobalAction(GLOBAL_ACTION_BACK)
        stepJobRunning = false
        UssdSessionManager.endSession()
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val previousClip = clipboard.primaryClip
        if (previousClip != null) {
            clipboard.setPrimaryClip(previousClip)
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    private fun trySmartFallbackForCurrentPrompt(root: AccessibilityNodeInfo) {
        val promptText = collectPromptText(root).lowercase()
        val amountKeywords = listOf(
            "enter amount",
            "amount",
            "amount to send",
            "amount to transfer",
            "amount to pay",
            "montant",
            "monto",
            "montante",
            "valor",
            "value to send"
        )
        val phoneKeywords = listOf(
            "mobile number",
            "mobile no",
            "phone number",
            "phone no",
            "confirm number",
            "recipient",
            "destination",
            "enter number",
            "enter phone",
            "entrez le",
            "numero de",
            "numéro de",
            "telefone",
            "número",
            "numero"
        )

        val isAmountPrompt = amountKeywords.any { promptText.contains(it) }
        val isPhonePrompt = phoneKeywords.any { promptText.contains(it) }

        // Amount paste only fires after the phone session has been completed
        // at least once. This keeps the two sessions separate.
        if (isAmountPrompt && UssdSessionManager.phonePastedCount > 0) {
            trySmartAmountPromptFallback(root, promptText)
            return
        }
        if (isPhonePrompt) {
            trySmartPhonePromptFallback(root, promptText)
            return
        }
        Log.d(TAG, "Smart fallback: prompt='$promptText' — no keyword match, skipping")
    }

    private fun trySmartPhonePromptFallback(root: AccessibilityNodeInfo, promptText: String) {
        val phone = UssdSessionManager.preferredPhoneNumber
            ?: readPhoneFromClipboard()
        if (phone.isNullOrEmpty()) {
            Log.d(TAG, "Smart fallback: phone prompt detected but no phone available")
            return
        }

        val inputField = findUssdInputField(root)
        if (inputField == null) {
            Log.d(TAG, "Smart fallback: phone prompt detected but no input field")
            return
        }

        val existing = inputField.text?.toString().orEmpty()
        if (existing == phone) {
            Log.d(TAG, "Smart fallback: input already contains the phone, skipping paste")
            return
        }

        Log.d(TAG, "Smart fallback: pasting '$phone' into phone prompt ('$promptText')")
        performPasteAndOk(inputField, phone)
        UssdSessionManager.registerPhonePast()
    }

    private fun trySmartAmountPromptFallback(root: AccessibilityNodeInfo, promptText: String) {
        // The clipboard is unreliable for amounts — both the phone and amount
        // listeners copy to it, but only the most recent value survives.
        // The dial stage stashes the chosen amount into UssdSessionManager;
        // trust that as the single source of truth.
        val amount = UssdSessionManager.preferredAmount
        if (amount.isNullOrEmpty()) {
            Log.d(TAG, "Smart fallback: amount prompt detected but no amount available")
            return
        }

        val inputField = findUssdInputField(root)
        if (inputField == null) {
            Log.d(TAG, "Smart fallback: amount prompt detected but no input field")
            return
        }

        val existing = inputField.text?.toString().orEmpty()
        if (existing == amount) {
            Log.d(TAG, "Smart fallback: input already contains the amount, skipping paste")
            return
        }

        Log.d(TAG, "Smart fallback: pasting '$amount' into amount prompt ('$promptText')")
        performPasteAndOk(inputField, amount)
    }

    private fun collectPromptText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        val own = node.text?.toString().orEmpty()
        val cd = node.contentDescription?.toString().orEmpty()
        if (own.isNotEmpty() && node.isEditable.not()) {
            sb.append(own).append(' ')
        }
        if (cd.isNotEmpty()) sb.append(cd).append(' ')
        for (i in 0 until node.childCount) {
            sb.append(collectPromptText(node.getChild(i)))
        }
        return sb.toString()
    }

    private fun readPhoneFromClipboard(): String {
        return try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip ?: return ""
            val item = clip.getItemAt(0)
            val raw = item.coerceToText(this)?.toString().orEmpty()
            raw.filter { it.isDigit() || it == '+' }.takeIf { it.length in 7..15 } ?: ""
        } catch (e: SecurityException) {
            Log.w(TAG, "Smart fallback: clipboard read denied: ${e.message}")
            ""
        } catch (e: Exception) {
            Log.w(TAG, "Smart fallback: clipboard read failed: ${e.message}")
            ""
        }
    }

    private fun readAmountFromClipboard(): String {
        return try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip ?: return ""
            val item = clip.getItemAt(0)
            val raw = item.coerceToText(this)?.toString().orEmpty()
            val cleaned = raw.replace(Regex("[^0-9.]"), "")
            if (cleaned.isEmpty() || cleaned.count { it == '.' } > 1) return ""
            cleaned.takeIf { it.toDoubleOrNull() != null } ?: ""
        } catch (e: SecurityException) {
            Log.w(TAG, "Smart fallback: clipboard read denied: ${e.message}")
            ""
        } catch (e: Exception) {
            Log.w(TAG, "Smart fallback: clipboard read failed: ${e.message}")
            ""
        }
    }

    private fun performPasteAndOk(field: AccessibilityNodeInfo, text: String) {
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        field.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val previousClip = clipboard.primaryClip
        clipboard.setPrimaryClip(ClipData.newPlainText("ussd", text))
        field.performAction(AccessibilityNodeInfo.ACTION_PASTE)

        handler.postDelayed({
            val fresh = rootInActiveWindow
            val btn = fresh?.let { findUssdButton(it) } ?: findUssdButton(field)
            btn?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (previousClip != null) {
                clipboard.setPrimaryClip(previousClip)
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }, 100)
    }
}
