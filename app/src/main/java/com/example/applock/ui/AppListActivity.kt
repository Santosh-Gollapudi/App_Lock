package com.example.applock.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applock.R
import android.view.View
class AppListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: AppListAdapter
    private var allApps = listOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_app_list)

        prefs = getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE)
        recyclerView = findViewById(R.id.recyclerViewApps)
        etSearch = findViewById(R.id.etSearch)
        val glassHeader = findViewById<View>(R.id.glassHeader)

        recyclerView.layoutManager = LinearLayoutManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appListRoot)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            glassHeader.setPadding(0, systemBars.top, 0, 16)

            glassHeader.post {
                recyclerView.setPadding(0, glassHeader.height, 0, systemBars.bottom + 24)
            }
            insets
        }

        loadApps()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString())
            }
        })
    }

    private fun loadApps() {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val tempAppList = mutableListOf<AppInfo>()
        val lockedApps = prefs.getStringSet("LOCKED_APPS", setOf()) ?: setOf()

        for (packageInfo in packages) {
            if (pm.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                val name = pm.getApplicationLabel(packageInfo).toString()
                val icon = pm.getApplicationIcon(packageInfo)
                val isLocked = lockedApps.contains(packageInfo.packageName)
                tempAppList.add(AppInfo(name, packageInfo.packageName, icon, isLocked))
            }
        }

        allApps = tempAppList.sortedBy { it.name.lowercase() }

        adapter = AppListAdapter(this, allApps) { packageName, isLocked ->
            val currentLocked = prefs.getStringSet("LOCKED_APPS", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            if (isLocked) {
                currentLocked.add(packageName)
            } else {
                currentLocked.remove(packageName)
            }
            prefs.edit().putStringSet("LOCKED_APPS", currentLocked).apply()
        }
        recyclerView.adapter = adapter
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.updateList(filtered)
    }
}