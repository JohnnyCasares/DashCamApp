package com.kasahirotech.dashcamapp.service

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import com.kasahirotech.dashcamapp.MainActivity
import com.kasahirotech.dashcamapp.databinding.ActivityMainBinding
import com.kasahirotech.dashcamapp.interfaces.CameraService
import java.text.SimpleDateFormat
import java.util.Locale

class Camera : CameraService {
    override var imageCapture: ImageCapture? = null
    override var videoCapture: VideoCapture<Recorder>? = null
    override var recording: Recording? = null


    override fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
// The ProcessCameraProvider instance  is binded to the parent this. Binds the lifecycle of the camera to the owner
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            //Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val recorder =
                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
            videoCapture = VideoCapture.withOutput(recorder)

            //Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                //Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                //Unbind use case to camera
                cameraProvider.unbindAll()


                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, " Failed to get camera provider or binding use cases", exc)
            }
            //ContextCompat.getMainExecutor() as the second argument. This returns an Executor that runs on the main thread.
        }, ContextCompat.getMainExecutor(context))
    }

    override fun captureVideo(
        context: Context,
         contentResolver: ContentResolver
    ) {

        val videoCapture = this.videoCapture ?: return

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

//        recording = videoCapture.output
//            .prepareRecording(this, mediaStoreOutputOptions)
//            .apply {
//                if (PermissionChecker.checkSelfPermission(
//                        context@MainActivity,
//                        Manifest.permission.RECORD_AUDIO
//                    ) ==
//                    PermissionChecker.PERMISSION_GRANTED
//                ) {
//                    withAudioEnabled()
//                }
//            }
//            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
//                when (recordEvent) {
//                    is VideoRecordEvent.Start -> {
//                        binding.videoCaptureButton.apply {
//                            text = getString(R.string.stop_capture)
//                            isEnabled = true
//                        }
//                    }
//
//                    is VideoRecordEvent.Finalize -> {
//                        if (!recordEvent.hasError()) {
//                            val msg = "Video capture succeeded: " +
//                                    "${recordEvent.outputResults.outputUri}"
//                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
//                                .show()
//                            Log.d(com.kasahirotech.cameraxcodelab.MainActivity.Companion.TAG, msg)
//                        } else {
//                            recording?.close()
//                            recording = null
//                            Log.e(
//                                com.kasahirotech.cameraxcodelab.MainActivity.Companion.TAG, "Video capture ends with error: " +
//                                        "${recordEvent.error}"
//                            )
//                        }
//                        binding.videoCaptureButton.apply {
//                            text = getString(R.string.start_capture)
//                            isEnabled = true
//                        }
//                    }
//                }
//            }

    }



    override fun takePhoto(context: Context) {

    }


    companion object {
        private const val TAG = "Camera Class"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

}