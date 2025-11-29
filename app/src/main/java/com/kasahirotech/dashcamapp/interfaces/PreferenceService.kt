package com.kasahirotech.dashcamapp.interfaces

import android.content.Context
import androidx.camera.video.Quality

/**
 * Interface for managing application preferences.
 * Provides abstraction for preference storage operations, enabling
 * testability and flexibility in storage implementation.
 */
interface PreferenceService {
    
    /**
     * Checks if audio recording is enabled in user preferences.
     * 
     * @param context Application or Activity context
     * @return true if audio recording is enabled, false otherwise
     */
    fun isAudioRecordingEnabled(context: Context): Boolean
    
    /**
     * Sets the audio recording preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable audio recording, false to disable
     */
    fun setAudioRecordingEnabled(context: Context, enabled: Boolean)
    
    /**
     * Checks if dual camera recording is enabled in user preferences.
     * 
     * @param context Application or Activity context
     * @return true if dual camera recording is enabled, false otherwise
     */
    fun isDualCameraEnabled(context: Context): Boolean
    
    /**
     * Sets the dual camera recording preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable dual camera recording, false to disable
     */
    fun setDualCameraEnabled(context: Context, enabled: Boolean)
    
    /**
     * Gets the video recording quality from user preferences.
     * 
     * @param context Application or Activity context
     * @return Quality enum value
     */
    fun getVideoQuality(context: Context): Quality
    
    /**
     * Sets the video recording quality preference.
     * 
     * @param context Application or Activity context
     * @param quality Quality enum value to store
     */
    fun setVideoQuality(context: Context, quality: Quality)
    
    /**
     * Checks if speed display is enabled in user preferences.
     * 
     * @param context Application or Activity context
     * @return true if speed display is enabled, false otherwise
     */
    fun isSpeedDisplayEnabled(context: Context): Boolean
    
    /**
     * Sets the speed display enabled preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable speed display, false to disable
     */
    fun setSpeedDisplayEnabled(context: Context, enabled: Boolean)
    
    /**
     * Gets the speed unit preference.
     * 
     * @param context Application or Activity context
     * @return Speed unit string ("mph" or "kmh")
     */
    fun getSpeedUnit(context: Context): String
    
    /**
     * Sets the speed unit preference.
     * 
     * @param context Application or Activity context
     * @param unit Speed unit string ("mph" or "kmh")
     */
    fun setSpeedUnit(context: Context, unit: String)
    
    /**
     * Checks if trip logging is enabled in user preferences.
     * 
     * @param context Application or Activity context
     * @return true if trip logging is enabled, false otherwise
     */
    fun isTripLogEnabled(context: Context): Boolean
    
    /**
     * Sets the trip logging preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable trip logging, false to disable
     */
    fun setTripLogEnabled(context: Context, enabled: Boolean)
    
    // Google Drive Upload Settings
    
    /**
     * Checks if auto-upload to Google Drive is enabled.
     * 
     * @param context Application or Activity context
     * @return true if auto-upload is enabled, false otherwise
     */
    fun isAutoUploadEnabled(context: Context): Boolean
    
    /**
     * Sets the auto-upload preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable auto-upload, false to disable
     */
    fun setAutoUploadEnabled(context: Context, enabled: Boolean)
    
    /**
     * Checks if WiFi-only mode is enabled for uploads.
     * 
     * @param context Application or Activity context
     * @return true if WiFi-only mode is enabled, false otherwise
     */
    fun isWifiOnlyMode(context: Context): Boolean
    
    /**
     * Sets the WiFi-only mode preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to enable WiFi-only mode, false to disable
     */
    fun setWifiOnlyMode(context: Context, enabled: Boolean)
    
    /**
     * Gets the minimum battery level required for uploads.
     * 
     * @param context Application or Activity context
     * @return Minimum battery level percentage (0-100)
     */
    fun getMinimumBatteryLevel(context: Context): Int
    
    /**
     * Sets the minimum battery level required for uploads.
     * 
     * @param context Application or Activity context
     * @param level Minimum battery level percentage (0-100)
     */
    fun setMinimumBatteryLevel(context: Context, level: Int)
    
    /**
     * Gets the maximum number of retry attempts for failed uploads.
     * 
     * @param context Application or Activity context
     * @return Maximum retry attempts
     */
    fun getMaxRetryAttempts(context: Context): Int
    
    /**
     * Sets the maximum number of retry attempts for failed uploads.
     * 
     * @param context Application or Activity context
     * @param attempts Maximum retry attempts
     */
    fun setMaxRetryAttempts(context: Context, attempts: Int)
    
    /**
     * Checks if local files should be deleted after successful upload.
     * 
     * @param context Application or Activity context
     * @return true if local files should be deleted after upload, false otherwise
     */
    fun isDeleteLocalAfterUpload(context: Context): Boolean
    
    /**
     * Sets the delete local after upload preference.
     * 
     * @param context Application or Activity context
     * @param enabled true to delete local files after upload, false to keep them
     */
    fun setDeleteLocalAfterUpload(context: Context, enabled: Boolean)
    
    // Camera Selection and Zoom Settings
    
    /**
     * Gets the selected camera ID from user preferences.
     * 
     * @param context Application or Activity context
     * @return Camera ID string, or empty string if not set
     */
    fun getSelectedCameraId(context: Context): String
    
    /**
     * Sets the selected camera ID preference.
     * 
     * @param context Application or Activity context
     * @param cameraId Camera ID string to store
     */
    fun setSelectedCameraId(context: Context, cameraId: String)
    
    /**
     * Gets the zoom ratio for a specific camera from user preferences.
     * 
     * @param context Application or Activity context
     * @param cameraId Camera ID to get zoom ratio for
     * @return Zoom ratio value, or 1.0 if not set
     */
    fun getZoomRatio(context: Context, cameraId: String): Float
    
    /**
     * Sets the zoom ratio for a specific camera.
     * 
     * @param context Application or Activity context
     * @param cameraId Camera ID to set zoom ratio for
     * @param ratio Zoom ratio value to store
     */
    fun setZoomRatio(context: Context, cameraId: String, ratio: Float)
    
    /**
     * Gets the default zoom ratio for new cameras.
     * 
     * @param context Application or Activity context
     * @return Default zoom ratio value
     */
    fun getDefaultZoomRatio(context: Context): Float
    
    /**
     * Sets the default zoom ratio for new cameras.
     * 
     * @param context Application or Activity context
     * @param ratio Default zoom ratio value to store
     */
    fun setDefaultZoomRatio(context: Context, ratio: Float)
}
