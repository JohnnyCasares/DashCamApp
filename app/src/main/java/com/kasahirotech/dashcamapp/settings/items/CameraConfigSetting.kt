package com.kasahirotech.dashcamapp.settings.items

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.screens.CameraConfigActivity
import com.kasahirotech.dashcamapp.service.PreferenceManager

/**
 * Setting item for configuring camera selection and zoom.
 * Provides a clickable setting that opens the camera configuration screen.
 */
class CameraConfigSetting(
    private val context: Context,
    private val onConfigChanged: (() -> Unit)? = null
) : SettingItem {

    override val icon: Int = R.drawable.ic_camera
    override val title: String = context.getString(R.string.camera_config_setting_title)

    /**
     * Returns the display text showing current camera and zoom configuration.
     *
     * @return Formatted string showing current settings
     */
    fun getCurrentConfigText(): String {
        val cameraId = PreferenceManager.getSelectedCameraId(context)
        val zoomRatio = if (cameraId.isNotEmpty()) {
            PreferenceManager.getZoomRatio(context, cameraId)
        } else {
            1.0f
        }
        
        return if (cameraId.isNotEmpty()) {
            context.getString(R.string.camera_config_current, cameraId, zoomRatio)
        } else {
            context.getString(R.string.camera_config_default)
        }
    }

    /**
     * Opens the camera configuration activity when the setting item is clicked.
     */
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?) {
        val intent = Intent(context, CameraConfigActivity::class.java)
        context.startActivity(intent)
        onConfigChanged?.invoke()
    }
}
