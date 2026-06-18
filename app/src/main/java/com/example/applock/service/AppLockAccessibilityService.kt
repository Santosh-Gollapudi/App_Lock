package com.example.applock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.applock.R
import com.example.applock.ui.LockScreenActivity

class AppLockAccessibilityService : AccessibilityService() {

    // ─── Companion: Shared state between Service & Activity ──────────────────
    companion object {
        private const val CHANNEL_ID   = "app_lock_service_channel"
        private const val NOTIF_ID     = 1001

        @Volatile var isLockScreenActive = false
        @Volatile var interceptedPackage: String? = null

        // THE AMNESIA FIX: Remembers the app you just unlocked
        @Volatile var unlockedPackage: String? = null
    }

    private val lockedPackages = mutableSetOf(
        "com.instagram.android",
        "com.whatsapp"
    )

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

        // Gate 1: Never intercept our own app to avoid inception crashes
        if (currentPackage == packageName) return

        // Gate 2: Ignore the absolute base Android system to be safe
        if (currentPackage == "android" || currentPackage == "com.android.systemui") return

        // Gate 3: Don't re-trigger if the lock screen is already visible
        if (isLockScreenActive) return

        // Gate 4: THE AMNESIA CHECK - Is this the app we literally just unlocked?
        if (currentPackage == unlockedPackage) return

        // ─── THE HOLY GRAIL FIX ──────────────────────────────────────────────
        // Does this package have an app icon? (System overlays like the Samsung
        // Control Panel, volume sliders, and keyboards do not).
        val isRealApp = packageManager.getLaunchIntentForPackage(currentPackage) != null

        if (isRealApp) {
            // The user opened an actual app.
            if (lockedPackages.contains(currentPackage)) {
                // It's a locked app. Throw the lock screen.
                interceptedPackage = currentPackage
                isLockScreenActive = true
                launchLockScreen(currentPackage)
            } else {
                // It's an unlocked app (like the Home Screen or Calculator).
                // They fully left the locked app, so we MUST erase the memory!
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Lock Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the App Lock service running"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

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