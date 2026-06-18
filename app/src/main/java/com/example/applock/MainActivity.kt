package com.example.applock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.applock.service.AppLockAccessibilityService
import com.example.applock.service.AppLockDeviceAdminReceiver
import com.example.applock.ui.AppListActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity() {

    private lateinit var btnAccessibility: MaterialCardView
    private lateinit var btnDeviceAdmin: MaterialCardView
    private lateinit var chipAccessibilityStatus: Chip
    private lateinit var chipDeviceAdminStatus: Chip
    private lateinit var btnActivate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnDeviceAdmin = findViewById(R.id.btnDeviceAdmin)
        chipAccessibilityStatus = findViewById(R.id.chipAccessibilityStatus)
        chipDeviceAdminStatus = findViewById(R.id.chipDeviceAdminStatus)
        btnActivate = findViewById(R.id.btnActivate)

        btnAccessibility.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            if (!isAccessibilityEnabled()) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, "Find App Lock in 'Downloaded Apps' and turn it on.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Interceptor already active!", Toast.LENGTH_SHORT).show()
            }
        }

        btnDeviceAdmin.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            if (!isDeviceAdminActive()) {
                val compName = ComponentName(this, AppLockDeviceAdminReceiver::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Activate this to prevent unauthorized uninstallation of your App Lock.")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Shield already active!", Toast.LENGTH_SHORT).show()
            }
        }

        btnActivate.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            val intent = Intent(this, AppListActivity::class.java)
            startActivity(intent)

            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        val accEnabled = isAccessibilityEnabled()
        val adminEnabled = isDeviceAdminActive()

        if (accEnabled) {
            chipAccessibilityStatus.text = "Granted ✓"
            chipAccessibilityStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
        } else {
            chipAccessibilityStatus.text = "Required"
        }

        if (adminEnabled) {
            chipDeviceAdminStatus.text = "Granted ✓"
            chipDeviceAdminStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
        } else {
            chipDeviceAdminStatus.text = "Required"
        }

        btnActivate.isEnabled = accEnabled && adminEnabled
    }


    private fun isAccessibilityEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, AppLockAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false

        return enabledServicesSetting.contains(expectedComponentName.flattenToString())
    }

    private fun isDeviceAdminActive(): Boolean {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val compName = ComponentName(this, AppLockDeviceAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(compName)
    }
}