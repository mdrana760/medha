package com.medha.app.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.medha.app.R
import com.medha.app.data.SupportedApp

/**
 * Lists supported messaging apps with a toggle each. Greys out apps that aren't
 * installed. The toggle state is owned by the caller via [onToggle].
 */
class AppAdapter(
    private val apps: List<SupportedApp>,
    private val isEnabled: (String) -> Boolean,
    private val isInstalled: (String) -> Boolean,
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<android.widget.TextView>(R.id.tvAppName)
        val sub = view.findViewById<android.widget.TextView>(R.id.tvAppSub)
        val toggle = view.findViewById<MaterialSwitch>(R.id.swApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        val installed = isInstalled(app.packageName)
        holder.name.text = app.label
        holder.sub.text = if (installed) app.packageName else "ইনস্টল করা নেই"
        holder.itemView.alpha = if (installed) 1f else 0.5f

        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.isChecked = isEnabled(app.packageName)
        holder.toggle.isEnabled = installed
        holder.toggle.setOnCheckedChangeListener { _, checked ->
            onToggle(app.packageName, checked)
        }
    }

    companion object {
        fun isInstalled(pm: PackageManager, pkg: String): Boolean = try {
            pm.getApplicationInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
