package com.kasahirotech.dashcamapp.settings.items

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.kasahirotech.dashcamapp.MainActivity
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.service.PermissionHandler
import com.kasahirotech.dashcamapp.service.PreferenceManager

/**
 * Setting item for enabling/disabling GPS-based speed display.
 * Provides a toggle switch to enable or disable the speed display feature.
 */
class SpeedDisplayToggleSetting(
    private val context: Context,
    private val onToggle: () -> Unit
) : SettingItem {
    
    override val icon: Int = R.drawable.ic_speed
    override val title: String = "Show Speed"
    
    var isEnabled: Boolean = PreferenceManager.isSpeedDisplayEnabled(context)
        private set
    
    /**
     * Toggles the speed display preference and persists the change.
     * Requests location permission if enabling and permission not granted.
     */
    fun toggle() {
        isEnabled = !isEnabled
        PreferenceManager.setSpeedDisplayEnabled(context, isEnabled)
        
        // If enabling, check for location permission
        if (isEnabled) {
            val permissionHandler = PermissionHandler(context)
            if (!permissionHandler.hasLocationPermission()) {
                // Request permission through MainActivity
                if (context is MainActivity) {
                    context.requestLocationPermissionForSpeedDisplay()
                }
            }
        }
        
        onToggle()
    }
    
    /**
     * Not used for toggle settings - toggle is handled by the switch widget.
     */
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?) {
        // Toggle handled by switch widget, not click
    }
}
