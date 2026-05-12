package com.kasahirotech.dashcamapp

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import com.kasahirotech.dashcamapp.databinding.ActivityMainBinding
import com.kasahirotech.dashcamapp.interfaces.SpeedTrackingService
import com.kasahirotech.dashcamapp.screens.Gallery
import com.kasahirotech.dashcamapp.service.Camera
import com.kasahirotech.dashcamapp.service.DualCameraManager
import com.kasahirotech.dashcamapp.service.PermissionHandler
import com.kasahirotech.dashcamapp.service.PreferenceManager
import com.kasahirotech.dashcamapp.service.RecordingService
import com.kasahirotech.dashcamapp.service.SessionManager
import com.kasahirotech.dashcamapp.service.SpeedTracker
import com.kasahirotech.dashcamapp.service.Storage
import com.kasahirotech.dashcamapp.service.TripLogger
import com.kasahirotech.dashcamapp.settings.Settings
import java.io.File

/**
 * MainActivity — orchestrates the hybrid CameraX ↔ Camera2 recording session.
 *
 * State machine:
 *
 *   IDLE
 *     │ [Record tapped]
 *     ▼
 *   CAMERAX_RECORDING  ── preview visible, CameraX segment running
 *     │ [onStop — app backgrounded]
 *     ▼
 *   TRANSITIONING_TO_CAMERA2  ── CameraX segment stopping (async)
 *     │ [onSegmentFinalized fires]
 *     ▼
 *   CAMERA2_RECORDING  ── Camera2 service running in background
 *     │ [onResume — app foregrounded]
 *     ▼
 *   CAMERA2_BANNER_VISIBLE  ── banner shown, Camera2 still recording
 *     │ [btnOpenPreview tapped]
 *     ▼
 *   TRANSITIONING_TO_CAMERAX  ── Camera2 segment stopping (via broadcast)
 *     │ [BROADCAST_RECORDING_STOPPED received]
 *     ▼
 *   CAMERAX_RECORDING  ── (cycle repeats)
 *     │ [Stop tapped in any recording state]
 *     ▼
 *   IDLE
 */
class MainActivity : AppCompatActivity() {

    // ── View binding ─────────────────────────────────────────────────────────
    private lateinit var binding: ActivityMainBinding

    // ── Services / managers ──────────────────────────────────────────────────
    private lateinit var camera: Camera
    private lateinit var dualCameraManager: DualCameraManager
    private lateinit var permissionHandler: PermissionHandler
    private var speedTracker: SpeedTrackingService? = null
    private lateinit var tripLogger: TripLogger
    private var recordingService: RecordingService? = null
    private var serviceBound = false

    // ── State machine ─────────────────────────────────────────────────────────
    private enum class RecordingState {
        IDLE,
        CAMERAX_RECORDING,
        TRANSITIONING_TO_CAMERA2,   // CameraX stopping (async), Camera2 not yet started
        CAMERA2_RECORDING,
        CAMERA2_BANNER_VISIBLE,     // Camera2 running; app is in foreground with banner
        TRANSITIONING_TO_CAMERAX,   // Camera2 stopping; CameraX not yet started
        STOPPING_SESSION,           // User hit Stop; current segment finishing up
    }
    private var recordingState = RecordingState.IDLE

    // ── Banner timer ─────────────────────────────────────────────────────────
    private val bannerHandler = Handler(Looper.getMainLooper())
    private var bannerTimerRunnable: Runnable? = null
    private var sessionStartTime: Long = 0L
    private var bannerPulseAnimator: ObjectAnimator? = null

    // ── Dual-camera (single camera is the hybrid target) ─────────────────────
    private var isDualCameraMode = false

    // ── Permission context ────────────────────────────────────────────────────
    private enum class PermissionRequestContext { STARTUP, RECORD, GALLERY, SPEED_DISPLAY, NONE }
    private var currentPermissionContext = PermissionRequestContext.NONE

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            when (currentPermissionContext) {
                PermissionRequestContext.RECORD -> handleRecordButtonAction()
                PermissionRequestContext.GALLERY -> openGallery()
                PermissionRequestContext.STARTUP -> initializeCameraMode()
                PermissionRequestContext.SPEED_DISPLAY -> startSpeedTrackingIfEnabled()
                PermissionRequestContext.NONE -> {}
            }
        } else {
            val msg = when (currentPermissionContext) {
                PermissionRequestContext.RECORD -> "Camera and microphone permissions are required to record"
                PermissionRequestContext.GALLERY -> "Media access permission is required to view videos"
                PermissionRequestContext.STARTUP -> "Permission request denied"
                PermissionRequestContext.SPEED_DISPLAY -> "Location permission is required for speed display"
                PermissionRequestContext.NONE -> "Permission request denied"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        currentPermissionContext = PermissionRequestContext.NONE
    }

    // ── Broadcast receiver ────────────────────────────────────────────────────
    /**
     * Receives BROADCAST_RECORDING_STOPPED from RecordingService.
     * Carries EXTRA_OUTPUT_FILE with the Camera2 segment path.
     */
    private val recordingStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != RecordingService.BROADCAST_RECORDING_STOPPED) return

            val filePath = intent.getStringExtra(RecordingService.EXTRA_OUTPUT_FILE)
            Log.d(TAG, "Broadcast: recording stopped — file=$filePath")

            // Register the Camera2 segment
            filePath?.let { SessionManager.addSegment(this@MainActivity, File(it)) }

            unbindFromService()

            when (recordingState) {
                RecordingState.TRANSITIONING_TO_CAMERAX -> {
                    // User tapped "Open Preview" — restart CameraX recording
                    hideBanner()
                    restartCameraXRecording()
                }
                RecordingState.STOPPING_SESSION -> {
                    // User tapped Stop while Camera2 was active
                    finalizeSession()
                }
                else -> {
                    // Stopped from notification or unexpected — treat as session end
                    Log.w(TAG, "Unexpected stop broadcast in state=$recordingState")
                    finalizeSession()
                }
            }
        }
    }

    // ── Service connection ────────────────────────────────────────────────────
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            recordingService = (service as RecordingService.LocalBinder).getService()
            serviceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            serviceBound = false
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHandler = PermissionHandler(this)
        camera = Camera(
            activity = this,
            binding = binding,
            surfaceProvider = binding.viewFinder.surfaceProvider,
        )
        camera.onSegmentFinalized = { file -> onCameraXSegmentFinalized(file) }

        dualCameraManager = DualCameraManager(
            activity = this,
            frontPreviewView = binding.viewFinderFront,
            backPreviewView = binding.viewFinderBack,
        )
        tripLogger = TripLogger(this, Storage(this))

        initializeSpeedDisplay()
        setupClickListeners()
        permissionCheck()
    }

    override fun onStart() {
        super.onStart()
        // Receiver is registered in onResume / unregistered in onPause (symmetric)
    }

    override fun onPause() {
        super.onPause()
        speedTracker?.stopTracking()

        // ── KEY HAND-OFF: foreground → background ──────────────────────────
        // onPause fires while the app is still considered "foreground" by Android.
        // This is the LAST point where we can legally start a camera-type FGS on API 34+.
        if (recordingState == RecordingState.CAMERAX_RECORDING) {
            Log.d(TAG, "App pausing during CameraX recording — phase-1: prepare FGS")
            recordingState = RecordingState.TRANSITIONING_TO_CAMERA2

            // Phase 1: establish the foreground service notification NOW (camera not opened yet)
            prepareCamera2ForegroundService()

            // Phase 2: stop CameraX; camera2 recording begins in onCameraXSegmentFinalized()
            camera.stopRecordingSegment()
        }

        // Unregister broadcast receiver on every pause
        try {
            unregisterReceiver(recordingStoppedReceiver)
        } catch (_: IllegalArgumentException) {}

        Log.d(TAG, "onPause — state=$recordingState")
    }

    override fun onStop() {
        super.onStop()
        // Nothing to do here for recording — hand-off already happened in onPause.
        Log.d(TAG, "onStop — state=$recordingState")
    }

    override fun onResume() {
        super.onResume()

        // Re-register broadcast receiver
        val filter = IntentFilter(RecordingService.BROADCAST_RECORDING_STOPPED)
        registerReceiver(recordingStoppedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "Broadcast receiver registered")

        startSpeedTrackingIfEnabled()

        when (recordingState) {
            RecordingState.IDLE -> {
                if (permissionHandler.allPermissionsGranted()) initializeCameraMode()
            }
            RecordingState.CAMERA2_RECORDING -> {
                showBanner()
                recordingState = RecordingState.CAMERA2_BANNER_VISIBLE
            }
            RecordingState.CAMERA2_BANNER_VISIBLE -> {
                startBannerTimer()
            }
            else -> { /* mid-transition or CameraX recording — nothing extra */ }
        }

        Log.d(TAG, "onResume — state=$recordingState")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBannerTimer()
        bannerPulseAnimator?.cancel()
        unbindFromService()
    }

    // ════════════════════════════════════════════════════════════════════════
    // Click listeners
    // ════════════════════════════════════════════════════════════════════════

    private fun setupClickListeners() {
        binding.imgBtnGallery.setOnClickListener {
            val perms = permissionHandler.getGalleryPermissions()
            if (perms.isEmpty() || permissionHandler.arePermissionsGranted(perms)) {
                openGallery()
            } else {
                currentPermissionContext = PermissionRequestContext.GALLERY
                activityResultLauncher.launch(perms)
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, Settings::class.java))
        }

        binding.btnRecordAndStop.setOnClickListener {
            val perms = permissionHandler.getRecordingPermissions()
            if (permissionHandler.arePermissionsGranted(perms)) {
                handleRecordButtonAction()
            } else {
                currentPermissionContext = PermissionRequestContext.RECORD
                activityResultLauncher.launch(perms)
            }
        }

        // "Open Preview" banner — whole overlay or dedicated button
        binding.layoutPreviewBanner?.setOnClickListener { onOpenPreviewRequested() }
        binding.btnOpenPreview?.setOnClickListener { onOpenPreviewRequested() }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Record button dispatch
    // ════════════════════════════════════════════════════════════════════════

    private fun handleRecordButtonAction() {
        when (recordingState) {
            RecordingState.IDLE -> startSession()
            RecordingState.CAMERAX_RECORDING -> stopSessionFromCameraX()
            RecordingState.CAMERA2_RECORDING,
            RecordingState.CAMERA2_BANNER_VISIBLE -> stopSessionFromCamera2()
            else -> Log.w(TAG, "Record button tapped during transition state=$recordingState — ignoring")
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Session start
    // ════════════════════════════════════════════════════════════════════════

    private fun startSession() {
        SessionManager.startSession()
        sessionStartTime = System.currentTimeMillis()
        Log.d(TAG, "Session started: ${SessionManager.sessionId}")

        // Start CameraX segment (preview stays visible)
        val outputFile = SessionManager.nextCameraXSegmentFile(this) ?: run {
            Toast.makeText(this, "Cannot create session directory", Toast.LENGTH_SHORT).show()
            SessionManager.endSession()
            return
        }

        val audioEnabled = PreferenceManager.isAudioRecordingEnabled(this)
        camera.startRecordingSegment(outputFile, audioEnabled)
        recordingState = RecordingState.CAMERAX_RECORDING
        updateRecordButtonState(isRecording = true)
        Log.d(TAG, "CameraX segment started: ${outputFile.name}")
    }

    // ════════════════════════════════════════════════════════════════════════
    // Hand-off: CameraX → Camera2 (two-phase)
    // Phase 1: prepareCamera2ForegroundService() — called from onPause
    // Phase 2: beginCamera2Recording()           — called from onCameraXSegmentFinalized
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Phase 1: start the RecordingService as a foreground service while the app is
     * still in the foreground (called from onPause). The service starts its
     * notification but does NOT open the camera yet.
     */
    private fun prepareCamera2ForegroundService() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_PREPARE_FOREGROUND
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "Camera2 FGS prepared (phase 1)")
    }

    /**
     * Phase 2: tell the already-running service to open Camera2 and start recording.
     * Called once CameraX has released the camera hardware.
     */
    private fun beginCamera2Recording() {
        val sessionDir = SessionManager.getSessionDir(this)?.absolutePath
        val audioEnabled = PreferenceManager.isAudioRecordingEnabled(this)
        val quality = PreferenceManager.getVideoQuality(this)
        val qualityName = when (quality) {
            Quality.HD -> "HD"; Quality.FHD -> "FHD"; Quality.UHD -> "UHD"; else -> "FHD"
        }
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_BEGIN_RECORDING
            putExtra(RecordingService.EXTRA_AUDIO_ENABLED, audioEnabled)
            putExtra(RecordingService.EXTRA_VIDEO_QUALITY, qualityName)
            sessionDir?.let { putExtra(RecordingService.EXTRA_SESSION_DIR, it) }
        }
        startService(intent)

        // Hide preview — Camera2 is headless
        binding.viewFinder.visibility = View.GONE
        binding.dualPreviewContainer.visibility = View.GONE
        Log.d(TAG, "Camera2 recording begun (phase 2) — sessionDir=$sessionDir")
    }

    /**
     * Called on main thread when a CameraX segment has been flushed to disk.
     */
    private fun onCameraXSegmentFinalized(file: File?) {
        Log.d(TAG, "CameraX segment finalized: ${file?.name} in state=$recordingState")

        file?.let { SessionManager.addSegment(this, it) }

        // Unbind CameraX so Camera2 can claim the hardware
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try { providerFuture.get().unbindAll() } catch (_: Exception) {}

            when (recordingState) {
                RecordingState.TRANSITIONING_TO_CAMERA2 -> {
                    // Phase 2: now that CameraX released the camera, start Camera2 recording
                    beginCamera2Recording()
                    recordingState = RecordingState.CAMERA2_RECORDING
                }
                RecordingState.STOPPING_SESSION -> {
                    finalizeSession()
                }
                else -> {
                    Log.w(TAG, "onCameraXSegmentFinalized in unexpected state=$recordingState")
                    finalizeSession()
                }
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(this))
    }

    // ════════════════════════════════════════════════════════════════════════
    // Banner: Camera2 running, app is in foreground
    // ════════════════════════════════════════════════════════════════════════

    private fun showBanner() {
        binding.layoutPreviewBanner?.visibility = View.VISIBLE
        startBannerTimer()
        startBannerPulse()
        Log.d(TAG, "Banner shown")
    }

    private fun hideBanner() {
        binding.layoutPreviewBanner?.visibility = View.GONE
        stopBannerTimer()
        bannerPulseAnimator?.cancel()
    }

    private fun startBannerTimer() {
        stopBannerTimer()
        bannerTimerRunnable = object : Runnable {
            override fun run() {
                val elapsed = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
                val h = elapsed / 3600
                val m = (elapsed % 3600) / 60
                val s = elapsed % 60
                val text = if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
                binding.tvBannerTimer?.text = text
                bannerHandler.postDelayed(this, 1000)
            }
        }
        bannerHandler.post(bannerTimerRunnable!!)
    }

    private fun stopBannerTimer() {
        bannerTimerRunnable?.let { bannerHandler.removeCallbacks(it) }
        bannerTimerRunnable = null
    }

    private fun startBannerPulse() {
        bannerPulseAnimator?.cancel()
        bannerPulseAnimator = ObjectAnimator.ofFloat(binding.ivBannerIcon, "alpha", 1f, 0.2f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Hand-off: Camera2 → CameraX (banner tap)
    // ════════════════════════════════════════════════════════════════════════

    private fun onOpenPreviewRequested() {
        if (recordingState != RecordingState.CAMERA2_BANNER_VISIBLE) return
        Log.d(TAG, "Open Preview tapped — handing off Camera2 → CameraX")
        recordingState = RecordingState.TRANSITIONING_TO_CAMERAX
        stopCamera2Service()
        // Broadcast received → restartCameraXRecording() is called
    }

    private fun stopCamera2Service() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP_RECORDING
        }
        startService(intent)
    }

    private fun restartCameraXRecording() {
        // Re-show the preview surface
        binding.viewFinder.visibility = View.VISIBLE

        // Give CameraX a moment to bind before starting segment
        binding.viewFinder.postDelayed({
            if (!permissionHandler.allPermissionsGranted()) return@postDelayed

            // Restart the camera (binds CameraX to lifecycle)
            camera.startCamera()

            // Then start the next segment after the provider is ready
            binding.viewFinder.postDelayed({
                val outputFile = SessionManager.nextCameraXSegmentFile(this) ?: run {
                    Toast.makeText(this, "Cannot create segment file", Toast.LENGTH_SHORT).show()
                    finalizeSession()
                    return@postDelayed
                }
                val audioEnabled = PreferenceManager.isAudioRecordingEnabled(this)
                camera.startRecordingSegment(outputFile, audioEnabled)
                recordingState = RecordingState.CAMERAX_RECORDING
                updateRecordButtonState(isRecording = true)
                Log.d(TAG, "CameraX restarted — new segment: ${outputFile.name}")
            }, 600)
        }, 300)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Stop session
    // ════════════════════════════════════════════════════════════════════════

    /** User hit Stop while CameraX is recording. */
    private fun stopSessionFromCameraX() {
        Log.d(TAG, "Stopping session from CameraX state")
        recordingState = RecordingState.STOPPING_SESSION
        camera.stopRecordingSegment()
        stopTripLogging()
        // finalizeSession() is called from onCameraXSegmentFinalized when state==STOPPING_SESSION
    }

    /** User hit Stop while Camera2 is recording (or banner is visible). */
    private fun stopSessionFromCamera2() {
        Log.d(TAG, "Stopping session from Camera2 state")
        recordingState = RecordingState.STOPPING_SESSION
        hideBanner()
        stopTripLogging()
        stopCamera2Service()
        // finalizeSession() is called from broadcast receiver when state==STOPPING_SESSION
    }

    private fun finalizeSession() {
        val segments = SessionManager.endSession()
        Log.d(TAG, "Session finalized — ${segments.size} segment(s)")

        recordingState = RecordingState.IDLE
        updateRecordButtonState(isRecording = false)
        stopBannerTimer()

        val msg = if (segments.isEmpty()) {
            "Recording stopped"
        } else if (segments.size == 1) {
            "Recording saved"
        } else {
            "Recording saved (${segments.size} parts)"
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

        // Restart CameraX preview
        binding.viewFinder.postDelayed({
            if (permissionHandler.allPermissionsGranted()) initializeCameraMode()
        }, 500)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Camera initialization (preview only — no recording)
    // ════════════════════════════════════════════════════════════════════════

    private fun initializeCameraMode() {
        isDualCameraMode = PreferenceManager.isDualCameraEnabled(this) &&
                DualCameraManager.isDeviceCapable(this)

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

    // ════════════════════════════════════════════════════════════════════════
    // Speed display
    // ════════════════════════════════════════════════════════════════════════

    private fun initializeSpeedDisplay() {
        speedTracker = SpeedTracker(this) { speed, unit ->
            runOnUiThread { updateSpeedDisplay(speed, unit) }
        }
    }

    private fun startSpeedTrackingIfEnabled() {
        if (PreferenceManager.isSpeedDisplayEnabled(this)) {
            if (permissionHandler.hasLocationPermission()) {
                val unit = if (PreferenceManager.getSpeedUnit(this) == "kmh")
                    SpeedTrackingService.SpeedUnit.KMH else SpeedTrackingService.SpeedUnit.MPH
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
        val label = if (unit == SpeedTrackingService.SpeedUnit.MPH) "mph" else "km/h"
        binding.tvSpeed.text = if (speed < 0) "-- $label" else "${speed.toInt()} $label"
    }

    // ════════════════════════════════════════════════════════════════════════
    // Trip logging
    // ════════════════════════════════════════════════════════════════════════

    fun startTripLoggingIfEnabled() {
        if (!PreferenceManager.isTripLogEnabled(this)) return
        val ok = tripLogger.startLogging(this)
        val msg = if (ok) {
            if (permissionHandler.hasLocationPermission()) "Trip logging started" else "Trip logging started (no GPS)"
        } else "Unable to start trip logging"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun stopTripLogging() {
        tripLogger.stopLogging()
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════

    private fun permissionCheck() {
        if (permissionHandler.allPermissionsGranted()) {
            initializeCameraMode()
        } else {
            currentPermissionContext = PermissionRequestContext.STARTUP
            activityResultLauncher.launch(PermissionHandler.REQUIRED_PERMISSIONS)
        }
    }

    private fun openGallery() {
        startActivity(Intent(this, Gallery::class.java))
    }

    private fun updateRecordButtonState(isRecording: Boolean) {
        binding.btnRecordAndStop.setBackgroundResource(
            if (isRecording) R.drawable.ic_stop_recording else R.drawable.bg_record_button
        )
    }

    private fun unbindFromService() {
        if (serviceBound) {
            try {
                unbindService(serviceConnection)
            } catch (_: Exception) {}
            serviceBound = false
            recordingService = null
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}