# Implementation Plan

- [x] 1. Add MediaStyle dependency and update notification icons


  - Add `androidx.media:media:1.6.0` dependency to `app/build.gradle.kts`
  - Create new icon resources for notification actions (if not already present)
  - Verify existing icons (ic_stop_recording) are suitable for notification use
  - _Requirements: 2.1, 3.1, 3.2, 3.5_

- [x] 2. Enhance NotificationHelper with MediaStyle notification

  - [x] 2.1 Update buildRecordingNotification to use MediaStyle


    - Replace NotificationCompat.Builder with MediaStyle configuration
    - Add setStyle() with MediaStyle
    - Configure setShowActionsInCompactView() to show primary actions
    - Add large icon for better visual presence
    - Ensure ongoing flag remains true for non-dismissibility
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4_
  
  - [ ]* 2.2 Write property test for notification configuration
    - **Property 1: Notification non-dismissibility**
    - **Property 2: MediaStyle configuration completeness**
    - **Validates: Requirements 1.1, 1.2, 2.1, 2.2, 2.3**
  
  - [x] 2.3 Update notification action button with correct icon

    - Update Stop action to use ic_stop_recording drawable (matching MainActivity)
    - Ensure Stop action has proper PendingIntent configuration
    - Remove unused Open App action
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [ ]* 2.4 Write property test for action presence and icon
    - **Property 3: Stop action presence with correct icon**
    - **Validates: Requirements 3.1, 3.4**
  
  - [x] 2.5 Enhance notification content with better status display

    - Update content title and text formatting
    - Add support for low storage warning indicator
    - Ensure elapsed time display is clear and prominent
    - _Requirements: 4.1, 4.2, 4.3_
  
  - [ ]* 2.6 Write property test for state reflection
    - **Property 5: State reflection in notification**
    - **Validates: Requirements 4.1, 4.2, 4.3**

- [x] 3. Update RecordingService action handling

  - [x] 3.1 Remove unused ACTION_OPEN_APP code


    - Remove ACTION_OPEN_APP constant
    - Remove ACTION_OPEN_APP case from onStartCommand()
    - Remove openMainActivity() method
    - Ensure null intent handling remains robust
    - _Requirements: 5.2_
  
  - [ ]* 3.2 Write property test for action intent routing
    - **Property 4: Action intent routing**
    - **Validates: Requirements 3.2, 3.3, 7.3**
  
  - [x] 3.3 Update notification building calls to use enhanced MediaStyle notification


    - Update startForegroundService() to use new notification
    - Update updateNotification() to use new notification
    - Verify notification updates remain silent (setOnlyAlertOnce)
    - _Requirements: 4.5_
  
  - [ ]* 3.4 Write property test for silent updates
    - **Property 8: Silent notification updates**
    - **Validates: Requirements 4.5**

- [x] 4. Verify notification channel configuration

  - [x] 4.1 Review and confirm notification channel settings


    - Verify IMPORTANCE_HIGH is set
    - Verify sound is disabled for silent updates
    - Verify vibration is disabled
    - Ensure channel description is clear
    - _Requirements: 1.3, 4.5_
  
  - [ ]* 4.2 Write property test for channel priority
    - **Property 9: Notification channel priority**
    - **Validates: Requirements 1.3**

- [x] 5. Test existing functionality preservation

  - [ ]* 5.1 Write property test for functionality preservation
    - **Property 6: Existing functionality preservation**
    - **Validates: Requirements 5.1, 5.2, 6.1, 6.2, 6.3**
  
  - [ ]* 5.2 Write property test for notification removal
    - **Property 7: Notification removal on stop**
    - **Validates: Requirements 1.4**

- [x] 6. Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Manual testing and UI verification



  - Test notification appearance on device
  - Verify MediaStyle layout in collapsed and expanded states
  - Test Stop action from notification (verify it uses correct icon and stops recording)
  - Verify notification cannot be dismissed while recording
  - Test notification updates during recording
  - Verify low storage warning display (if applicable)
  - Test notification across app backgrounding and foregrounding
  - _Requirements: All_
