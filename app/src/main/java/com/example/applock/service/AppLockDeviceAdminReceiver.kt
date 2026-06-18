package com.example.applock.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AppLockDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "App Lock Shield Enabled. Uninstallation blocked.", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // This is the warning message that pops up if they try to deactivate it.
        return "WARNING: Disabling this will remove all protection from your locked apps."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "App Lock Shield Disabled. App is vulnerable.", Toast.LENGTH_SHORT).show()
    }

}