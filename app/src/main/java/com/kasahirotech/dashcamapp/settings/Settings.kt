package com.kasahirotech.dashcamapp.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasahirotech.dashcamapp.databinding.ActivitySettingsBinding
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.settings.items.AudioToggleSetting
import com.kasahirotech.dashcamapp.settings.items.DualCameraToggleSetting
import com.kasahirotech.dashcamapp.settings.items.VideoQualitySetting

class Settings : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySettingsBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lateinit var adapter: SettingsAdapter
        
        val audioToggle = AudioToggleSetting(this)
        val dualCameraToggle = DualCameraToggleSetting(this)
        val videoQualitySetting = VideoQualitySetting(this) {
            // Callback when quality changes - refresh the adapter
            adapter.notifyDataSetChanged()
        }

        var settingsList = mutableListOf<SettingItem>(
            audioToggle,
            dualCameraToggle,
            videoQualitySetting
        )

        adapter = SettingsAdapter(settingsList){ clickedSettingItem ->
            clickedSettingItem.onItemClick(this, null)
        }

        binding.rvSettings.adapter = adapter
        binding.rvSettings.layoutManager = LinearLayoutManager(this)

        binding.fabBack.setOnClickListener {
            finish()
        }
    }
}
