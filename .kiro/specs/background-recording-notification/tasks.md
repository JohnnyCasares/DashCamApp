# Implementation Plan

- [x] 1. Add foreground service permissions and declarations


  - Add FOREGROUND_SERVICE permission to AndroidManifest.xml
  - Add FOREGROUND_SERVICE_CAMERA permission for Android 14+
  - Add POST_NOTIFICATIONS permission for Android 13+
  - Declare RecordingService in AndroidManifest with foregroundServiceType="camera"
  - _Requirements: 6.1, 6.5_

- [x] 2. Create notification infrastructure


  - Create NotificationHelper object with channel creation method
  - Implement notification channel creation for Android O+
  - Set channel importance to HIGH with no sound
  - _Requirements: 2.1, 7.4_

- [x] 3. Implement notification builder


  - Create buildRecordingNotification() method in NotificationHelper
  - Add app icon, recording status text, and elapsed time to notification
  - Make notification ongoing (non-dismissible)
  - Add "Stop" action button with PendingIntent
  - Add tap action to open MainActivity
  - _Requirements: 2.2, 2.3, 2.4, 3.1, 4.1, 7.1_

- [ ]* 3.1 Write property test for notification content
  - **Property 6: Notification displays required information**
  - **Validates: Requirements 2.2, 2.3, 7.1**

- [ ]* 3.2 Write property test for notification non-dismissible
  - **Property 7: Notification is non-dismissible during recording**
  - **Validates: Requirements 2.4**

- [ ]* 3.3 Write property test for stop button presence
  - **Property 9: Stop action button present in notification**
  - **Validates: Requirements 3.1**

- [x] 4. Create RecordingService class


  - Create RecordingService extending Service
  - Define service constants (ACTION_START_RECORDING, ACTION_STOP_RECORDING, NOTIFICATION_ID, CHANNEL_ID)
  - Implement onBind() to return null (started service, not bound)
  - _Requirements: 6.1_

- [x] 5. Implement service lifecycle methods


  - Implement onStartCommand() to handle start and stop actions
  - Implement onCreate() for service initialization
  - Implement onDestroy() for cleanup
  - _Requirements: 6.1, 6.3_

- [x] 6. Implement foreground service promotion


  - In onStartCommand(), call startForeground() with notification
  - Create notification channel before starting foreground
  - Handle notification creation failure
  - _Requirements: 6.1, 6.2_

- [ ]* 6.1 Write property test for foreground service creation
  - **Property 17: Foreground service created on recording start**
  - **Validates: Requirements 6.1**

- [ ]* 6.2 Write property test for notification binding
  - **Property 18: Notification bound to foreground service**
  - **Validates: Requirements 6.2**

- [x] 7. Implement timer for elapsed time tracking


  - Create Handler and Runnable for timer updates
  - Start timer when recording begins
  - Update notification every second with elapsed time
  - Format time as MM:SS or HH:MM:SS based on duration
  - Cancel timer in onDestroy()
  - _Requirements: 7.1, 7.2, 7.3_

- [ ]* 7.1 Write property test for time formatting
  - **Property 22: Time formatted correctly**
  - **Validates: Requirements 7.3, 7.5**

- [ ]* 7.2 Write property test for silent updates
  - **Property 23: Silent notification updates**
  - **Validates: Requirements 7.4**

- [-] 8. Create BackgroundCameraManager class using Camera2 API



  - Create BackgroundCameraManager class in service package
  - Define RecordingCallback interface for recording events
  - Add private fields for CameraDevice, CameraCaptureSession, MediaRecorder
  - Add isRecording state tracking
  - _Requirements: 1.1, 1.2_

- [x] 8.1 Implement camera opening with Camera2 API




  - Get CameraManager system service
  - Implement CameraDevice.StateCallback for camera state changes
  - Open back camera (camera ID "0") using cameraManager.openCamera()
  - Handle camera opened, disconnected, and error callbacks
  - _Requirements: 1.2_



- [ ] 8.2 Implement MediaRecorder setup


  - Create setupMediaRecorder() method
  - Configure audio source (if enabled) and video source
  - Set output format to MPEG_4
  - Configure video size and bitrate based on quality setting
  - Set video encoder to H264, audio encoder to AAC
  - Set output file path


  - Call prepare() on MediaRecorder
  - _Requirements: 1.3_

- [ ] 8.3 Implement capture session creation


  - Create CameraCaptureSession.StateCallback
  - Get MediaRecorder surface

  - Create capture session with MediaRecorder surface
  - Build capture request for recording
  - Set repeating request on session
  - _Requirements: 1.2_

- [ ] 8.4 Implement startRecording() method


  - Call openCamera() to get CameraDevice
  - Setup MediaRecorder with output file and settings

  - Create capture session
  - Start MediaRecorder
  - Set isRecording to true
  - Invoke callback.onRecordingStarted()
  - _Requirements: 1.1, 1.2_

- [ ] 8.5 Implement stopRecording() method



  - Stop MediaRecorder
  - Close capture session
  - Close camera device
  - Set isRecording to false
  - Invoke callback.onRecordingStopped() with output file
  - _Requirements: 3.2, 3.3_

- [ ] 8.6 Implement resource cleanup and release()


  - Create release() method to clean up all resources
  - Stop MediaRecorder if recording
  - Close capture session if open
  - Close camera device if open
  - Reset all state variables
  - _Requirements: 3.5_

- [x] 8.7 Add error handling to BackgroundCameraManager



  - Handle camera open errors (in use, disconnected, access denied)
  - Handle MediaRecorder errors (prepare failed, recording failed)
  - Invoke callback.onRecordingError() with appropriate error message
  - Clean up resources on error
  - _Requirements: 5.5, 1.2_

- [x] 9. Integrate BackgroundCameraManager with RecordingService




  - Add BackgroundCameraManager instance to RecordingService
  - Implement RecordingCallback in RecordingService
  - Extract audio and quality settings from intent extras
  - Create output file for recording
  - Call backgroundCamera.startRecording() when service starts
  - Handle recording callbacks (started, stopped, error)
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 10. Implement stop recording from notification



  - Handle ACTION_STOP_RECORDING in onStartCommand()
  - Call backgroundCamera.stopRecording() when stop action received
  - Wait for callback with finalized video file
  - Release camera resources via backgroundCamera.release()
  - Stop foreground service and remove notification
  - _Requirements: 3.2, 3.3, 3.4, 3.5_

- [ ]* 10.1 Write property test for stop button functionality
  - **Property 10: Stop button stops recording immediately**
  - **Validates: Requirements 3.2**

- [ ]* 10.2 Write property test for video saved on notification stop
  - **Property 11: Video saved when stopped via notification**
  - **Validates: Requirements 3.3**

- [ ]* 10.3 Write property test for camera resource release
  - **Property 12: Camera resources released on stop**
  - **Validates: Requirements 3.5**


- [x] 11. Update MainActivity to start RecordingService with Camera2



  - Modify captureVideo button click handler to start RecordingService
  - Create intent with ACTION_START_RECORDING
  - Pass recording settings (audio enabled, video quality) via intent extras
  - Start service using startForegroundService() on Android O+
  - Keep existing Camera (CameraX) for preview display only
  - _Requirements: 1.3, 6.1_

- [ ]* 11.1 Write property test for settings preservation
  - **Property 3: Recording settings preservation**
  - **Validates: Requirements 1.3**

- [x] 12. Handle notification tap to open app


  - Create PendingIntent in NotificationHelper for MainActivity
  - Use FLAG_ACTIVITY_SINGLE_TOP to reuse existing activity
  - Ensure MainActivity shows recording screen when opened
  - _Requirements: 4.1, 4.2_

- [ ]* 12.1 Write property test for notification tap
  - **Property 13: Notification tap brings app to foreground**
  - **Validates: Requirements 4.1, 4.2**

- [x] 13. Implement UI state synchronization


  - Update MainActivity onResume() to check if recording is active
  - Update recording button state based on service state
  - Display elapsed time in UI if recording is active
  - _Requirements: 4.4_

- [ ]* 13.1 Write property test for UI state sync
  - **Property 15: UI reflects recording state on foreground return**
  - **Validates: Requirements 4.4**

- [x] 14. Handle back button during recording


  - Override onBackPressed() in MainActivity
  - If recording is active, move app to background (don't stop recording)
  - If not recording, follow standard back navigation
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [ ]* 14.1 Write property test for back button behavior
  - **Property 24: Back button backgrounds app during recording**
  - **Validates: Requirements 8.1, 8.3**

- [ ]* 14.2 Write property test for Camera2 API usage
  - **Property 25: Camera2 API used for background recording**
  - **Validates: Requirements 9.1**

- [ ]* 14.3 Write property test for camera lifecycle independence
  - **Property 26: Camera lifecycle independence**
  - **Validates: Requirements 9.2**

- [ ]* 14.4 Write property test for MediaRecorder integration
  - **Property 27: MediaRecorder integration**
  - **Validates: Requirements 9.3**

- [ ]* 14.5 Write property test for service camera access
  - **Property 28: Camera access maintained in service**
  - **Validates: Requirements 9.4**

- [x] 15. Implement graceful service termination


  - Override onTaskRemoved() in RecordingService
  - Save current recording when service is terminated
  - Clean up resources properly
  - _Requirements: 5.3_

- [ ]* 15.1 Write property test for graceful shutdown
  - **Property 16: Graceful shutdown saves video**
  - **Validates: Requirements 5.3**

- [x] 16. Add error handling for camera access loss


  - Implement error callback in Camera recording listener
  - Handle camera access loss during background recording
  - Stop recording gracefully and save video
  - Display error notification to user
  - _Requirements: 1.2, 5.5_

- [x] 17. Add low storage monitoring


  - Check available storage before starting recording
  - Monitor storage during recording
  - Stop recording if storage falls below threshold (100MB)
  - Display notification about storage issue
  - _Requirements: 5.2_

- [x] 18. Handle notification permission for Android 13+


  - Add permission check for POST_NOTIFICATIONS on Android 13+
  - Request permission if not granted
  - Handle permission denial gracefully
  - _Requirements: 6.5_

- [x] 19. Test Camera2 background recording functionality





  - Manually test: start recording using BackgroundCameraManager
  - Verify video file is created and contains valid video data
  - Verify audio is recorded when enabled
  - Verify video quality matches selected setting
  - Test camera resource cleanup after recording stops
  - _Requirements: 1.2, 1.3_

- [x] 19.1 Test recording persistence across app states



  - Manually test: start recording, minimize app, verify recording continues
  - Manually test: start recording, switch apps, verify recording continues
  - Manually test: start recording, turn off screen, verify recording continues
  - Verify Camera2 maintains camera access in all scenarios
  - _Requirements: 1.1, 1.4, 1.5_

- [ ]* 19.1 Write property test for recording persistence
  - **Property 1: Recording persistence across app state transitions**
  - **Validates: Requirements 1.1, 1.5, 8.2**

- [ ]* 19.2 Write property test for screen off recording
  - **Property 4: Recording continues when screen off**
  - **Validates: Requirements 1.4**

- [x] 20. Checkpoint - Ensure all Camera2 implementation tests pass




  - Ensure all tests pass, ask the user if questions arise.
  - Verify BackgroundCameraManager works correctly
  - Verify RecordingService integrates properly with Camera2

- [x] 21. Test notification functionality with Camera2 recording



  - Manually test: verify notification appears when recording starts
  - Manually test: tap stop button in notification, verify recording stops
  - Manually test: tap notification body, verify app opens
  - Manually test: verify elapsed time updates in notification
  - Verify notification works with Camera2-based recording
  - _Requirements: 2.1, 3.1, 3.2, 4.1, 7.2_

- [ ]* 21.1 Write property test for notification creation
  - **Property 5: Notification creation on recording start**
  - **Validates: Requirements 2.1**

- [ ]* 21.2 Write property test for notification removal
  - **Property 8: Notification removed when recording stops**
  - **Validates: Requirements 2.5, 3.4**

- [ ]* 21.3 Write property test for recording persistence on foreground return
  - **Property 14: Recording persists when returning to foreground**
  - **Validates: Requirements 4.3**

- [x] 22. Test service lifecycle with Camera2



  - Manually test: verify service starts when recording begins
  - Manually test: verify service stops when recording ends
  - Manually test: verify service survives app backgrounding with Camera2
  - Manually test: swipe away app during recording, verify video is saved
  - Verify Camera2 resources are properly cleaned up
  - _Requirements: 6.1, 6.3, 5.3_

- [ ]* 22.1 Write property test for service stop
  - **Property 19: Foreground service stopped when recording stops**
  - **Validates: Requirements 6.3**

- [ ]* 22.2 Write property test for service priority
  - **Property 20: Foreground service maintains high priority**
  - **Validates: Requirements 6.4**

- [x] 23. Final checkpoint - Ensure all Camera2 tests pass




  - Ensure all tests pass, ask the user if questions arise.
  - Verify complete Camera2 implementation works end-to-end
  - Verify background recording works reliably across all scenarios
  - Verify no regressions in existing CameraX foreground functionality
