package com.kasahirotech.dashcamapp.settings.items

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.camera.video.Quality
import androidx.fragment.app.FragmentManager
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.service.PreferenceManager

/**
 * Setting item for controlling video recording quality.
 * Provides a clickable setting that opens a dialog for quality selection.
 */
class VideoQualitySetting(
    private val context: Context,
    private val onQualityChanged: (() -> Unit)? = null
) : SettingItem {

    override val icon: Int = R.drawable.ic_video_quality
    override val title: String = "Video Quality"

    var currentQuality: Quality = PreferenceManager.getVideoQuality(context)
        private set

    /**
     * Returns user-friendly display name for a Quality enum value.
     *
     * @param quality The Quality enum value to convert
     * @return Human-readable quality label (e.g., "UHD (4K)", "FHD (1080p)")
     */
    fun getQualityDisplayName(quality: Quality): String {
        return when (quality) {
            Quality.UHD -> "UHD (4K)"
            Quality.FHD -> "FHD (1080p)"
            Quality.HD -> "HD (720p)"
            Quality.SD -> "SD (480p)"
            Quality.HIGHEST -> "Highest Available"
            Quality.LOWEST -> "Lowest Available"
            else -> quality.toString()
        }
    }

    /**
     * Returns the display text for the currently selected quality.
     *
     * @return Formatted string showing current quality
     */
    fun getCurrentQualityText(): String {
        return getQualityDisplayName(currentQuality)
    }

    /**
     * Updates the current quality value from preferences.
     * Should be called after quality is changed to refresh the display.
     */
    fun refreshCurrentQuality() {
        currentQuality = PreferenceManager.getVideoQuality(context)
    }

    /**
     * Opens the quality selection dialog when the setting item is clicked.
     * Displays all quality options in descending order with the current selection pre-selected.
     */
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?) {
        // Define quality options in descending order (highest to lowest)
        val qualityOptions = arrayOf(
            Quality.UHD,
            Quality.FHD,
            Quality.HD,
            Quality.SD,
            Quality.HIGHEST,
            Quality.LOWEST
        )

        // Map quality options to display strings
        val qualityDisplayNames = qualityOptions.map { getQualityDisplayName(it) }.toTypedArray()

        // Find the index of the currently selected quality
        val currentIndex = qualityOptions.indexOf(currentQuality)

        // Track the selected quality in the dialog
        var selectedQuality = currentQuality

        // Create and show the dialog
        AlertDialog.Builder(context)
            .setTitle("Select Video Quality")
            .setSingleChoiceItems(qualityDisplayNames, currentIndex) { _, which ->
                selectedQuality = qualityOptions[which]
            }
            .setPositiveButton("OK") { dialog, _ ->
                // Save the selected quality
                PreferenceManager.setVideoQuality(context, selectedQuality)
                refreshCurrentQuality()
                onQualityChanged?.invoke()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
