package com.kasahirotech.dashcamapp.service

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Manages concurrent dual camera recording operations.
 * Handles device capability detection, camera initialization, and synchronized recording.
 */
class DualCameraManager(
    private val activity: AppCompatActivity,
    private val frontPreviewView: PreviewView,
    private val backPreviewView: PreviewView
) {
    
    private var frontVideoCapture: VideoCapture<Recorder>? = null
    private var backVideoCapture: VideoCapture<Recorder>? = null
    private var frontRecording: Recording? = null
    private var backRecording: Recording? = null
    private var isDualCameraStarted: Boolean = false
    
    companion object {
        private const val TAG = "DualCameraManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        
        /**
         * Checks if the device supports concurrent dual camera recording.
         * Requires Android 11+ (API 30) and hardware support for concurrent cameras.
         * 
         * @param context Application or Activity context
         * @return true if device supports dual camera recording, false otherwise
         */
        fun isDeviceCapable(context: Context): Boolean {
            return try {
                // Check API level requirement
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    Log.d(TAG, "Dual camera requires Android 11+ (API 30+). Current API: ${Build.VERSION.SDK_INT}")
                    return false
                }
                
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val concurrentCameraIds = cameraManager.concurrentCameraIds
                
                if (concurrentCameraIds.isEmpty()) {
                    Log.d(TAG, "Device does not support concurrent cameras")
                    return false
                }
                
                // Check if we have at least one set with both front and back cameras
                val hasValidSet = concurrentCameraIds.any { cameraIdSet ->
                    val hasFront = cameraIdSet.any { id ->
                        val characteristics = cameraManager.getCameraCharacteristics(id)
                        characteristics.get(CameraCharacteristics.LENS_FACING) == 
                            CameraCharacteristics.LENS_FACING_FRONT
                    }
                    val hasBack = cameraIdSet.any { id ->
                        val characteristics = cameraManager.getCameraCharacteristics(id)
                        characteristics.get(CameraCharacteristics.LENS_FACING) == 
                            CameraCharacteristics.LENS_FACING_BACK
                    }
                    hasFront && hasBack
                }
                
                Log.d(TAG, "Dual camera capability check result: $hasValidSet")
                hasValidSet
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking dual camera capability", e)
                false
            }
        }
    }
    
    /**
     * Initializes and starts both front and back cameras with preview use cases.
     * 
     * @return true if both cameras were successfully initialized, false otherwise
     */
    fun startDualCamera(): Boolean {
        return try {
            val cameraProvider = ProcessCameraProvider.getInstance(activity).get()
            
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()
            
            // Create preview use cases for both cameras
            val frontPreview = Preview.Builder().build().also {
                it.surfaceProvider = frontPreviewView.surfaceProvider
            }
            
            val backPreview = Preview.Builder().build().also {
                it.surfaceProvider = backPreviewView.surfaceProvider
            }
            
            // Create video capture use cases with recorders
            val frontRecorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            frontVideoCapture = VideoCapture.withOutput(frontRecorder)
            
            val backRecorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            backVideoCapture = VideoCapture.withOutput(backRecorder)
            
            // Bind front camera
            val frontCamera = cameraProvider.bindToLifecycle(
                activity,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                frontPreview,
                frontVideoCapture
            )
            
            // Bind back camera
            val backCamera = cameraProvider.bindToLifecycle(
                activity,
                CameraSelector.DEFAULT_BACK_CAMERA,
                backPreview,
                backVideoCapture
            )
            
            isDualCameraStarted = true
            Log.d(TAG, "Dual camera initialization successful")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize dual camera", e)
            isDualCameraStarted = false
            false
        }
    }
    
    /**
     * Stops the dual camera and releases resources.
     */
    fun stopDualCamera() {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(activity).get()
            cameraProvider.unbindAll()
            
            frontVideoCapture = null
            backVideoCapture = null
            isDualCameraStarted = false
            
            Log.d(TAG, "Dual camera stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping dual camera", e)
        }
    }
    
    /**
     * Checks if dual camera is currently active.
     * 
     * @return true if dual camera is started, false otherwise
     */
    fun isDualCameraActive(): Boolean {
        return isDualCameraStarted
    }
    
    /**
     * Starts synchronized recording on both front and back cameras.
     * 
     * @param enableAudio true to include audio in recordings, false otherwise
     * @return true if both recordings started successfully, false otherwise
     */
    fun startDualRecording(enableAudio: Boolean): Boolean {
        val frontCapture = frontVideoCapture
        val backCapture = backVideoCapture
        
        if (frontCapture == null || backCapture == null) {
            Log.e(TAG, "Cannot start recording: cameras not initialized")
            return false
        }
        
        // Check if already recording
        if (frontRecording != null || backRecording != null) {
            Log.w(TAG, "Recording already in progress")
            return false
        }
        
        return try {
            // Generate synchronized timestamp for both recordings
            val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())
            
            // Create output options for front camera
            val frontName = "${timestamp}_front"
            val frontContentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, frontName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/DashCam")
            }
            val frontOutputOptions = MediaStoreOutputOptions.Builder(
                activity.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(frontContentValues).build()
            
            // Create output options for back camera
            val backName = "${timestamp}_back"
            val backContentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, backName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/DashCam")
            }
            val backOutputOptions = MediaStoreOutputOptions.Builder(
                activity.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(backContentValues).build()
            
            // Check audio permission and preference
            val shouldIncludeAudio = enableAudio && 
                PermissionChecker.checkSelfPermission(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            
            // Start front camera recording
            frontRecording = frontCapture.output
                .prepareRecording(activity, frontOutputOptions)
                .apply {
                    if (shouldIncludeAudio) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(activity)) { /* Event handling in stopDualRecording */ }
            
            // Start back camera recording
            backRecording = backCapture.output
                .prepareRecording(activity, backOutputOptions)
                .apply {
                    if (shouldIncludeAudio) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(activity)) { /* Event handling in stopDualRecording */ }
            
            Log.d(TAG, "Dual recording started: $timestamp")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start dual recording", e)
            // Clean up if one recording started but the other failed
            frontRecording?.close()
            backRecording?.close()
            frontRecording = null
            backRecording = null
            false
        }
    }
    
    /**
     * Stops both camera recordings synchronously.
     * Handles errors gracefully and logs which camera failed if applicable.
     */
    fun stopDualRecording() {
        try {
            val frontRec = frontRecording
            val backRec = backRecording
            
            if (frontRec == null && backRec == null) {
                Log.w(TAG, "No active recordings to stop")
                return
            }
            
            var frontStopped = false
            var backStopped = false
            var frontError: Exception? = null
            var backError: Exception? = null
            
            // Stop front camera recording
            if (frontRec != null) {
                try {
                    frontRec.stop()
                    frontStopped = true
                    Log.d(TAG, "Front camera recording stopped")
                } catch (e: Exception) {
                    frontError = e
                    Log.e(TAG, "Error stopping front camera recording", e)
                }
            }
            
            // Stop back camera recording (within 200ms of front)
            if (backRec != null) {
                try {
                    backRec.stop()
                    backStopped = true
                    Log.d(TAG, "Back camera recording stopped")
                } catch (e: Exception) {
                    backError = e
                    Log.e(TAG, "Error stopping back camera recording", e)
                }
            }
            
            // Clean up recording references
            frontRecording = null
            backRecording = null
            
            // Log summary
            if (frontError != null || backError != null) {
                val failedCameras = mutableListOf<String>()
                if (frontError != null) failedCameras.add("front")
                if (backError != null) failedCameras.add("back")
                Log.e(TAG, "Recording stop completed with errors on: ${failedCameras.joinToString(", ")}")
            } else {
                Log.d(TAG, "Both recordings stopped successfully")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during stopDualRecording", e)
            frontRecording = null
            backRecording = null
        }
    }
    
    /**
     * Checks if dual recording is currently active.
     * 
     * @return true if either camera is recording, false otherwise
     */
    fun isRecording(): Boolean {
        return frontRecording != null || backRecording != null
    }
}
