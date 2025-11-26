package com.kasahirotech.dashcamapp.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.camera.video.Quality
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

/**
 * BackgroundCameraManager handles Camera2 API-based video recording
 * that operates independently of Activity lifecycle for background recording.
 */
class BackgroundCameraManager(private val context: Context) {

    companion object {
        private const val TAG = "BackgroundCameraManager"
        private const val CAMERA_ID = "0" // Back camera
    }

    // Camera2 components
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    
    // Background thread for camera operations
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    // Recording configuration
    private var currentOutputFile: File? = null
    
    // Callback interface
    interface RecordingCallback {
        fun onRecordingStarted()
        fun onRecordingStopped(outputFile: File)
        fun onRecordingError(error: String)
    }
    
    private var callback: RecordingCallback? = null
    
    /**
     * Set the callback for recording events
     */
    fun setCallback(callback: RecordingCallback) {
        this.callback = callback
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Start recording with specified settings
     */
    fun startRecording(
        audioEnabled: Boolean,
        quality: Quality,
        outputFile: File
    ): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }
        
        if (!checkCameraPermission()) {
            callback?.onRecordingError("Camera permission not granted")
            return false
        }
        
        currentOutputFile = outputFile
        
        try {
            startBackgroundThread()
            setupMediaRecorder(outputFile, audioEnabled, quality)
            openCamera()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            callback?.onRecordingError("Failed to start recording: ${e.message}")
            cleanup()
            return false
        }
    }
    
    /**
     * Stop the current recording
     */
    fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not currently recording")
            return
        }
        
        try {
            stopMediaRecorder()
            isRecording = false
            
            currentOutputFile?.let { file ->
                callback?.onRecordingStopped(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            callback?.onRecordingError("Error stopping recording: ${e.message}")
        } finally {
            cleanup()
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        if (isRecording) {
            stopRecording()
        } else {
            cleanup()
        }
    }
    
    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start background thread for camera operations
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }
    
    /**
     * Stop background thread
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }
    
    /**
     * Setup MediaRecorder with specified settings
     */
    private fun setupMediaRecorder(outputFile: File, audioEnabled: Boolean, quality: Quality) {
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                // Set audio source first if enabled
                if (audioEnabled) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                
                // Set video source
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                
                // Set output format
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                
                // Set output file
                setOutputFile(outputFile.absolutePath)
                
                // Configure quality settings
                when (quality) {
                    Quality.HD -> {
                        setVideoSize(1280, 720)
                        setVideoEncodingBitRate(8_000_000)
                    }
                    Quality.FHD -> {
                        setVideoSize(1920, 1080)
                        setVideoEncodingBitRate(12_000_000)
                    }
                    Quality.UHD -> {
                        setVideoSize(3840, 2160)
                        setVideoEncodingBitRate(20_000_000)
                    }
                    else -> {
                        // Default to FHD
                        setVideoSize(1920, 1080)
                        setVideoEncodingBitRate(12_000_000)
                    }
                }
                
                // Set encoders
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (audioEnabled) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44100)
                }
                
                // Set frame rate
                setVideoFrameRate(30)
                
                // Prepare MediaRecorder
                prepare()
            }
            
            Log.d(TAG, "MediaRecorder setup complete")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to setup MediaRecorder", e)
            throw e
        } catch (e: IllegalStateException) {
            Log.e(TAG, "MediaRecorder in illegal state", e)
            throw e
        }
    }
    
    /**
     * Open the camera device
     */
    private fun openCamera() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        try {
            if (!checkCameraPermission()) {
                callback?.onRecordingError("Camera permission not granted")
                return
            }
            
            manager.openCamera(CAMERA_ID, cameraStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera", e)
            callback?.onRecordingError("Failed to open camera: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission denied", e)
            callback?.onRecordingError("Camera permission denied")
        }
    }
    
    /**
     * Create capture session for recording
     */
    private fun createCaptureSession() {
        val camera = cameraDevice ?: run {
            callback?.onRecordingError("Camera device is null")
            return
        }
        
        val recorder = mediaRecorder ?: run {
            callback?.onRecordingError("MediaRecorder is null")
            return
        }
        
        try {
            val surface = recorder.surface
            
            // Create capture request builder
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            captureRequestBuilder.addTarget(surface)
            
            // Create capture session
            @Suppress("DEPRECATION")
            camera.createCaptureSession(
                listOf(surface),
                captureSessionCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create capture session", e)
            callback?.onRecordingError("Failed to create capture session: ${e.message}")
        }
    }
    
    /**
     * Capture session state callback
     */
    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(TAG, "Capture session configured")
            captureSession = session
            
            try {
                val camera = cameraDevice ?: return
                val recorder = mediaRecorder ?: return
                
                // Build capture request
                val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                    addTarget(recorder.surface)
                }.build()
                
                // Set repeating request
                session.setRepeatingRequest(captureRequest, null, backgroundHandler)
                
                // Start MediaRecorder
                startMediaRecorder()
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to start capture request", e)
                callback?.onRecordingError("Failed to start capture: ${e.message}")
            }
        }
        
        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(TAG, "Capture session configuration failed")
            callback?.onRecordingError("Failed to configure capture session")
        }
    }
    
    /**
     * Start MediaRecorder
     */
    private fun startMediaRecorder() {
        try {
            mediaRecorder?.start()
            isRecording = true
            Log.d(TAG, "MediaRecorder started")
            callback?.onRecordingStarted()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to start MediaRecorder", e)
            callback?.onRecordingError("Failed to start recording: ${e.message}")
        }
    }
    
    /**
     * Stop MediaRecorder
     */
    private fun stopMediaRecorder() {
        try {
            mediaRecorder?.stop()
            Log.d(TAG, "MediaRecorder stopped")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
            // Continue with cleanup even if stop fails
        } catch (e: RuntimeException) {
            Log.e(TAG, "Runtime error stopping MediaRecorder", e)
            // Continue with cleanup
        }
    }
    
    /**
     * Camera state callback
     */
    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "Camera opened successfully")
            cameraDevice = camera
            createCaptureSession()
        }
        
        override fun onDisconnected(camera: CameraDevice) {
            Log.w(TAG, "Camera disconnected")
            camera.close()
            cameraDevice = null
            
            if (isRecording) {
                callback?.onRecordingError("Camera disconnected during recording")
                stopRecording()
            }
        }
        
        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "Camera error: $error")
            camera.close()
            cameraDevice = null
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "Camera is in use by another app"
                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                ERROR_CAMERA_DISABLED -> "Camera is disabled"
                ERROR_CAMERA_DEVICE -> "Camera device error"
                ERROR_CAMERA_SERVICE -> "Camera service error"
                else -> "Unknown camera error: $error"
            }
            
            callback?.onRecordingError(errorMessage)
            
            if (isRecording) {
                stopRecording()
            }
        }
    }
    
    /**
     * Clean up all resources
     */
    private fun cleanup() {
        try {
            captureSession?.close()
            captureSession = null
            
            cameraDevice?.close()
            cameraDevice = null
            
            mediaRecorder?.release()
            mediaRecorder = null
            
            stopBackgroundThread()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
