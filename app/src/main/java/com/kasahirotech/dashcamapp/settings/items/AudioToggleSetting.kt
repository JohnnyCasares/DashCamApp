package com.kasahirotech.dashcamapp.settings.items

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.service.PreferenceManager

/**
 * Setting item for controlling audio recording in video captures.
 * Provides a toggle switch to enable or disable audio recording.
 */
class AudioToggleSetting(private val context: Context) : SettingItem {
    
    override val icon: Int = R.drawable.ic_mic
    override val title: String = "Record Audio"
    
    var isEnabled: Boolean = PreferenceManager.isAudioRecordingEnabled(context)
        private set
    
    /**
     * Toggles the audio recording preference and persists the change.
     */
    fun toggle() {
        isEnabled = !isEnabled
        PreferenceManager.setAudioRecordingEnabled(context, isEnabled)
    }
    
    /**
     * Not used for toggle settings - toggle is handled by the switch widget.
     */
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?) {
        // Toggle handled by switch widget, not click
    }
}
