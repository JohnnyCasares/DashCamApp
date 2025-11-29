package com.kasahirotech.dashcamapp.interfaces

import android.content.Context
import com.kasahirotech.dashcamapp.models.CameraInfo

/**
 * Interface for camera enumeration and discovery operations.
 * Provides methods to discover and retrieve information about available cameras.
 */
interface CameraEnumeratorService {
    
    /**
     * Gets a list of all available cameras on the device.
     * 
     * @param context Application or Activity context
     * @return List of CameraInfo objects representing available cameras
     */
    suspend fun getCameraList(context: Context): List<CameraInfo>
    
    /**
     * Gets information about a specific camera by ID.
     * 
     * @param context Application or Activity context
     * @param cameraId Unique camera identifier
     * @return CameraInfo object if camera exists, null otherwise
     */
    suspend fun getCameraInfo(context: Context, cameraId: String): CameraInfo?
    
    /**
     * Gets the default camera ID (typically the back camera).
     * 
     * @return Default camera ID string
     */
    fun getDefaultCameraId(): String
}
