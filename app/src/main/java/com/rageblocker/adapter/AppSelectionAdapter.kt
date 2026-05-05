package com.rageblocker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rageblocker.data.model.AppInfo
import com.rageblocker.databinding.ItemAppBinding

class AppSelectionAdapter(
    private val onAppSelected: (AppInfo, Boolean) -> Unit
) : ListAdapter<AppInfo, AppSelectionAdapter.AppViewHolder>(AppDiffCallback()) {

    private val selectedPackages = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appInfo: AppInfo) {
            binding.tvAppName.text = appInfo.appName
            binding.tvPackageName.text = appInfo.packageName
            binding.ivAppIcon.setImageDrawable(appInfo.icon)
            
            // Remove listener before setting checked to avoid recycling triggers
            binding.cbSelect.setOnCheckedChangeListener(null)
            binding.cbSelect.isChecked = selectedPackages.contains(appInfo.packageName)
            
            binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedPackages.add(appInfo.packageName)
                } else {
                    selectedPackages.remove(appInfo.packageName)
                }
                onAppSelected(appInfo, isChecked)
            }
            
            binding.root.setOnClickListener {
                binding.cbSelect.isChecked = !binding.cbSelect.isChecked
            }
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName && oldItem.appName == newItem.appName
        }
    }

    fun getSelectedPackages(): Set<String> = selectedPackages.toSet()
    
    fun selectPackages(packages: Set<String>) {
        selectedPackages.clear()
        selectedPackages.addAll(packages)
        notifyDataSetChanged()
    }
}
