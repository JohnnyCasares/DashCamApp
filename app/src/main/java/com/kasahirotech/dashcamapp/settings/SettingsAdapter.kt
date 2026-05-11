package com.kasahirotech.dashcamapp.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasahirotech.dashcamapp.databinding.ItemSettingBinding
import com.kasahirotech.dashcamapp.databinding.ItemSettingToggleBinding
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.settings.items.AudioToggleSetting
import com.kasahirotech.dashcamapp.settings.items.DualCameraToggleSetting
import com.kasahirotech.dashcamapp.settings.items.SpeedDisplayToggleSetting
import com.kasahirotech.dashcamapp.settings.items.SpeedUnitSetting
import com.kasahirotech.dashcamapp.settings.items.TripLogToggleSetting
import com.kasahirotech.dashcamapp.settings.items.VideoQualitySetting

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
            
            // Handle subtitle for settings with subtitles
            when (setting) {
                is VideoQualitySetting -> {
                    binding.tvSubtitle.text = setting.getCurrentQualityText()
                    binding.tvSubtitle.visibility = android.view.View.VISIBLE
                    binding.root.alpha = 1.0f
                    binding.root.isEnabled = true
                }
                is SpeedUnitSetting -> {
                    binding.tvSubtitle.text = setting.getCurrentUnit()
                    binding.tvSubtitle.visibility = android.view.View.VISIBLE
                    
                    // Gray out if speed display is disabled
                    if (setting.isEnabled) {
                        binding.root.alpha = 1.0f
                        binding.root.isEnabled = true
                    } else {
                        binding.root.alpha = 0.5f
                        binding.root.isEnabled = false
                    }
                }
                else -> {
                    binding.tvSubtitle.visibility = android.view.View.GONE
                    binding.root.alpha = 1.0f
                    binding.root.isEnabled = true
                }
            }
            
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
            
            when (setting) {
                is AudioToggleSetting -> {
                    binding.switchToggle.isChecked = setting.isEnabled
                    binding.switchToggle.isEnabled = true
                    binding.root.alpha = 1.0f
                    binding.switchToggle.setOnCheckedChangeListener { _, _ ->
                        setting.toggle()
                    }
                }
                is DualCameraToggleSetting -> {
                    binding.switchToggle.isChecked = setting.isEnabled
                    binding.switchToggle.isEnabled = setting.isDeviceCapable
                    
                    if (!setting.isDeviceCapable) {
                        binding.root.alpha = 0.5f
                        // Note: Subtitle text would require layout modification
                        // For now, the disabled state with reduced alpha indicates unavailability
                    } else {
                        binding.root.alpha = 1.0f
                    }
                    
                    binding.switchToggle.setOnCheckedChangeListener { _, _ ->
                        if (setting.isDeviceCapable) {
                            setting.toggle()
                        }
                    }
                }
                is SpeedDisplayToggleSetting -> {
                    binding.switchToggle.isChecked = setting.isEnabled
                    binding.switchToggle.isEnabled = true
                    binding.root.alpha = 1.0f
                    binding.switchToggle.setOnCheckedChangeListener { _, _ ->
                        setting.toggle()
                    }
                }
                is TripLogToggleSetting -> {
                    binding.switchToggle.isChecked = setting.isEnabled
                    binding.switchToggle.isEnabled = true
                    binding.root.alpha = 1.0f
                    binding.switchToggle.setOnCheckedChangeListener { _, _ ->
                        setting.toggle()
                    }
                    // Also handle click to view logs
                    binding.root.setOnClickListener {
                        onItemClicked(setting)
                    }
                }
//                is GoogleDriveSetting -> {
//                    binding.switchToggle.isChecked = setting.isEnabled
//                    binding.switchToggle.isEnabled = false
//                    binding.root.alpha = 0.5f
//                    binding.switchToggle.setOnCheckedChangeListener(null)
//                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (settings[position]) {
            is AudioToggleSetting,
            is DualCameraToggleSetting,
            is SpeedDisplayToggleSetting,
            is TripLogToggleSetting -> VIEW_TYPE_TOGGLE
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
