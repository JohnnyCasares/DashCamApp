# Camera2 Background Recording - Testing Guide

## Overview
This guide provides step-by-step instructions for testing the new Camera2-based background recording implementation in DashCamApp.

## Prerequisites
1. Android device or emulator running Android 10+ (API 29+)
2. Camera and microphone permissions granted
3. Sufficient storage space (at least 500MB free)
4. Notification permissions granted (Android 13+)

## Build and Install

### Step 1: Clean Build
```bash
# Close Android Studio first to release file locks
.\gradlew clean build
```

### Step 2: Install on Device
```bash
.\gradlew installDebug
```

Or use Android Studio's Run button.

## Test Scenarios

### Test 1: Basic Background Recording
**Objective**: Verify recording continues when app is minimized

**Steps**:
1. Launch DashCamApp
2. Tap the record button
3. Verify notification appears with "Recording" text and elapsed time
4. Press the Home button to minimize the app
5. Wait 30 seconds
6. Open the notification shade
7. Verify elapsed time is updating (should show ~00:30)
8. Tap "Stop" button in notification
9. Open the app and go to Gallery
10. Verify the video was saved and is approximately 30 seconds long

**Expected Results**:
- ✅ Recording continues in background
- ✅ Notification shows and updates every second
- ✅ Video file is created and playable
- ✅ Video duration matches recording time

### Test 2: Screen Off Recording
**Objective**: Verify recording continues when screen is turned off

**Steps**:
1. Launch DashCamApp
2. Tap the record button
3. Verify notification appears
4. Press the power button to turn off the screen
5. Wait 30 seconds
6. Turn screen back on
7. Open notification shade
8. Verify elapsed time shows ~00:30
9. Tap "Stop" button
10. Verify video was saved

**Expected Results**:
- ✅ Recording continues with screen off
- ✅ Video file is created successfully
- ✅ No battery drain warnings

### Test 3: App Switching
**Objective**: Verify recording continues when switching to other apps

**Steps**:
1. Launch DashCamApp
2. Tap the record button
3. Open another app (e.g., Chrome, Messages)
4. Use the other app for 30 seconds
5. Open notification shade
6. Verify recording notification is still present
7. Tap notification body to return to DashCamApp
8. Verify app shows recording is active
9. Tap record button to stop recording
10. Verify video was saved

**Expected Results**:
- ✅ Recording continues while using other apps
- ✅ Tapping notification returns to DashCamApp
- ✅ UI reflects recording state correctly
- ✅ Video file is created

### Test 4: Notification Stop Button
**Objective**: Verify stop button in notification works correctly

**Steps**:
1. Launch DashCamApp
2. Tap the record button
3. Minimize the app
4. Wait 10 seconds
5. Open notification shade
6. Tap "Stop" button in notification
7. Verify notification disappears
8. Open DashCamApp
9. Verify recording has stopped
10. Go to Gallery and verify video exists

**Expected Results**:
- ✅ Stop button stops recording immediately
- ✅ Notification is removed
- ✅ Video file is saved and finalized
- ✅ Camera resources are released

### Test 5: Task Removal (Swipe Away)
**Objective**: Verify recording is saved when app is swiped away from recents

**Steps**:
1. Launch DashCamApp
2. Tap the record button
3. Press Home button
4. Open recent apps (square button or swipe up)
5. Swipe DashCamApp away to close it
6. Wait 5 seconds
7. Reopen DashCamApp
8. Go to Gallery
9. Verify video was saved

**Expected Results**:
- ✅ Recording is saved before app closes
- ✅ Video file is finalized and playable
- ✅ No corrupted video files

### Test 6: Low Storage Handling
**Objective**: Verify graceful handling when storage is low

**Steps**:
1. Fill device storage to less than 100MB free
2. Launch DashCamApp
3. Tap the record button
4. Observe behavior

**Expected Results**:
- ✅ Toast message: "Insufficient storage space"
- ✅ Recording does not start
- ✅ No crash or error

### Test 7: Audio Recording
**Objective**: Verify audio is recorded when enabled

**Steps**:
1. Go to Settings
2. Enable "Record Audio"
3. Return to main screen
4. Tap record button
5. Speak or make noise near the device
6. Record for 10 seconds
7. Stop recording
8. Go to Gallery and play the video
9. Verify audio is present

**Expected Results**:
- ✅ Audio is recorded in video
- ✅ Audio quality is clear
- ✅ Audio is synchronized with video

### Test 8: Video Quality Settings
**Objective**: Verify different quality settings work

**Steps**:
1. Go to Settings
2. Set video quality to HD (720p)
3. Record a 10-second video
4. Stop and check file size
5. Repeat for FHD (1080p) and UHD (4K)
6. Compare file sizes

**Expected Results**:
- ✅ HD video: ~10-15MB for 10 seconds
- ✅ FHD video: ~15-20MB for 10 seconds
- ✅ UHD video: ~25-35MB for 10 seconds
- ✅ All videos are playable

### Test 9: Camera Permission Revocation
**Objective**: Verify graceful handling when camera permission is revoked during recording

**Steps**:
1. Start recording
2. Minimize app
3. Go to Settings > Apps > DashCamApp > Permissions
4. Revoke Camera permission
5. Check notification shade
6. Verify error notification appears
7. Reopen app

**Expected Results**:
- ✅ Recording stops gracefully
- ✅ Error notification: "Recording stopped - permission denied"
- ✅ Partial video is saved (if possible)
- ✅ No app crash

### Test 10: Long Recording Session
**Objective**: Verify stability during extended recording

**Steps**:
1. Start recording
2. Minimize app
3. Let record for 5+ minutes
4. Periodically check notification for time updates
5. Stop recording via notification
6. Verify video file

**Expected Results**:
- ✅ Recording continues for full duration
- ✅ Time format changes to HH:MM:SS after 1 hour
- ✅ Video file is complete and playable
- ✅ No memory leaks or performance issues

## Verification Checklist

After completing all tests, verify:

- [ ] All videos are saved to Movies/DashCam directory
- [ ] Videos appear in Gallery app
- [ ] Videos are playable in default video player
- [ ] No corrupted video files
- [ ] App doesn't crash during any scenario
- [ ] Battery usage is reasonable
- [ ] No memory leaks (check in Android Studio Profiler)
- [ ] Notification always shows during recording
- [ ] Notification is removed when recording stops

## Known Limitations

1. **CameraX Preview**: The app still uses CameraX for the preview display in the foreground. Only the actual recording uses Camera2 API.

2. **Dual Camera Mode**: The current implementation focuses on single camera (back camera) recording. Dual camera mode may need additional work.

3. **Build Lock Issue**: On Windows, you may need to close Android Studio before running `gradlew clean` due to file locking.

## Troubleshooting

### Recording Doesn't Start
- Check camera permissions are granted
- Verify sufficient storage space (>100MB)
- Check logcat for error messages: `adb logcat | grep BackgroundCameraManager`

### Notification Doesn't Appear
- Check notification permissions (Android 13+)
- Verify foreground service permission is granted
- Check notification channel settings

### Video File Not Saved
- Check storage permissions
- Verify Movies/DashCam directory exists
- Check logcat for MediaRecorder errors

### App Crashes
- Check logcat for stack traces
- Verify all permissions are granted
- Try clean build: `.\gradlew clean build`

## Logcat Monitoring

To monitor the Camera2 implementation:

```bash
# Filter for relevant logs
adb logcat | grep -E "BackgroundCameraManager|RecordingService|MainActivity"

# Or on Windows PowerShell
adb logcat | Select-String "BackgroundCameraManager|RecordingService|MainActivity"
```

## Success Criteria

The implementation is successful if:
1. ✅ All 10 test scenarios pass
2. ✅ No crashes or ANRs
3. ✅ Videos are saved correctly in all scenarios
4. ✅ Background recording works reliably
5. ✅ Notification functions properly
6. ✅ Camera resources are properly released

## Next Steps

After successful testing:
1. Consider adding property-based tests (optional tasks marked with *)
2. Test on multiple devices with different Android versions
3. Perform stress testing with very long recordings
4. Test with low battery scenarios
5. Test with incoming phone calls during recording
