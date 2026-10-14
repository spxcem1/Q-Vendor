package com.qvendor.momo.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.qvendor.momo.QVendorApp
import com.qvendor.momo.R
import com.qvendor.momo.ui.login.LoginActivity
import com.qvendor.momo.ui.main.MainActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val splashDelay = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val app = application as QVendorApp
        val next = if (app.credentials.isLoggedIn()) MainActivity::class.java else LoginActivity::class.java

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, next).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, splashDelay)
    }
}
