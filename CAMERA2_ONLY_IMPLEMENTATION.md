# Camera2 Preview During Recording

## Overview
Implemented **Camera2 preview** that displays during recording, solving the blank screen issue when recording starts.

## Problem
When recording starts:
- CameraX preview is stopped to release the camera
- Camera2 in RecordingService takes over for recording
- The preview goes blank because no preview is shown during recording

## Solution
Use **dual preview approach**:
- **CameraX** for normal preview (when not recording)
- **Camera2 SurfaceView** for preview during recording
- Switch between them seamlessly when recording starts/stops

## Changes Made

### 1. MainActivity.kt
**Added Camera2 preview components:**
- `previewCameraDevice: CameraDevice?` - Camera device for preview during recording
- `previewCaptureSession: CameraCaptureSession?` - Capture session for preview
- `previewBackgroundThread: HandlerThread?` - Background thread for camera operations
- `previewBackgroundHandler: Handler?` - Handler for background operations

**New methods:**
- `setupCamera2PreviewSurface()` - Sets up SurfaceView callbacks for Camera2 preview
- `startCamera2Preview(surface: Surface)` - Opens Camera2 and starts preview during recording
- `createCamera2PreviewSession(surface: Surface)` - Creates capture session for preview
- `stopCamera2Preview()` - Stops Camera2 preview and releases resources
- `startPreviewBackgroundThread()` - Starts background thread for Camera2
- `stopPreviewBackgroundThread()` - Stops background thread

**Updated methods:**
- `onCreate()` - Calls `setupCamera2PreviewSurface()` to initialize SurfaceView
- `initializeCameraMode()` - Hides Camera2 preview when showing CameraX
- `startBackgroundRecording()` - Shows Camera2 SurfaceView and starts preview after 1 second
- `stopBackgroundRecording()` - Stops Camera2 preview and switches back to CameraX
- `onPause()` - Stops Camera2 preview if recording (recording continues in service)
- `onResume()` - Shows Camera2 preview if recording, otherwise shows CameraX
- `onDestroy()` - Cleans up Camera2 resources

### 2. BackgroundCameraManager.kt
**No changes needed** - Recording service continues to work independently without preview.

### 3. activity_main.xml
**Already has both views:**
- `PreviewView` (id: viewFinder) - Used for CameraX preview when not recording
- `SurfaceView` (id: camera2Preview) - Used for Camera2 preview during recording
- Views are toggled based on recording state

## Benefits

1. **Preview During Recording**: Camera2 preview shows while recording is active
2. **No Camera Conflicts**: Recording and preview use separate camera instances
3. **Background Recording**: Recording continues when app is in background
4. **Seamless Switching**: Smooth transition between CameraX and Camera2 preview
5. **Best of Both**: CameraX for ease of use, Camera2 for recording control

## How It Works

### Normal Preview (Not Recording)
1. CameraX preview shows in PreviewView
2. SurfaceView is hidden
3. User sees normal camera feed

### Starting Recording
1. User taps record button
2. CameraX preview is stopped and hidden
3. SurfaceView becomes visible
4. RecordingService starts Camera2 recording
5. After 1 second, MainActivity opens separate Camera2 instance for preview
6. Preview displays in SurfaceView while recording continues

### During Recording
- RecordingService: Camera2 recording to file
- MainActivity: Separate Camera2 preview in SurfaceView
- Both use different camera instances (no conflict)

### Stopping Recording
1. User taps stop button
2. RecordingService stops recording
3. MainActivity stops Camera2 preview
4. SurfaceView is hidden
5. CameraX preview restarts in PreviewView
6. User sees normal camera feed again

## Testing Notes

To test the implementation:
1. Close Android Studio to release file locks
2. Run `.\gradlew clean assembleDebug`
3. Install on device: `.\gradlew installDebug`
4. Test preview appears on app launch
5. Test recording starts and preview remains visible
6. Test recording continues when app goes to background
7. Test preview resumes after stopping recording

## Technical Details

### Camera2 Preview During Recording
```kotlin
// SurfaceView callback
surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        if (isRecording) {
            startCamera2Preview(holder.surface)
        }
    }
})

// Open camera for preview
cameraManager.openCamera("0", stateCallback, backgroundHandler)

// Create preview session
cameraDevice.createCaptureSession(listOf(surface), sessionCallback, handler)

// Start preview
val captureRequest = cameraDevice.createCaptureRequest(TEMPLATE_PREVIEW)
captureRequest.addTarget(surface)
session.setRepeatingRequest(captureRequest.build(), null, handler)
```

### Camera2 Recording (Separate Instance)
```kotlin
// In RecordingService - completely independent
mediaRecorder.setVideoSource(VideoSource.SURFACE)
mediaRecorder.prepare()

val recorderSurface = mediaRecorder.surface
cameraDevice.createCaptureSession(listOf(recorderSurface), sessionCallback, handler)

val captureRequest = cameraDevice.createCaptureRequest(TEMPLATE_RECORD)
captureRequest.addTarget(recorderSurface)
session.setRepeatingRequest(captureRequest.build(), null, handler)
mediaRecorder.start()
```

## Key Points

1. **Two Camera Instances**: Recording and preview use separate Camera2 instances
2. **No Conflicts**: Android allows multiple camera instances on modern devices
3. **View Switching**: PreviewView (CameraX) ↔ SurfaceView (Camera2) based on state
4. **1 Second Delay**: Gives recording time to start before preview opens
5. **Lifecycle Management**: Preview stops on pause, restarts on resume

## Future Enhancements

1. **Single Camera Session**: Use one Camera2 session with multiple surfaces (recording + preview)
2. **Dual Camera with Camera2**: Implement dual camera mode using Camera2 instead of CameraX
3. **Camera2 Extensions**: Use Camera2 extensions for HDR, night mode, etc.
4. **Smoother Transitions**: Reduce delay between recording start and preview
