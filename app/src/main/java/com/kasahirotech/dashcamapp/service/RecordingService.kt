package com.kasahirotech.dashcamapp.service

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.video.Quality
import androidx.lifecycle.LifecycleService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class RecordingService : LifecycleService() {

    private val binder = LocalBinder()
    private var backgroundCamera: BackgroundCameraManager? = null
    private var isRecording = false
    private var currentOutputFile: File? = null

    companion object {
        const val ACTION_START_RECORDING = "com.kasahirotech.dashcamapp.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.kasahirotech.dashcamapp.STOP_RECORDING"
        /**
         * Phase 1 of background hand-off: called from onPause (app still foreground).
         * Starts the FGS notification so the OS grants camera access in the background.
         * Does NOT open the camera yet — CameraX still owns it.
         */
        const val ACTION_PREPARE_FOREGROUND = "com.kasahirotech.dashcamapp.PREPARE_FOREGROUND"
        /**
         * Phase 2 of background hand-off: called once CameraX has released the camera.
         * Carries the same EXTRA_* extras as ACTION_START_RECORDING.
         */
        const val ACTION_BEGIN_RECORDING = "com.kasahirotech.dashcamapp.BEGIN_RECORDING"
        const val BROADCAST_RECORDING_STOPPED = "com.kasahirotech.dashcamapp.RECORDING_STOPPED"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "recording_channel"
        
        // Intent extras
        const val EXTRA_AUDIO_ENABLED = "audio_enabled"
        const val EXTRA_VIDEO_QUALITY = "video_quality"
        /** Absolute path to the session directory; Camera2 segment is written here. */
        const val EXTRA_SESSION_DIR = "session_dir"
        /** Broadcast extra: absolute path of the completed segment file. */
        const val EXTRA_OUTPUT_FILE = "output_file"
        
        private const val TAG = "RecordingService"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        
        private const val MIN_STORAGE_BYTES = 100L * 1024 * 1024 // 100MB
        private const val STORAGE_CHECK_INTERVAL = 10000L // Check every 10 seconds
        
        // Static flag to track recording state across the app
        @Volatile
        private var isServiceRecording = false
        
        /**
         * Check if the recording service is currently recording.
         * This is a reliable way to check recording state from any component.
         */
        fun isRecording(): Boolean = isServiceRecording
    }
    
    private var startTime: Long = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var storageCheckRunnable: Runnable? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        NotificationHelper.createNotificationChannel(this)
        
        // Initialize BackgroundCameraManager
        backgroundCamera = BackgroundCameraManager(this).apply {
            setCallback(recordingCallback)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        // Handle null intent gracefully
        if (intent == null) {
            Log.w(TAG, "Received null intent in onStartCommand")
            return START_STICKY
        }
        
        when (intent.action) {
            ACTION_PREPARE_FOREGROUND -> {
                // Phase 1: establish the FGS notification while app is still foreground.
                // Do NOT open the camera yet — CameraX still owns it.
                Log.d(TAG, "Preparing foreground service (camera not opened yet)")
                startForegroundService()
            }
            ACTION_BEGIN_RECORDING -> {
                // Phase 2: CameraX has released the camera, start Camera2 recording.
                Log.d(TAG, "Beginning Camera2 recording")
                val audioEnabled = intent.getBooleanExtra(EXTRA_AUDIO_ENABLED, false)
                val qualityName = intent.getStringExtra(EXTRA_VIDEO_QUALITY) ?: "FHD"
                val sessionDirPath = intent.getStringExtra(EXTRA_SESSION_DIR)
                val quality = when (qualityName) {
                    "HD" -> Quality.HD; "FHD" -> Quality.FHD; "UHD" -> Quality.UHD; else -> Quality.FHD
                }
                startRecording(audioEnabled, quality, sessionDirPath)
            }
            ACTION_START_RECORDING -> {
                // Legacy single-shot path (kept for compatibility)
                Log.d(TAG, "Starting recording (legacy path)")
                val audioEnabled = intent.getBooleanExtra(EXTRA_AUDIO_ENABLED, false)
                val qualityName = intent.getStringExtra(EXTRA_VIDEO_QUALITY) ?: "FHD"
                val sessionDirPath = intent.getStringExtra(EXTRA_SESSION_DIR)
                val quality = when (qualityName) {
                    "HD" -> Quality.HD; "FHD" -> Quality.FHD; "UHD" -> Quality.UHD; else -> Quality.FHD
                }
                startForegroundService()
                startRecording(audioEnabled, quality, sessionDirPath)
            }
            ACTION_STOP_RECORDING -> {
                Log.d(TAG, "Stopping recording via intent")
                stopRecordingAndService()
            }
            null -> Log.w(TAG, "Received intent with null action")
            else -> Log.w(TAG, "Received unknown action: ${intent.action}")
        }
        return START_STICKY
    }
    
    fun isRecording(): Boolean {
        return isRecording
    }
    
    /**
     * Recording callback implementation
     */
    private val recordingCallback = object : BackgroundCameraManager.RecordingCallback {
        override fun onRecordingStarted() {
            Log.d(TAG, "Recording started callback")
            isRecording = true
            isServiceRecording = true
        }
        
        override fun onRecordingStopped(outputFile: File) {
            Log.d(TAG, "Recording stopped callback: ${outputFile.absolutePath}")
            isRecording = false
            isServiceRecording = false
            
            // Broadcast stopped + file path so MainActivity can register the segment
            // MainActivity's broadcast receiver will call SessionManager.addSegment,
            // which now handles the MediaStore scanning.
            val broadcastIntent = Intent(BROADCAST_RECORDING_STOPPED).apply {
                putExtra(EXTRA_OUTPUT_FILE, outputFile.absolutePath)
            }
            sendBroadcast(broadcastIntent)
            Log.d(TAG, "Broadcast sent: Recording stopped — ${outputFile.name}")
        }
        
        override fun onRecordingError(error: String) {
            Log.e(TAG, "Recording error: $error")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    applicationContext,
                    "Recording error: $error",
                    Toast.LENGTH_LONG
                ).show()
            }
            stopRecordingAndService()
        }
    }
    
    /**
     * Start recording with BackgroundCameraManager.
     * @param sessionDirPath If provided, the Camera2 segment is written into this directory.
     *                       Otherwise falls back to the legacy Movies/DashCam/ directory.
     */
    private fun startRecording(audioEnabled: Boolean, quality: Quality, sessionDirPath: String? = null) {
        if (!hasEnoughStorage()) {
            Toast.makeText(this, "Insufficient storage space", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }
        
        try {
            val outputFile = createOutputFile(sessionDirPath)
            currentOutputFile = outputFile
            
            val success = backgroundCamera?.startRecording(audioEnabled, quality, outputFile) ?: false
            
            if (!success) {
                Log.e(TAG, "Failed to start recording")
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            Toast.makeText(this, "Error starting recording: ${e.message}", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }
    
    /**
     * Create output file for recording.
     * @param sessionDirPath If non-null, writes into the session subfolder;
     *                       otherwise writes into the legacy Movies/DashCam/ root.
     */
    private fun createOutputFile(sessionDirPath: String? = null): File {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val storageDir = if (sessionDirPath != null) {
            File(sessionDirPath).also { if (!it.exists()) it.mkdirs() }
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "DashCam"
            ).also { if (!it.exists()) it.mkdirs() }
        }
        return File(storageDir, "$name.mp4")
    }

    private fun stopRecordingAndService() {
        try {
            backgroundCamera?.stopRecording()
            Log.d(TAG, "Recording stopped, camera resources released")
            
            // Update static flag
            isRecording = false
            isServiceRecording = false
            
            stopTimer()
            
            // NOTE: broadcast is now sent from the onRecordingStopped callback
            // (which carries the file path). Don't send a duplicate here.
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            isServiceRecording = false
            // Still send a broadcast so the UI can recover
            sendBroadcast(Intent(BROADCAST_RECORDING_STOPPED))
            stopSelf()
        }
    }
    
    private fun startForegroundService() {
        try {
            val notification = NotificationHelper.buildRecordingNotification(
                this, 
                "00:00",
                false // Storage is checked before starting, so initially not low
            )
            startForeground(NOTIFICATION_ID, notification)
            startTimer()
            Log.d(TAG, "Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            stopSelf()
        }
    }
    
    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timerRunnable = object : Runnable {
            override fun run() {
                updateNotification()
                timerHandler.postDelayed(this, 1000) // Update every second
            }
        }
        timerHandler.post(timerRunnable!!)
        
        startStorageMonitoring()
    }
    
    private fun startStorageMonitoring() {
        storageCheckRunnable = object : Runnable {
            override fun run() {
                if (!hasEnoughStorage()) {
                    Log.w(TAG, "Low storage detected, stopping recording")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            applicationContext,
                            "Recording stopped: Low storage space",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    stopRecordingAndService()
                } else {
                    timerHandler.postDelayed(this, STORAGE_CHECK_INTERVAL)
                }
            }
        }
        timerHandler.postDelayed(storageCheckRunnable!!, STORAGE_CHECK_INTERVAL)
    }
    
    private fun hasEnoughStorage(): Boolean {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes >= MIN_STORAGE_BYTES
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage", e)
            true // If we can't check, assume storage is okay
        }
    }
    
    private fun updateNotification() {
        val elapsedMillis = System.currentTimeMillis() - startTime
        val elapsedSeconds = (elapsedMillis / 1000).toInt()
        val formattedTime = formatElapsedTime(elapsedSeconds)
        
        // Check if storage is low for notification warning
        val isLowStorage = !hasEnoughStorage()
        
        val notification = NotificationHelper.buildRecordingNotification(
            this, 
            formattedTime,
            isLowStorage
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun formatElapsedTime(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    private fun stopTimer() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        timerRunnable = null
        storageCheckRunnable?.let { timerHandler.removeCallbacks(it) }
        storageCheckRunnable = null
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopTimer()
        if (isRecording) {
            backgroundCamera?.stopRecording()
        }
        backgroundCamera?.release()
        backgroundCamera = null
        isRecording = false
        isServiceRecording = false
        super.onDestroy()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "Task removed, saving recording")
        if (isRecording) {
            backgroundCamera?.stopRecording()
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
    
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }
}
