# Design Document: On-Demand Permission Requests

## Overview

This design implements an on-demand permission request system that prompts users for permissions when they interact with specific features, rather than only at app startup. The solution enhances the existing PermissionHandler to support feature-specific permission checks and modifies MainActivity to request permissions contextually based on user actions.

## Architecture

### Current State

Currently, the app:
1. Requests all permissions at startup in `MainActivity.onCreate()`
2. If permissions are denied, the app continues but features don't work
3. No mechanism exists to re-request permissions after initial denial
4. Button clicks don't check or request permissions before attempting operations

### Proposed Changes

The new architecture will:
1. Keep the startup permission check for better UX (camera preview ready immediately)
2. Add permission checks before each feature operation (Record, Gallery)
3. Request only the permissions needed for each specific feature
4. Handle permission results and proceed with the operation or show appropriate feedback

## Components and Interfaces

### 1. Enhanced PermissionHandler

**Location:** `app/src/main/java/com/kasahirotech/dashcamapp/service/PermissionHandler.kt`

**New Methods:**

```kotlin
// Check if specific permissions are granted
fun arePermissionsGranted(permissions: Array<String>): Boolean

// Get permissions required for recording
fun getRecordingPermissions(): Array<String>

// Get permissions required for gallery access
fun getGalleryPermissions(): Array<String>
```

**Implementation Details:**
- `arePermissionsGranted()`: Accepts an array of permissions and returns true only if all are granted
- `getRecordingPermissions()`: Returns `[CAMERA, RECORD_AUDIO]` for all API levels
- `getGalleryPermissions()`: Returns `[READ_MEDIA_VIDEO]` for API 33+, empty array for API 29-32
- Maintains existing `allPermissionsGranted()` method for backward compatibility

### 2. Modified MainActivity

**Location:** `app/src/main/java/com/kasahirotech/dashcamapp/MainActivity.kt`

**Changes:**

1. **Permission Request Callback Enhancement:**
   - Track which feature triggered the permission request
   - Route to appropriate action after permissions are granted
   - Show feature-specific error messages on denial

2. **Record Button Click Handler:**
   - Check if recording permissions are granted before calling `camera.captureVideo()`
   - If not granted, request recording permissions
   - After grant, initialize camera if needed and start recording

3. **Gallery Button Click Handler:**
   - Check if gallery permissions are granted (API 33+ only)
   - If not granted, request gallery permissions
   - After grant (or if no permissions needed), open Gallery activity

**New State Management:**
- Add enum or sealed class to track pending action (RECORD, GALLERY, NONE)
- Set pending action before requesting permissions
- Execute pending action in permission callback

### 3. Camera Service Integration

**Location:** `app/src/main/java/com/kasahirotech/dashcamapp/service/Camera.kt`

**No Changes Required:**
- Camera service already handles missing permissions gracefully
- `captureVideo()` checks for RECORD_AUDIO permission before enabling audio
- Camera initialization will be called from MainActivity after permissions are granted

## Data Models

### Permission Request Context

```kotlin
enum class PermissionRequestContext {
    STARTUP,    // Initial app startup permission request
    RECORD,     // User clicked record button
    GALLERY,    // User clicked gallery button
    NONE        // No pending permission request
}
```

This enum tracks why permissions were requested, allowing the callback to take appropriate action.

## Error Handling

### Permission Denial Scenarios

1. **Record Button - Permissions Denied:**
   - Show Toast: "Camera and microphone permissions are required to record videos"
   - Do not attempt to start recording
   - Do not initialize camera

2. **Gallery Button - Permissions Denied (API 33+ only):**
   - Show Toast: "Media access permission is required to view videos"
   - Do not open Gallery activity

3. **Startup Permissions Denied:**
   - Show existing Toast: "Permission request denied"
   - Do not initialize camera
   - Allow user to trigger permission requests via button clicks

### Edge Cases

1. **Partial Permission Grant:**
   - If user grants CAMERA but denies RECORD_AUDIO for recording
   - Treat as full denial (both are required for recording)

2. **Camera Already Initialized:**
   - Before calling `camera.startCamera()`, check if camera is already running
   - Avoid redundant initialization

3. **Permission Request Already in Progress:**
   - Android system prevents multiple simultaneous permission requests
   - No additional handling needed

## Testing Strategy

### Unit Tests

Not applicable for this feature as it involves Android runtime permissions and UI interactions.

### Manual Testing Scenarios

1. **Fresh Install - Grant All Permissions:**
   - Install app
   - Grant all permissions at startup
   - Verify camera preview starts
   - Verify record button works
   - Verify gallery button works

2. **Fresh Install - Deny All Permissions:**
   - Install app
   - Deny all permissions at startup
   - Verify camera preview doesn't start
   - Click record button → verify permission request appears
   - Grant permissions → verify recording starts
   - Click gallery button → verify permission request appears (API 33+) or gallery opens (API 29-32)

3. **Partial Permission Grant:**
   - Deny permissions at startup
   - Click record button
   - Grant only CAMERA, deny RECORD_AUDIO
   - Verify error message appears
   - Click record button again
   - Grant both permissions
   - Verify recording starts

4. **API Level Variations:**
   - Test on Android 10-12 device: Gallery should work without permission request
   - Test on Android 13+ device: Gallery should request READ_MEDIA_VIDEO permission

5. **Permission Revocation:**
   - Grant all permissions
   - Use app successfully
   - Revoke permissions via system settings
   - Return to app
   - Click record/gallery buttons
   - Verify permission requests appear again

## Implementation Notes

### Permission Request Flow

```
User clicks Record button
    ↓
Check if CAMERA + RECORD_AUDIO granted
    ↓
    ├─ Yes → Start recording
    │
    └─ No → Set context = RECORD
            Request permissions
                ↓
            Permission callback
                ↓
                ├─ Granted → Initialize camera if needed
                │            Start recording
                │
                └─ Denied → Show error toast
```

### Backward Compatibility

- Existing startup permission check remains functional
- `allPermissionsGranted()` method unchanged
- Camera service requires no modifications
- Gallery activity requires no modifications

### Android API Considerations

- **API 29-32:** Gallery access uses Scoped Storage, no permissions needed
- **API 33+:** Gallery access requires READ_MEDIA_VIDEO permission
- Recording permissions (CAMERA, RECORD_AUDIO) required on all API levels

## Dependencies

- Existing AndroidX Activity Result API (already in use)
- Existing PermissionHandler class
- Existing Camera service
- No new dependencies required
