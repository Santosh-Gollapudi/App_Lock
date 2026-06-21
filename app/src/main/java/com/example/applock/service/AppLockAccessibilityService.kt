package com.example.applock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.applock.R
import com.example.applock.ui.LockScreenActivity

class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val CHANNEL_ID = "app_lock_service_channel"
        private const val NOTIF_ID = 1001

        @Volatile var isLockScreenActive: Boolean = false
        @Volatile var interceptedPackage: String? = null
        @Volatile var unlockedPackage: String? = null
    }

    private lateinit var prefs: SharedPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()

        prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE)

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100L
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val currentPackage = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        if (currentPackage == packageName) return

        if (currentPackage == "android" ||
            currentPackage.contains("systemui", ignoreCase = true) ||
            currentPackage.contains("incallui", ignoreCase = true) ||
            currentPackage.contains("inputmethod", ignoreCase = true) ||
            currentPackage.contains("swiftkey", ignoreCase = true) ||
            currentPackage.contains("biometrics", ignoreCase = true) ||
            currentPackage.contains("clockpackage", ignoreCase = true)) {
            return
        }

        val lockedPackages = prefs.getStringSet("LOCKED_APPS", setOf()) ?: setOf()

        if (lockedPackages.contains(currentPackage)) {
            if (currentPackage != unlockedPackage && !isLockScreenActive) {
                interceptedPackage = currentPackage
                isLockScreenActive = true
                launchLockScreen(currentPackage)
            }
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val launcherPkg = packageManager.resolveActivity(homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            val isRealApp = packageManager.getLaunchIntentForPackage(currentPackage) != null

            if (isRealApp || (currentPackage == launcherPkg && !className.lowercase().contains("layout") && !className.lowercase().contains("view"))) {
                unlockedPackage = null
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isLockScreenActive = false
        interceptedPackage = null
        unlockedPackage = null
    }

    private fun launchLockScreen(lockedPackage: String) {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(LockScreenActivity.EXTRA_LOCKED_PACKAGE, lockedPackage)
        }
        startActivity(intent)
    }
}