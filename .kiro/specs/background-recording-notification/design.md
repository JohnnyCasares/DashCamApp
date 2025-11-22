# Design Document

## Overview

This design implements background recording functionality for the DashCamApp using Android's Foreground Service architecture. When a user starts recording and minimizes the app, a persistent notification will be displayed showing the recording status, elapsed time, and controls to stop recording or return to the app. The foreground service ensures the recording continues uninterrupted even when the app is not visible.

The implementation follows Android best practices for long-running operations and integrates seamlessly with the existing Camera service architecture. The design maintains separation of concerns by creating a dedicated RecordingService that manages the lifecycle of background recording while coordinating with the existing Camera class.

## Architecture

### Component Overview

```
MainActivity
    ├── RecordingService (Foreground Service)
    │   ├── NotificationManager
    │   ├── Camera (recording operations)
    │   └── Timer (elapsed time tracking)
    └── Camera (existing service)
```

### Key Components

1. **RecordingService**: A foreground service that manages background recording
   - Extends Android Service class
   - Manages notification lifecycle
   - Coordinates with Camera service for recording operations
   - Tracks recording duration
   - Handles stop recording commands from notification

2. **NotificationBuilder**: Utility for creating and updating notifications
   - Creates notification channel for Android O+
   - Builds notification with recording status
   - Updates elapsed time display
   - Adds action buttons (Stop, Open App)

3. **Camera Service Integration**: Modified to support service-based recording
   - Exposes recording state
   - Allows external control of recording lifecycle
   - Provides callbacks for recording events

4. **MainActivity Updates**: Modified to start/stop the service
   - Starts RecordingService when recording begins
   - Binds to service for state updates
   - Handles service lifecycle

## Components and Interfaces

### RecordingService

```kotlin
class RecordingService : Service() {
    companion object {
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "recording_channel"
    }
    
    private var camera: Camera? = null
    private var startTime: Long = 0
    private var timerHandler: Handler? = null
    private var notificationManager: NotificationManager? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    override fun onBind(intent: Intent?): IBinder?
    override fun onDestroy()
    
    private fun startForegroundService()
    private fun createNotificationChannel()
    private fun buildNotification(elapsedTime: String): Notification
    private fun updateNotification()
    private fun stopRecordingAndService()
}
```

### RecordingServiceInterface

```kotlin
interface RecordingServiceInterface {
    fun startRecording(context: Context, camera: Camera)
    fun stopRecording()
    fun isRecording(): Boolean
    fun getElapsedTime(): Long
}
```

### Camera Service Modifications

```kotlin
// Add to Camera class
interface RecordingStateListener {
    fun onRecordingStarted()
    fun onRecordingStopped()
    fun onRecordingError(error: String)
}

// Add to Camera class
private var recordingStateListener: RecordingStateListener? = null

fun setRecordingStateListener(listener: RecordingStateListener)
fun isRecording(): Boolean
fun stopRecording()
```

### NotificationHelper

```kotlin
object NotificationHelper {
    fun createNotificationChannel(context: Context)
    fun buildRecordingNotification(
        context: Context,
        elapsedTime: String
    ): Notification
    
    private fun getStopPendingIntent(context: Context): PendingIntent
    private fun getOpenAppPendingIntent(context: Context): PendingIntent
}
```

## Data Models

### Recording State

```kotlin
data class RecordingState(
    val isRecording: Boolean,
    val startTime: Long,
    val elapsedSeconds: Int
)
```

### Service Intent Extras

```kotlin
object ServiceExtras {
    const val EXTRA_AUDIO_ENABLED = "audio_enabled"
    const val EXTRA_DUAL_CAMERA_MODE = "dual_camera_mode"
    const val EXTRA_VIDEO_QUALITY = "video_quality"
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

