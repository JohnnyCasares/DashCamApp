package com.kasahirotech.dashcamapp.service

import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import com.kasahirotech.dashcamapp.interfaces.ZoomControllerService
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of ZoomControllerService for managing camera zoom operations.
 * Handles zoom ratio clamping, gesture-based zoom, and zoom state management.
 */
class ZoomController : ZoomControllerService {
    
    companion object {
        private const val TAG = "ZoomController"
        private const val DEFAULT_ZOOM_RATIO = 1.0f
        private const val PINCH_ZOOM_SENSITIVITY = 0.1f
    }
    
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var currentZoomRatio: Float = DEFAULT_ZOOM_RATIO
    private var minZoomRatio: Float = DEFAULT_ZOOM_RATIO
    private var maxZoomRatio: Float = DEFAULT_ZOOM_RATIO
    
    /**
     * Sets the camera control and info for zoom operations.
     * 
     * @param cameraControl CameraControl instance for applying zoom
     * @param cameraInfo CameraInfo instance for zoom capabilities
     */
    override fun setCamera(cameraControl: CameraControl, cameraInfo: CameraInfo) {
        this.cameraControl = cameraControl
        this.cameraInfo = cameraInfo
        
        // Get zoom capabilities from camera info
        val zoomState = cameraInfo.zoomState.value
        minZoomRatio = zoomState?.minZoomRatio ?: DEFAULT_ZOOM_RATIO
        maxZoomRatio = zoomState?.maxZoomRatio ?: DEFAULT_ZOOM_RATIO
        currentZoomRatio = zoomState?.zoomRatio ?: DEFAULT_ZOOM_RATIO
        
        Log.d(TAG, "Camera set with zoom range: $minZoomRatio - $maxZoomRatio")
    }
    
    /**
     * Sets the zoom ratio with bounds clamping.
     * 
     * @param ratio Desired zoom ratio
     * @return true if zoom was applied successfully, false otherwise
     */
    override fun setZoomRatio(ratio: Float): Boolean {
        val control = cameraControl ?: run {
            Log.w(TAG, "Cannot set zoom: camera control not initialized")
            return false
        }
        
        // Clamp ratio to valid range
        val clampedRatio = clampZoomRatio(ratio)
        
        return try {
            control.setZoomRatio(clampedRatio)
            currentZoomRatio = clampedRatio
            Log.d(TAG, "Zoom ratio set to: $clampedRatio")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting zoom ratio", e)
            false
        }
    }
    
    /**
     * Gets the current zoom ratio.
     * 
     * @return Current zoom ratio value
     */
    override fun getZoomRatio(): Float {
        // Try to get live zoom state from camera
        val liveZoom = cameraInfo?.zoomState?.value?.zoomRatio
        if (liveZoom != null && liveZoom != currentZoomRatio) {
            currentZoomRatio = liveZoom
        }
        return currentZoomRatio
    }
    
    /**
     * Gets the minimum supported zoom ratio.
     * 
     * @return Minimum zoom ratio
     */
    override fun getMinZoomRatio(): Float {
        return minZoomRatio
    }
    
    /**
     * Gets the maximum supported zoom ratio.
     * 
     * @return Maximum zoom ratio
     */
    override fun getMaxZoomRatio(): Float {
        return maxZoomRatio
    }
    
    /**
     * Handles pinch gesture for zoom control.
     * Translates scale factor to zoom ratio changes.
     * 
     * @param scaleFactor Scale factor from pinch gesture detector (> 1.0 for zoom in, < 1.0 for zoom out)
     */
    override fun handlePinchGesture(scaleFactor: Float) {
        if (cameraControl == null) {
            Log.w(TAG, "Cannot handle pinch: camera control not initialized")
            return
        }
        
        // Calculate new zoom ratio based on scale factor
        // Apply sensitivity to make zoom more controllable
        val zoomChange = (scaleFactor - 1.0f) * PINCH_ZOOM_SENSITIVITY
        val newZoomRatio = currentZoomRatio * (1.0f + zoomChange)
        
        // Apply the new zoom ratio (will be clamped automatically)
        setZoomRatio(newZoomRatio)
    }
    
    /**
     * Resets zoom to default (1.0).
     */
    override fun resetZoom() {
        setZoomRatio(DEFAULT_ZOOM_RATIO)
        Log.d(TAG, "Zoom reset to default")
    }
    
    /**
     * Clamps a zoom ratio to the valid range [minZoomRatio, maxZoomRatio].
     * 
     * @param ratio Zoom ratio to clamp
     * @return Clamped zoom ratio
     */
    private fun clampZoomRatio(ratio: Float): Float {
        return max(minZoomRatio, min(maxZoomRatio, ratio))
    }
}
