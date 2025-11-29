package com.kasahirotech.dashcamapp.interfaces

import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo

/**
 * Interface for zoom control operations.
 * Manages zoom ratios and gesture-based zoom interactions.
 */
interface ZoomControllerService {
    
    /**
     * Sets the camera control and info for zoom operations.
     * 
     * @param cameraControl CameraControl instance for applying zoom
     * @param cameraInfo CameraInfo instance for zoom capabilities
     */
    fun setCamera(cameraControl: CameraControl, cameraInfo: CameraInfo)
    
    /**
     * Sets the zoom ratio.
     * 
     * @param ratio Desired zoom ratio
     * @return true if zoom was applied successfully, false otherwise
     */
    fun setZoomRatio(ratio: Float): Boolean
    
    /**
     * Gets the current zoom ratio.
     * 
     * @return Current zoom ratio value
     */
    fun getZoomRatio(): Float
    
    /**
     * Gets the minimum supported zoom ratio.
     * 
     * @return Minimum zoom ratio
     */
    fun getMinZoomRatio(): Float
    
    /**
     * Gets the maximum supported zoom ratio.
     * 
     * @return Maximum zoom ratio
     */
    fun getMaxZoomRatio(): Float
    
    /**
     * Handles pinch gesture for zoom control.
     * 
     * @param scaleFactor Scale factor from pinch gesture detector
     */
    fun handlePinchGesture(scaleFactor: Float)
    
    /**
     * Resets zoom to default (1.0).
     */
    fun resetZoom()
}
