package com.kasahirotech.dashcamapp.interfaces

import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture

interface CameraService {
    var imageCapture: ImageCapture?
    var videoCapture: VideoCapture<Recorder>?
    var recording: Recording?

    fun captureVideo()
    fun startCamera()
    fun takePhoto()


}