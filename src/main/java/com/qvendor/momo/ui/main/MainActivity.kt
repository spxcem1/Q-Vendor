package com.qvendor.momo.ui.main

import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qvendor.momo.QVendorApp
import com.qvendor.momo.R
import com.qvendor.momo.data.ISP
import com.qvendor.momo.data.ProfileAction
import com.qvendor.momo.data.VendorProfile
import com.qvendor.momo.databinding.ActivityMainBinding
import com.qvendor.momo.ui.login.LoginActivity
import com.qvendor.momo.ui.profile.ProfileEditActivity
import com.qvendor.momo.util.UssdDialer
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app by lazy { application as QVendorApp }
    private var profiles = listOf<VendorProfile>()
    private var activeProfile: VendorProfile? = null
    private val CALL_REQUEST = 1001
    private var populatingFields = false
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        @Volatile
        var lastTypedAmount: String? = null
            private set

        fun setLastTypedAmount(value: String?) {
            lastTypedAmount = value
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = getString(R.string.app_name)
        binding.toolbar.subtitle = "Select a profile"

        setupClickListeners()
        observeProfiles()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityStatus()
    }

    private fun setupClickListeners() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_manage_profiles -> {
                    showProfilePicker()
                    true
                }
                R.id.action_history -> {
                    val intent = Intent(this, com.qvendor.momo.ui.history.TransactionHistoryActivity::class.java)
                    activeProfile?.id?.let { intent.putExtra(com.qvendor.momo.ui.history.TransactionHistoryActivity.EXTRA_PROFILE_ID, it) }
                    startActivity(intent)
                    true
                }
                R.id.action_logout -> {
                    confirmLogout()
                    true
                }
                else -> false
            }
        }

        binding.btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }

        binding.etDefaultPhone.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                saveQuickSendValues()
                val digits = s?.toString()?.filter { it.isDigit() } ?: ""
                if (digits.length == 10) {
                    val clip = ClipData.newPlainText("Phone Number", s.toString())
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(clip)
                    Toast.makeText(this@MainActivity, "Phone number copied", Toast.LENGTH_SHORT).show()
                }
            }
        })
        binding.etDefaultAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { saveQuickSendValues() }
        })
        binding.etDefaultAmount.setOnEditorActionListener { _, actionId, event ->
            val isDone = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.action == android.view.KeyEvent.ACTION_DOWN &&
                            event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
            if (isDone) {
                val typed = binding.etDefaultAmount.text?.toString()?.trim().orEmpty()
                if (typed.isNotEmpty()) {
                    setLastTypedAmount(typed)
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val existing = try {
                        cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
                    } catch (_: SecurityException) { "" } catch (_: Exception) { "" }
                    val existingDigits = existing.filter { it.isDigit() }
                    val combined = existingDigits + typed.filter { it.isDigit() }
                    val clip = ClipData.newPlainText("PhoneAndAmount", combined)
                    cm.setPrimaryClip(clip)
                    Toast.makeText(this@MainActivity, "Amount copied", Toast.LENGTH_SHORT).show()
                }
            }
            false
        }
        // Clear clipboard button listener
        binding.btnClearClipboard.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("", ""))
            clearAmountFieldSilently()
            clearPhoneFieldSilently()
            setLastTypedAmount(null)
        }
    }

    private fun observeProfiles() {
        app.database.profileDao().getAllProfiles().observe(this) { list ->
            profiles = list
            renderProfileChips()
            updateActiveProfile()
            renderActionCards()
        }
    }

    private fun renderProfileChips() {
        binding.profileChipGroup.removeAllViews()

        profiles.forEach { profile ->
            val chip = Chip(this).apply {
                text = profile.name
                isCheckable = true
                isChecked = profile.isActive
                chipIconSize = 20f
                setChipIconTintResource(getIspTint(profile.isp))
                setChipBackgroundColorResource(getIspChipBg(profile.isp))
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                id = View.generateViewId()
                tag = profile.id
                setOnClickListener { switchProfile(profile.id) }
            }
            binding.profileChipGroup.addView(chip)
        }
    }

    private fun switchProfile(id: Long) {
        lifecycleScope.launch {
            app.database.profileDao().deactivateAll()
            app.database.profileDao().setActive(id)
        }
    }

    private fun updateActiveProfile() {
        activeProfile = profiles.find { it.isActive }
        val p = activeProfile
        if (p != null) {
            binding.toolbar.subtitle = "${p.isp.displayName} · ${p.name}"
            binding.tilDefaultPhone.visibility = View.VISIBLE
            binding.tilDefaultAmount.visibility = View.VISIBLE
            populatingFields = true
            val currentPhone = binding.etDefaultPhone.text?.toString() ?: ""
            val currentAmount = binding.etDefaultAmount.text?.toString() ?: ""
            if (currentPhone != p.defaultPhone) {
                if (p.defaultPhone.isNotEmpty()) {
                    binding.etDefaultPhone.setText(p.defaultPhone)
                } else {
                    binding.etDefaultPhone.text?.clear()
                }
            }
            if (currentAmount != p.defaultAmount) {
                if (p.defaultAmount.isNotEmpty()) {
                    binding.etDefaultAmount.setText(p.defaultAmount)
                } else {
                    binding.etDefaultAmount.text?.clear()
                }
            }
            populatingFields = false
        } else {
            binding.toolbar.subtitle = "Select a profile"
            binding.tilDefaultPhone.visibility = View.GONE
            binding.tilDefaultAmount.visibility = View.GONE
        }
    }

    private fun renderActionCards() {
        binding.actionCardsContainer.removeAllViews()

        val p = activeProfile
        if (p == null) {
            binding.tvNoProfiles.visibility = View.VISIBLE
            return
        }

        binding.tvNoProfiles.visibility = View.GONE
        val actions = p.profileActions.actions

        if (actions.isEmpty()) return

        actions.forEach { action ->
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.action_button_height)
                ).apply {
                    setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.spacing_md))
                }
                radius = resources.getDimension(R.dimen.action_button_corner)
                cardElevation = resources.getDimension(R.dimen.card_elevation)
                strokeWidth = 0
                try {
                    setCardBackgroundColor(Color.parseColor(action.color))
                } catch (_: Exception) {
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.qvendor_green))
                }
                setOnClickListener { dialAction(action) }
            }

            val label = TextView(this).apply {
                text = action.name
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                textSize = 18f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            }

            card.addView(label)
            binding.actionCardsContainer.addView(card)
        }
    }

    private fun dialAction(action: ProfileAction) {
        val profile = activeProfile
        if (profile == null) {
            Toast.makeText(this, "Select or create a profile first", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), CALL_REQUEST)
            return
        }

        if (action.isDynamic()) {
            val prefill = mutableMapOf<String, String>()
            if (action.amountIndex >= 0 && profile.defaultAmount.isNotEmpty()) {
                prefill["amount"] = profile.defaultAmount
            }
            if (action.phoneIndex >= 0 && profile.defaultPhone.isNotEmpty()) {
                prefill["phone"] = profile.defaultPhone
            }
            if (action.referenceIndex >= 0 && profile.defaultReference.isNotEmpty()) {
                prefill["reference"] = profile.defaultReference
            }

            val needsPin = action.pinIndex >= 0
            val needsAmount = action.amountIndex >= 0 && profile.defaultAmount.isEmpty()
            val needsPhone = action.phoneIndex >= 0 && profile.defaultPhone.isEmpty()
            val needsReference = action.referenceIndex >= 0 && profile.defaultReference.isEmpty()
            val needsDialog = needsPin || needsAmount || needsPhone || needsReference

            if (!needsDialog && prefill.isNotEmpty()) {
                UssdDialer.dial(this, profile, action, prefill)
            } else {
                showDynamicInputDialog(profile, action, prefill)
            }
        } else {
            UssdDialer.dial(this, profile, action)
        }
    }

    private fun showDynamicInputDialog(profile: VendorProfile, action: ProfileAction, prefill: Map<String, String> = emptyMap()) {
        val view = layoutInflater.inflate(R.layout.dialog_input_params, null)

        val tilPin = view.findViewById<TextInputLayout>(R.id.tilInputPin)
        val tilNumber = view.findViewById<TextInputLayout>(R.id.tilInputNumber)
        val tilAmount = view.findViewById<TextInputLayout>(R.id.tilInputAmount)
        val tilReference = view.findViewById<TextInputLayout>(R.id.tilInputReference)

        val etPin = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etInputPin)
        val etNumber = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etInputNumber)
        val etAmount = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etInputAmount)
        val etReference = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etInputReference)

        if (action.pinIndex >= 0) tilPin.visibility = View.VISIBLE
        if (action.phoneIndex >= 0) tilNumber.visibility = View.VISIBLE
        if (action.amountIndex >= 0) tilAmount.visibility = View.VISIBLE
        if (action.referenceIndex >= 0) tilReference.visibility = View.VISIBLE

        if (prefill.containsKey("phone")) {
            etNumber.setText(prefill["phone"])
        } else if (profile.defaultPhone.isNotEmpty()) {
            etNumber.setText(profile.defaultPhone)
        }
        if (prefill.containsKey("amount")) {
            etAmount.setText(prefill["amount"])
        } else if (profile.defaultAmount.isNotEmpty()) {
            etAmount.setText(profile.defaultAmount)
        }
        if (prefill.containsKey("reference")) {
            etReference.setText(prefill["reference"])
        } else if (profile.defaultReference.isNotEmpty()) {
            etReference.setText(profile.defaultReference)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()

        view.findViewById<View>(R.id.btnInputDialogOk).setOnClickListener {
            val inputs = mutableMapOf<String, String>()
            if (action.pinIndex >= 0) inputs["pin"] = etPin.text?.toString()?.trim() ?: ""
            if (action.phoneIndex >= 0) inputs["phone"] = etNumber.text?.toString()?.trim() ?: ""
            if (action.amountIndex >= 0) inputs["amount"] = etAmount.text?.toString()?.trim() ?: ""
            if (action.referenceIndex >= 0) inputs["reference"] = etReference.text?.toString()?.trim() ?: ""

            UssdDialer.dial(this, profile, action, inputs)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnInputDialogCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showProfilePicker() {
        val names = profiles.map { "${it.name} (${it.isp.displayName})" }.toMutableList()
        names.add("+ Add new profile")
        AlertDialog.Builder(this)
            .setTitle(R.string.my_profiles)
            .setItems(names.toTypedArray()) { _, which ->
                if (which == profiles.size) {
                    startActivity(Intent(this, ProfileEditActivity::class.java))
                } else {
                    val profile = profiles[which]
                    showProfileActions(profile)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showProfileActions(profile: VendorProfile) {
        val options = arrayOf("Switch to this profile", "Edit profile", "Delete profile")
        AlertDialog.Builder(this)
            .setTitle(profile.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> switchProfile(profile.id)
                    1 -> startActivity(Intent(this, ProfileEditActivity::class.java).apply {
                        putExtra(ProfileEditActivity.EXTRA_PROFILE_ID, profile.id)
                    })
                    2 -> confirmDeleteProfile(profile)
                }
            }
            .show()
    }

    private fun confirmDeleteProfile(profile: VendorProfile) {
        val view = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()

        view.findViewById<View>(R.id.btnDialogYes).setOnClickListener {
            lifecycleScope.launch {
                app.database.profileDao().delete(profile)
                Toast.makeText(this@MainActivity, "Deleted", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnDialogNo).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.confirm_logout)
            .setPositiveButton(R.string.yes) { _, _ ->
                app.credentials.logout()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun saveQuickSendValues() {
        if (populatingFields) return
        val p = activeProfile ?: return
        val phone = binding.etDefaultPhone.text?.toString()?.trim() ?: ""
        val amount = binding.etDefaultAmount.text?.toString()?.trim() ?: ""
        lifecycleScope.launch {
            val updated = p.copy(defaultPhone = phone, defaultAmount = amount)
            app.database.profileDao().update(updated)
        }
    }

    private fun clearPhoneFieldSilently() {
        populatingFields = true
        binding.etDefaultPhone.text?.clear()
        populatingFields = false
        saveQuickSendValues()
    }

    private fun clearAmountFieldSilently() {
        populatingFields = true
        binding.etDefaultAmount.text?.clear()
        populatingFields = false
        saveQuickSendValues()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE), CALL_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_REQUEST && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Phone permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.grant_call_permission), Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAccessibilityStatus() {
        val enabled = isAccessibilityEnabled()
        if (!enabled) {
            binding.tvAccessibilityHint.visibility = View.VISIBLE
            binding.btnEnableAccessibility.visibility = View.VISIBLE
        } else {
            binding.tvAccessibilityHint.visibility = View.GONE
            binding.btnEnableAccessibility.visibility = View.GONE
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "$packageName/${packageName}.service.UssdAccessibilityService"
        try {
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(service)
        } catch (e: Exception) {
            return false
        }
    }

    private fun getIspTint(isp: ISP): Int = when (isp) {
        ISP.MTN -> R.color.qvendor_mtn
        ISP.TELECEL -> R.color.qvendor_telecel
        ISP.AIRTELTIGO -> R.color.qvendor_airteltigo
        ISP.CUSTOM -> R.color.qvendor_custom
    }

    private fun getIspChipBg(isp: ISP): Int = when (isp) {
        ISP.MTN -> R.color.qvendor_mtn
        ISP.TELECEL -> R.color.qvendor_telecel
        ISP.AIRTELTIGO -> R.color.qvendor_airteltigo
        ISP.CUSTOM -> R.color.qvendor_custom
    }
}
