package com.kasahirotech.dashcamapp.settings.items

import android.app.AlertDialog
import android.content.Context
import androidx.fragment.app.FragmentManager
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.service.PreferenceManager

/**
 * Setting item for selecting speed unit (mph or km/h).
 * Displays a dialog with radio buttons for unit selection.
 */
class SpeedUnitSetting(
    private val context: Context,
    private val onUnitChange: () -> Unit
) : SettingItem {
    
    override val icon: Int = R.drawable.ic_speed_unit
    override val title: String = "Speed Unit"
    
    /**
     * Checks if this setting is enabled based on whether speed display is enabled.
     */
    val isEnabled: Boolean
        get() = PreferenceManager.isSpeedDisplayEnabled(context)
    
    /**
     * Gets the current speed unit selection.
     * 
     * @return Current unit as display string ("mph" or "km/h")
     */
    fun getCurrentUnit(): String {
        val unit = PreferenceManager.getSpeedUnit(context)
        return if (unit == "kmh") "km/h" else "mph"
    }
    
    /**
     * Shows a dialog for selecting speed unit.
     * Only works if speed display is enabled.
     */
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?) {
        // Only allow interaction if speed display is enabled
        if (!isEnabled) return
        
        val currentUnit = PreferenceManager.getSpeedUnit(context)
        val options = arrayOf("Miles per hour (mph)", "Kilometers per hour (km/h)")
        val selectedIndex = if (currentUnit == "kmh") 1 else 0
        
        AlertDialog.Builder(context)
            .setTitle("Select Speed Unit")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                val newUnit = if (which == 1) "kmh" else "mph"
                PreferenceManager.setSpeedUnit(context, newUnit)
                onUnitChange()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
