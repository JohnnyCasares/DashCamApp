package com.kasahirotech.dashcamapp.settings.items

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.screens.TripLogViewerActivity
import com.kasahirotech.dashcamapp.service.PreferenceManager

/**
 * Setting item for enabling/disabling trip logging.
 * Provides a toggle switch to enable or disable the trip log feature.
 * Clicking the item opens the trip log viewer to view saved logs.
 */
class TripLogToggleSetting(
    private val context: Context
) : SettingItem {
    
    override val icon: Int = R.drawable.ic_trip_log
    override val title: String = "Trip Log"
    
    var isEnabled: Boolean = PreferenceManager.isTripLogEnabled(context)
        private set
    
    /**
     * Toggles the trip logging preference and persists the change.
     */
    fun toggle() {
        isEnabled = !isEnabled
        PreferenceManager.setTripLogEnabled(context, isEnabled)
    }
    
    /**
     * Opens the trip log viewer activity to view saved logs.
     */
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?) {
        val intent = Intent(context, TripLogViewerActivity::class.java)
        context.startActivity(intent)
    }
}
