package com.kasahirotech.dashcamapp.settings.items

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.service.DualCameraManager
import com.kasahirotech.dashcamapp.service.PreferenceManager

/**
 * Setting item for controlling dual camera recording.
 * Provides a toggle switch to enable or disable dual camera recording.
 * Automatically disables if device does not support concurrent cameras.
 */
class DualCameraToggleSetting(private val context: Context) : SettingItem {
    
    override val icon: Int = R.drawable.ic_dual_camera
    override val title: String = "Dual Camera Recording"
    
    val isDeviceCapable: Boolean = DualCameraManager.isDeviceCapable(context)
    
    var isEnabled: Boolean = PreferenceManager.isDualCameraEnabled(context)
        private set
    
    /**
     * Toggles the dual camera recording preference and persists the change.
     * Only works if device is capable of dual camera recording.
     */
    fun toggle() {
        if (isDeviceCapable) {
            isEnabled = !isEnabled
            PreferenceManager.setDualCameraEnabled(context, isEnabled)
        }
    }
    
    /**
     * Not used for toggle settings - toggle is handled by the switch widget.
     */
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?) {
        // Toggle handled by switch widget, not click
    }
}
