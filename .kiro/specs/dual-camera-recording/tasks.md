# Implementation Plan

- [x] 1. Extend PreferenceManager with dual camera preference support


  - Add KEY_DUAL_CAMERA_ENABLED constant and DEFAULT_DUAL_CAMERA_ENABLED (false) constant
  - Implement isDualCameraEnabled() method to retrieve preference
  - Implement setDualCameraEnabled() method to persist preference
  - _Requirements: 1.2, 1.3_

- [x] 2. Create DualCameraManager class for concurrent camera operations

  - [x] 2.1 Implement device capability detection


    - Create isDeviceCapable() static method that checks Android API level (30+)
    - Query CameraManager.getConcurrentCameraIds() to verify hardware support
    - Check for at least one concurrent camera set containing both front and back cameras
    - Add error handling and logging for capability check failures
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [x] 2.2 Implement dual camera initialization


    - Create DualCameraManager constructor accepting activity and two PreviewView references
    - Implement startDualCamera() method to bind both front and back cameras using CameraX
    - Create Preview use cases for both cameras
    - Create VideoCapture use cases with Recorder for both cameras
    - Bind both cameras to lifecycle with appropriate CameraSelectors
    - Return success/failure boolean from startDualCamera()
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  
  - [x] 2.3 Implement synchronized dual recording


    - Create startDualRecording() method accepting enableAudio parameter
    - Generate synchronized timestamp for both recordings
    - Create MediaStoreOutputOptions for both cameras with "_back" and "_front" suffixes
    - Start both recordings with prepareRecording() and start()
    - Apply audio recording based on enableAudio parameter and permission status
    - Store Recording references for both cameras
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [x] 2.4 Implement synchronized recording stop


    - Create stopDualRecording() method
    - Stop both recordings within 200ms of each other
    - Handle VideoRecordEvent.Finalize for both cameras
    - Implement error detection and logging for individual camera failures
    - Clean up Recording references
    - _Requirements: 3.5, 3.6_
  
  - [x] 2.5 Add error handling and fallback logic

    - Implement try-catch blocks around camera initialization
    - Log detailed error messages including which camera failed
    - Handle single camera failure during recording by stopping both
    - Implement isDualCameraActive() method to check current state
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 3. Update activity_main.xml layout for dual preview support


  - Keep existing single PreviewView (viewFinder) with visibility="visible"
  - Add new LinearLayout container (dualPreviewContainer) with visibility="gone"
  - Add PreviewView for back camera (viewFinderBack) with layout_weight="1"
  - Add divider View between previews (2dp height)
  - Add PreviewView for front camera (viewFinderFront) with layout_weight="1"
  - Ensure both preview modes fit within the existing CardView
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 4. Enhance Camera service to support dual camera mode

  - [x] 4.1 Add dual camera mode fields and initialization


    - Add dualCameraManager field (nullable DualCameraManager)
    - Add isDualModeEnabled boolean field
    - Create initializeCameraMode() method that checks preference and device capability
    - _Requirements: 1.3, 4.2, 4.3_
  
  - [x] 4.2 Implement camera mode setup methods


    - Create setupDualCameraMode() method that initializes DualCameraManager
    - Create setupSingleCameraMode() method that uses existing single camera logic
    - Update layout visibility based on mode (show/hide dual preview container)
    - Implement fallback to single camera if dual initialization fails
    - _Requirements: 6.1, 6.2_
  
  - [x] 4.3 Enhance captureVideo() for dual camera support


    - Check if dual mode is enabled at start of captureVideo()
    - Delegate to dualCameraManager.startDualRecording() when in dual mode
    - Pass audio recording preference to dual recording
    - Delegate to dualCameraManager.stopDualRecording() when stopping in dual mode
    - Keep existing single camera recording logic for single mode
    - Update recording button animation to work with both modes
    - _Requirements: 3.1, 3.5, 5.1, 5.2, 5.3_
  
  - [x] 4.4 Add storage check before dual recording


    - Implement storage space check before starting dual recording
    - Estimate required space (2x single recording)
    - Show error toast if insufficient storage
    - Prevent recording start when storage is low
    - _Requirements: 6.5_

- [x] 5. Create DualCameraToggleSetting class


  - Implement SettingItem interface
  - Add icon property using new dual camera drawable resource
  - Add title property "Dual Camera Recording"
  - Add isDeviceCapable property using DualCameraManager.isDeviceCapable()
  - Add isEnabled property reading from PreferenceManager
  - Implement toggle() method to update preference (only if device capable)
  - Implement onItemClick() as no-op (toggle handled by switch)
  - _Requirements: 1.1, 1.4, 1.5, 4.2, 4.3_

- [x] 6. Create dual camera icon drawable resource


  - Create ic_dual_camera.xml vector drawable
  - Design icon showing two overlapping camera symbols
  - Use consistent style with existing app icons
  - _Requirements: 1.1_

- [x] 7. Update SettingsAdapter to handle disabled toggle state


  - Modify onBindViewHolder() to detect DualCameraToggleSetting
  - Set switch enabled state based on isDeviceCapable property
  - Apply 0.5f alpha to item view when not capable
  - Add subtitle text "Requires Android 11+ and device support" when disabled
  - _Requirements: 1.4, 1.5_

- [x] 8. Update Settings activity to include dual camera toggle


  - Import DualCameraToggleSetting class
  - Create instance of DualCameraToggleSetting
  - Add to settingsList before or after AudioToggleSetting
  - _Requirements: 1.1_

- [x] 9. Update MainActivity to initialize camera mode on startup


  - Call camera.initializeCameraMode() after camera initialization
  - Ensure mode is checked each time activity resumes
  - Handle layout visibility changes when mode switches
  - _Requirements: 1.3, 2.5_

- [x] 10. Add error handling and user feedback


  - Add toast messages for dual camera initialization failures
  - Add toast messages for recording errors
  - Add toast messages for insufficient storage
  - Implement logging throughout dual camera operations
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ]* 11. Write integration tests for dual camera functionality
  - Test device capability detection with mocked CameraManager
  - Test preference storage and retrieval
  - Test camera mode switching
  - Test recording file naming with camera suffixes
  - Test error handling and fallback scenarios
  - _Requirements: All_
