/# Implementation Plan

- [x] 1. Enhance PermissionHandler with feature-specific permission methods





  - Add `arePermissionsGranted(permissions: Array<String>): Boolean` method to check if specific permissions are granted
  - Add `getRecordingPermissions(): Array<String>` method that returns `[CAMERA, RECORD_AUDIO]`
  - Add `getGalleryPermissions(): Array<String>` method that returns `[READ_MEDIA_VIDEO]` for API 33+ or empty array for API 29-32
  - Ensure existing `allPermissionsGranted()` method remains unchanged for backward compatibility
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 2. Add permission request context tracking to MainActivity





  - Create `PermissionRequestContext` enum with values: STARTUP, RECORD, GALLERY, NONE
  - Add private property `currentPermissionContext` to track the current permission request context
  - Initialize `currentPermissionContext` to NONE
  - _Requirements: 1.1, 2.1_

- [x] 3. Implement on-demand permission request for Record button





  - Modify Record button click listener to check if recording permissions are granted using `PermissionHandler.arePermissionsGranted()`
  - If permissions are not granted, set `currentPermissionContext` to RECORD and request recording permissions
  - If permissions are granted, proceed with `camera.captureVideo()`
  - _Requirements: 1.1, 1.2_

- [x] 4. Implement on-demand permission request for Gallery button





  - Modify Gallery button click listener to check if gallery permissions are granted (only for API 33+)
  - If permissions are not granted, set `currentPermissionContext` to GALLERY and request gallery permissions
  - If permissions are granted or no permissions needed (API 29-32), open Gallery activity
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 5. Update permission result callback to handle context-specific actions





  - Modify `activityResultLauncher` callback to check `currentPermissionContext`
  - When context is RECORD and permissions granted: initialize camera if not already started, then call `camera.captureVideo()`
  - When context is GALLERY and permissions granted: open Gallery activity
  - When permissions are denied: show context-specific error message
  - Reset `currentPermissionContext` to NONE after handling
  - _Requirements: 1.3, 1.4, 2.4, 2.5, 4.1, 4.2, 4.3_


- [x] 6. Update startup permission check to use new context tracking



  - Modify `permissionCheck()` to set `currentPermissionContext` to STARTUP before requesting permissions
  - Ensure camera initialization only happens when startup permissions are granted
  - _Requirements: 4.2_
