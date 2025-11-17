package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.util.Log
import androidx.camera.video.Quality

/**
 * Utility object for managing application preferences using SharedPreferences.
 * Centralizes preference operations for audio recording and other app settings.
 */
object PreferenceManager {
    
    private const val PREFS_NAME = "dashcam_preferences"
    private const val KEY_AUDIO_ENABLED = "audio_recording_enabled"
    private const val DEFAULT_AUDIO_ENABLED = true
    private const val KEY_DUAL_CAMERA_ENABLED = "dual_camera_enabled"
    private const val DEFAULT_DUAL_CAMERA_ENABLED = false
    private const val KEY_VIDEO_QUALITY = "video_quality"
    private const val DEFAULT_VIDEO_QUALITY = "HIGHEST"
    private const val TAG = "PreferenceManager"
    
    /**
     * Checks if audio recording is enabled in user preferences.
     * 
     * @param context Application or Activity context
     * @return true if audio recording is enabled, false otherwise. Defaults to true on first launch.
     */
    fun isAudioRecordingEnabled(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_AUDIO_ENABLED, DEFAULT_AUDIO_ENABLED)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading audio preference", e)
            DEFAULT_AUDIO_ENABLED
        }
    }
    
    /**
     * Sets the audio recording preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable audio recording, false to disable
     */
    fun setAudioRecordingEnabled(context: Context, enabled: Boolean) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AUDIO_ENABLED, enabled).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing audio preference", e)
        }
    }
    
    /**
     * Checks if dual camera recording is enabled in user preferences.
     * 
     * @param context Application or Activity context
     * @return true if dual camera recording is enabled, false otherwise. Defaults to false on first launch.
     */
    fun isDualCameraEnabled(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_DUAL_CAMERA_ENABLED, DEFAULT_DUAL_CAMERA_ENABLED)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading dual camera preference", e)
            DEFAULT_DUAL_CAMERA_ENABLED
        }
    }
    
    /**
     * Sets the dual camera recording preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable dual camera recording, false to disable
     */
    fun setDualCameraEnabled(context: Context, enabled: Boolean) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_DUAL_CAMERA_ENABLED, enabled).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing dual camera preference", e)
        }
    }
    
    /**
     * Gets the video recording quality from user preferences.
     * 
     * @param context Application or Activity context
     * @return Quality enum value. Defaults to HIGHEST on first launch or if stored value is invalid.
     */
    fun getVideoQuality(context: Context): Quality {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val qualityString = prefs.getString(KEY_VIDEO_QUALITY, DEFAULT_VIDEO_QUALITY) ?: DEFAULT_VIDEO_QUALITY
            
            // Convert string to Quality enum
            when (qualityString) {
                "UHD" -> Quality.UHD
                "FHD" -> Quality.FHD
                "HD" -> Quality.HD
                "SD" -> Quality.SD
                "LOWEST" -> Quality.LOWEST
                "HIGHEST" -> Quality.HIGHEST
                else -> {
                    Log.w(TAG, "Invalid quality value: $qualityString, falling back to HIGHEST")
                    Quality.HIGHEST
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading video quality preference", e)
            Quality.HIGHEST
        }
    }
    
    /**
     * Sets the video recording quality preference.
     * 
     * @param context Application or Activity context
     * @param quality Quality enum value to store
     */
    fun setVideoQuality(context: Context, quality: Quality) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val qualityString = when (quality) {
                Quality.UHD -> "UHD"
                Quality.FHD -> "FHD"
                Quality.HD -> "HD"
                Quality.SD -> "SD"
                Quality.LOWEST -> "LOWEST"
                Quality.HIGHEST -> "HIGHEST"
                else -> "HIGHEST"
            }
            prefs.edit().putString(KEY_VIDEO_QUALITY, qualityString).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing video quality preference", e)
        }
    }
}
