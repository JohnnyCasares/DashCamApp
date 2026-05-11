package com.kasahirotech.dashcamapp.service

import android.Manifest
import android.content.ContentValues
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
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
import com.kasahirotech.dashcamapp.MainActivity
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
    
    private var dualCameraManager: DualCameraManager? = null
    private var isDualModeEnabled: Boolean = false
    
    // Camera selection and zoom fields
    private var currentCameraId: String = "0"
    private var currentCameraInfo: com.kasahirotech.dashcamapp.models.CameraInfo? = null
    private val zoomController: ZoomController = ZoomController()
    private var cameraControl: androidx.camera.core.CameraControl? = null


    override fun startCamera() {
        // Load saved camera ID from preferences
        val savedCameraId = PreferenceManager.getSelectedCameraId(activity)
        currentCameraId = if (savedCameraId.isNotEmpty()) savedCameraId else "0"
        
        // Start camera with saved or default ID
        startCameraWithId(currentCameraId)
    }

    override fun captureVideo() {
        // Check if dual mode is enabled
        if (isDualModeEnabled && dualCameraManager != null) {
            captureDualVideo()
            return
        }
        
        // Single camera mode
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
                    PermissionChecker.PERMISSION_GRANTED &&
                    PreferenceManager.isAudioRecordingEnabled(activity)
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(activity)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        val animation = AnimationUtils.loadAnimation(activity, R.anim.circle_to_square)
                        animation.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation?) { /* Not used */ }
                            override fun onAnimationEnd(animation: Animation?) {
                                binding.btnRecordAndStop.setBackgroundResource(R.drawable.ic_stop_recording)
                            }
                            override fun onAnimationRepeat(animation: Animation?) { /* Not used */ }
                        })
                        binding.btnRecordAndStop.startAnimation(animation)
                        
                        // Start trip logging if enabled
                        if (activity is MainActivity) {
                            activity.startTripLoggingIfEnabled()
                        }
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
//                            Toast.makeText(activity, msg, Toast.LENGTH_SHORT)
//                                .show()
                            Log.d(TAG, msg)
                            
                            // Auto-upload to Google Drive if enabled
                            handleAutoUpload(recordEvent.outputResults.outputUri.toString())
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(
                                TAG,
                                "Video capture ends with error: " +
                                        "${recordEvent.error}"
                            )
                        }
                        val animation = AnimationUtils.loadAnimation(activity, R.anim.square_to_circle)
                        animation.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation?) { /* Not used */ }
                            override fun onAnimationEnd(animation: Animation?) {
                                binding.btnRecordAndStop.setBackgroundResource(R.drawable.bg_record_button)
                            }
                            override fun onAnimationRepeat(animation: Animation?) { /* Not used */ }
                        })
                        binding.btnRecordAndStop.startAnimation(animation)
                        
                        // Stop trip logging
                        if (activity is MainActivity) {
                            activity.stopTripLogging()
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
    
    /**
     * Initializes the camera mode based on user preferences and device capability.
     * Sets up either dual camera mode or single camera mode.
     */
    fun initializeCameraMode() {
        isDualModeEnabled = PreferenceManager.isDualCameraEnabled(activity) &&
                           DualCameraManager.isDeviceCapable(activity)
        
        if (isDualModeEnabled) {
            setupDualCameraMode()
        } else {
            setupSingleCameraMode()
        }
    }
    
    /**
     * Sets up dual camera mode with two preview views.
     * Falls back to single camera mode if initialization fails.
     */
    private fun setupDualCameraMode() {
        try {
            // Get views from binding
            val dualContainer = binding.root.findViewById<View>(com.kasahirotech.dashcamapp.R.id.dualPreviewContainer)
            val frontPreview = binding.root.findViewById<androidx.camera.view.PreviewView>(com.kasahirotech.dashcamapp.R.id.viewFinderFront)
            val backPreview = binding.root.findViewById<androidx.camera.view.PreviewView>(com.kasahirotech.dashcamapp.R.id.viewFinderBack)
            
            // Show dual preview container, hide single preview
            binding.viewFinder.visibility = View.GONE
            dualContainer.visibility = View.VISIBLE
            
            // Initialize dual camera manager
            dualCameraManager = DualCameraManager(
                activity = activity,
                frontPreviewView = frontPreview,
                backPreviewView = backPreview
            )
            
            // Start dual camera
            val success = dualCameraManager?.startDualCamera() ?: false
            if (!success) {
                Log.w(TAG, "Dual camera initialization failed, falling back to single camera")
                Toast.makeText(
                    activity,
                    "Dual camera not available, using single camera",
                    Toast.LENGTH_SHORT
                ).show()
                setupSingleCameraMode()
            } else {
                cameraStarted = true
                Log.d(TAG, "Dual camera mode initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during dual camera setup", e)
            Toast.makeText(
                activity,
                "Unable to start dual camera recording",
                Toast.LENGTH_SHORT
            ).show()
            setupSingleCameraMode()
        }
    }
    
    /**
     * Sets up single camera mode with one preview view.
     * This is the default mode and fallback for dual camera failures.
     */
    private fun setupSingleCameraMode() {
        // Get dual container view
        val dualContainer = binding.root.findViewById<View>(com.kasahirotech.dashcamapp.R.id.dualPreviewContainer)
        
        // Show single preview, hide dual preview container
        binding.viewFinder.visibility = View.VISIBLE
        dualContainer.visibility = View.GONE
        
        // Clean up dual camera manager if it exists
        dualCameraManager?.stopDualCamera()
        dualCameraManager = null
        isDualModeEnabled = false
        
        // Start single camera using existing logic
        startCamera()
    }
    
    /**
     * Handles video capture for dual camera mode.
     * Starts or stops recording on both cameras simultaneously.
     */
    private fun captureDualVideo() {
        val manager = dualCameraManager ?: return
        
        // Check if currently recording
        if (manager.isRecording()) {
            // Stop recording
            manager.stopDualRecording()
            
            // Animate button back to record state
            val animation = AnimationUtils.loadAnimation(activity, R.anim.square_to_circle)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) { /* Not used */ }
                override fun onAnimationEnd(animation: Animation?) {
                    binding.btnRecordAndStop.setBackgroundResource(R.drawable.bg_record_button)
                }
                override fun onAnimationRepeat(animation: Animation?) { /* Not used */ }
            })
            binding.btnRecordAndStop.startAnimation(animation)
            
            Toast.makeText(activity, "Dual recording stopped", Toast.LENGTH_SHORT).show()
        } else {
            // Check storage before starting
            if (!hasEnoughStorageForDualRecording()) {
                Toast.makeText(
                    activity,
                    "Insufficient storage for dual camera recording",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            
            // Start recording
            val audioEnabled = PreferenceManager.isAudioRecordingEnabled(activity)
            val success = manager.startDualRecording(audioEnabled)
            
            if (success) {
                // Animate button to stop state
                val animation = AnimationUtils.loadAnimation(activity, R.anim.circle_to_square)
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) { /* Not used */ }
                    override fun onAnimationEnd(animation: Animation?) {
                        binding.btnRecordAndStop.setBackgroundResource(R.drawable.ic_stop_recording)
                    }
                    override fun onAnimationRepeat(animation: Animation?) { /* Not used */ }
                })
                binding.btnRecordAndStop.startAnimation(animation)
                
                Toast.makeText(activity, "Dual recording started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    activity,
                    "Failed to start dual recording",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Checks if there is enough storage space for dual camera recording.
     * Estimates that dual recording requires approximately 2x the space of single recording.
     * 
     * @return true if sufficient storage is available, false otherwise
     */
    private fun hasEnoughStorageForDualRecording(): Boolean {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            
            // Require at least 500MB for dual recording (conservative estimate)
            val requiredBytes = 500L * 1024 * 1024 // 500MB
            
            val hasEnough = availableBytes >= requiredBytes
            if (!hasEnough) {
                Log.w(TAG, "Insufficient storage: ${availableBytes / (1024 * 1024)}MB available, ${requiredBytes / (1024 * 1024)}MB required")
            }
            hasEnough
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage space", e)
            // If we can't check, allow the recording attempt
            true
        }
    }
    
    /**
     * Handle auto-upload of recorded video to Google Drive
     */
    private fun handleAutoUpload(videoUri: String) {
        try {
            // Check if auto-upload is enabled
//            if (!PreferenceManager.isAutoUploadEnabled(activity)) {
//                Log.d(TAG, "Auto-upload disabled, skipping")
//                return
//            }
//
//            // Check if authenticated with Google Drive
//            if (!GoogleDriveManager.isAuthenticated(activity)) {
//                Log.d(TAG, "Not authenticated with Google Drive, skipping auto-upload")
//                return
//            }
            
            // Convert URI to file path
            val videoPath = getFilePathFromUri(videoUri)
            if (videoPath == null) {
                Log.e(TAG, "Could not get file path from URI: $videoUri")
                return
            }
            
            // Add to upload queue
//            UploadQueueManager.addToQueue(activity, videoPath)
//            Log.d(TAG, "Added video to upload queue: $videoPath")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling auto-upload", e)
        }
    }
    
    /**
     * Convert content URI to file path
     */
    private fun getFilePathFromUri(uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val cursor = activity.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(MediaStore.Video.Media.DATA)
                    if (columnIndex >= 0) {
                        it?.getString(columnIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file path from URI", e)
            null
        }
    }

    // Camera selection methods
    
    /**
     * Switches to a different camera by ID.
     * 
     * @param cameraId Camera ID to switch to
     * @return true if switch was successful, false otherwise
     */
    override fun switchCamera(cameraId: String): Boolean {
        return try {
            currentCameraId = cameraId
            
            // Save selected camera to preferences
            PreferenceManager.setSelectedCameraId(activity, cameraId)
            
            // Restart camera with new ID
            startCameraWithId(cameraId)
            
            Log.d(TAG, "Switched to camera: $cameraId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error switching camera", e)
            false
        }
    }
    
    /**
     * Gets the current camera ID.
     * 
     * @return Current camera ID string
     */
    override fun getCurrentCameraId(): String {
        return currentCameraId
    }
    
    /**
     * Gets information about the current camera.
     * 
     * @return CameraInfo object or null if not available
     */
    override fun getCameraInfo(): com.kasahirotech.dashcamapp.models.CameraInfo? {
        return currentCameraInfo
    }
    
    // Zoom control methods
    
    /**
     * Sets the zoom ratio.
     * 
     * @param ratio Desired zoom ratio
     * @return true if zoom was applied successfully, false otherwise
     */
    override fun setZoomRatio(ratio: Float): Boolean {
        val success = zoomController.setZoomRatio(ratio)
        if (success) {
            // Save zoom ratio for current camera
            PreferenceManager.setZoomRatio(activity, currentCameraId, ratio)
        }
        return success
    }
    
    /**
     * Gets the current zoom ratio.
     * 
     * @return Current zoom ratio value
     */
    override fun getZoomRatio(): Float {
        return zoomController.getZoomRatio()
    }
    
    /**
     * Gets the zoom range for the current camera.
     * 
     * @return Pair of (minZoom, maxZoom)
     */
    override fun getZoomRange(): Pair<Float, Float> {
        return Pair(zoomController.getMinZoomRatio(), zoomController.getMaxZoomRatio())
    }
    
    /**
     * Starts camera with a specific camera ID.
     * Loads and applies saved zoom ratio for the camera.
     */
    private fun startCameraWithId(cameraId: String) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = surfaceProvider
            }
            imageCapture = ImageCapture.Builder().build()

            val selectedQuality = PreferenceManager.getVideoQuality(activity)
            val recorder =
                Recorder.Builder().setQualitySelector(QualitySelector.from(selectedQuality)).build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Build camera selector based on camera ID
            val cameraSelector = buildCameraSelectorForId(cameraId)

            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    activity, cameraSelector, preview, videoCapture
                )
                
                // Store camera control for zoom operations
                cameraControl = camera.cameraControl
                
                // Initialize zoom controller with camera
                zoomController.setCamera(camera.cameraControl, camera.cameraInfo)
                
                // Load and apply saved zoom ratio for this camera
                val savedZoom = PreferenceManager.getZoomRatio(activity, cameraId)
                if (savedZoom != 1.0f) {
                    zoomController.setZoomRatio(savedZoom)
                }
                
                cameraStarted = true
                Log.d(TAG, "Camera started with ID: $cameraId, zoom: $savedZoom")

            } catch (exc: Exception) {
                Log.e(TAG, "Failed to start camera with ID: $cameraId", exc)
                cameraStarted = false
                
                // Try fallback to default camera
                if (cameraId != "0") {
                    Log.w(TAG, "Falling back to default camera")
                    currentCameraId = "0"
                    PreferenceManager.setSelectedCameraId(activity, "0")
                    startCameraWithId("0")
                }
            }
        }, ContextCompat.getMainExecutor(activity))
    }
    
    /**
     * Builds a CameraSelector for a specific camera ID.
     */
    private fun buildCameraSelectorForId(cameraId: String): CameraSelector {
        return try {
            // For camera ID "0", use default back camera
            if (cameraId == "0") {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                // Build selector by filtering available cameras
                CameraSelector.Builder()
                    .addCameraFilter { cameraInfos ->
                        cameraInfos.filterIndexed { index, _ -> index.toString() == cameraId }
                    }
                    .build()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building camera selector for ID: $cameraId", e)
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    companion object {
        private const val TAG = "Camera Class"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }

}