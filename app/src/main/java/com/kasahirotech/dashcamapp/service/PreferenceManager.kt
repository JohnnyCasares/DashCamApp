package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.util.Log
import androidx.camera.video.Quality
import com.kasahirotech.dashcamapp.interfaces.PreferenceService

/**
 * Utility object for managing application preferences using SharedPreferences.
 * Centralizes preference operations for audio recording and other app settings.
 */
object PreferenceManager : PreferenceService {
    
    private const val PREFS_NAME = "dashcam_preferences"
    private const val KEY_AUDIO_ENABLED = "audio_recording_enabled"
    private const val DEFAULT_AUDIO_ENABLED = true
    private const val KEY_DUAL_CAMERA_ENABLED = "dual_camera_enabled"
    private const val DEFAULT_DUAL_CAMERA_ENABLED = false
    private const val KEY_VIDEO_QUALITY = "video_quality"
    private const val DEFAULT_VIDEO_QUALITY = "HIGHEST"
    private const val KEY_SPEED_DISPLAY_ENABLED = "speed_display_enabled"
    private const val DEFAULT_SPEED_DISPLAY_ENABLED = false
    private const val KEY_SPEED_UNIT = "speed_unit"
    private const val DEFAULT_SPEED_UNIT = "mph"
    private const val KEY_TRIP_LOG_ENABLED = "trip_log_enabled"
    private const val DEFAULT_TRIP_LOG_ENABLED = false
    

    // Camera Selection and Zoom Settings
    private const val KEY_SELECTED_CAMERA_ID = "selected_camera_id"
    private const val DEFAULT_SELECTED_CAMERA_ID = ""
    private const val KEY_ZOOM_RATIO_PREFIX = "zoom_ratio_"
    private const val DEFAULT_ZOOM_RATIO = 1.0f
    private const val KEY_DEFAULT_ZOOM_RATIO = "default_zoom_ratio"
    
    private const val TAG = "PreferenceManager"
    
    /**
     * Checks if audio recording is enabled in user preferences.
     * 
     * @param context Application or Activity context
     * @return true if audio recording is enabled, false otherwise. Defaults to true on first launch.
     */
    override fun isAudioRecordingEnabled(context: Context): Boolean {
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
    override fun setAudioRecordingEnabled(context: Context, enabled: Boolean) {
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
    override fun isDualCameraEnabled(context: Context): Boolean {
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
    override fun setDualCameraEnabled(context: Context, enabled: Boolean) {
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
    override fun getVideoQuality(context: Context): Quality {
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
    override fun setVideoQuality(context: Context, quality: Quality) {
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
    
    /**
     * Checks if speed display is enabled in user preferences.
     * 
     * @param context Application or Activity context
     * @return true if speed display is enabled, false otherwise. Defaults to false on first launch.
     */
    override fun isSpeedDisplayEnabled(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_SPEED_DISPLAY_ENABLED, DEFAULT_SPEED_DISPLAY_ENABLED)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading speed display preference", e)
            DEFAULT_SPEED_DISPLAY_ENABLED
        }
    }
    
    /**
     * Sets the speed display enabled preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable speed display, false to disable
     */
    override fun setSpeedDisplayEnabled(context: Context, enabled: Boolean) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SPEED_DISPLAY_ENABLED, enabled).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing speed display preference", e)
        }
    }
    
    /**
     * Gets the speed unit preference.
     * 
     * @param context Application or Activity context
     * @return Speed unit string ("mph" or "kmh"). Defaults to "mph" on first launch.
     */
    override fun getSpeedUnit(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_SPEED_UNIT, DEFAULT_SPEED_UNIT) ?: DEFAULT_SPEED_UNIT
        } catch (e: Exception) {
            Log.e(TAG, "Error reading speed unit preference", e)
            DEFAULT_SPEED_UNIT
        }
    }
    
    /**
     * Sets the speed unit preference.
     * 
     * @param context Application or Activity context
     * @param unit Speed unit string ("mph" or "kmh")
     */
    override fun setSpeedUnit(context: Context, unit: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SPEED_UNIT, unit).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing speed unit preference", e)
        }
    }
    
    /**
     * Checks if trip logging is enabled in user preferences.
     * 
     * @param context Application or Activity context
     * @return true if trip logging is enabled, false otherwise. Defaults to false on first launch.
     */
    override fun isTripLogEnabled(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_TRIP_LOG_ENABLED, DEFAULT_TRIP_LOG_ENABLED)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading trip log preference", e)
            DEFAULT_TRIP_LOG_ENABLED
        }
    }
    
    /**
     * Sets the trip logging preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable trip logging, false to disable
     */
    override fun setTripLogEnabled(context: Context, enabled: Boolean) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_TRIP_LOG_ENABLED, enabled).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing trip log preference", e)
        }
    }
    

    // Camera Selection and Zoom Settings Implementation
    
    /**
     * Gets the selected camera ID from user preferences.
     * 
     * @param context Application or Activity context
     * @return Camera ID string, or empty string if not set
     */
    override fun getSelectedCameraId(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_SELECTED_CAMERA_ID, DEFAULT_SELECTED_CAMERA_ID) ?: DEFAULT_SELECTED_CAMERA_ID
        } catch (e: Exception) {
            Log.e(TAG, "Error reading selected camera ID preference", e)
            DEFAULT_SELECTED_CAMERA_ID
        }
    }
    
    /**
     * Sets the selected camera ID preference.
     * 
     * @param context Application or Activity context
     * @param cameraId Camera ID string to store
     */
    override fun setSelectedCameraId(context: Context, cameraId: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SELECTED_CAMERA_ID, cameraId).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing selected camera ID preference", e)
        }
    }
    
    /**
     * Gets the zoom ratio for a specific camera from user preferences.
     * 
     * @param context Application or Activity context
     * @param cameraId Camera ID to get zoom ratio for
     * @return Zoom ratio value, or 1.0 if not set
     */
    override fun getZoomRatio(context: Context, cameraId: String): Float {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = KEY_ZOOM_RATIO_PREFIX + cameraId
            prefs.getFloat(key, DEFAULT_ZOOM_RATIO)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading zoom ratio preference for camera $cameraId", e)
            DEFAULT_ZOOM_RATIO
        }
    }
    
    /**
     * Sets the zoom ratio for a specific camera.
     * 
     * @param context Application or Activity context
     * @param cameraId Camera ID to set zoom ratio for
     * @param ratio Zoom ratio value to store
     */
    override fun setZoomRatio(context: Context, cameraId: String, ratio: Float) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = KEY_ZOOM_RATIO_PREFIX + cameraId
            prefs.edit().putFloat(key, ratio).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing zoom ratio preference for camera $cameraId", e)
        }
    }
    
    /**
     * Gets the default zoom ratio for new cameras.
     * 
     * @param context Application or Activity context
     * @return Default zoom ratio value
     */
    override fun getDefaultZoomRatio(context: Context): Float {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getFloat(KEY_DEFAULT_ZOOM_RATIO, DEFAULT_ZOOM_RATIO)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading default zoom ratio preference", e)
            DEFAULT_ZOOM_RATIO
        }
    }
    
    /**
     * Sets the default zoom ratio for new cameras.
     * 
     * @param context Application or Activity context
     * @param ratio Default zoom ratio value to store
     */
    override fun setDefaultZoomRatio(context: Context, ratio: Float) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_DEFAULT_ZOOM_RATIO, ratio).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing default zoom ratio preference", e)
        }
    }
}
