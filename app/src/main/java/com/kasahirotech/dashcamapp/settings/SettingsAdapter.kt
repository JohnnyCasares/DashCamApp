package com.kasahirotech.dashcamapp.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasahirotech.dashcamapp.databinding.ItemSettingBinding
import com.kasahirotech.dashcamapp.interfaces.SettingItem

class SettingsAdapter(
    private var settings: List<SettingItem>,
    private val onItemClicked: (SettingItem) -> Unit

) : RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    inner class SettingsViewHolder(private val binding: ItemSettingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(setting: SettingItem) {
            binding.tvTitle.text = setting.title
            binding.ivIcon.setImageResource(setting.icon)
            binding.root.setOnClickListener {
                onItemClicked(setting)
            }

        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SettingsViewHolder {
        val binding = ItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingsViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SettingsViewHolder,
        position: Int
    ) {

        holder.bind(settings[position])
    }

    override fun getItemCount(): Int {
        return settings.size
    }

}