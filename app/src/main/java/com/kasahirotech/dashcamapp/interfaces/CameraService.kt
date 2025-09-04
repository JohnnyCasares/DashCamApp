package com.kasahirotech.dashcamapp.interfaces

import android.content.ContentResolver
import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.lifecycle.LifecycleOwner

interface CameraService {
    var imageCapture: ImageCapture?
    var videoCapture: VideoCapture<Recorder>?
    var recording: Recording?

    fun startCamera(context: Context,lifecycleOwner: LifecycleOwner,  surfaceProvider: Preview.SurfaceProvider)
    fun captureVideo(context: Context, contentResolver: ContentResolver)
    fun takePhoto(context: Context)


}