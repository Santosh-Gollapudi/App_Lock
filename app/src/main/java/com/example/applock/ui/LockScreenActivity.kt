// LockScreenActivity.kt
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

    // Flag to prevent re-showing the prompt while it is already on screen.
    // This is the key fix for the lifecycle death-loop.
    private var isPromptShowing = false

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CRITICAL: Apply window flags BEFORE setContentView.
        // These allow the Activity to appear over the lock screen and
        // prevent it from being obscured by the keyguard.
        applyWindowFlags()

        // Use a fully transparent layout — the lock screen UI is the
        // BiometricPrompt system sheet itself.
        setContentView(R.layout.activity_lock_screen) // just a transparent FrameLayout

        setupBiometricPrompt()
    }

    override fun onResume() {
        super.onResume()
        // onResume is the correct place to trigger the prompt.
        // onCreate fires only once; onResume fires on every return to foreground
        // (e.g., if the user backgrounded our Activity).
        // The isPromptShowing guard prevents double-showing.
        if (!isPromptShowing) {
            showBiometricPrompt()
        }
    }

    // ─── Window Flags ─────────────────────────────────────────────────────────
    private fun applyWindowFlags() {
        // Modern API (Android O MR1 / API 27+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // Legacy flags (still required for older APIs AND for some OEM ROMs
        // that don't fully respect the newer API calls)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED    or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD    or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON      or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    // ─── Biometric Setup ──────────────────────────────────────────────────────
    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,           // FragmentActivity — this is why we MUST use an Activity
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                // ✅ Auth success — let the user through
                // ✅ Auth success — let the user through
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    isPromptShowing = false

                    // THE AMNESIA FIX: Tell the Service exactly which app we just unlocked
                    // so it doesn't instantly throw the lock screen back up!
                    AppLockAccessibilityService.unlockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)

                    AppLockAccessibilityService.isLockScreenActive = false
                    AppLockAccessibilityService.interceptedPackage = null
                    finish()
                }

                // ❌ Unrecoverable error (timeout, lockout, user pressed Cancel)
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    isPromptShowing = false

                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // User explicitly cancelled — kick them to the home screen
                            // so they don't land inside the locked app
                            navigateToHome()
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            // Too many failures — also go home; app stays locked
                            navigateToHome()
                        }
                        else -> {
                            // Transient errors (e.g., ERROR_TIMEOUT) — re-show the prompt
                            showBiometricPrompt()
                        }
                    }
                }

                // ⚠️ Recoverable failure (finger not recognized) —
                // BiometricPrompt handles its own "try again" UI automatically.
                // Do NOT call showBiometricPrompt() here; it would double-show.
                override fun onAuthenticationFailed() {
                    // Intentionally empty — let the system handle retry UI
                }
            }
        )

        val lockedApp = intent.getStringExtra(EXTRA_LOCKED_PACKAGE) ?: "this app"

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Locked")
            .setSubtitle("Authenticate to open $lockedApp")
            // setAllowedAuthenticators replaces the deprecated setDeviceCredentialAllowed.
            // BIOMETRIC_STRONG | DEVICE_CREDENTIAL = fingerprint, face, or PIN/pattern/password.
            // Do NOT combine this with setNegativeButtonText — it will throw an exception.
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

    // ─── Navigation ───────────────────────────────────────────────────────────
    private fun navigateToHome() {
        // Tell the service the lock screen is gone
        AppLockAccessibilityService.isLockScreenActive = false
        AppLockAccessibilityService.interceptedPackage = null

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }

    // ─── Block back-stack escape ──────────────────────────────────────────────
    @SuppressLint("MissingSuperCall")
    @Deprecated("Using legacy onBackPressed for broad API compatibility")
    override fun onBackPressed() {
        // Swallow the back press entirely.
        // The only exits are: auth success → finish(), or cancel → home screen.
        // Do NOT call super.onBackPressed() — that would pop the Activity and
        // reveal the locked app underneath.
    }

    // ─── CRITICAL: Do NOT finish() in onPause ────────────────────────────────
    // BiometricPrompt renders in a separate system window (SurfaceView overlay).
    // When it appears, it steals window focus, which triggers YOUR Activity's
    // onPause(). If you finish() here, the Activity dies, the service detects
    // the original app window again, relaunches LockScreen → infinite crash loop.
    // Leave onPause() completely unimplemented (or call super only).
    override fun onPause() {
        super.onPause()
        // Intentionally empty — do not finish() here
    }
}