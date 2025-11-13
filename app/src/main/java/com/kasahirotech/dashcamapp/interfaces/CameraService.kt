package com.kasahirotech.dashcamapp.interfaces


interface CameraService {
    fun startCamera()
    fun captureVideo()
    fun takePhoto()
    fun isCameraStarted(): Boolean
}