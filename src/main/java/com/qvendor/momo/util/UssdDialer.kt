package com.qvendor.momo.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.qvendor.momo.QVendorApp
import com.qvendor.momo.data.ProfileAction
import com.qvendor.momo.data.TransactionRecorder
import com.qvendor.momo.data.VendorProfile
import com.qvendor.momo.service.UssdSessionManager
import kotlinx.coroutines.launch

object UssdDialer {

    fun dial(
        context: Context,
        profile: VendorProfile,
        action: ProfileAction,
        inputs: Map<String, String> = emptyMap(),
        recordTransaction: Boolean = true
    ) {
        val code = action.ussdCode

        if (code.isBlank()) {
            Toast.makeText(context, "No USSD code configured for this action", Toast.LENGTH_SHORT).show()
            return
        }

        val combinedClipboard = readCombinedClipboard(context)
        UssdSessionManager.submit(action, inputs, combinedClipboard)

        val encoded = Uri.encode(code)
        val uri = Uri.parse("tel:$encoded")

        if (recordTransaction && context is androidx.lifecycle.LifecycleOwner) {
            try {
                val app = context.applicationContext as QVendorApp
                val phone = inputs["phone"].orEmpty()
                    .ifBlank { profile.defaultPhone }
                val amountRaw = inputs["amount"].orEmpty()
                    .ifBlank { profile.defaultAmount }
                val amount = amountRaw.toDoubleOrNull() ?: 0.0
                context.lifecycleScope.launch {
                    TransactionRecorder.record(app.database.transactionDao(), profile, action, phone, amount)
                }
            } catch (_: Exception) {
            }
        }

        try {
            val intent = Intent(Intent.ACTION_CALL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: SecurityException) {
            val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
        }
    }

    private fun readCombinedClipboard(context: Context): String? {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = cm.primaryClip ?: return null
            val item = clip.getItemAt(0)
            item.coerceToText(context)?.toString().orEmpty()
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
