# Implementation Plan

- [x] 1. Extend PreferenceService interface and implementation for camera and zoom preferences


  - Add methods to PreferenceService interface for camera ID and zoom ratio storage
  - Implement new methods in PreferenceManager using SharedPreferences
  - Store zoom ratios per camera ID (use camera ID as key prefix)
  - Add default zoom ratio preference
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ]* 1.1 Write property test for camera selection persistence
  - **Property 2: Camera selection persistence**
  - **Validates: Requirements 1.4**

- [ ]* 1.2 Write property test for zoom persistence per camera
  - **Property 4: Zoom ratio persistence per camera**
  - **Validates: Requirements 5.4**

- [x] 2. Create CameraEnumerator service for camera discovery


  - Create CameraEnumeratorService interface with getCameraList and getCameraInfo methods
  - Implement CameraEnumerator class using CameraX ProcessCameraProvider
  - Extract camera characteristics (lens facing, zoom capabilities) from CameraInfo
  - Classify cameras by field of view type (ultra-wide, wide, standard, telephoto)
  - Generate user-friendly display names for cameras
  - Handle cases where camera enumeration fails
  - _Requirements: 1.1, 1.2, 6.1, 6.2, 6.5_

- [ ]* 2.1 Write property test for camera enumeration completeness
  - **Property 1: Camera enumeration completeness**
  - **Validates: Requirements 1.1**

- [ ]* 2.2 Write property test for camera characteristics accuracy
  - **Property 8: Camera characteristics accuracy**
  - **Validates: Requirements 6.1, 6.2**

- [x] 3. Create ZoomController service for zoom management


  - Create ZoomControllerService interface with zoom control methods
  - Implement ZoomController class with CameraControl and CameraInfo references
  - Implement setZoomRatio with bounds clamping
  - Implement getZoomRatio, getMinZoomRatio, getMaxZoomRatio methods
  - Add handlePinchGesture method to translate scale factor to zoom ratio
  - Implement resetZoom method
  - _Requirements: 2.3, 2.4, 2.5, 2.6_

- [ ]* 3.1 Write property test for zoom bounds enforcement
  - **Property 3: Zoom ratio bounds enforcement**
  - **Validates: Requirements 2.3, 2.4**

- [ ]* 3.2 Write unit tests for ZoomController
  - Test zoom ratio clamping with boundary values (0.5, 1.0, 10.0, 100.0)
  - Test pinch gesture translation to zoom ratios
  - Test zoom state management and reset functionality

- [x] 4. Extend CameraService interface and Camera implementation


  - Add switchCamera, getCurrentCameraId, getCameraInfo methods to CameraService interface
  - Add setZoomRatio, getZoomRatio, getZoomRange methods to CameraService interface
  - Implement new methods in Camera class
  - Integrate ZoomController into Camera class
  - Update startCamera to support camera ID parameter
  - Implement camera switching logic with proper resource cleanup
  - Load and apply saved camera and zoom preferences on startup
  - Save camera and zoom preferences when changed
  - _Requirements: 1.3, 1.4, 1.5, 2.6, 5.3, 5.4, 5.5_

- [ ]* 4.1 Write property test for fallback to default camera
  - **Property 6: Fallback to default camera**
  - **Validates: Requirements 1.5, 5.5**

- [ ]* 4.2 Write unit tests for Camera extensions
  - Test camera switching with valid and invalid camera IDs
  - Test zoom ratio application and retrieval
  - Test preference loading and saving

- [x] 5. Create CameraConfigActivity UI


  - Create activity_camera_config.xml layout with PreviewView, RecyclerView, and zoom indicator
  - Create CameraConfigActivity class extending AppCompatActivity
  - Initialize CameraEnumerator and get camera list
  - Set up PreviewView for live camera preview
  - Implement camera initialization with selected camera from preferences
  - Display current camera info and zoom level
  - _Requirements: 3.1, 3.2, 6.3_

- [x] 6. Implement camera list UI in CameraConfigActivity


  - Create camera_item.xml layout for RecyclerView items
  - Create CameraListAdapter extending RecyclerView.Adapter
  - Display camera display name, lens facing, FOV type, and zoom range
  - Highlight currently selected camera
  - Handle camera selection clicks
  - Update preview when camera is selected
  - _Requirements: 1.2, 3.3, 6.1, 6.2, 6.5_

- [x] 7. Implement pinch-to-zoom gesture handling

  - Create ScaleGestureDetector in CameraConfigActivity
  - Implement OnScaleGestureListener
  - Translate scale factor to zoom ratio changes
  - Apply zoom changes through ZoomController
  - Update zoom indicator during gesture
  - _Requirements: 2.1, 2.2, 2.5_

- [x] 8. Implement zoom indicator overlay

  - Create zoom indicator TextView in layout
  - Position indicator in non-intrusive location (top-right corner)
  - Update indicator text when zoom changes
  - Implement fade-out animation after 3 seconds of inactivity
  - Show indicator immediately when zoom adjustment begins
  - _Requirements: 4.1, 4.2, 4.4, 4.5_

- [ ]* 8.1 Write property test for zoom indicator visibility timing
  - **Property 7: Zoom indicator visibility timing**
  - **Validates: Requirements 4.4, 4.5**

- [ ]* 8.2 Write property test for preview update responsiveness
  - **Property 5: Preview update responsiveness**
  - **Validates: Requirements 2.5, 4.2**

- [x] 9. Implement save and exit functionality

  - Add save button to CameraConfigActivity
  - Save selected camera ID to preferences on save
  - Save current zoom ratio for selected camera to preferences
  - Add cancel button to discard changes
  - Handle back button press (prompt to save or discard)
  - Return to previous activity after save
  - _Requirements: 3.5, 5.1, 5.2_

- [x] 10. Add camera configuration option to Settings screen


  - Create CameraConfigSetting class implementing SettingItem interface
  - Add camera configuration item to Settings screen
  - Launch CameraConfigActivity when item is clicked
  - Display current camera and zoom info in setting description
  - _Requirements: 1.2, 3.1_

- [x] 11. Update MainActivity to use saved camera preferences

  - Load selected camera ID from preferences in MainActivity
  - Pass camera ID to Camera.startCamera() method
  - Load and apply saved zoom ratio for selected camera
  - Handle case where saved camera is unavailable (fallback to default)
  - _Requirements: 1.5, 5.3, 5.5_

- [x] 12. Handle edge cases and error conditions


  - Implement error handling for camera enumeration failures
  - Handle camera switch timeout with retry logic
  - Disable zoom controls when camera doesn't support zoom
  - Show appropriate error messages for camera initialization failures
  - Handle corrupted preferences by resetting to defaults
  - Prevent camera switching during active recording
  - _Requirements: 1.5, 6.4, 7.5_

- [ ]* 12.1 Write unit tests for error handling
  - Test camera enumeration with no cameras available
  - Test camera switch with unavailable camera ID
  - Test zoom operations on camera without zoom support
  - Test preference loading with corrupted data

- [x] 13. Checkpoint - Ensure all tests pass


  - Ensure all tests pass, ask the user if questions arise.

- [x] 14. Add resource strings and UI polish


  - Add all user-facing strings to strings.xml
  - Add icons for camera types (wide, ultra-wide, telephoto)
  - Apply Material Design styling to CameraConfigActivity
  - Ensure consistent theming with rest of app
  - Add accessibility content descriptions
  - _Requirements: 6.1, 6.3_

- [ ]* 14.1 Write UI tests for camera configuration flow
  - Test camera list display using Espresso
  - Test camera selection interaction
  - Test zoom indicator appearance and fade-out
  - Test navigation to and from CameraConfigActivity

- [x] 15. Final checkpoint - Ensure all tests pass



  - Ensure all tests pass, ask the user if questions arise.
