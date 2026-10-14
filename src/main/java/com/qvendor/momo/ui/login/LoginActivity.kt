package com.qvendor.momo.ui.login

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qvendor.momo.QVendorApp
import com.qvendor.momo.R
import com.qvendor.momo.data.CredentialsStore
import com.qvendor.momo.databinding.ActivityLoginBinding
import com.qvendor.momo.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { application as QVendorApp }
    private val credentials by lazy { app.credentials }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyMode()
        setupClicks()
    }

    private fun applyMode() {
        if (credentials.isRegistered) {
            binding.loginTitle.text = getString(R.string.login_welcome_back)
            binding.loginSubtitle.text = getString(R.string.login_subtitle)
            binding.loginSubtitle.visibility = View.VISIBLE
            binding.btnLogin.text = getString(R.string.login_button)
            binding.tvAuxAction.visibility = View.GONE
            binding.tvFooterPrompt.visibility = View.GONE
            binding.tvFooterLink.visibility = View.GONE
        } else {
            binding.loginTitle.text = getString(R.string.login_signup_title)
            binding.loginSubtitle.visibility = View.GONE
            binding.btnLogin.text = getString(R.string.login_create_account)
            binding.tvAuxAction.visibility = View.GONE
            binding.tvFooterPrompt.visibility = View.GONE
            binding.tvFooterLink.visibility = View.GONE
        }
    }

    private fun setupClicks() {
        binding.btnLogin.setOnClickListener { onPrimaryAction() }
    }

    private fun onPrimaryAction() {
        clearErrors()
        val code = binding.etAgentCode.text?.toString().orEmpty()
        val pin = binding.etPin.text?.toString().orEmpty()

        if (!validate(code, pin)) return

        if (!credentials.isRegistered) {
            promptConfirmPin(code, pin)
        } else if (credentials.login(code, pin)) {
            launchMain()
        } else {
            binding.tilAgentCode.error = getString(R.string.login_error_invalid_credentials)
            binding.tilPin.error = getString(R.string.login_error_invalid_credentials)
        }
    }

    private fun validate(code: String, pin: String): Boolean {
        var ok = true
        if (!CredentialsStore.isValidPhone(code)) {
            val msg = if (code.isBlank()) R.string.login_error_phone_required
            else R.string.login_error_phone_invalid
            binding.tilAgentCode.error = getString(msg)
            ok = false
        }
        if (!CredentialsStore.isValidPin(pin)) {
            val msg = when {
                pin.isBlank() -> R.string.login_error_pin_required
                pin.length != 6 -> R.string.login_error_pin_length
                else -> R.string.login_error_pin_digits
            }
            binding.tilPin.error = getString(msg)
            ok = false
        }
        return ok
    }

    private fun promptConfirmPin(code: String, pin: String) {
        val view = layoutInflater.inflate(R.layout.dialog_signup, null)
        val etCode = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSignupAgentCode)
        val etPin = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSignupPin)
        val etConfirm = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSignupPinConfirm)
        val tilPin = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSignupPin)
        val tilConfirm = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSignupPinConfirm)

        etCode.setText(code)
        etCode.isEnabled = false

        val dialog = AlertDialog.Builder(this).setView(view).create()

        view.findViewById<View>(R.id.btnSignupCancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnSignupCreate).setOnClickListener {
            val enteredPin = etPin.text?.toString().orEmpty()
            val confirm = etConfirm.text?.toString().orEmpty()

            tilPin.error = null
            tilConfirm.error = null

            var ok = true
            if (!CredentialsStore.isValidPin(enteredPin)) {
                tilPin.error = getString(R.string.login_error_pin_length)
                ok = false
            }
            if (enteredPin != confirm) {
                tilConfirm.error = getString(R.string.login_error_pin_mismatch)
                ok = false
            }
            if (!ok) return@setOnClickListener

            val created = credentials.register(code, enteredPin)
            if (created) {
                Toast.makeText(this, R.string.login_account_created, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                launchMain()
            } else {
                Toast.makeText(this, R.string.login_error_pin_length, Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun launchMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun clearErrors() {
        binding.tilAgentCode.error = null
        binding.tilPin.error = null
    }
}
