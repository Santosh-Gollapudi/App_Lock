package com.example.applock.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.applock.R
import com.google.android.material.materialswitch.MaterialSwitch

class AppListAdapter(
    private val context: Context,
    private var appList: List<AppInfo>,
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgAppIcon)
        val name: TextView = view.findViewById(R.id.txtAppName)
        val toggle: MaterialSwitch = view.findViewById(R.id.switchAppLock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = appList[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.isChecked = app.isLocked
        holder.toggle.setOnCheckedChangeListener { _, isChecked ->
            app.isLocked = isChecked
            onToggle(app.packageName, isChecked)
        }
    }

    override fun getItemCount() = appList.size

    fun updateList(newList: List<AppInfo>) {
        appList = newList
        notifyDataSetChanged()
    }
}