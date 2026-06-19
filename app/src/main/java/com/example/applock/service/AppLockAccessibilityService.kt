package com.example.applock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.applock.R
import com.example.applock.ui.LockScreenActivity

class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val CHANNEL_ID   = "app_lock_service_channel"
        private const val NOTIF_ID     = 1001

        @Volatile var isLockScreenActive = false
        @Volatile var interceptedPackage: String? = null

        @Volatile var unlockedPackage: String? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = serviceInfo.apply {
            eventTypes      = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType    = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags           = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100L
        }

        startForegroundNotification()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val currentPackage = event.packageName?.toString() ?: return

        if (currentPackage == packageName) return
        if (currentPackage == "android" || currentPackage == "com.android.systemui") return
        if (isLockScreenActive) return
        if (currentPackage == unlockedPackage) return

        val isRealApp = packageManager.getLaunchIntentForPackage(currentPackage) != null

        if (isRealApp) {
            val prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE)
            val lockedPackages = prefs.getStringSet("LOCKED_APPS", setOf()) ?: setOf()

            if (lockedPackages.contains(currentPackage)) {
                interceptedPackage = currentPackage
                isLockScreenActive = true
                launchLockScreen(currentPackage)
            } else {
                unlockedPackage = null
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isLockScreenActive  = false
        interceptedPackage  = null
        unlockedPackage = null
    }

    private fun launchLockScreen(lockedPackage: String) {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
            )
            putExtra(LockScreenActivity.EXTRA_LOCKED_PACKAGE, lockedPackage)
        }
        startActivity(intent)
    }

    private fun startForegroundNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Lock Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the App Lock service running"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Lock Active")
            .setContentText("Protecting your selected apps")
            .setSmallIcon(R.drawable.ic_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }
}