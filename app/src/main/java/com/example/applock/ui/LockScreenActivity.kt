package com.example.applock.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.applock.service.AppLockAccessibilityService
import com.example.applock.R

class LockScreenActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "locked_package"
    }

    private var isPromptShowing = false
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyWindowFlags()
        setContentView(R.layout.activity_lock_screen)
        setupBiometricPrompt()
    }

    override fun onResume() {
        super.onResume()
        if (!isPromptShowing) {
            showBiometricPrompt()
        }
    }

    private fun applyWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isPromptShowing = false
                    AppLockAccessibilityService.unlockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)
                    AppLockAccessibilityService.isLockScreenActive = false
                    AppLockAccessibilityService.interceptedPackage = null
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    isPromptShowing = false

                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            navigateToHome()
                        }
                        else -> {
                            if (!isFinishing && !isDestroyed) {
                                navigateToHome() // Safer fallback than infinite loops
                            }
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                }
            }
        )

        val lockedApp = intent.getStringExtra(EXTRA_LOCKED_PACKAGE) ?: "this app"

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Locked")
            .setSubtitle("Authenticate to open $lockedApp")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    private fun showBiometricPrompt() {
        isPromptShowing = true
        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onStop() {
        super.onStop()
        AppLockAccessibilityService.isLockScreenActive = false
        AppLockAccessibilityService.interceptedPackage = null
        isPromptShowing = false

        if (::biometricPrompt.isInitialized) {
            biometricPrompt.cancelAuthentication()
        }
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Using legacy onBackPressed for broad API compatibility")
    override fun onBackPressed() {
    }
}