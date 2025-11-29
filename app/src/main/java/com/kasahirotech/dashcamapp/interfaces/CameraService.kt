package com.kasahirotech.dashcamapp.interfaces

import com.kasahirotech.dashcamapp.models.CameraInfo

interface CameraService {
    fun startCamera()
    fun captureVideo()
    fun takePhoto()
    fun isCameraStarted(): Boolean
    
    // Camera selection methods
    fun switchCamera(cameraId: String): Boolean
    fun getCurrentCameraId(): String
    fun getCameraInfo(): CameraInfo?
    
    // Zoom control methods
    fun setZoomRatio(ratio: Float): Boolean
    fun getZoomRatio(): Float
    fun getZoomRange(): Pair<Float, Float>
}