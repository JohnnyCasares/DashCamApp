package com.kasahirotech.dashcamapp

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.Quality
import com.kasahirotech.dashcamapp.databinding.ActivityMainBinding
import com.kasahirotech.dashcamapp.screens.Gallery
import com.kasahirotech.dashcamapp.interfaces.SpeedTrackingService
import com.kasahirotech.dashcamapp.service.Camera
import com.kasahirotech.dashcamapp.service.DualCameraManager
import com.kasahirotech.dashcamapp.service.PermissionHandler
import com.kasahirotech.dashcamapp.service.PreferenceManager
import com.kasahirotech.dashcamapp.service.RecordingService
import com.kasahirotech.dashcamapp.service.SpeedTracker
import com.kasahirotech.dashcamapp.service.Storage
import com.kasahirotech.dashcamapp.service.TripLogger
import com.kasahirotech.dashcamapp.settings.Settings

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var camera: Camera
    private lateinit var dualCameraManager: DualCameraManager
    private lateinit var permissionHandler: PermissionHandler
    private var speedTracker: SpeedTrackingService? = null
    private lateinit var tripLogger: TripLogger
    private var recordingService: RecordingService? = null
    private var serviceBound = false

    private var isDualCameraMode = false
    private var isCurrentlyRecording = false
    
    /**
     * Broadcast receiver to listen for recording stopped events from RecordingService
     */
    private val recordingStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == RecordingService.BROADCAST_RECORDING_STOPPED) {
                android.util.Log.d("MainActivity", "Received broadcast: Recording stopped")
                handleRecordingStopped()
            }
        }
    }

    private enum class PermissionRequestContext {
        STARTUP,
        RECORD,
        GALLERY,
        SPEED_DISPLAY,
        NONE
    }

    private var currentPermissionContext = PermissionRequestContext.NONE

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.value == false)
                permissionGranted = false
        }
        
        if (permissionGranted) {
            when (currentPermissionContext) {
                PermissionRequestContext.RECORD -> {
                    if (isDualCameraMode) {
                        dualCameraManager.startDualRecording(PreferenceManager.isAudioRecordingEnabled(this))
                    } else {
                        camera.captureVideo()
                    }
                }
                PermissionRequestContext.GALLERY -> {
                    Intent(this, Gallery::class.java).also {
                        this.startActivity(it)
                    }
                }
                PermissionRequestContext.STARTUP -> {
                    initializeCameraMode()
                }
                PermissionRequestContext.SPEED_DISPLAY -> {
                    startSpeedTrackingIfEnabled()
                }
                PermissionRequestContext.NONE -> {}
            }
        } else {
            val errorMessage = when (currentPermissionContext) {
                PermissionRequestContext.RECORD -> "Camera and microphone permissions are required to record videos"
                PermissionRequestContext.GALLERY -> "Media access permission is required to view videos"
                PermissionRequestContext.STARTUP -> "Permission request denied"
                PermissionRequestContext.SPEED_DISPLAY -> "Location permission is required for speed display"
                PermissionRequestContext.NONE -> "Permission request denied"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
        
        currentPermissionContext = PermissionRequestContext.NONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        permissionHandler = PermissionHandler(this)
        camera = Camera(
            activity = this,
            binding = binding,
            surfaceProvider = binding.viewFinder.surfaceProvider,
        )
        dualCameraManager = DualCameraManager(
            activity = this,
            frontPreviewView = binding.viewFinderFront,
            backPreviewView = binding.viewFinderBack
        )
        val storage = Storage(this)
        tripLogger = TripLogger(this, storage)
        setContentView(binding.root)

        initializeSpeedDisplay()
        this.permissionCheck()

        binding.imgBtnGallery.setOnClickListener {
            val galleryPermissions = permissionHandler.getGalleryPermissions()
            if (galleryPermissions.isEmpty() || permissionHandler.arePermissionsGranted(galleryPermissions)) {
                Intent(this, Gallery::class.java).also {
                    this.startActivity(it)
                }
            } else {
                currentPermissionContext = PermissionRequestContext.GALLERY
                activityResultLauncher.launch(galleryPermissions)
            }
        }

        binding.btnSettings.setOnClickListener {
            Intent(this, Settings::class.java).also {
                this.startActivity(it)
            }
        }

        binding.btnRecordAndStop.setOnClickListener {
            val recordingPermissions = permissionHandler.getRecordingPermissions()
            if (permissionHandler.arePermissionsGranted(recordingPermissions)) {
                // Start RecordingService for background recording using Camera2
                startBackgroundRecording()
            } else {
                currentPermissionContext = PermissionRequestContext.RECORD
                activityResultLauncher.launch(recordingPermissions)
            }
        }
    }

    private fun initializeCameraMode() {
        isDualCameraMode = PreferenceManager.isDualCameraEnabled(this) && DualCameraManager.isDeviceCapable(this)
        
        if (isDualCameraMode) {
            binding.viewFinder.visibility = View.GONE
            binding.dualPreviewContainer.visibility = View.VISIBLE
            dualCameraManager.startDualCamera()
        } else {
            binding.viewFinder.visibility = View.VISIBLE
            binding.dualPreviewContainer.visibility = View.GONE
            camera.startCamera()
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(PermissionHandler.REQUIRED_PERMISSIONS)
    }

    private fun permissionCheck() {
        if (permissionHandler.allPermissionsGranted()) {
            initializeCameraMode()
        } else {
            currentPermissionContext = PermissionRequestContext.STARTUP
            requestPermissions()
        }
    }
    

    
    override fun onPause() {
        super.onPause()
        speedTracker?.stopTracking()
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(recordingStoppedReceiver)
            android.util.Log.d("MainActivity", "Broadcast receiver unregistered")
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered, ignore
            android.util.Log.d("MainActivity", "Receiver was not registered")
        }
        
        // Recording continues in background via Camera2 service - no action needed
        android.util.Log.d("MainActivity", "onPause - recording continues in background")
    }
    
    override fun onResume() {
        super.onResume()
        
        // Register broadcast receiver for recording stopped events
        val filter = IntentFilter(RecordingService.BROADCAST_RECORDING_STOPPED)
        registerReceiver(recordingStoppedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        android.util.Log.d("MainActivity", "Broadcast receiver registered")
        
        // Sync recording state with service
        syncRecordingState()
        
        // Restart CameraX preview only if NOT recording
        if (permissionHandler.allPermissionsGranted() && !isCurrentlyRecording) {
            initializeCameraMode()
        }
        
        startSpeedTrackingIfEnabled()
        android.util.Log.d("MainActivity", "onResume - isRecording: $isCurrentlyRecording")
    }
    
    private fun initializeSpeedDisplay() {
        speedTracker = SpeedTracker(this) { speed, unit ->
            runOnUiThread {
                updateSpeedDisplay(speed, unit)
            }
        }
    }
    
    private fun startSpeedTrackingIfEnabled() {
        if (PreferenceManager.isSpeedDisplayEnabled(this)) {
            if (permissionHandler.hasLocationPermission()) {
                val unitString = PreferenceManager.getSpeedUnit(this)
                val unit = if (unitString == "kmh") SpeedTrackingService.SpeedUnit.KMH else SpeedTrackingService.SpeedUnit.MPH
                speedTracker?.setSpeedUnit(unit)
                speedTracker?.startTracking()
                binding.tvSpeed.visibility = View.VISIBLE
            } else {
                binding.tvSpeed.visibility = View.GONE
                requestLocationPermissionForSpeedDisplay()
            }
        } else {
            speedTracker?.stopTracking()
            binding.tvSpeed.visibility = View.GONE
        }
    }
    
    fun requestLocationPermissionForSpeedDisplay() {
        if (shouldShowRequestPermissionRationale(PermissionHandler.ACCESS_FINE_LOCATION)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Location Permission Required")
                .setMessage("Speed display requires location access to show your current speed using GPS.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    currentPermissionContext = PermissionRequestContext.SPEED_DISPLAY
                    activityResultLauncher.launch(permissionHandler.getLocationPermissions())
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            currentPermissionContext = PermissionRequestContext.SPEED_DISPLAY
            activityResultLauncher.launch(permissionHandler.getLocationPermissions())
        }
    }
    
    private fun updateSpeedDisplay(speed: Float, unit: SpeedTrackingService.SpeedUnit) {
        val unitLabel = if (unit == SpeedTrackingService.SpeedUnit.MPH) "mph" else "km/h"
        
        if (speed < 0) {
            binding.tvSpeed.text = "-- $unitLabel"
        } else {
            binding.tvSpeed.text = "${speed.toInt()} $unitLabel"
        }
    }
    
    /**
     * Starts trip logging if the feature is enabled in preferences.
     * Called when video recording starts.
     * Location permission is optional - logs will be created without GPS data if not granted.
     */
    fun startTripLoggingIfEnabled() {
        val isTripLogEnabled = PreferenceManager.isTripLogEnabled(this)
        val hasLocationPerm = permissionHandler.hasLocationPermission()
        
        android.util.Log.d("MainActivity", "Trip log enabled: $isTripLogEnabled, Location permission: $hasLocationPerm")
        
        if (isTripLogEnabled) {
            val success = tripLogger.startLogging(this)
            android.util.Log.d("MainActivity", "Trip logging start result: $success")
            
            if (!success) {
                Toast.makeText(this, "Unable to start trip logging", Toast.LENGTH_SHORT).show()
            } else {
                if (hasLocationPerm) {
                    Toast.makeText(this, "Trip logging started", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Trip logging started (no GPS)", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            android.util.Log.d("MainActivity", "Trip logging not started: feature disabled in settings")
        }
    }
    
    /**
     * Stops trip logging.
     * Called when video recording stops.
     */
    fun stopTripLogging() {
        tripLogger.stopLogging()
    }
    
    /**
     * Start recording using RecordingService with Camera2 API
     * This works both in foreground and background
     */
    private fun startBackgroundRecording() {
        // Check if already recording
        if (isCurrentlyRecording) {
            // Stop recording
            stopBackgroundRecording()
            return
        }
        
        // Update button to recording state immediately
        updateRecordButtonState(true)
        isCurrentlyRecording = true
        
        android.util.Log.d("MainActivity", "Starting Camera2 recording")
        
        // Stop CameraX preview to release camera for Camera2
        val cameraProvider = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this)
        cameraProvider.addListener({
            try {
                cameraProvider.get().unbindAll()
                android.util.Log.d("MainActivity", "CameraX unbound")
                
                // Hide preview - Camera2 records without preview
                binding.viewFinder.visibility = View.GONE
                binding.dualPreviewContainer.visibility = View.GONE
                
                // Start Camera2 recording service
                val intent = Intent(this, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START_RECORDING
                    putExtra(RecordingService.EXTRA_AUDIO_ENABLED, PreferenceManager.isAudioRecordingEnabled(this@MainActivity))
                    val quality = PreferenceManager.getVideoQuality(this@MainActivity)
                    val qualityName = when (quality) {
                        Quality.HD -> "HD"
                        Quality.FHD -> "FHD"
                        Quality.UHD -> "UHD"
                        else -> "FHD"
                    }
                    putExtra(RecordingService.EXTRA_VIDEO_QUALITY, qualityName)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                
                // Start trip logging if enabled
                startTripLoggingIfEnabled()
                
                android.util.Log.d("MainActivity", "Camera2 recording started")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error starting recording", e)
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
                updateRecordButtonState(false)
                isCurrentlyRecording = false
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(this))
    }
    
    /**
     * Stop recording
     */
    private fun stopBackgroundRecording() {
        if (!isCurrentlyRecording) return
        
        android.util.Log.d("MainActivity", "Stopping Camera2 recording")
        
        isCurrentlyRecording = false
        
        // Stop Camera2 recording service
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP_RECORDING
        }
        startService(intent)
        
        // Unbind from service
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        
        // Stop trip logging
        stopTripLogging()
        
        // Update button to stopped state
        updateRecordButtonState(false)
        
        // Restart CameraX preview
        binding.viewFinder.postDelayed({
            if (permissionHandler.allPermissionsGranted()) {
                initializeCameraMode()
            }
        }, 500)
    }
    
    /**
     * Handle recording stopped event from RecordingService broadcast.
     * This is called when recording is stopped from the notification.
     */
    private fun handleRecordingStopped() {
        android.util.Log.d("MainActivity", "Handling recording stopped from notification")
        
        // Update recording state
        isCurrentlyRecording = false
        
        // Unbind from service if bound
        if (serviceBound) {
            try {
                unbindService(serviceConnection)
                serviceBound = false
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error unbinding service", e)
            }
        }
        
        // Stop trip logging
        stopTripLogging()
        
        // Update button to stopped state
        updateRecordButtonState(false)
        
        // Restart CameraX preview
        binding.viewFinder.postDelayed({
            if (permissionHandler.allPermissionsGranted()) {
                initializeCameraMode()
            }
        }, 500)
    }
    
    /**
     * Sync recording state with the actual service state.
     * This is called when the activity resumes to ensure UI matches reality.
     * Handles the case where recording was stopped while the app was minimized.
     */
    private fun syncRecordingState() {
        val serviceRecording = RecordingService.isRecording()
        android.util.Log.d("MainActivity", "Syncing state - Service recording: $serviceRecording, Local state: $isCurrentlyRecording")
        
        if (!serviceRecording && isCurrentlyRecording) {
            // Service stopped while app was minimized
            android.util.Log.d("MainActivity", "Service stopped while minimized, updating UI")
            isCurrentlyRecording = false
            updateRecordButtonState(false)
            
            // Unbind from service if still bound
            if (serviceBound) {
                try {
                    unbindService(serviceConnection)
                    serviceBound = false
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error unbinding service during sync", e)
                }
            }
        } else if (serviceRecording && !isCurrentlyRecording) {
            // Service is running but local state says it's not (shouldn't happen, but handle it)
            android.util.Log.d("MainActivity", "Service recording but local state incorrect, updating UI")
            isCurrentlyRecording = true
            updateRecordButtonState(true)
            
            // Try to bind to the service
            val intent = Intent(this, RecordingService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    /**
     * Update the record button appearance based on recording state
     */
    private fun updateRecordButtonState(isRecording: Boolean) {
        if (isRecording) {
            // Change to stop button
            binding.btnRecordAndStop.setBackgroundResource(R.drawable.ic_stop_recording)
        } else {
            // Change to record button
            binding.btnRecordAndStop.setBackgroundResource(R.drawable.bg_record_button)
        }
    }
    
    /**
     * Service connection for RecordingService
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.LocalBinder
            recordingService = binder.getService()
            serviceBound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            serviceBound = false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}