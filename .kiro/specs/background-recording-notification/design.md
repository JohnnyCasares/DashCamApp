# Design Document

## Overview

This design implements background recording functionality for the DashCamApp using Android's Foreground Service architecture. When a user starts recording and minimizes the app, a persistent notification will be displayed showing the recording status, elapsed time, and controls to stop recording or return to the app. The foreground service ensures the recording continues uninterrupted even when the app is not visible.

The implementation follows Android best practices for long-running operations and integrates seamlessly with the existing Camera service architecture. The design maintains separation of concerns by creating a dedicated RecordingService that manages the lifecycle of background recording while coordinating with the existing Camera class.

## Architecture

### Critical Design Decision: Camera2 API for Background Recording

**Problem**: CameraX binds camera lifecycle to Activity/LifecycleOwner, which causes issues when the app is backgrounded. CameraX may release camera resources when the Activity is not in the foreground, breaking background recording.

**Solution**: Implement a new Camera2 API-based recording system that operates independently of Activity lifecycle and can run properly within a foreground service.

### Component Overview

```
MainActivity
    ├── RecordingService (Foreground Service)
    │   ├── BackgroundCameraManager (Camera2 API)
    │   │   ├── CameraDevice
    │   │   ├── CameraCaptureSession
    │   │   └── MediaRecorder
    │   ├── NotificationHelper
    │   └── Timer (elapsed time tracking)
    └── Camera (existing CameraX - for foreground use)
```

### Key Components

1. **BackgroundCameraManager**: New Camera2-based recording manager
   - Uses Camera2 API directly (not CameraX)
   - Operates independently of Activity lifecycle
   - Manages CameraDevice and CameraCaptureSession
   - Integrates with MediaRecorder for video encoding
   - Runs within RecordingService context
   - Handles camera permissions and availability

2. **RecordingService**: A foreground service that manages background recording
   - Extends LifecycleService for lifecycle awareness
   - Owns BackgroundCameraManager instance
   - Manages notification lifecycle
   - Tracks recording duration
   - Handles stop recording commands from notification
   - Monitors storage space during recording

3. **NotificationHelper**: Utility for creating and updating notifications
   - Creates notification channel for Android O+
   - Builds notification with recording status
   - Updates elapsed time display
   - Adds action buttons (Stop, Open App)

4. **Camera Service (existing)**: Remains for foreground recording
   - Continues to use CameraX for foreground UI
   - Not used for background recording
   - Provides preview and UI-bound recording

5. **MainActivity Updates**: Modified to coordinate both camera systems
   - Uses Camera (CameraX) for foreground preview
   - Starts RecordingService with BackgroundCameraManager for recording
   - Handles transitions between foreground and background states
   - Manages service lifecycle

## Components and Interfaces

### BackgroundCameraManager

New Camera2-based manager for background recording:

```kotlin
class BackgroundCameraManager(private val context: Context) {
    
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    
    // Callbacks
    interface RecordingCallback {
        fun onRecordingStarted()
        fun onRecordingStopped(outputFile: File)
        fun onRecordingError(error: String)
    }
    
    private var callback: RecordingCallback? = null
    
    // Public API
    fun setCallback(callback: RecordingCallback)
    fun startRecording(
        audioEnabled: Boolean,
        quality: Quality,
        outputFile: File
    ): Boolean
    fun stopRecording()
    fun isRecording(): Boolean
    fun release()
    
    // Camera2 API methods
    private fun openCamera()
    private fun createCaptureSession()
    private fun setupMediaRecorder(outputFile: File, audioEnabled: Boolean, quality: Quality)
    private fun startMediaRecorder()
    private fun stopMediaRecorder()
    private fun closeCamera()
    
    // Camera state callbacks
    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice)
        override fun onDisconnected(camera: CameraDevice)
        override fun onError(camera: CameraDevice, error: Int)
    }
    
    // Capture session callbacks
    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession)
        override fun onConfigureFailed(session: CameraCaptureSession)
    }
}
```

### RecordingService

```kotlin
class RecordingService : LifecycleService() {
    companion object {
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "recording_channel"
        
        // Intent extras
        const val EXTRA_AUDIO_ENABLED = "audio_enabled"
        const val EXTRA_VIDEO_QUALITY = "video_quality"
    }
    
    private var backgroundCamera: BackgroundCameraManager? = null
    private var startTime: Long = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var storageCheckRunnable: Runnable? = null
    private var isRecording = false
    
    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }
    
    override fun onCreate()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    override fun onBind(intent: Intent): IBinder
    override fun onDestroy()
    override fun onTaskRemoved(rootIntent: Intent?)
    
    private fun startForegroundService()
    private fun startRecording(audioEnabled: Boolean, quality: Quality)
    private fun stopRecordingAndService()
    private fun startTimer()
    private fun stopTimer()
    private fun updateNotification()
    private fun formatElapsedTime(totalSeconds: Int): String
    private fun startStorageMonitoring()
    private fun hasEnoughStorage(): Boolean
}
```

### NotificationHelper

```kotlin
object NotificationHelper {
    const val CHANNEL_ID = "recording_channel"
    const val CHANNEL_NAME = "Recording"
    
    fun createNotificationChannel(context: Context)
    fun buildRecordingNotification(
        context: Context,
        elapsedTime: String
    ): Notification
    
    private fun getStopPendingIntent(context: Context): PendingIntent
    private fun getOpenAppPendingIntent(context: Context): PendingIntent
}
```

### Camera Service (Existing - No Changes Required)

The existing Camera class using CameraX remains unchanged and continues to handle:
- Foreground preview display
- UI-bound camera operations
- Photo capture
- Dual camera mode (if using CameraX for that)

Background recording will bypass the Camera class entirely and use BackgroundCameraManager.

## Data Models

### Recording State

```kotlin
data class RecordingState(
    val isRecording: Boolean,
    val startTime: Long,
    val elapsedSeconds: Int,
    val outputFile: File?
)
```

### Camera Configuration

```kotlin
data class CameraConfiguration(
    val audioEnabled: Boolean,
    val videoQuality: Quality,
    val cameraId: String = "0" // Back camera by default
)
```

### Recording Error Types

```kotlin
enum class RecordingError {
    CAMERA_ACCESS_DENIED,
    CAMERA_IN_USE,
    STORAGE_INSUFFICIENT,
    MEDIA_RECORDER_FAILED,
    CAMERA_DISCONNECTED,
    UNKNOWN
}
```

## Implementation Details

### Camera2 API Recording Flow

1. **Service Starts**: RecordingService receives ACTION_START_RECORDING intent
2. **Camera Initialization**: BackgroundCameraManager opens camera using CameraManager
3. **MediaRecorder Setup**: Configure MediaRecorder with output file, quality, audio settings
4. **Capture Session**: Create CameraCaptureSession with MediaRecorder surface
5. **Recording Starts**: MediaRecorder.start() begins encoding video
6. **Notification Updates**: Timer updates notification every second
7. **Storage Monitoring**: Check available storage every 10 seconds
8. **Recording Stops**: MediaRecorder.stop(), release resources, finalize file
9. **Service Stops**: Remove notification, stop foreground service

### Camera2 vs CameraX Trade-offs

**Why Camera2 for Background Recording:**
- Independent of Activity lifecycle
- Direct control over camera resources
- Works reliably in foreground services
- No automatic resource management that conflicts with background operation

**Why Keep CameraX for Foreground:**
- Simpler API for preview and UI
- Automatic lifecycle management (beneficial when in foreground)
- Better integration with UI components
- Existing code already works well

### MediaRecorder Configuration

```kotlin
private fun setupMediaRecorder(outputFile: File, audioEnabled: Boolean, quality: Quality) {
    mediaRecorder = MediaRecorder().apply {
        if (audioEnabled) {
            setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(outputFile.absolutePath)
        
        // Quality settings based on Quality enum
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
        }
        
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        if (audioEnabled) {
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44100)
        }
        
        setVideoFrameRate(30)
        
        prepare()
    }
}
```

### Thread Safety

- BackgroundCameraManager operations run on background thread (CameraManager callbacks)
- UI updates (notifications) posted to main thread via Handler
- Synchronization on recording state changes
- Proper cleanup in onDestroy() even if called from different thread

## Error Handling

### Camera Access Errors

1. **Camera In Use**: Another app has camera access
   - Stop recording attempt
   - Show notification: "Camera unavailable"
   - Stop service gracefully

2. **Camera Disconnected**: Camera disconnected during recording
   - Save current recording
   - Show notification: "Recording stopped - camera disconnected"
   - Stop service

3. **Permission Denied**: Camera permission revoked during recording
   - Save current recording
   - Show notification: "Recording stopped - permission denied"
   - Stop service

### Storage Errors

1. **Low Storage Before Start**: Not enough space to begin recording
   - Don't start recording
   - Show toast: "Insufficient storage space"
   - Don't start service

2. **Low Storage During Recording**: Storage drops below threshold
   - Stop recording immediately
   - Save current video
   - Show notification: "Recording stopped - low storage"
   - Stop service

### MediaRecorder Errors

1. **Preparation Failed**: MediaRecorder.prepare() throws exception
   - Release camera resources
   - Show notification: "Recording failed to start"
   - Stop service

2. **Recording Failed**: Error during recording
   - Attempt to save partial recording
   - Show notification: "Recording error occurred"
   - Stop service

### Service Lifecycle Errors

1. **Task Removed**: User swipes away app from recents
   - Save current recording in onTaskRemoved()
   - Clean up resources
   - Stop service

2. **System Kills Service**: Low memory or system pressure
   - Android will call onDestroy()
   - Attempt to save recording
   - Release all resources

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Recording persistence across app state transitions
*For any* recording session, when the app is moved to the background or another app is opened, the recording should continue without interruption
**Validates: Requirements 1.1, 1.5, 8.2**

### Property 2: Camera resource independence
*For any* background recording session, the camera resources should be managed independently of the Activity lifecycle
**Validates: Requirements 1.2**

### Property 3: Recording settings preservation
*For any* recording session started with specific settings (audio, quality), those settings should be maintained throughout the entire recording regardless of app state
**Validates: Requirements 1.3**

### Property 4: Recording continues when screen off
*For any* active recording session, turning off the device screen should not stop or interrupt the recording
**Validates: Requirements 1.4**

### Property 5: Notification creation on recording start
*For any* recording session, when recording starts, a persistent notification should be displayed immediately
**Validates: Requirements 2.1**

### Property 6: Notification displays required information
*For any* recording notification, it should display the app icon, recording status text, and elapsed time
**Validates: Requirements 2.2, 2.3, 7.1**

### Property 7: Notification is non-dismissible during recording
*For any* active recording session, the notification should not be dismissible by the user
**Validates: Requirements 2.4**

### Property 8: Notification removed when recording stops
*For any* recording session, when recording stops (by any means), the notification should be removed
**Validates: Requirements 2.5, 3.4**

### Property 9: Stop action button present in notification
*For any* recording notification, it should include a "Stop" action button
**Validates: Requirements 3.1**

### Property 10: Stop button stops recording immediately
*For any* recording session, tapping the stop button in the notification should immediately stop the recording
**Validates: Requirements 3.2**

### Property 11: Video saved when stopped via notification
*For any* recording stopped via notification button, the video file should be finalized and saved
**Validates: Requirements 3.3**

### Property 12: Camera resources released on stop
*For any* recording session that stops, all camera resources (CameraDevice, CaptureSession, MediaRecorder) should be released
**Validates: Requirements 3.5**

### Property 13: Notification tap brings app to foreground
*For any* recording notification, tapping the notification body should bring the app to the foreground
**Validates: Requirements 4.1, 4.2**

### Property 14: Recording persists when returning to foreground
*For any* recording session, when the app returns to foreground via notification tap, the recording should still be active
**Validates: Requirements 4.3**

### Property 15: UI reflects recording state on foreground return
*For any* recording session, when the app returns to foreground, the UI should display the current recording state and elapsed time
**Validates: Requirements 4.4**

### Property 16: Graceful shutdown saves video
*For any* recording session, if the service is terminated by the system, the current video should be saved before shutdown
**Validates: Requirements 5.3**

### Property 17: Foreground service created on recording start
*For any* recording session, when recording starts, a foreground service should be created
**Validates: Requirements 6.1**

### Property 18: Notification bound to foreground service
*For any* foreground service running, the notification should be bound to the service via startForeground()
**Validates: Requirements 6.2**

### Property 19: Foreground service stopped when recording stops
*For any* recording session, when recording stops, the foreground service should be stopped
**Validates: Requirements 6.3**

### Property 20: Foreground service maintains high priority
*For any* active foreground service, it should maintain high process priority to prevent system termination
**Validates: Requirements 6.4**

### Property 21: Foreground service permissions requested
*For any* device running Android 9+, the app should request foreground service permissions as required
**Validates: Requirements 6.5**

### Property 22: Time formatted correctly
*For any* elapsed time value, it should be formatted as MM:SS when under one hour, and HH:MM:SS when one hour or more
**Validates: Requirements 7.3, 7.5**

### Property 23: Silent notification updates
*For any* notification update for elapsed time, it should not produce sound or vibration
**Validates: Requirements 7.4**

### Property 24: Back button backgrounds app during recording
*For any* recording session, pressing the back button should move the app to background without stopping recording
**Validates: Requirements 8.1, 8.3**

### Property 25: Camera2 API used for background recording
*For any* background recording session, the system should use Camera2 API (not CameraX) to access camera hardware
**Validates: Requirements 9.1**

### Property 26: Camera lifecycle independence
*For any* Camera2-based recording, the CameraDevice lifecycle should be managed independently of the Activity lifecycle
**Validates: Requirements 9.2**

### Property 27: MediaRecorder integration
*For any* background recording, MediaRecorder should be used for video encoding with the Camera2 surface
**Validates: Requirements 9.3**

### Property 28: Camera access maintained in service
*For any* foreground service with active recording, camera access should be maintained regardless of whether the Activity is in foreground or background
**Validates: Requirements 9.4**

## Testing Strategy

### Unit Testing

Unit tests will focus on specific components and their behavior:

1. **NotificationHelper Tests**
   - Notification channel creation
   - Notification content (icon, text, buttons)
   - PendingIntent creation
   - Notification flags (ongoing, non-dismissible)

2. **Time Formatting Tests**
   - Format seconds as MM:SS
   - Format seconds as HH:MM:SS when >= 1 hour
   - Edge cases (0 seconds, 59 seconds, 3599 seconds, 3600 seconds)

3. **Storage Monitoring Tests**
   - Storage check logic
   - Threshold calculations
   - Mock StatFs for testing

4. **Recording State Tests**
   - State transitions (not recording -> recording -> stopped)
   - State persistence across configuration changes

### Integration Testing

Integration tests will verify component interactions:

1. **Service Lifecycle Tests**
   - Service starts when ACTION_START_RECORDING received
   - Service stops when ACTION_STOP_RECORDING received
   - Service handles onTaskRemoved correctly
   - Service handles onDestroy correctly

2. **Camera Integration Tests**
   - BackgroundCameraManager opens camera successfully
   - MediaRecorder configured correctly
   - Recording starts and stops properly
   - Resources released after recording

3. **Notification Integration Tests**
   - Notification appears when service starts
   - Notification updates with elapsed time
   - Stop button triggers ACTION_STOP_RECORDING
   - Notification tap opens MainActivity

### Manual Testing

Manual tests for scenarios difficult to automate:

1. **Background Recording Scenarios**
   - Start recording, press home button, verify recording continues
   - Start recording, switch to another app, verify recording continues
   - Start recording, turn off screen, verify recording continues
   - Start recording, swipe away app from recents, verify video is saved

2. **Notification Interaction**
   - Tap stop button, verify recording stops and video is saved
   - Tap notification body, verify app opens to recording screen
   - Verify notification shows correct elapsed time
   - Verify notification cannot be dismissed during recording

3. **Error Scenarios**
   - Start recording with low storage, verify graceful handling
   - Revoke camera permission during recording, verify graceful handling
   - Receive phone call during recording, verify graceful handling
   - Low battery during recording, verify graceful handling

4. **State Transitions**
   - Start recording in foreground, background app, return to foreground
   - Verify UI state matches recording state
   - Verify elapsed time displayed correctly

### Property-Based Testing

Property-based tests will use a PBT library (e.g., Kotest Property Testing) to verify correctness properties:

- Each property test will run a minimum of 100 iterations
- Tests will generate random inputs (recording durations, quality settings, etc.)
- Tests will verify universal properties hold across all inputs
- Each test will be tagged with the property number from the design document

**Example Property Test Structure:**

```kotlin
class RecordingPropertiesTest : StringSpec({
    "Property 22: Time formatted correctly" {
        checkAll(Arb.int(0..86400)) { seconds ->
            val formatted = formatElapsedTime(seconds)
            if (seconds < 3600) {
                formatted shouldMatch Regex("\\d{2}:\\d{2}")
            } else {
                formatted shouldMatch Regex("\\d{2}:\\d{2}:\\d{2}")
            }
        }
    }
})
```

### Test Coverage Goals

- Unit test coverage: 80%+ for utility classes
- Integration test coverage: Key user flows covered
- Property tests: All 24 correctness properties implemented
- Manual tests: All critical scenarios verified before release

