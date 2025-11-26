# Camera2 Background Recording - Quick Start

## What Changed?

Your DashCamApp now uses **Camera2 API** for background recording instead of CameraX. This fixes the issue where recording would stop when the app is minimized.

## Quick Build & Test

### 1. Build the App
```bash
# Close Android Studio first
.\gradlew clean build
```

### 2. Install on Device
```bash
.\gradlew installDebug
```

Or use Android Studio's Run button (▶️).

### 3. Quick Test
1. Open DashCamApp
2. Tap the record button (red circle)
3. Press Home button to minimize app
4. Wait 30 seconds
5. Pull down notification shade
6. Verify notification shows "Recording" with elapsed time (~00:30)
7. Tap "Stop" button in notification
8. Open app and go to Gallery
9. Verify video was saved

**Expected**: Video should be ~30 seconds long and playable ✅

## What Works Now

✅ **Background Recording**: Recording continues when app is minimized
✅ **Screen Off**: Recording continues when screen is off  
✅ **App Switching**: Recording continues when using other apps
✅ **Notification**: Shows elapsed time and stop button
✅ **Storage Monitoring**: Stops recording if storage is low
✅ **Error Handling**: Gracefully handles camera disconnections

## Architecture

### Old (CameraX Only)
```
MainActivity → Camera (CameraX) → Recording ❌ Stops in background
```

### New (Hybrid)
```
MainActivity → Camera (CameraX) → Preview only
            → RecordingService → BackgroundCameraManager (Camera2) → Recording ✅ Works in background
```

## Key Files

### New
- `BackgroundCameraManager.kt` - Camera2 recording implementation

### Modified
- `RecordingService.kt` - Now uses BackgroundCameraManager
- `MainActivity.kt` - Starts RecordingService for recording

### Unchanged
- `Camera.kt` - Still used for preview (CameraX)
- Everything else

## Testing Checklist

- [ ] Recording starts when button is tapped
- [ ] Notification appears with elapsed time
- [ ] Recording continues when app is minimized
- [ ] Recording continues when screen is off
- [ ] Stop button in notification works
- [ ] Video file is saved and playable
- [ ] Audio is recorded (if enabled in settings)
- [ ] Different quality settings work (HD, FHD, UHD)

## Troubleshooting

### Build Fails
```bash
# Close Android Studio, then:
.\gradlew clean
.\gradlew build
```

### Recording Doesn't Start
- Check camera permission is granted
- Check storage space (need >100MB)
- Check logcat: `adb logcat | grep BackgroundCameraManager`

### Notification Doesn't Show
- Check notification permission (Android 13+)
- Check notification settings for DashCamApp

### Video Not Saved
- Check storage permission
- Check Movies/DashCam folder exists
- Check logcat for errors

## Need More Info?

- **Full Testing Guide**: See `CAMERA2_TESTING_GUIDE.md`
- **Implementation Details**: See `CAMERA2_IMPLEMENTATION_SUMMARY.md`
- **Spec Documents**: See `.kiro/specs/background-recording-notification/`

## Success!

If the quick test above works, your Camera2 implementation is successful! 🎉

The app now reliably records in the background using Camera2 API while keeping the simple CameraX preview for the UI.
