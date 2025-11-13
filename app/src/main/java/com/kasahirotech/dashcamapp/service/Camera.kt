package com.kasahirotech.dashcamapp.service

import android.Manifest
import android.content.ContentValues
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.databinding.ActivityMainBinding
import com.kasahirotech.dashcamapp.interfaces.CameraService
import java.text.SimpleDateFormat
import java.util.Locale

class Camera
    (
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val surfaceProvider: Preview.SurfaceProvider,
) : CameraService {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraStarted: Boolean = false


    override fun startCamera() {
// The ProcessCameraProvider instance is bound to the parent this. Binds the lifecycle of the camera to the owner
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            //Preview
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = surfaceProvider
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
                    activity, cameraSelector, preview, videoCapture
                )
                
                cameraStarted = true

            } catch (exc: Exception) {
                Log.e(TAG, " Failed to get camera provider or binding use cases", exc)
                cameraStarted = false
            }
            //ContextCompat.getMainExecutor() as the second argument. This returns an Executor that runs on the main thread.
        }, ContextCompat.getMainExecutor(activity))
    }

    override fun captureVideo() {

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
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/DashCam")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(activity.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(activity, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        activity,
                        Manifest.permission.RECORD_AUDIO
                    ) ==
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(activity)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.btnRecordAndStop.apply {
                            text = activity.getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(activity, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(
                                TAG,
                                "Video capture ends with error: " +
                                        "${recordEvent.error}"
                            )
                        }
                        binding.btnRecordAndStop.apply {
                            text = activity.getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }

    }


    override fun takePhoto() {

    }

    override fun isCameraStarted(): Boolean {
        return cameraStarted
    }

    companion object {
        private const val TAG = "Camera Class"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }

}