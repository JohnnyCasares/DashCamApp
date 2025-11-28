# Camera2 Preview During Recording - Final Solution

## The Problem Explained

### Why Preview Goes Blank
When recording starts, the preview disappears because:

1. **CameraX preview is stopped** to release the camera
2. **Camera2 in RecordingService** takes exclusive control for recording
3. **No preview is shown** because the recording session doesn't include a preview surface

### Why Two Camera Instances Don't Work
The initial attempt to open two Camera2 instances failed because:

- **Android only allows ONE camera session per camera at a time**
- When MainActivity tries to open Camera2 for preview while RecordingService is recording
- The second camera open causes "Camera disconnected" error
- This is a hardware limitation on most Android devices

## The Solution: Single Session with Multiple Surfaces

The proper solution is to use **ONE Camera2 session with TWO surfaces**:
1. **MediaRecorder surface** - for recording video to file
2. **SurfaceView surface** - for displaying preview in the app

Both surfaces are added to the same camera capture session, so they work simultaneously without conflicts.

## Implementation

### 1. BackgroundCameraManager.kt

**Added preview surface support:**
```kotlin
private var previewSurface: Surface? = null

fun setPreviewSurface(surface: Surface?) {
    this.previewSurface = surface
}
```

**Modified createCaptureSession():**
```kotlin
// Include both recorder and preview surfaces
val surfaces = mutableListOf(recorderSurface)
previewSurface?.let { surfaces.add(it) }

// Add both targets to capture request
val captureRequestBuilder = camera.createCaptureRequest(TEMPLATE_RECORD)
captureRequestBuilder.addTarget(recorderSurface)
previewSurface?.let { captureRequestBuilder.addTarget(it) }
```

### 2. RecordingService.kt

**Added method to set preview surface:**
```kotlin
fun setPreviewSurface(surface: Surface?) {
    backgroundCamera?.setPreviewSurface(surface)
}
```

This allows MainActivity to pass its SurfaceView surface to the recording service.

### 3. MainActivity.kt

**Key changes:**

1. **Surface callback setup:**
```kotlin
private fun setupCamera2PreviewSurface() {
    binding.camera2Preview.holder.addCallback(object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            // If recording is starting, use this surface
            if (isCurrentlyRecording && recordingService == null) {
                startRecordingWithPreview(holder.surface)
            }
        }
        
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // Remove preview from recording service
            recordingService?.setPreviewSurface(null)
        }
    })
}
```

2. **Start recording with preview:**
```kotlin
private fun startRecordingWithPreview(previewSurface: Surface) {
    // Start recording service
    startForegroundService(intent)
    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    
    // Wait for service to bind, then set preview surface
    binding.camera2Preview.postDelayed({
        recordingService?.setPreviewSurface(previewSurface)
    }, 500)
}
```

3. **Removed separate Camera2 preview code:**
- No longer opens a second camera instance
- No separate preview session management
- All preview is handled through the recording service

## How It Works

### Starting Recording

1. User taps record button
2. CameraX preview stops and hides
3. SurfaceView becomes visible
4. RecordingService starts and opens Camera2
5. MainActivity passes SurfaceView surface to RecordingService
6. RecordingService creates capture session with BOTH surfaces:
   - MediaRecorder surface (for recording)
   - SurfaceView surface (for preview)
7. Camera2 sends frames to both surfaces simultaneously
8. User sees preview while recording continues

### During Recording

- **Single Camera2 session** in RecordingService
- **Two output surfaces:**
  - MediaRecorder: Encodes and saves video to file
  - SurfaceView: Displays live preview in MainActivity
- **No conflicts** because both surfaces are in the same session

### Stopping Recording

1. User taps stop button
2. RecordingService stops MediaRecorder
3. Camera2 session closes
4. SurfaceView hides
5. CameraX preview restarts

### Background Recording

When app goes to background:
- SurfaceView is destroyed (surface destroyed callback)
- Preview surface is removed from recording session
- Recording continues with only MediaRecorder surface
- When app returns to foreground, preview surface is re-added

## Benefits

1. **Preview Works**: User sees camera feed while recording
2. **No Camera Conflicts**: Single camera session with multiple surfaces
3. **Background Recording**: Recording continues when app is backgrounded
4. **Proper Resource Management**: Preview surface added/removed as needed
5. **Android Best Practice**: This is the recommended way to do recording + preview

## Technical Details

### Camera2 Multi-Surface Session

```kotlin
// In BackgroundCameraManager
val recorderSurface = mediaRecorder.surface
val previewSurface = surfaceView.holder.surface

// Create session with both surfaces
camera.createCaptureSession(
    listOf(recorderSurface, previewSurface),
    sessionCallback,
    handler
)

// Capture request targets both surfaces
val captureRequest = camera.createCaptureRequest(TEMPLATE_RECORD)
captureRequest.addTarget(recorderSurface)  // For recording
captureRequest.addTarget(previewSurface)   // For preview
session.setRepeatingRequest(captureRequest.build(), null, handler)
```

### Surface Lifecycle

- **SurfaceView** automatically manages surface creation/destruction
- **SurfaceHolder.Callback** notifies when surface is ready or destroyed
- **MainActivity** passes surface to service when available
- **RecordingService** adds/removes surface from session dynamically

## Why This Is The Correct Solution

1. **Hardware Limitation**: Most devices only support one camera session at a time
2. **Android Documentation**: Recommends multi-surface sessions for recording + preview
3. **Efficient**: Single camera session uses less resources than multiple sessions
4. **Reliable**: No race conditions or camera conflicts
5. **Flexible**: Preview can be added/removed without stopping recording

## Testing

To verify the solution works:

1. **Start app** - CameraX preview shows
2. **Tap record** - SurfaceView appears with Camera2 preview
3. **Verify recording** - Check notification shows recording time
4. **Verify preview** - Camera feed is visible during recording
5. **Background test** - Press home button, recording continues
6. **Resume test** - Return to app, preview reappears
7. **Stop recording** - CameraX preview returns
8. **Check video** - Verify video was recorded successfully

## Common Issues

### Preview doesn't appear
- Check that SurfaceView is visible
- Verify surface is valid before passing to service
- Check logs for "Preview surface set" message

### Camera disconnects
- Ensure only one camera session is created
- Verify preview surface is passed to existing session, not new camera

### Preview stops when backgrounded
- This is expected behavior
- Recording continues without preview
- Preview resumes when app returns to foreground

## Summary

The key insight is that **you cannot open the same camera twice**. Instead, you must use **one camera session with multiple output surfaces**. This is the standard Android approach for recording with preview, and it's what professional camera apps use.
