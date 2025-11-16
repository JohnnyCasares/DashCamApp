package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.util.Log

/**
 * Utility object for managing application preferences using SharedPreferences.
 * Centralizes preference operations for audio recording and other app settings.
 */
object PreferenceManager {
    
    private const val PREFS_NAME = "dashcam_preferences"
    private const val KEY_AUDIO_ENABLED = "audio_recording_enabled"
    private const val DEFAULT_AUDIO_ENABLED = true
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
}
