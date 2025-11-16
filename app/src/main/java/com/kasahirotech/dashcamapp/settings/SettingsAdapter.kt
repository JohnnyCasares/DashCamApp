package com.kasahirotech.dashcamapp.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasahirotech.dashcamapp.databinding.ItemSettingBinding
import com.kasahirotech.dashcamapp.databinding.ItemSettingToggleBinding
import com.kasahirotech.dashcamapp.interfaces.SettingItem

class SettingsAdapter(
    private var settings: List<SettingItem>,
    private val onItemClicked: (SettingItem) -> Unit

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CLICK = 0
        private const val VIEW_TYPE_TOGGLE = 1
    }

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

    inner class ToggleSettingsViewHolder(private val binding: ItemSettingToggleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(setting: SettingItem) {
            binding.tvTitle.text = setting.title
            binding.ivIcon.setImageResource(setting.icon)
            
            if (setting is AudioToggleSetting) {
                binding.switchToggle.isChecked = setting.isEnabled
                binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
                    setting.toggle()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (settings[position]) {
            is AudioToggleSetting -> VIEW_TYPE_TOGGLE
            else -> VIEW_TYPE_CLICK
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TOGGLE -> {
                val binding = ItemSettingToggleBinding.inflate(
                    LayoutInflater.from(parent.context), 
                    parent, 
                    false
                )
                ToggleSettingsViewHolder(binding)
            }
            else -> {
                val binding = ItemSettingBinding.inflate(
                    LayoutInflater.from(parent.context), 
                    parent, 
                    false
                )
                SettingsViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (holder) {
            is ToggleSettingsViewHolder -> holder.bind(settings[position])
            is SettingsViewHolder -> holder.bind(settings[position])
        }
    }

    override fun getItemCount(): Int {
        return settings.size
    }

}