package com.kasahirotech.dashcamapp.settings

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager

import com.kasahirotech.dashcamapp.databinding.ActivitySettingsBinding
import com.kasahirotech.dashcamapp.interfaces.SettingItem

class Settings : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySettingsBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val settingOne = SettingOneTest()
        val settingTwo = SettingTwoTest()

        var settingsList = mutableListOf<SettingItem>(
            settingOne, settingTwo
        )

        val adapter = SettingsAdapter(settingsList){
            clickedSettingItem ->  clickedSettingItem.onItemClick(this, null)
        }

        binding.rvSettings.adapter = adapter
        binding.rvSettings.layoutManager = LinearLayoutManager(this)





    }
}



